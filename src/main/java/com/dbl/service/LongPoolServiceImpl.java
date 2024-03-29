package com.dbl.service;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.domain.ChangeType;
import com.dbl.domain.message.ChangeMessage;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties(DropBoxLibProperties.class)
public class LongPoolServiceImpl implements LongPoolService {
    private final Logger logger = LoggerFactory.getLogger(LongPoolServiceImpl.class);

    private final DropBoxUtils dropBoxUtils;
    private final DropBoxLibProperties appProperties;

    private long longpollTimeoutSecs = TimeUnit.MINUTES.toSeconds(2);

    private List<ChangeEventListener> eventListeners;

    private boolean health;

    public LongPoolServiceImpl(DropBoxLibProperties appProperties, DropBoxUtils dropBoxUtils) {
        this.dropBoxUtils = dropBoxUtils;
        this.appProperties = appProperties;
        eventListeners = new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.dbl.service.LongPoolService#register(com.dbl.service.FileEventListener)
     */
    @Override
    public void register(ChangeEventListener changeEventListener) {
        if (eventListeners.isEmpty() || !eventListeners.contains(changeEventListener)) {
            eventListeners.add(changeEventListener);
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
    public List<ChangeEventListener> getEventListeners() {
        return eventListeners;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.LongPoolService#updateListeners(com.dbl.domain.message.
     * FileMessage)
     */
    @Override
    public int updateListeners(ChangeMessage changeMessage) {
        int res = 0;

        List<ChangeEventListener> eventListeners2 = getEventListeners();
        for (ChangeEventListener changeEventListener : eventListeners2) {
//			if (isInterestingFileFormat(changeMessage.getMessageDetails().getPathLower(), changeEventListener)) {
            changeEventListener.change(changeMessage);
            res++;
//			}
        }

        return res;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.LongPoolService#connect()
     */
    @Override
    public void connect() throws DbxApiException, DbxException {
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

        // We only care about file changes, not existing files, so grab latest cursor
        // for this path and then longpoll for changes.
        String cursor = getLatestCursor(dbxClient, appProperties.getDropBoxRootPath());

        while (appProperties.isLongPull()) {
            try {
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
                health = true;
            } catch (Exception ex) {
                logger.error("Error on longPool service connection loop", ex);
                config = dropBoxUtils.getDefaultConfig(appProperties);
                longpollConfig = dropBoxUtils.getLongPoolConfig(appProperties);
                auth = dropBoxUtils.getAuth(appProperties);
                dbxClient = dropBoxUtils.createClient(auth, config, appProperties.getDropboxConfig());
                dbxLongpollClient = dropBoxUtils.createClient(auth, longpollConfig, appProperties.getDropboxConfig());
                try {
                    cursor = getLatestCursor(dbxClient, appProperties.getDropBoxRootPath());
                } catch (Exception e) {
                    logger.error("Error on getting last cursor", e);
                }
                health = false;
            }
        }
    }

    /**
     * Returns latest cursor for listing changes to a directory in Dropbox with the
     * given path.
     *
     * @param dbxClient Dropbox client to use for fetching the latest cursor
     * @param path      path to directory in Dropbox
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
     * @param client Dropbox client to use for fetching folder changes
     * @param cursor lastest cursor received since last set of changes
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
                if (metadata instanceof FileMetadata fileMetadata) {
                    type = ChangeType.FILE;
                    details = "(rev=" + fileMetadata.getRev() + ")";
                } else if (metadata instanceof FolderMetadata folderMetadata) {
                    type = ChangeType.FOLDER;
                    details = folderMetadata.getSharingInfo() != null ? "(shared)" : "";
                } else if (metadata instanceof DeletedMetadata) {
                    type = ChangeType.DELETE;
                    details = "";
                } else {
                    throw new IllegalStateException("Unrecognized metadata type: " + metadata.getClass());
                }

                ChangeMessage changeMessage = getFileMessage(type, metadata);
//				updateFileMessage(client, changeMessage);

                // channel.send(MessageBuilder.withPayload(changeMessage).build());
                int updateListeners = updateListeners(changeMessage);
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

    private void updateFileMessage(DbxClientV2 client, ChangeMessage fileMessage) throws DbxException, IOException {
        if (fileMessage.getMessageType() == ChangeType.FILE) {
            byte[] download = dropBoxUtils.download(fileMessage.getMessageDetails().getPathLower(), client);
            fileMessage.setFile(download);
        } else {
            logger.debug("did not download file");
        }
    }

    private boolean isInterestingFileFormat(String pathLower, ChangeEventListener changeEventListener) {
        List<String> fileEventListenerInterestingFileFormat = changeEventListener.getInterestingFileFormat();
        List<String> interestingFileFormat = fileEventListenerInterestingFileFormat == null ? appProperties.getInterestingFileFormat() : fileEventListenerInterestingFileFormat;

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

    private ChangeMessage getFileMessage(ChangeType type, Metadata details) {
        ChangeMessage changeMessage = new ChangeMessage();
        changeMessage.setMessageType(type);
        changeMessage.setMessageDetails(details);
        return changeMessage;
    }

    @Override
    public boolean isHealth() {
        return health;
    }

}
