package com.dbl.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.domain.ChangeType;
import com.dbl.domain.message.FileMessage;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderGetLatestCursorResult;
import com.dropbox.core.v2.files.ListFolderLongpollResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

@EnableConfigurationProperties(DropBoxLibProperties.class)
public class LongPoolServiceImpl implements LongPoolService {
	private final Logger logger = LoggerFactory.getLogger(LongPoolServiceImpl.class);

	private final DropBoxUtils dropBoxUtils;
	private final DropBoxLibProperties appProperties;

	private long longpollTimeoutSecs = TimeUnit.MINUTES.toSeconds(2);

	private List<FileEventListener> eventListeners;
	private LocalDateTime lastChangeTime;

	private Thread keepConnectionThread;

	public LongPoolServiceImpl(DropBoxLibProperties appProperties, DropBoxUtils dropBoxUtils) {
		this.dropBoxUtils = dropBoxUtils;
		this.appProperties = appProperties;
		eventListeners = new ArrayList<>();
		lastChangeTime = LocalDateTime.now();
	}

	@Override
	public Boolean isHealth() {
		return lastChangeTime.isAfter(LocalDateTime.now().minusMinutes(5));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.LongPoolService#getLastChangeTime()
	 */
	@Override
	public LocalDateTime getLastChangeTime() {
		return lastChangeTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dbl.service.LongPoolService#register(com.dbl.service.FileEventListener)
	 */
	@Override
	public void register(FileEventListener fileEventListener) {
		if (eventListeners.isEmpty() || !eventListeners.contains(fileEventListener)) {
			eventListeners.add(fileEventListener);
		} else {
			logger.error("Check why we are registering again the same listener");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.LongPoolService#getEventListeners()
	 */
	@Override
	public List<FileEventListener> getEventListeners() {
		return eventListeners;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.LongPoolService#updateListeners(com.dbl.domain.message.
	 * FileMessage)
	 */
	@Override
	public int updateListeners(FileMessage fileMessage) {
		int res = 0;

		List<FileEventListener> eventListeners2 = getEventListeners();
		for (FileEventListener fileEventListener : eventListeners2) {
			if (isInterestingFileFormat(fileMessage.getMessageDetails().getPathLower(),fileEventListener)){
				fileEventListener.fileChanged(fileMessage);
				res++;
			}
		}

		return res;
	}

	private Boolean firstConnect = true;
	@Override
	public void keepConnection() {
		keepConnectionThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (appProperties.isLongPull()) {
					if (!isHealth()||firstConnect) {
						logger.error("reconnecting!!");
						connect();
						firstConnect = false;
					}

					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						logger.error(e.getMessage(), e);
					}
				}

			}
		});

		keepConnectionThread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.LongPoolService#connect()
	 */
	@Override
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
				// TODO: check what happends when the connection is lost - we might need to stop
				// the while and restart
				if (result.getChanges()) {
					cursor = getChanges(dbxClient, cursor);
				}

				Long wait = result.getBackoff();
				if (wait != null) {
					try {
						logger.debug("backing off for %d secs...\n", wait.longValue());
						Thread.sleep(TimeUnit.SECONDS.toMillis(wait));
					} catch (InterruptedException ex) {
						logger.error("", ex);
					}
				}
				lastChangeTime = LocalDateTime.now();
			}
		} catch (Exception ex) {
			logger.error("Error ", ex);
			lastChangeTime = LocalDateTime.now().minusYears(1);
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
	 * @throws IOException
	 */
	private String getChanges(DbxClientV2 client, String cursor) throws DbxApiException, DbxException, IOException {

		// TODO: remove true
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

				FileMessage fileMessage = getFileMessage(type, metadata);
				updateFileMessage(client, fileMessage);

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

	private void updateFileMessage(DbxClientV2 client, FileMessage fileMessage) throws DbxException, IOException {
		if (fileMessage.getMessageType() == ChangeType.FILE) {
			byte[] download = dropBoxUtils.download(fileMessage.getMessageDetails().getPathLower(), client);
			fileMessage.setFile(download);
		} else {
			logger.debug("did not download file");
		}
	}

	private boolean isInterestingFileFormat(String pathLower, FileEventListener fileEventListener) {
		List<String> fileEventListenerInterestingFileFormat = fileEventListener.getInterestingFileFormat()
		List<String> interestingFileFormat = fileEventListenerInterestingFileFormat==null?appProperties.getInterestingFileFormat():fileEventListenerInterestingFileFormat;
		
		if (interestingFileFormat.size() == 0) {
			return true;
		} else {
			for (String format : interestingFileFormat) {
				if (pathLower.endsWith(format)) {
					return true;
				}
			}
			return false;
		}
	}

	private FileMessage getFileMessage(ChangeType type, Metadata details) {
		FileMessage fileMessage = new FileMessage();
		fileMessage.setMessageType(type);
		fileMessage.setMessageDetails(details);
		return fileMessage;
	}

}
