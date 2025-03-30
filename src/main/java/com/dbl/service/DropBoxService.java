package com.dbl.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.dbl.exception.DropBoxLibException;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.ListRevisionsErrorException;
import com.dropbox.core.v2.files.ListRevisionsResult;
import com.dropbox.core.v2.files.SearchErrorException;
import com.dropbox.core.v2.files.SearchResult;

public interface DropBoxService {

	
	/**
	 * This method will login to DropBox
	 */
	void connect();

	Map<String, byte[]> downloadAllZip(String folderPath) throws DropBoxLibException;

	/**
	 * This method will download all files in dropBox folder 
	 * @param folderPath
	 * @return
	 * @throws DropBoxLibException
	 */
	Map<String,byte[]> downloadAll(String folderPath) throws DropBoxLibException;
	/**
	 * This method will download files from DropBox
	 * 
	 * @param filePath
	 * @return
	 * @throws DropBoxLibException
	 */
	byte[] download(String filePath) throws DropBoxLibException;

	/**
	 *
	 * @param inputFile
	 * @param fullPath
	 * @return
	 * @throws DropBoxLibException
	 */
	FileMetadata upload(InputStream inputFile, String fullPath) throws DropBoxLibException;

	/**
	 * This method will return all files under folder path recursive if the path is
	 * empty all files in all folders will return.
	 * 
	 * @param path
	 * @param recursive
	 * @return
	 * @throws DropBoxLibException
	 */
	List<FileMetadata> allFiles(String path, boolean recursive) throws DropBoxLibException;

	/**
	 * This method will return all the folders names in the root folder path 
	 * @param rootfolderPath
	 * @return
	 * @throws DropBoxLibException
	 */
	List<FolderMetadata> allFolders(String rootfolderPath,boolean recursive) throws DropBoxLibException;
	
	String syncFiles(List<FolderMetadata> folders,List<FileMetadata> files, String path, boolean recursive)
			throws DropBoxLibException;

	/**
	 * This method will rename files on DropBox
	 * 
	 * @param fromPath
	 * @param toPath
	 * @throws DropBoxLibException
	 */
	void rename(String fromPath, String toPath) throws DropBoxLibException;

	ListFolderResult getResult();

	void setResult(ListFolderResult result);

	/**
	 * 
	 * @param inputFile
	 * @param fullPath
	 * @param override
	 * @return
	 * @throws DropBoxLibException
	 */
	FileMetadata upload(InputStream inputFile, String fullPath, boolean override) throws DropBoxLibException;

	/**
	 * This method will search drop box
	 * @param path
	 * @param query
	 * @return
	 * @throws DropBoxLibException
	 */
	SearchResult search(String path, String query) throws DropBoxLibException;

	/**
	 * This method will retrieve all file revisions
	 * @param path
	 * @return
	 * @throws DropBoxLibException
	 */
	ListRevisionsResult getRevisions(String path) throws DropBoxLibException;

	/**
	 * This method will get all files of the file types
	 * @param folderPath
	 * @param b
	 * @param fileTypes
	 * @return
	 */
	List<FileMetadata> allFiles(String folderPath, boolean b, List<String> fileTypes);

    Boolean checkPath(String path) throws DropBoxLibException;

    void delete(String oldFileName) throws DropBoxLibException;
}