package com.network.task.bean;

import java.util.Date;

public class AdvertisersOfferUpdate {

	private Long id;

	private Long advertisers_id;
	private Integer befroreallsize;
	private Integer nowallsize;
	private Integer offlinesize;
	private Integer updatesize;
	private Integer addsize;
	private Date ctime;
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
	public Integer getBefroreallsize() {
		return befroreallsize;
	}
	public void setBefroreallsize(Integer befroreallsize) {
		this.befroreallsize = befroreallsize;
	}
	public Integer getNowallsize() {
		return nowallsize;
	}
	public void setNowallsize(Integer nowallsize) {
		this.nowallsize = nowallsize;
	}
	public Integer getOfflinesize() {
		return offlinesize;
	}
	public void setOfflinesize(Integer offlinesize) {
		this.offlinesize = offlinesize;
	}
	public Integer getUpdatesize() {
		return updatesize;
	}
	public void setUpdatesize(Integer updatesize) {
		this.updatesize = updatesize;
	}
	public Integer getAddsize() {
		return addsize;
	}
	public void setAddsize(Integer addsize) {
		this.addsize = addsize;
	}
	public Date getCtime() {
		return ctime;
	}
	public void setCtime(Date ctime) {
		this.ctime = ctime;
	}
	
	
	@Override
	public String toString() {
		return "AdvertisersOfferUpdate [id=" + id + ", advertisers_id="
				+ advertisers_id + ", befroreallsize=" + befroreallsize
				+ ", nowallsize=" + nowallsize + ", offlinesize=" + offlinesize
				+ ", updatesize=" + updatesize + ", addsize=" + addsize
				+ ", ctime=" + ctime + "]";
	}

	
}
