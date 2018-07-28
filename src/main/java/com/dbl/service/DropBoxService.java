package com.dbl.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;

public interface DropBoxService {

	
	/**
	 * This method will login to DropBox
	 */
	void connect();

	/**
	 * This method will download files from DropBox
	 * 
	 * @param filePath
	 * @return
	 * @throws DbxException
	 * @throws IOException
	 */
	byte[] download(String filePath) throws DbxException, IOException;

	/**
	 * This method will upload files to DropBox
	 * 
	 * @param inputFile
	 * @param path
	 * @throws DbxException
	 * @throws IOException
	 */
	FileMetadata upload(InputStream inputFile, String fullPath) throws DbxException, IOException;

	/**
	 * This method will return all files under folder path recursive if the path is
	 * empty all files in all folders will return.
	 * 
	 * @param path
	 * @param recursive
	 * @return
	 */
	List<FileMetadata> allFiles(String path, boolean recursive);

	/**
	 * This method will return all the folders names in the root folder path 
	 * @param rootfolderPath
	 * @return
	 */
	List<FolderMetadata> allFolders(String rootfolderPath,boolean recursive);
	
	String syncFiles(List<FolderMetadata> folders,List<FileMetadata> files, String path, boolean recursive)
			throws ListFolderErrorException, DbxException;

	/**
	 * This method will rename files on DropBox
	 * 
	 * @param fromPath
	 * @param toPath
	 * @throws DbxException
	 */
	void rename(String fromPath, String toPath) throws DbxException;

	ListFolderResult getResult();

	void setResult(ListFolderResult result);

}