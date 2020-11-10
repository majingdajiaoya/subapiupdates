package com.network.task.bean;

public class AdIcon {

	private Long id;
	private String pkg;//
	private String icon;//
	private String os;//
	private Integer status;//
	private String previewlink;//

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getPreviewlink() {
		return previewlink;
	}

	public void setPreviewlink(String previewlink) {
		this.previewlink = previewlink;
	}

}
