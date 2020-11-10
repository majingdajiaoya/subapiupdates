package com.network.task.bean;

import java.util.Date;

public class APILimit {

	private Long id;
	private Long advertisers_id;
	private String country_white_list;
	private String country_black_list;
	private String pkg_white_list;
	private String pkg_black_list;
	private Float min_payout;
	private Date ctime;
	private Date utime;

	@Override
	public String toString() {
		return "APILimit [id=" + id + ", advertisers_id=" + advertisers_id + ", country_white_list=" + country_white_list + ", country_black_list=" + country_black_list + ", pkg_white_list=" + pkg_white_list + ", min_payout=" + min_payout + ", ctime=" + ctime + ", utime=" + utime + "]";
	}

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

	public String getCountry_white_list() {
		return country_white_list;
	}

	public void setCountry_white_list(String country_white_list) {
		this.country_white_list = country_white_list;
	}

	public String getCountry_black_list() {
		return country_black_list;
	}

	public void setCountry_black_list(String country_black_list) {
		this.country_black_list = country_black_list;
	}

	public String getPkg_white_list() {
		return pkg_white_list;
	}

	public void setPkg_white_list(String pkg_white_list) {
		this.pkg_white_list = pkg_white_list;
	}

	public String getPkg_black_list() {
		return pkg_black_list;
	}

	public void setPkg_black_list(String pkg_black_list) {
		this.pkg_black_list = pkg_black_list;
	}

	public Float getMin_payout() {
		return min_payout;
	}

	public void setMin_payout(Float min_payout) {
		this.min_payout = min_payout;
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

}
