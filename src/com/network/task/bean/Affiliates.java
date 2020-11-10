package com.network.task.bean;

import java.util.Date;

public class Affiliates {

	private Long id;
	private String name;// 渠道名称
	private Integer type;// 类型(0:外部渠道、1:内部渠道)
	private Float profit_rate;// 利润率(渠道扣量比率)
	private String token;// 渠道Token
	private String postback;// 渠道回调地址
	private String domain;// 渠道tracking域名
	private Integer status;// 状态(0 正常 -1暂停 -2 永久禁止)
	private Date ctime;
	private Date utime;
	private Integer addpayoutrate;// 扣量比例
	private Integer click;// 最大点击上限
	private Float cvr;
	private String country;// 允许下发国家
	private String ecountry;// 不允许下发国家
	private String limit_os;// 限制系统

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Float getProfit_rate() {
		return profit_rate;
	}

	public void setProfit_rate(Float profit_rate) {
		this.profit_rate = profit_rate;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getPostback() {
		return postback;
	}

	public void setPostback(String postback) {
		this.postback = postback;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
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

	public Integer getAddpayoutrate() {
		return addpayoutrate;
	}

	public void setAddpayoutrate(Integer addpayoutrate) {
		this.addpayoutrate = addpayoutrate;
	}

	public Integer getClick() {
		return click;
	}

	public void setClick(Integer click) {
		this.click = click;
	}

	public Float getCvr() {
		return cvr;
	}

	public void setCvr(Float cvr) {
		this.cvr = cvr;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getEcountry() {
		return ecountry;
	}

	public void setEcountry(String ecountry) {
		this.ecountry = ecountry;
	}

	public String getLimit_os() {
		return limit_os;
	}

	public void setLimit_os(String limit_os) {
		this.limit_os = limit_os;
	}
}
