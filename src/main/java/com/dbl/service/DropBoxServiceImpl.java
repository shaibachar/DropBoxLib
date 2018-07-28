package com.dbl.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.dbl.config.DropBoxLibProperties;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

/**
 * 
 * @author shai
 *
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
	public void connect() {
		Config config = dropBoxUtils.getDefaultConfig(appProperties);
		DbxAuthInfo auth = dropBoxUtils.getAuth(appProperties);
		client = dropBoxUtils.createClient(auth, config, appProperties.getDropboxConfig());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.DropBoxService#download(java.lang.String)
	 */
	@Override
	public byte[] download(String filePath) throws DbxException, IOException {
		byte[] download = dropBoxUtils.download(filePath, client);
		return download;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.DropBoxService#upload(java.io.InputStream,
	 * java.lang.String)
	 */
	@Override
	public FileMetadata upload(InputStream inputFile, String fullPath) throws DbxException, IOException {
		FileMetadata upload = dropBoxUtils.upload(inputFile, fullPath, client);
		return upload;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.DropBoxService#allFiles(java.lang.String, boolean)
	 */
	@Override
	public List<FileMetadata> allFiles(String path, boolean recursive) {
		List<FileMetadata> res = new ArrayList<>();
		try {
			String syncFiles = syncFiles(null, res, path, recursive);
			logger.debug(syncFiles);

		} catch (Exception e) {
			logger.error("Error on retrive all files", e);
		}
		return res;
	}

	@Override
	public List<FolderMetadata> allFolders(String path, boolean recursive) {
		List<FolderMetadata> res = new ArrayList<>();
		try {
			String syncFiles = syncFiles(res, null, path, recursive);
			logger.debug(syncFiles);

		} catch (Exception e) {
			logger.error("Error on retrive all folders", e);
		}
		return res;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.DropBoxService#syncFiles(java.util.List,
	 * java.lang.String, boolean)
	 */
	@Override
	public String syncFiles(List<FolderMetadata> folders, List<FileMetadata> files, String path, boolean recursive) throws ListFolderErrorException, DbxException {

		ListFolderBuilder listFolderBuilder = client.files().listFolderBuilder(path == null ? "" : path);
		ListFolderResult result = listFolderBuilder.withRecursive(recursive).start();
		while (result != null) {

			for (Metadata entry : result.getEntries()) {
				if (entry instanceof FileMetadata) {
					if (files != null) {
						logger.debug("Added file: " + entry.getPathLower());
						files.add((FileMetadata) entry);
					}
				} else if (entry instanceof FolderMetadata) {
					if (folders != null) {
						logger.debug("Added file: " + entry.getPathLower());
						folders.add((FolderMetadata) entry);
					}
				}
			}

			if (!result.getHasMore()) {
				logger.info("GET LATEST CURSOR");
				return result.getCursor();
			}

			try {
				result = client.files().listFolderContinue(result.getCursor());
			} catch (DbxException e) {
				logger.info("Couldn't get listFolderContinue");
			}
		}
		return result.getCursor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dbl.service.DropBoxService#rename(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void rename(String fromPath, String toPath) throws DbxException {
		logger.debug("Going to rename from:" + fromPath + " to path:" + toPath);
		Metadata metadata = client.files().moveV2(fromPath, toPath).getMetadata();
		logger.debug(metadata.toStringMultiline());
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

}
