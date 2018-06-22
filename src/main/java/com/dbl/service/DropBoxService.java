package com.dbl.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.dbl.config.ApplicationProperties;
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
@EnableConfigurationProperties(ApplicationProperties.class)
public class DropBoxService {
	private final Logger logger = LoggerFactory.getLogger(DropBoxService.class);

	private DbxClientV2 client;
	private final ApplicationProperties appProperties;
	private final DropBoxUtils dropBoxUtils;

	private ListFolderResult result;

	public DropBoxService(ApplicationProperties boxProperties, DropBoxUtils dropBoxUtils) {
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

	/**
	 * This method will upload files to DropBox
	 * 
	 * @param inputFile
	 * @param path
	 * @throws DbxException
	 * @throws IOException
	 */
	public void upload(File inputFile, String path) throws DbxException, IOException {
		logger.debug("going to upload file" + inputFile.getName() + " path:" + path);
		try (InputStream in = new FileInputStream(inputFile)) {
			FileMetadata metadata = client.files().uploadBuilder(path + inputFile.getName()).uploadAndFinish(in);
		}
	}

	public List<String> allFiles(){
		List<String> res = new ArrayList<>();
		try {
			String syncFiles = syncFiles(res);
			logger.debug(syncFiles);
			
		} catch (Exception e) {
			logger.error("Error on retrive all files",e);
		}
		return res;
	}
	
	public String syncFiles(List<String> files) throws ListFolderErrorException, DbxException {

		ListFolderBuilder listFolderBuilder = client.files().listFolderBuilder("");
		ListFolderResult result = listFolderBuilder.withRecursive(true).start();
			
		while (true) {

			if (result != null) {
				for ( Metadata entry : result.getEntries()) {
					if (entry instanceof FileMetadata){
						logger.info("Added file: "+entry.getPathLower());
					}
				}

				if (!result.getHasMore()) {
					logger.info("GET LATEST CURSOR");
					return result.getCursor();
				}

				try {
					result = client.files().listFolderContinue(result.getCursor());
				} catch (DbxException e) {
					logger.info ("Couldn't get listFolderContinue");
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
