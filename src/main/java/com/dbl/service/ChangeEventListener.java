package com.dbl.service;

import java.util.List;

import com.dbl.domain.message.ChangeMessage;

public interface ChangeEventListener {

	public List<String> getInterestingFileFormat();
	public void change(ChangeMessage fileMessage);
	
}
