package com.network.task.bean;

import java.util.Date;

public class BlackListSubAffiliates {

	private Long id;
	private Long advertisers_id;//上游网盟ID
	private Long affiliates_id;//下游渠道ID
	private String affiliates_sub_id;//下游子渠道
	private String pkg;//pkg
	private Integer status;
	private Date ctime;
	private Date utime;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getAdvertisers_id() {
		return advertisers_id;
	}
	public void setAdvertisers_id(Long advertisers_id) {
		this.advertisers_id = advertisers_id;
	}
	public Long getAffiliates_id() {
		return affiliates_id;
	}
	public void setAffiliates_id(Long affiliates_id) {
		this.affiliates_id = affiliates_id;
	}
	public String getAffiliates_sub_id() {
		return affiliates_sub_id;
	}
	public void setAffiliates_sub_id(String affiliates_sub_id) {
		this.affiliates_sub_id = affiliates_sub_id;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Date getCtime() {
		return ctime;
	}
	public void setCtime(Date ctime) {
		this.ctime = ctime;
	}
	public Date getUtime() {
		return utime;
	}
	public void setUtime(Date utime) {
		this.utime = utime;
	}
	public String getPkg() {
		return pkg;
	}
	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

}
