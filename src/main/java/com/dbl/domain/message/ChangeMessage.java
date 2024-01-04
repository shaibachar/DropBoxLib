package com.dbl.domain.message;

import java.util.Arrays;

import com.dbl.domain.ChangeType;
import com.dropbox.core.v2.files.Metadata;

public class ChangeMessage {

	private byte[] file;
	private ChangeType messageType;
	private Metadata messageDetails;

	public ChangeMessage() {

	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

	public ChangeType getMessageType() {
		return messageType;
	}

	public void setMessageType(ChangeType messageType) {
		this.messageType = messageType;
	}

	public Metadata getMessageDetails() {
		return messageDetails;
	}

	public void setMessageDetails(Metadata messageDetails) {
		this.messageDetails = messageDetails;
	}

	@Override
	public String toString() {
		return "FileMessage [file=" + Arrays.toString(file) + ", messageType=" + messageType + ", messageDetails=" + messageDetails + "]";
	}

}
