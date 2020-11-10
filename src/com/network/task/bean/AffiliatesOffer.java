package com.network.task.bean;

import java.util.Date;

public class AffiliatesOffer {

	private Long id;
	private String tracking_id;//我方系统Offer跟踪ID
	private String adv_offer_id;//上游网盟自身offer ID
	private Long advertisers_offer_id;// 上游网盟offer ID
	private Long advertisers_id;//上游网盟ID
	private String advertisers_click_url;//上游网盟广告URL
	private String advertisers_url_params;//网盟URL参数
	private Long affiliates_id;//下游渠道ID
	private String affiliates_sub_id;//下游子渠道
	private String affiliates_click_url;//下游渠道广告URL
	private Integer cost_type;//Offer成本类型(0:CPI、1:CPA、2:CPS等)
	private Integer offer_type;//Offer分类(0:GP、1:订阅、2:健康、3:美容)
	private Integer conversion_flow;//转化类型(cpi, One click, two click,SOI)
	private String country;//推广国家
	private String pkg;//PKG包名
	private Integer cap;//cap限制
	private String currency;//货币单位
	private Float payout;//价格
	private Float hack_convert_rate;//回调扣除百分比
	private Integer send_type;// 下发类型(0:自动下发，1:手动下发)
	private String os;// 操作系统类型,可以多个，用:分割(0:andriod:1:ios:2:pc)
	private Integer device_type;//设备类型(0:mobile, 1:PC)
	private Integer status;//状态(0 在线 -1 手动下线 -2 系统下线 -3 规则下线)
	private Date ctime;
	private Date utime;
	private String description;
	private String creatives;
	private Integer incent_type;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTracking_id() {
		return tracking_id;
	}
	public void setTracking_id(String tracking_id) {
		this.tracking_id = tracking_id;
	}
	public String getAdv_offer_id() {
		return adv_offer_id;
	}
	public void setAdv_offer_id(String adv_offer_id) {
		this.adv_offer_id = adv_offer_id;
	}
	public Long getAdvertisers_offer_id() {
		return advertisers_offer_id;
	}
	public void setAdvertisers_offer_id(Long advertisers_offer_id) {
		this.advertisers_offer_id = advertisers_offer_id;
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
	public String getAdvertisers_click_url() {
		return advertisers_click_url;
	}
	public void setAdvertisers_click_url(String advertisers_click_url) {
		this.advertisers_click_url = advertisers_click_url;
	}
	public String getAdvertisers_url_params() {
		return advertisers_url_params;
	}
	public void setAdvertisers_url_params(String advertisers_url_params) {
		this.advertisers_url_params = advertisers_url_params;
	}
	public String getAffiliates_click_url() {
		return affiliates_click_url;
	}
	public void setAffiliates_click_url(String affiliates_click_url) {
		this.affiliates_click_url = affiliates_click_url;
	}
	public Integer getCost_type() {
		return cost_type;
	}
	public void setCost_type(Integer cost_type) {
		this.cost_type = cost_type;
	}
	public Integer getOffer_type() {
		return offer_type;
	}
	public void setOffer_type(Integer offer_type) {
		this.offer_type = offer_type;
	}
	public Integer getConversion_flow() {
		return conversion_flow;
	}
	public void setConversion_flow(Integer conversion_flow) {
		this.conversion_flow = conversion_flow;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getPkg() {
		return pkg;
	}
	public void setPkg(String pkg) {
		this.pkg = pkg;
	}
	public Integer getCap() {
		return cap;
	}
	public void setCap(Integer cap) {
		this.cap = cap;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Float getPayout() {
		return payout;
	}
	public void setPayout(Float payout) {
		this.payout = payout;
	}
	public Float getHack_convert_rate() {
		return hack_convert_rate;
	}
	public void setHack_convert_rate(Float hack_convert_rate) {
		this.hack_convert_rate = hack_convert_rate;
	}
	public Integer getSend_type() {
		return send_type;
	}
	public void setSend_type(Integer send_type) {
		this.send_type = send_type;
	}
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}
	public Integer getDevice_type() {
		return device_type;
	}
	public void setDevice_type(Integer device_type) {
		this.device_type = device_type;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCreatives() {
		return creatives;
	}
	public void setCreatives(String creatives) {
		this.creatives = creatives;
	}
	public Integer getIncent_type() {
		return incent_type;
	}
	public void setIncent_type(Integer incent_type) {
		this.incent_type = incent_type;
	}
}
