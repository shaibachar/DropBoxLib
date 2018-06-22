package com.dbl.domain;

public enum ManagedFileFormats {
	DOCX(".docx"), DOC(".doc");

	private final String type;

	private ManagedFileFormats(String type) {
		this.type = type;
	}

	public boolean equalsName(String otherName) {
		return type.equals(otherName);
	}

	public String toString() {
		return this.type;
	}
}
