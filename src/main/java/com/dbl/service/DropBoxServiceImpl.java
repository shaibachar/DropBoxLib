package com.dbl.service;

import com.dbl.config.DropBoxLibProperties;
import com.dbl.exception.DropBoxLibException;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shai
 */
@EnableConfigurationProperties(DropBoxLibProperties.class)
public class DropBoxServiceImpl implements DropBoxService {
    private final Logger logger = LoggerFactory.getLogger(DropBoxServiceImpl.class);

    private DbxClientV2 client;
    private final DropBoxLibProperties appProperties;
    private final DropBoxUtils dropBoxUtils;

    private ListFolderResult result;

    public DropBoxServiceImpl(DropBoxLibProperties boxProperties, DropBoxUtils dropBoxUtils) {
        this.appProperties = boxProperties;
        this.dropBoxUtils = dropBoxUtils;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#connect()
     */
    @Override
    public void connect() throws DropBoxLibException {
        try {
            Config config = dropBoxUtils.getDefaultConfig(appProperties);
            DbxAuthInfo auth = dropBoxUtils.getAuth(appProperties);
            client = dropBoxUtils.createClient(auth, config, appProperties.getDropboxConfig());
        } catch (Exception e) {
            String message = "error connecting to dropbox";
            logger.error(message, e);
            throw new DropBoxLibException(message, e);
        }
    }

    @Override
    public ListRevisionsResult getRevisions(String path) throws DropBoxLibException {
        ListRevisionsResult listRevisions;
        try {
            DbxUserFilesRequests files = client.files();
            listRevisions = files.listRevisions(path);
        } catch (Exception e) {
            String message = MessageFormat.format("error while getting revisions for path {0} from dropbox", path);
            throw new DropBoxLibException(message, e);
        }
        return listRevisions;
    }

    @Override
    public Map<String, byte[]> downloadAllZip(String folderPath) throws DropBoxLibException {
        Map<String, byte[]> res = new HashMap<>();
        if (folderPath == null || folderPath.isEmpty()) {
            return res;
        }

        Map<String, byte[]> stringMap;
        try {
            stringMap = dropBoxUtils.downloadZip(folderPath, client);
        } catch (Exception e) {
            String message = MessageFormat.format("error while download zip file {0} from dropbox", folderPath);
            throw new DropBoxLibException(message, e);
        }
        return stringMap;
    }

    @Override
    public Map<String, byte[]> downloadAll(String folderPath) {
        Map<String, byte[]> res = new HashMap<>();
        if (folderPath == null || folderPath.isEmpty()) {
            return res;
        }
        List<FileMetadata> allFiles = allFiles(folderPath, true);
        for (FileMetadata fileMetadata : allFiles) {
            res.put(fileMetadata.getPathLower(), download(fileMetadata.getPathLower()));
        }
        return res;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#download(java.lang.String)
     */
    @Override
    public byte[] download(String filePath) throws DropBoxLibException {
        byte[] download;
        try {
            download = dropBoxUtils.download(filePath, client);
        } catch (Exception e) {
            String message = MessageFormat.format("error while download file {0} from dropbox", filePath);
            throw new DropBoxLibException(message, e);
        }
        return download;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#upload(java.io.InputStream,
     * java.lang.String)
     */
    @Override
    public FileMetadata upload(InputStream inputFile, String fullPath, boolean override) throws DropBoxLibException {
        FileMetadata upload;
        try {
            upload = dropBoxUtils.upload(inputFile, fullPath, client, override);
        } catch (Exception e) {
            String message = MessageFormat.format("error while upload file {0} to dropbox", fullPath);
            throw new DropBoxLibException(message, e);
        }
        return upload;
    }

    @Override
    public FileMetadata upload(InputStream inputFile, String fullPath) throws DbxException, IOException {
        return upload(inputFile, fullPath, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#allFiles(java.lang.String, boolean)
     */
    @Override
    public List<FileMetadata> allFiles(String path, boolean recursive) throws DropBoxLibException {
        List<FileMetadata> res = new ArrayList<>();
        try {
            String syncFiles = syncFiles(null, res, path, recursive);
            logger.debug(syncFiles);

        } catch (Exception e) {
            String message = "Error on retrive all files";
            logger.error(message, e);
            throw new DropBoxLibException(message, e);
        }
        return res;
    }

    @Override
    public List<FileMetadata> allFiles(String folderPath, boolean recursive, List<String> fileTypes) {
        List<FileMetadata> res = new ArrayList<>();
        List<FileMetadata> allFiles = allFiles(folderPath, recursive);

        for (FileMetadata fileMetadata : allFiles) {
            String pathLower = fileMetadata.getPathLower();
            for (String types : fileTypes) {
                if (pathLower != null && pathLower.contains(types)) {
                    res.add(fileMetadata);
                    break;
                }
            }
        }

        return res;
    }

    @Override
    public List<FolderMetadata> allFolders(String path, boolean recursive) throws DropBoxLibException {
        List<FolderMetadata> res = new ArrayList<>();
        try {
            String syncFiles = syncFiles(res, null, path, recursive);
            logger.debug(syncFiles);

        } catch (Exception e) {
            String message = "Error on retrive all folders";
            logger.error(message, e);
            throw new DropBoxLibException(message, e);
        }
        return res;
    }

    @Override
    public String syncFiles(List<FolderMetadata> folders, List<FileMetadata> files, String path, boolean recursive) throws DropBoxLibException {

        String resolvedPath = (path == null) ? "" : path;

        ListFolderResult result;
        try {
            DbxUserListFolderBuilder listFolderBuilder = client.files().listFolderBuilder(resolvedPath);
            result = listFolderBuilder.withRecursive(recursive).start();
        } catch (Exception e) {
            throw new DropBoxLibException("error to get folder list", e);
        }
        do {
            for (Metadata entry : result.getEntries()) {
                if (entry instanceof FileMetadata) {
                    if (files != null) {
                        logger.debug("Added file: {}", entry.getPathLower());
                        files.add((FileMetadata) entry);
                    }
                } else if (entry instanceof FolderMetadata) {
                    if (folders != null) {
                        logger.debug("Added folder: {}", entry.getPathLower());
                        folders.add((FolderMetadata) entry);
                    }
                }
            }

            if (result.getHasMore()) {
                try {
                    result = client.files().listFolderContinue(result.getCursor());
                } catch (DbxException e) {
                    String msg = MessageFormat.format("Failed to continue folder listing at path: {0} | Cursor: {1}",
                            resolvedPath, result.getCursor());
                    logger.error(msg, e);
                    throw new DropBoxLibException(msg, e);
                }
            }
        } while (result.getHasMore());

        logger.info("Completed sync for path: {} | Cursor: {}", resolvedPath, result.getCursor());
        return result.getCursor();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#rename(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void rename(String fromPath, String toPath) throws DropBoxLibException {
        try {
            String message = "Going to rename from:%s to path:%s".formatted(fromPath, toPath);
            logger.debug(message);
            Metadata metadata = client.files().moveV2(fromPath, toPath).getMetadata();
            logger.debug(metadata.toStringMultiline());
        } catch (Exception e) {
            String message = MessageFormat.format("error while rename path {0} to path {1}", fromPath, toPath);
            throw new DropBoxLibException(message, e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#getResult()
     */
    @Override
    public ListFolderResult getResult() {
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxService#setResult(com.dropbox.core.v2.files.
     * ListFolderResult)
     */
    @Override
    public void setResult(ListFolderResult result) {
        this.result = result;
    }

    @Override
    public SearchResult search(String path, String query) throws DropBoxLibException {
        SearchResult search;
        try {
            search = client.files().search(path, query);
        } catch (Exception e) {
            String message = MessageFormat.format("error while search path {0} with query {1}", path, query);
            throw new DropBoxLibException(message, e);
        }
        return search;
    }

    @Override
    public Boolean checkPath(String path) throws DropBoxLibException {
        Metadata metadata;
        try {
            metadata = client.files().getMetadata(path);
        } catch (Exception e) {
            String message = MessageFormat.format("error while check path {0} on dropbox", path);
            throw new DropBoxLibException(message, e);
        }
        return metadata != null;

    }

    @Override
    public void delete(@NotNull String oldFileName) throws DropBoxLibException {
        try {
            client.files().deleteV2(oldFileName);
        } catch (Exception e) {
            String message = MessageFormat.format("error while delete file path {0} on dropbox", oldFileName);
            throw new DropBoxLibException(message, e);
        }
    }


}
