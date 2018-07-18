package com.dbl.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
import com.dropbox.core.v2.files.DeleteResult;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;

public class DropBoxUtilsImpl implements DropBoxUtils {

	private final Logger logger = LoggerFactory.getLogger(DropBoxUtilsImpl.class);
	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#createClient(com.dropbox.core.DbxAuthInfo, com.dropbox.core.http.StandardHttpRequestor.Config, java.lang.String)
	 */
	@Override
	public DbxClientV2 createClient(DbxAuthInfo auth, StandardHttpRequestor.Config config, String clientUserAgentId) {
		StandardHttpRequestor requestor = new StandardHttpRequestor(config);
		DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(clientUserAgentId).withHttpRequestor(requestor)
				.build();

		return new DbxClientV2(requestConfig, auth.getAccessToken(), auth.getHost());
	}

	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#getAuth(com.dbl.config.DropBoxLibProperties)
	 */
	@Override
	public DbxAuthInfo getAuth(DropBoxLibProperties appProperties) {
		
		DbxHost host = DbxHost.DEFAULT;
		String accessToken = appProperties.getAccessToken();
		DbxAuthInfo authInfo = new DbxAuthInfo(accessToken, host);
		return authInfo;
	}

	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#getDefaultConfig(com.dbl.config.DropBoxLibProperties)
	 */
	@Override
	public Config getDefaultConfig(DropBoxLibProperties appProperties) {
		//TODO: create another getConfig method with config construction
		StandardHttpRequestor.Config config = Config.DEFAULT_INSTANCE;
		return config;
	}
	
	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#getLongPoolConfig(com.dbl.config.DropBoxLibProperties)
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
	
	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#download(java.lang.String, com.dropbox.core.v2.DbxClientV2)
	 */
	@Override
	public byte[] download(String filePath,DbxClientV2 client) throws DbxException, IOException {
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
	
	/* (non-Javadoc)
	 * @see com.dbl.service.DropBoxUtils#upload(java.io.InputStream, java.lang.String, com.dropbox.core.v2.DbxClientV2)
	 */
	@Override
	public FileMetadata upload(InputStream inputFile, String fullPath,DbxClientV2 client) throws DbxException, IOException {
		if (fullPath == null || fullPath.isEmpty() || inputFile == null) {
			logger.error("no file to upload - full path or input stream is empty/null");
		}

		logger.debug("going to upload file " + fullPath);
		try {
			DeleteResult deleteV2 = client.files().deleteV2(fullPath);
			Metadata metadata = deleteV2.getMetadata();
			logger.debug(metadata.toStringMultiline());
		} catch (Exception e) {
			logger.debug("tried to delete before update");
		}
		FileMetadata metadata = client.files().uploadBuilder(fullPath).uploadAndFinish(inputFile);
		return metadata;
	}
}