package com.dbl.service;

import java.util.List;

import com.dbl.domain.message.FileMessage;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;

public interface LongPoolService {

	void register(FileEventListener fileEventListener);

	List<FileEventListener> getEventListeners();

	/**
	 * This method will call observers
	 * 
	 * @param fileMessage
	 * @return number of observers been called
	 */
	int updateListeners(FileMessage fileMessage);

	/**
	 * This method will login to DropBox
	 * @throws DbxException 
	 * @throws DbxApiException 
	 */
	void connect() throws DbxApiException, DbxException;

	boolean isHealth();


}