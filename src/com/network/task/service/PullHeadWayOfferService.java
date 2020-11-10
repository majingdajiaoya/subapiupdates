package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullHeadWayOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullHeadWayOfferService.class);

	private Advertisers advertisers;

	public PullHeadWayOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull HeadWay Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();// 获取请求的url
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				String generate = "https://api.mobra.in/v1/campaign/feed?status=active";

				Map<String, String> header2 = new HashMap<String, String>();
				String posturl = "http://api.mobra.in/v1/auth/login";
				String cookeis = HttpUtil.Post(posturl, apikey);
				header2.put("cookie", cookeis);

				String str2 = HttpUtil.sendGet(generate, header2);

				JSONObject jsonObject = JSON.parseObject(str2);

				int total = jsonObject.getIntValue("total");
				if (total <= 0) {
					logger.info("status is not 1 return. total := " + total);
					return;
				}

				JSONArray offersArray = jsonObject.getJSONArray("data");
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				if (offersArray != null && offersArray.size() > 0) {
					logger.info("begin pull offer " + offersArray.size());
					for (int i = 0; i < offersArray.size(); i++) {
						JSONObject offerValueJsonObject = offersArray
								.getJSONObject(i);

						String id = offerValueJsonObject.getString("offer_id");
						String name = offerValueJsonObject.getString("name");
						String preview_url = offerValueJsonObject
								.getString("app_id");// preview_Url
						Float price = offerValueJsonObject.getFloat("payout");// 价格
						String description = offerValueJsonObject
								.getString("restrictions");// 描述

						String kpis = offerValueJsonObject.getString("kpis");// 描述
						if (kpis != null) {
							description = kpis + " ;" + description;
						}

						// String
						// expiration=offerObject.getString("expiration_date");//过期时间
						// String payout_type =
						// offerObject.getString("payout_type");//类型 "cpa_flat"
						String conversion_cap = offerValueJsonObject
								.getString("leads_budget_remaining");// "1500"
						int daycap = 99999;
						if (conversion_cap == null
								|| "null".equals(conversion_cap)) {

						} else {
							if ("0".equals(conversion_cap)) {
								continue;
							}
							if (conversion_cap.contains(".")) {
								continue;
							}
							daycap = Integer.valueOf(conversion_cap);
						}
						String click_url = offerValueJsonObject
								.getString("click_url");

						// https://go4.mobrain.xyz/dwtqfm5?p={subID}&sid={transaction_id}&android_a_id={GAID}&idfa={IDFA}&app_id={site_names}
						click_url = click_url.replace("{IDFA}", "{idfa}");
						click_url = click_url.replace("{GAID}", "{gaid}");
						click_url = click_url.replace("{subID}", "{channel}");
						click_url = click_url.replace("{transaction_id}",
								"{aff_sub}");
						click_url = click_url.replace("{app_id}", "{app_name}");

						String platform = offerValueJsonObject
								.getString("platform");

						String payout_type = offerValueJsonObject
								.getString("payout_type"); // CPI
						// 获取pkg包名
						String pkg = "";
						String osStr = "";
						if (platform == null) {
							continue;
						}
						if ("ANDROID".equals(platform.toUpperCase())) {
							osStr = "0";
						} else if ("IOS".equals(platform.toUpperCase())) {
							osStr = "1";

						}
						pkg = "";

						if (preview_url.contains("play.google.com")) {

							if (preview_url.contains("&hl")) {

								pkg = (String) preview_url.substring(
										preview_url.indexOf("?id=") + 4,
										preview_url.indexOf("&hl"));
							} else {
								if (preview_url.contains("&")) {
									pkg = (String) preview_url.substring(
											preview_url.indexOf("?id=") + 4,
											preview_url.indexOf("&"));
								} else {
									pkg = (String) preview_url
											.substring(preview_url
													.indexOf("?id=") + 4);

								}
							}

						} else if (preview_url.contains("itunes.apple.com")) {

							if (preview_url.contains("?mt")) {

								pkg = (String) preview_url.substring(
										preview_url.indexOf("/id") + 3,
										preview_url.indexOf("?mt"));
							} else {

								pkg = (String) preview_url
										.substring(preview_url.indexOf("/id") + 3);
							}

						}

						JSONArray countries = offerValueJsonObject
								.getJSONArray("countries");

						String countries_str = "";
						if (countries != null && countries.size() > 0) {

							for (int k = 0; k < countries.size(); k++) {

								String countriesItem = countries.getString(k);

								if (countriesItem != null) {

									countries_str += countriesItem
											.toUpperCase() + ":";
								}
							}
						}
						// 去除最后一个:号
						if (!OverseaStringUtil.isBlank(countries_str)) {

							countries_str = countries_str.substring(0,
									countries_str.length() - 1);
						}
						//
						String icon = null;
						JSONArray banners = offerValueJsonObject
								.getJSONArray("banners");
						JSONArray images_JSONArray = new JSONArray();

						ArrayList<Map> arrayList = new ArrayList<Map>();
						String images_crative = null;

						if (banners != null && banners.size() > 0) {
							for (int j = 0; j < banners.size(); j++) {
								if (j == 0) {
									JSONObject item11 = banners
											.getJSONObject(0);
									String path = item11.getString("path");
									icon = path;
								} else {
									JSONObject item11 = banners
											.getJSONObject(j);

									if (arrayList.size() < 3) {
										Map jsonObject11 = new HashMap();
										jsonObject11.put("url",
												item11.getString("path"));
										jsonObject11.put("size", "*");
										arrayList.add(jsonObject);
									}
								}
								if (arrayList.size() > 0) {
									images_JSONArray = JSONArray
											.parseArray(JSON
													.toJSONString(arrayList));
									images_crative = images_JSONArray
											.toString();
								}
							}
						}

						if (OverseaStringUtil.isBlank(id) || price == null
								|| OverseaStringUtil.isBlank(countries_str)
								|| OverseaStringUtil.isBlank(pkg)// 转换类型
								|| OverseaStringUtil.isBlank(click_url)// 点击url
								|| OverseaStringUtil.isBlank(conversion_cap)) {

							continue;
						}
						if (price < 0.06
								&& "CPI".equals(payout_type.toUpperCase())) {
							continue;
						}

						// 判断是否是非激励类型
						Integer incent_type = 0;
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						if ("CPI".equals(payout_type.toUpperCase())) {
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
						} else if ("CPA".equals(payout_type.toUpperCase())) {
							advertisersOffer.setCost_type(102);
							advertisersOffer.setOffer_type(102);
							advertisersOffer.setConversion_flow(102);
						}

						advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setName(name);// 网盟offer名称
						advertisersOffer.setPreview_url(preview_url);// 预览链接
						advertisersOffer.setCountry(countries_str);
						advertisersOffer.setPayout(price);
						advertisersOffer.setOs(osStr);
						advertisersOffer.setCreatives(null);
						// advertisersOffer.setExpiration(expiration);//设置过期时间
						advertisersOffer.setPkg(pkg);// 设置包名
						advertisersOffer.setDaily_cap(daycap);
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
						advertisersOffer
								.setDescription(filterEmoji(description));// 描述
						advertisersOffer.setIncent_type(incent_type);
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态
						advertisersOfferList.add(advertisersOffer);
					}

				}

				// 入网盟广告
				if (advertisersId != null && advertisersOfferList != null
						&& advertisersOfferList.size() > 0) {

					PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
							.getBean("pullOfferCommonService");

					pullOfferCommonService.doPullOffer(advertisers,
							advertisersOfferList);
				}

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}

		}

	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll(
					"[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) throws Exception {
		Advertisers tmp = new Advertisers();
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		tmp.setApiurl("https://api.mobra.in/v1/campaign/feed?status=active");
		tmp.setApikey("user=ye.ya@cappumedia.com&password=Mobrain123!");
		tmp.setId(2040L);

		PullHeadWayOfferService mmm = new PullHeadWayOfferService(
				tmp);
		mmm.run();
	}

}
