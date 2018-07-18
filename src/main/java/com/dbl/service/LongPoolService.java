package com.dbl.service;

import java.time.LocalDateTime;
import java.util.List;

import com.dbl.domain.message.FileMessage;

public interface LongPoolService {

	LocalDateTime getLastChangeTime();

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
	 */
	void connect();

}