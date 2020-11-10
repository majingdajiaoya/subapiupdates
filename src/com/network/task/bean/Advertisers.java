package com.network.task.bean;

import java.util.Date;

public class Advertisers {

	private Long id;
	private String name;// 网盟名称
	private String company;// 所属公司
	private String contacts;// 联系人
	private String email;// 邮箱
	private String im;// 其他联系方式(QQ、微信等)
	private Integer weight;// 权重
	private Float toppayout;// 总金额限制
	private Integer max_offer_num;// 最大拉取广告数量限制
	private String apikey;// 拉取网盟广告apikey
	private String apiurl;// 请求API的URL
	private String pullofferclass;// 拉取offer服务类全名
	private String advertisers_url_params;// 请求网盟offerURL时传递参数
	private String callback_url;// 我方回调地址
	private Integer status;// 0 激活 -1 手动下线 -2 系统下线
	private Date ctime;
	private Date utime;
	private String countryLimit;
	private Float minpayout;
	private String pkg;

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

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getContacts() {
		return contacts;
	}

	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getIm() {
		return im;
	}

	public void setIm(String im) {
		this.im = im;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Float getToppayout() {
		return toppayout;
	}

	public void setToppayout(Float toppayout) {
		this.toppayout = toppayout;
	}

	public Integer getMax_offer_num() {
		return max_offer_num;
	}

	public void setMax_offer_num(Integer max_offer_num) {
		this.max_offer_num = max_offer_num;
	}

	public String getApikey() {
		return apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public String getApiurl() {
		return apiurl;
	}

	public void setApiurl(String apiurl) {
		this.apiurl = apiurl;
	}

	public String getPullofferclass() {
		return pullofferclass;
	}

	public void setPullofferclass(String pullofferclass) {
		this.pullofferclass = pullofferclass;
	}

	public String getAdvertisers_url_params() {
		return advertisers_url_params;
	}

	public void setAdvertisers_url_params(String advertisers_url_params) {
		this.advertisers_url_params = advertisers_url_params;
	}

	public String getCallback_url() {
		return callback_url;
	}

	public void setCallback_url(String callback_url) {
		this.callback_url = callback_url;
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

	public String getCountryLimit() {
		return countryLimit;
	}

	public void setCountryLimit(String countryLimit) {
		this.countryLimit = countryLimit;
	}

	public Float getMinpayout() {
		return minpayout;
	}

	public void setMinpayout(Float minpayout) {
		this.minpayout = minpayout;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

}
