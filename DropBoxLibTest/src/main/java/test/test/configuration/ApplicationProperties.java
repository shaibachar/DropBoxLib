package test.test.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Drop Box Driver.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

	private String dropboxConfig;
	private String accessToken;
	private String dropBoxRootPath;
	private boolean longPull;
	
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
