package com.dbl.service;

import java.io.IOException;
import java.io.InputStream;

import com.dbl.config.DropBoxLibProperties;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

public interface DropBoxUtils {

	/**
	 * Create a new Dropbox client using the given authentication information and
	 * HTTP client config.
	 *
	 * @param auth
	 *            Authentication information
	 * @param config
	 *            HTTP request configuration
	 *
	 * @return new Dropbox V2 client
	 */
	DbxClientV2 createClient(DbxAuthInfo auth, StandardHttpRequestor.Config config, String clientUserAgentId);

	/**
	 * 
	 * @param appProperties
	 * @return
	 */
	DbxAuthInfo getAuth(DropBoxLibProperties appProperties);

	Config getDefaultConfig(DropBoxLibProperties appProperties);

	Config getLongPoolConfig(DropBoxLibProperties appProperties);

	byte[] download(String filePath, DbxClientV2 client) throws DbxException, IOException;

	FileMetadata upload(InputStream inputFile, String fullPath, DbxClientV2 client) throws DbxException, IOException;

}