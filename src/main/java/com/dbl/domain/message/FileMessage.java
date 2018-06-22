package com.dbl.domain.message;

import java.util.Arrays;

public class FileMessage {

	private byte[] file;
	private String messageType;
	private String messageDetails;

	public FileMessage() {

	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getMessageDetails() {
		return messageDetails;
	}

	public void setMessageDetails(String messageDetails) {
		this.messageDetails = messageDetails;
	}

	@Override
	public String toString() {
		return "FileMessage [file=" + Arrays.toString(file) + ", messageType=" + messageType + ", messageDetails=" + messageDetails + "]";
	}

}
