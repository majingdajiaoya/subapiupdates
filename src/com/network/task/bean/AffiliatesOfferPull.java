package com.network.task.bean;

public class AffiliatesOfferPull {

	private String tracking_id;// 我方系统Offer跟踪ID
	private Long advertisers_offer_id;// 上游网盟offer ID
	private Long advertisers_id;// 上游网盟ID
	private Long affiliates_id;// 下游渠道ID
	private String affiliates_sub_id;// 下游子渠道
	private String affiliates_click_url;// 下游渠道广告URL
	private Integer cap;// cap限制
	private Float payout;// 价格
	private Float hack_convert_rate;// 回调扣除百分比
	private Integer send_type;// 下发类型(0:自动下发，1:手动下发)
	private Integer status;// 状态(0 在线 -1 手动下线 -2 系统下线)
	private Integer addpayoutrate;// 加价比例
	private Integer max_click;// 点击上限
	private Float cvr;

	public String getTracking_id() {
		return tracking_id;
	}

	public void setTracking_id(String tracking_id) {
		this.tracking_id = tracking_id;
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

	public String getAffiliates_click_url() {
		return affiliates_click_url;
	}

	public void setAffiliates_click_url(String affiliates_click_url) {
		this.affiliates_click_url = affiliates_click_url;
	}

	public Integer getCap() {
		return cap;
	}

	public void setCap(Integer cap) {
		this.cap = cap;
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getAddpayoutrate() {
		return addpayoutrate;
	}

	public void setAddpayoutrate(Integer addpayoutrate) {
		this.addpayoutrate = addpayoutrate;
	}

	public Integer getMax_click() {
		return max_click;
	}

	public void setMax_click(Integer max_click) {
		this.max_click = max_click;
	}

	public Float getCvr() {
		return cvr;
	}

	public void setCvr(Float cvr) {
		this.cvr = cvr;
	}

}
