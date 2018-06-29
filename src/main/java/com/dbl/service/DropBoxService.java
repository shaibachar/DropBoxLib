package com.dbl.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.dbl.config.DropBoxLibProperties;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.http.StandardHttpRequestor.Config;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

/**
 * 
 * @author shai
 *
 */
@Service
@EnableConfigurationProperties(DropBoxLibProperties.class)
public class DropBoxService {
	private final Logger logger = LoggerFactory.getLogger(DropBoxService.class);

	private DbxClientV2 client;
	private final DropBoxLibProperties appProperties;
	private final DropBoxUtils dropBoxUtils;

	private ListFolderResult result;

	public DropBoxService(DropBoxLibProperties boxProperties, DropBoxUtils dropBoxUtils) {
		this.appProperties = boxProperties;
		this.dropBoxUtils = dropBoxUtils;
	}

	/**
	 * This method will login to DropBox
	 */
	public void connect() {
		Config config = dropBoxUtils.getDefaultConfig(appProperties);
		DbxAuthInfo auth = dropBoxUtils.getAuth(appProperties);
		client = dropBoxUtils.createClient(auth, config, appProperties.getDropboxConfig());
	}

	/**
	 * This method will download files from DropBox
	 * 
	 * @param filePath
	 * @return
	 * @throws DbxException
	 * @throws IOException
	 */
	public byte[] download(String filePath) throws DbxException, IOException {
		byte[] download = dropBoxUtils.download(filePath, client);
		return download;
	}

	/**
	 * This method will upload files to DropBox
	 * 
	 * @param inputFile
	 * @param path
	 * @throws DbxException
	 * @throws IOException
	 */
	public FileMetadata upload(InputStream inputFile, String fullPath) throws DbxException, IOException {
		FileMetadata upload = dropBoxUtils.upload(inputFile, fullPath, client);
		return upload;
	}

	/**
	 * This method will return all files under folder path recursive if the path is
	 * empty all files in all folders will return.
	 * 
	 * @param path
	 * @param recursive
	 * @return
	 */
	public List<FileMetadata> allFiles(String path, boolean recursive) {
		List<FileMetadata> res = new ArrayList<>();
		try {
			String syncFiles = syncFiles(res, path, recursive);
			logger.debug(syncFiles);

		} catch (Exception e) {
			logger.error("Error on retrive all files", e);
		}
		return res;
	}

	public String syncFiles(List<FileMetadata> files, String path, boolean recursive) throws ListFolderErrorException, DbxException {

		ListFolderBuilder listFolderBuilder = client.files().listFolderBuilder(path == null ? "" : path);
		ListFolderResult result = listFolderBuilder.withRecursive(recursive).start();

		while (true) {

			if (result != null) {
				for (Metadata entry : result.getEntries()) {
					if (entry instanceof FileMetadata) {
						logger.info("Added file: " + entry.getPathLower());
						files.add((FileMetadata) entry);
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
		}
	}
	
	/**
	 * This method will rename files on DropBox
	 * 
	 * @param fromPath
	 * @param toPath
	 * @throws DbxException
	 */
	public void rename(String fromPath, String toPath) throws DbxException {
		logger.debug("Going to rename from:" + fromPath + " to path:" + toPath);
		Metadata metadata = client.files().moveV2(fromPath, toPath).getMetadata();
		logger.debug(metadata.toStringMultiline());
	}

	public ListFolderResult getResult() {
		return result;
	}

	public void setResult(ListFolderResult result) {
		this.result = result;
	}
}
