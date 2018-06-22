package com.dbl.service;

import com.dbl.domain.message.FileMessage;

public interface FileEventListener {

	public void fileChanged(FileMessage fileMessage);
	
}
