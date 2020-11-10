package com.network.task.bean;

import java.util.Date;

public class AdvertisersOfferTemp {

	private Long temp_id;
	private String temp_adv_offer_id;// 网盟自身offer ID
	private String temp_name;// 网盟offer名称
	private Integer temp_cost_type;// Offer转化类型(0:CPI、1:CPA、2:CPS等)
	private Integer temp_offer_type;// Offer业务分类(0:GP-CPI 1:订阅 2:国内IOS 3:五类)
	private Long temp_advertisers_id;// 所属网盟ID
	private String temp_pkg;// pkg包名
	private Float temp_pkg_size;// 包大小(M)
	private String temp_main_icon;// Icon连接
	private String temp_preview_url;// 预览连接
	private String temp_click_url;// 推广点击连接
	private String temp_country;// 推广国家
	private String temp_ecountry;// 推广排除国家
	private Integer temp_daily_cap;// 每日cap限制
	private Integer temp_send_cap;// 下发渠道cap限制
	private Float temp_payout;// 支付价格
	private String temp_expiration;// 过期日期
	private String temp_creatives;// 推广素材
	private Integer temp_conversion_flow;// 转化类型(0:cpi, 1:click, 2:two
											// click,3:SOI)
	private String temp_supported_carriers;// 支持运营商(WIFI, Carrier Network
											// Traffic)
	private String temp_description;// 其他方面描述(限制或者约束等)
	private String temp_os;// 操作系统类型，可多个用:分割(0:andriod:1:ios:2:pc)
	private String temp_os_version;// 系统版本要求
	private Integer temp_device_type;// 设备类型(0:mobile, 1:PC)
	private Integer temp_send_type;// 下发类型(0:自动下发，1:手动下发)
	private Integer temp_incent_type;// 是否激励(0:非激励 1:激励)
	private Integer temp_smartlink_type;// 是否Smartlink类型(0:是,1:否)
	private Integer temp_status;// 状态(0 激活 -1 手动下线 -2 系统下线)
	private Date temp_ctime;
	private Date temp_utime;

	private String temp_impress_url;

	public Long getTemp_id() {
		return temp_id;
	}

	public void setTemp_id(Long temp_id) {
		this.temp_id = temp_id;
	}

	public String getTemp_adv_offer_id() {
		return temp_adv_offer_id;
	}

	public void setTemp_adv_offer_id(String temp_adv_offer_id) {
		this.temp_adv_offer_id = temp_adv_offer_id;
	}

	public String getTemp_name() {
		return temp_name;
	}

	public void setTemp_name(String temp_name) {
		this.temp_name = temp_name;
	}

	public Integer getTemp_cost_type() {
		return temp_cost_type;
	}

	public void setTemp_cost_type(Integer temp_cost_type) {
		this.temp_cost_type = temp_cost_type;
	}

	public Integer getTemp_offer_type() {
		return temp_offer_type;
	}

	public void setTemp_offer_type(Integer temp_offer_type) {
		this.temp_offer_type = temp_offer_type;
	}

	public Long getTemp_advertisers_id() {
		return temp_advertisers_id;
	}

	public void setTemp_advertisers_id(Long temp_advertisers_id) {
		this.temp_advertisers_id = temp_advertisers_id;
	}

	public String getTemp_pkg() {
		return temp_pkg;
	}

	public void setTemp_pkg(String temp_pkg) {
		this.temp_pkg = temp_pkg;
	}

	public Float getTemp_pkg_size() {
		return temp_pkg_size;
	}

	public void setTemp_pkg_size(Float temp_pkg_size) {
		this.temp_pkg_size = temp_pkg_size;
	}

	public String getTemp_main_icon() {
		return temp_main_icon;
	}

	public void setTemp_main_icon(String temp_main_icon) {
		this.temp_main_icon = temp_main_icon;
	}

	public String getTemp_preview_url() {
		return temp_preview_url;
	}

	public void setTemp_preview_url(String temp_preview_url) {
		this.temp_preview_url = temp_preview_url;
	}

	public String getTemp_click_url() {
		return temp_click_url;
	}

	public void setTemp_click_url(String temp_click_url) {
		this.temp_click_url = temp_click_url;
	}

	public String getTemp_country() {
		return temp_country;
	}

	public void setTemp_country(String temp_country) {
		this.temp_country = temp_country;
	}

	public String getTemp_ecountry() {
		return temp_ecountry;
	}

	public void setTemp_ecountry(String temp_ecountry) {
		this.temp_ecountry = temp_ecountry;
	}

	public Integer getTemp_daily_cap() {
		return temp_daily_cap;
	}

	public void setTemp_daily_cap(Integer temp_daily_cap) {
		this.temp_daily_cap = temp_daily_cap;
	}

	public Integer getTemp_send_cap() {
		return temp_send_cap;
	}

	public void setTemp_send_cap(Integer temp_send_cap) {
		this.temp_send_cap = temp_send_cap;
	}

	public Float getTemp_payout() {
		return temp_payout;
	}

	public void setTemp_payout(Float temp_payout) {
		this.temp_payout = temp_payout;
	}

	public String getTemp_expiration() {
		return temp_expiration;
	}

	public void setTemp_expiration(String temp_expiration) {
		this.temp_expiration = temp_expiration;
	}

	public String getTemp_creatives() {
		return temp_creatives;
	}

	public void setTemp_creatives(String temp_creatives) {
		this.temp_creatives = temp_creatives;
	}

	public Integer getTemp_conversion_flow() {
		return temp_conversion_flow;
	}

	public void setTemp_conversion_flow(Integer temp_conversion_flow) {
		this.temp_conversion_flow = temp_conversion_flow;
	}

	public String getTemp_supported_carriers() {
		return temp_supported_carriers;
	}

	public void setTemp_supported_carriers(String temp_supported_carriers) {
		this.temp_supported_carriers = temp_supported_carriers;
	}

	public String getTemp_description() {
		return temp_description;
	}

	public void setTemp_description(String temp_description) {
		this.temp_description = temp_description;
	}

	public String getTemp_os() {
		return temp_os;
	}

	public void setTemp_os(String temp_os) {
		this.temp_os = temp_os;
	}

	public String getTemp_os_version() {
		return temp_os_version;
	}

	public void setTemp_os_version(String temp_os_version) {
		this.temp_os_version = temp_os_version;
	}

	public Integer getTemp_device_type() {
		return temp_device_type;
	}

	public void setTemp_device_type(Integer temp_device_type) {
		this.temp_device_type = temp_device_type;
	}

	public Integer getTemp_send_type() {
		return temp_send_type;
	}

	public void setTemp_send_type(Integer temp_send_type) {
		this.temp_send_type = temp_send_type;
	}

	public Integer getTemp_incent_type() {
		return temp_incent_type;
	}

	public void setTemp_incent_type(Integer temp_incent_type) {
		this.temp_incent_type = temp_incent_type;
	}

	public Integer getTemp_smartlink_type() {
		return temp_smartlink_type;
	}

	public void setTemp_smartlink_type(Integer temp_smartlink_type) {
		this.temp_smartlink_type = temp_smartlink_type;
	}

	public Integer getTemp_status() {
		return temp_status;
	}

	public void setTemp_status(Integer temp_status) {
		this.temp_status = temp_status;
	}

	public Date getTemp_ctime() {
		return temp_ctime;
	}

	public void setTemp_ctime(Date temp_ctime) {
		this.temp_ctime = temp_ctime;
	}

	public Date getTemp_utime() {
		return temp_utime;
	}

	public void setTemp_utime(Date temp_utime) {
		this.temp_utime = temp_utime;
	}

	public String getTemp_impress_url() {
		return temp_impress_url;
	}

	public void setTemp_impress_url(String temp_impress_url) {
		this.temp_impress_url = temp_impress_url;
	}

}
