package com.network.task.bean;

/**
 * 网盟广告待下发广告Bean
 * 
 * @author sundu
 * 
 */
public class AdvertisersOfferWaitPushBean {

	private Long advertisers_id;// 上游网盟ID
	private Long advertisers_offer_id;// 上游网盟offer ID
	private Float payout;// 价格
	private Integer daily_cap;// 每日cap
	private Integer cost_type;// Offer转化类型(101:CPI、102:CPA、103:CPS等)
	private Integer offer_type;// Offer业务分类(101:GP-CPI 102:订阅 103:国内IOS 104:其他)
	private String country;
	private String ecountry;
	private String os;

	public Long getAdvertisers_id() {
		return advertisers_id;
	}

	public void setAdvertisers_id(Long advertisers_id) {
		this.advertisers_id = advertisers_id;
	}

	public Long getAdvertisers_offer_id() {
		return advertisers_offer_id;
	}

	public void setAdvertisers_offer_id(Long advertisers_offer_id) {
		this.advertisers_offer_id = advertisers_offer_id;
	}

	public Float getPayout() {
		return payout;
	}

	public void setPayout(Float payout) {
		this.payout = payout;
	}

	public Integer getDaily_cap() {
		return daily_cap;
	}

	public void setDaily_cap(Integer daily_cap) {
		this.daily_cap = daily_cap;
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

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

}
