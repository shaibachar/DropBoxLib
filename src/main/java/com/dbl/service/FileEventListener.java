package com.dbl.service;

import java.util.List;
import com.dbl.domain.message.FileMessage;

public interface FileEventListener {

	public List<String> getInterestingFileFormat();
	public void fileChanged(FileMessage fileMessage);
	
}
