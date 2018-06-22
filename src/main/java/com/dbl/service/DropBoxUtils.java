package com.dbl.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dbl.config.ApplicationProperties;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxHost;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;

@Component
public class DropBoxUtils {

	private final Logger logger = LoggerFactory.getLogger(DropBoxUtils.class);
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
	public DbxClientV2 createClient(DbxAuthInfo auth, StandardHttpRequestor.Config config, String clientUserAgentId) {
		StandardHttpRequestor requestor = new StandardHttpRequestor(config);
		DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(clientUserAgentId).withHttpRequestor(requestor)
				.build();

		return new DbxClientV2(requestConfig, auth.getAccessToken(), auth.getHost());
	}

	/**
	 * 
	 * @param appProperties
	 * @return
	 */
	public DbxAuthInfo getAuth(ApplicationProperties appProperties) {
		
		DbxHost host = DbxHost.DEFAULT;
		String accessToken = appProperties.getAccessToken();
		DbxAuthInfo authInfo = new DbxAuthInfo(accessToken, host);
		return authInfo;
	}

	public Config getDefaultConfig(ApplicationProperties appProperties) {
		//TODO: create another getConfig method with config construction
		StandardHttpRequestor.Config config = Config.DEFAULT_INSTANCE;
		return config;
	}
	
	public Config getLongPoolConfig(ApplicationProperties appProperties) {
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
}
