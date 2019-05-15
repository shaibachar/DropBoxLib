package com.dbl.service;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.dropbox.core.v2.files.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dbl.config.DropBoxLibProperties;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;

public class DropBoxUtilsImpl implements DropBoxUtils {

    private final Logger logger = LoggerFactory.getLogger(DropBoxUtilsImpl.class);

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxUtils#createClient(com.dropbox.core.DbxAuthInfo,
     * com.dropbox.core.http.StandardHttpRequestor.Config, java.lang.String)
     */
    @Override
    public DbxClientV2 createClient(DbxAuthInfo auth, StandardHttpRequestor.Config config, String clientUserAgentId) {
        StandardHttpRequestor requestor = new StandardHttpRequestor(config);
        DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(clientUserAgentId).withHttpRequestor(requestor).build();

        return new DbxClientV2(requestConfig, auth.getAccessToken(), auth.getHost());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.dbl.service.DropBoxUtils#getAuth(com.dbl.config.DropBoxLibProperties)
     */
    @Override
    public DbxAuthInfo getAuth(DropBoxLibProperties appProperties) {

        DbxHost host = DbxHost.DEFAULT;
        String accessToken = appProperties.getAccessToken();
        DbxAuthInfo authInfo = new DbxAuthInfo(accessToken, host);
        return authInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxUtils#getDefaultConfig(com.dbl.config.
     * DropBoxLibProperties)
     */
    @Override
    public Config getDefaultConfig(DropBoxLibProperties appProperties) {
        // TODO: create another getConfig method with config construction
        StandardHttpRequestor.Config config = Config.DEFAULT_INSTANCE;
        return config;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxUtils#getLongPoolConfig(com.dbl.config.
     * DropBoxLibProperties)
     */
    @Override
    public Config getLongPoolConfig(DropBoxLibProperties appProperties) {
        StandardHttpRequestor.Config longpollConfig = getDefaultConfig(appProperties).copy()
                // read timeout should be greater than our longpoll timeout and include enough
                // buffer
                // for the jitter introduced by the server. The server will add a random amount
                // of delay
                // to our longpoll timeout to avoid the stampeding herd problem. See
                // DbxFiles.listFolderLongpoll(String, long) documentation for details.
                .withReadTimeout(5, TimeUnit.MINUTES).build();

        return longpollConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxUtils#download(java.lang.String,
     * com.dropbox.core.v2.DbxClientV2)
     */
    @Override
    public byte[] download(String filePath, DbxClientV2 client) throws DbxException, IOException {
        logger.debug("Going to download file" + filePath);
        byte[] out = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            DbxDownloader<FileMetadata> download = client.files().download(filePath);
            FileMetadata download2 = download.download(outputStream);
            logger.info("Metadata: " + download2.toString());
            out = outputStream.toByteArray();

        } finally {
            outputStream.close();
        }

        return out;
    }


    @Override
    public Map<String, byte[]> downloadZip(String filePath, DbxClientV2 client) throws DbxException, IOException {
        logger.debug("Going to download folder" + filePath);
        Map<String, byte[]> out = new HashMap<>();
        try {
            DbxDownloader<DownloadZipResult> downloadZipResultDbxDownloader = client.files().downloadZip(filePath);
            ZipInputStream zis = new ZipInputStream(downloadZipResultDbxDownloader.getInputStream());
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                out.put(zipEntry.getName(),baos.toByteArray());
                baos.close();
                zipEntry = zis.getNextEntry();
            }

        } finally {

        }

        return out;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.dbl.service.DropBoxUtils#upload(java.io.InputStream,
     * java.lang.String, com.dropbox.core.v2.DbxClientV2)
     */
    @Override
    public FileMetadata upload(InputStream inputFile, String fullPath, DbxClientV2 client) throws DbxException, IOException {
        return upload(inputFile, fullPath, client, true);
    }

    @Override
    public FileMetadata upload(InputStream inputFile, String fullPath, DbxClientV2 client, boolean override) throws DbxException, IOException {
        if (fullPath == null || fullPath.isEmpty() || inputFile == null) {
            logger.error("no file to upload - full path or input stream is empty/null");
        }

        logger.debug("going to upload file " + fullPath);

        //FileMetadata metadata = client.files().uploadBuilder(fullPath).uploadAndFinish(inputFile);

        final WriteMode writeMode = override ? WriteMode.OVERWRITE : WriteMode.ADD;
        final Boolean autoRename = override ? Boolean.FALSE : Boolean.TRUE;

        UploadBuilder uploadBuilder = client.files().uploadBuilder(fullPath);
        uploadBuilder.withMode(writeMode);
        uploadBuilder.withAutorename(autoRename);
        FileMetadata metadata = uploadBuilder.uploadAndFinish(inputFile);

        return metadata;
    }
}
