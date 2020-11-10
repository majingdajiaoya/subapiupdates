package com.network.task.bean;

import java.util.Date;

public class AdvertisersOffer {

	private Long id;
	private String adv_offer_id;// 网盟自身offer ID
	private String name;// 网盟offer名称
	private Integer cost_type;// Offer转化类型(101:CPI、102:CPA、103:CPS等)
	private Integer offer_type;// Offer业务分类(101:GP-CPI 102:订阅 103:国内IOS 104:其他)
	private Long advertisers_id;// 所属网盟ID
	private String pkg;// pkg包名
	private Float pkg_size;// 包大小(M)
	private String main_icon;// Icon连接
	private String preview_url;// 预览连接
	private String click_url;// 推广点击连接
	private String country;// 推广国家
	private String ecountry;// 推广排除国家
	private Integer daily_cap;// 每日cap限制
	private Integer send_cap;// 下发渠道cap限制
	private Float payout;// 支付价格
	private String expiration;// 过期日期
	private String creatives;// 推广素材
	private Integer conversion_flow;// 转化类型(0:cpi, 1:click, 2:two click,3:SOI)
	private String supported_carriers;// 支持运营商(WIFI, Carrier Network Traffic)
	private String description;// 其他方面描述(限制或者约束等)
	private String os;// 操作系统类型，可多个用:分割(0:andriod:1:ios:2:pc)
	private String os_version;// 系统版本要求
	private Integer device_type;// 设备类型(0:mobile, 1:PC)
	private Integer send_type;// 下发类型(0:自动下发，1:手动下发)
	private Integer incent_type;// 是否激励(0:非激励 1:激励)
	private Integer smartlink_type;// 是否Smartlink类型(0:是,1:否)
	private Integer status;// 状态(0 激活 -1 手动下线 -2 系统下线)
	private Date ctime;
	private Date utime;
	private String ImpressionUrl;//

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAdv_offer_id() {
		return adv_offer_id;
	}

	public void setAdv_offer_id(String adv_offer_id) {
		this.adv_offer_id = adv_offer_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Long getAdvertisers_id() {
		return advertisers_id;
	}

	public void setAdvertisers_id(Long advertisers_id) {
		this.advertisers_id = advertisers_id;
	}

	public String getPkg() {
		return pkg;
	}

	public void setPkg(String pkg) {
		this.pkg = pkg;
	}

	public Float getPkg_size() {
		return pkg_size;
	}

	public void setPkg_size(Float pkg_size) {
		this.pkg_size = pkg_size;
	}

	public String getMain_icon() {
		return main_icon;
	}

	public void setMain_icon(String main_icon) {
		this.main_icon = main_icon;
	}

	public String getPreview_url() {
		return preview_url;
	}

	public void setPreview_url(String preview_url) {
		this.preview_url = preview_url;
	}

	public String getClick_url() {
		return click_url;
	}

	public void setClick_url(String click_url) {
		this.click_url = click_url;
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

	public Integer getDaily_cap() {
		return daily_cap;
	}

	public void setDaily_cap(Integer daily_cap) {
		this.daily_cap = daily_cap;
	}

	public Integer getSend_cap() {
		return send_cap;
	}

	public void setSend_cap(Integer send_cap) {
		this.send_cap = send_cap;
	}

	public Float getPayout() {
		return payout;
	}

	public void setPayout(Float payout) {
		this.payout = payout;
	}

	public String getExpiration() {
		return expiration;
	}

	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}

	public String getCreatives() {
		return creatives;
	}

	public void setCreatives(String creatives) {
		this.creatives = creatives;
	}

	public Integer getConversion_flow() {
		return conversion_flow;
	}

	public void setConversion_flow(Integer conversion_flow) {
		this.conversion_flow = conversion_flow;
	}

	public String getSupported_carriers() {
		return supported_carriers;
	}

	public void setSupported_carriers(String supported_carriers) {
		this.supported_carriers = supported_carriers;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getOs_version() {
		return os_version;
	}

	public void setOs_version(String os_version) {
		this.os_version = os_version;
	}

	public Integer getDevice_type() {
		return device_type;
	}

	public void setDevice_type(Integer device_type) {
		this.device_type = device_type;
	}

	public Integer getSend_type() {
		return send_type;
	}

	public void setSend_type(Integer send_type) {
		this.send_type = send_type;
	}

	public Integer getIncent_type() {
		return incent_type;
	}

	public void setIncent_type(Integer incent_type) {
		this.incent_type = incent_type;
	}

	public Integer getSmartlink_type() {
		return smartlink_type;
	}

	public void setSmartlink_type(Integer smartlink_type) {
		this.smartlink_type = smartlink_type;
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

	public String getImpressionUrl() {
		return ImpressionUrl;
	}

	public void setImpressionUrl(String impressionUrl) {
		ImpressionUrl = impressionUrl;
	}

}
