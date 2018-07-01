package com.dbl.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dropboxlib", ignoreUnknownFields = false)
public class DropBoxLibProperties {

	private List<String> interestingFileFormat;
	private String dropboxConfig;
	private String accessToken;
	private String dropBoxRootPath;
	private boolean longPull;

	public List<String> getInterestingFileFormat() {
		return interestingFileFormat;
	}

	public void setInterestingFileFormat(List<String> interestingFileFormat) {
		this.interestingFileFormat = interestingFileFormat;
	}

	public String getDropboxConfig() {
		return dropboxConfig;
	}

	public void setDropboxConfig(String dropboxConfig) {
		this.dropboxConfig = dropboxConfig;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getDropBoxRootPath() {
		return dropBoxRootPath;
	}

	public void setDropBoxRootPath(String dropBoxRootPath) {
		this.dropBoxRootPath = dropBoxRootPath;
	}

	public boolean isLongPull() {
		return longPull;
	}

	public void setLongPull(boolean longPull) {
		this.longPull = longPull;
	}

}
