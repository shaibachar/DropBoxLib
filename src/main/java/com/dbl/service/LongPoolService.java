package com.dbl.service;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.domain.ChangeType;
import com.dbl.domain.message.FileMessage;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderGetLatestCursorResult;
import com.dropbox.core.v2.files.ListFolderLongpollResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

@Service
@EnableConfigurationProperties(DropBoxLibProperties.class)
public class LongPoolService {
	private final Logger logger = LoggerFactory.getLogger(LongPoolService.class);

	private final DropBoxUtils dropBoxUtils;
	private final DropBoxLibProperties appProperties;

	private long longpollTimeoutSecs = TimeUnit.MINUTES.toSeconds(2);

	private List<FileEventListener> eventListeners;

	public LongPoolService(DropBoxUtils dropBoxUtils, DropBoxLibProperties appProperties) {
		this.dropBoxUtils = dropBoxUtils;
		this.appProperties = appProperties;
		eventListeners = new ArrayList<>();
	}

	public void register(FileEventListener fileEventListener) {
		if (eventListeners.isEmpty() || !eventListeners.contains(fileEventListener)) {
			eventListeners.add(fileEventListener);
		} else {
			logger.error("Check why we are registering again the same listener");
		}
	}

	public List<FileEventListener> getEventListeners() {
		return eventListeners;
	}

	/**
	 * This method will call observers
	 * 
	 * @param fileMessage
	 * @return number of observers been called
	 */
	public int updateListeners(FileMessage fileMessage) {
		int res = 0;

		List<FileEventListener> eventListeners2 = getEventListeners();
		for (FileEventListener fileEventListener : eventListeners2) {
			fileEventListener.fileChanged(fileMessage);
			res++;
		}

		return res;
	}

	/**
	 * This method will login to DropBox
	 */
	public void connect() {
		// need 2 Dropbox clients for making calls:
		//
		// (1) One for longpoll requests, with its read timeout set longer than our
		// polling timeout
		// (2) One for all other requests, with its read timeout set to the default,
		// shorter timeout
		//
		Config config = dropBoxUtils.getDefaultConfig(appProperties);
		Config longpollConfig = dropBoxUtils.getLongPoolConfig(appProperties);
		DbxAuthInfo auth = dropBoxUtils.getAuth(appProperties);
		DbxClientV2 dbxClient = dropBoxUtils.createClient(auth, config, appProperties.getDropboxConfig());
		DbxClientV2 dbxLongpollClient = dropBoxUtils.createClient(auth, longpollConfig, appProperties.getDropboxConfig());

		try {
			// We only care about file changes, not existing files, so grab latest cursor
			// for this path and then longpoll for changes.
			String cursor = getLatestCursor(dbxClient, appProperties.getDropBoxRootPath());

			while (appProperties.isLongPull()) {
				// will block for longpollTimeoutSecs or until a change is made in the folder
				ListFolderLongpollResult result = dbxLongpollClient.files().listFolderLongpoll(cursor, longpollTimeoutSecs);

				// we have changes, list them
				if (result.getChanges()) {
					cursor = getChanges(dbxClient, cursor);
				}

				// we were asked to back off from our polling, wait the requested amount of
				// seconds
				// before issuing another longpoll request.
				Long backoff = result.getBackoff();
				if (backoff != null) {
					try {
						System.out.printf("backing off for %d secs...\n", backoff.longValue());
						Thread.sleep(TimeUnit.SECONDS.toMillis(backoff));
					} catch (InterruptedException ex) {
						logger.error("", ex);
					}
				}
			}
		} catch (DbxApiException ex) {
			// if a user message is available, try using that instead
			String message = ex.getUserMessage() != null ? ex.getUserMessage().getText() : ex.getMessage();
			logger.error("Error making API call: " + message);
		} catch (NetworkIOException ex) {
			logger.error("Error making API call: " + ex.getMessage());
			if (ex.getCause() instanceof SocketTimeoutException) {
				logger.error("Consider increasing socket read timeout or decreasing longpoll timeout.");
			}
		} catch (DbxException ex) {
			logger.error("Error making API call: " + ex.getMessage());
		}
	}

	/**
	 * Returns latest cursor for listing changes to a directory in Dropbox with the
	 * given path.
	 *
	 * @param dbxClient
	 *            Dropbox client to use for fetching the latest cursor
	 * @param path
	 *            path to directory in Dropbox
	 *
	 * @return cursor for listing changes to the given Dropbox directory
	 */
	private String getLatestCursor(DbxClientV2 dbxClient, String path) throws DbxApiException, DbxException {
		ListFolderGetLatestCursorResult result = dbxClient.files().listFolderGetLatestCursorBuilder(path).withIncludeDeleted(true).withIncludeMediaInfo(false).withRecursive(true).start();
		return result.getCursor();
	}

	/**
	 * Prints changes made to a folder in Dropbox since the given cursor was
	 * retrieved.
	 *
	 * @param dbxClient
	 *            Dropbox client to use for fetching folder changes
	 * @param cursor
	 *            lastest cursor received since last set of changes
	 *
	 * @return latest cursor after changes
	 */
	private String getChanges(DbxClientV2 client, String cursor) throws DbxApiException, DbxException {

		while (true) {
			ListFolderResult result = client.files().listFolderContinue(cursor);
			for (Metadata metadata : result.getEntries()) {
				ChangeType type;
				String details;
				if (metadata instanceof FileMetadata) {
					FileMetadata fileMetadata = (FileMetadata) metadata;
					type = ChangeType.FILE;
					details = "(rev=" + fileMetadata.getRev() + ")";
				} else if (metadata instanceof FolderMetadata) {
					FolderMetadata folderMetadata = (FolderMetadata) metadata;
					type = ChangeType.FOLDER;
					details = folderMetadata.getSharingInfo() != null ? "(shared)" : "";
				} else if (metadata instanceof DeletedMetadata) {
					type = ChangeType.DELETE;
					details = "";
				} else {
					throw new IllegalStateException("Unrecognized metadata type: " + metadata.getClass());
				}

				FileMessage fileMessage = getFileMessage(type, metadata.getPathLower());
				// channel.send(MessageBuilder.withPayload(fileMessage).build());
				int updateListeners = updateListeners(fileMessage);
				logger.debug(updateListeners + " where updated");
				logger.debug("type:" + type + " details:" + details + " meta:" + metadata.getPathLower());
			}
			// update cursor to fetch remaining results
			cursor = result.getCursor();

			if (!result.getHasMore()) {
				break;
			}
		}

		return cursor;
	}

	private FileMessage getFileMessage(ChangeType type, String details) {
		FileMessage fileMessage = new FileMessage();
		fileMessage.setMessageType(type.toString());
		fileMessage.setMessageDetails(details);
		return fileMessage;
	}

}
