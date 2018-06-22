package com.dbl.domain;

import java.util.Date;

public class WDCInformation {

	private String creator;
	private String lastModifiedByUser;
	private Date modified;
	private Date created;
	private String path;

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public void setLastModifiedByUser(String lastModifiedByUser) {
		this.lastModifiedByUser = lastModifiedByUser;

	}

	public void setModified(Date modified) {
		this.modified = modified;

	}

	public void setCreated(Date created) {
		this.created = created;

	}

	public void setFilePath(String path) {
		this.path = path;

	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCreator() {
		return creator;
	}

	public String getLastModifiedByUser() {
		return lastModifiedByUser;
	}

	public Date getModified() {
		return modified;
	}

	public Date getCreated() {
		return created;
	}

	@Override
	public String toString() {
		return "WDCInformation [creator=" + creator + ", lastModifiedByUser=" + lastModifiedByUser + ", modified=" + modified + ", created=" + created + ", path=" + path + "]";
	}

}
