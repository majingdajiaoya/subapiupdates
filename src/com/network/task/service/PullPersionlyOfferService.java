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
import com.network.task.bean.BlackListSubAffiliates;
import com.network.task.dao.Blacklist_affiliates_subidDao;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullPersionlyOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullPersionlyOfferService.class);

	private Advertisers advertisers;

	public PullPersionlyOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullPersionlyOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			logger.info("doPull Persionly Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.CuttGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							String roffer_id = item.getString("id");// 104573764
							Integer device_type = null;
							device_type = 0;
							String offer_model = item.getString("conversion_mode");
							;// "CPI"

							JSONObject traffic_restrictions = item.getJSONObject("traffic_restrictions");
							Integer incetive = 0;// 是否激励(0:非激励 1:激励)

							if (traffic_restrictions != null) {
								String traffic_type = traffic_restrictions.getString("traffic_incentivized");

								if ("no".equals(traffic_type)) {

									incetive = 0;
								} else if ("yes".equals(traffic_type)) {

									incetive = 1;
								}
							}
							JSONArray payouts = item.getJSONArray("payouts");
							Float payout = 0.0f;// 0.31
							String os_str = null;
							String min_os_version = null;
							String pkg = null;// "com.gtarcade.lod"
							String preview_url = null;
							String countries_str = "";

							if (payouts != null && payouts.size() > 0) {
								JSONObject payoutsone = payouts.getJSONObject(0);
								JSONArray countries = payoutsone.getJSONArray("countries");
								String platform = payoutsone.getString("platform");
								payout = payoutsone.getFloat("usd_payout");

								if (payout > 15) {
									continue;
								}

								if ("android".equals(platform.toLowerCase())) {
									os_str = "0";
									min_os_version = item.getString("android_min_version");
									pkg = item.getString("android_package_id");
									preview_url = item.getString("preview_url_android");
								} else if ("iphone".equals(platform.toLowerCase()) || "ipad".equals(platform.toLowerCase())) {
									os_str = "1";
									min_os_version = item.getString("ios_min_version");
									pkg = item.getString("store_app_id");
									preview_url = item.getString("preview_url_ios");
								} else {
									continue;
								}
								if (pkg != null && pkg.length() == 0) {
									continue;
								}
								if (countries != null && countries.size() > 0) {

									for (int k = 0; k < countries.size(); k++) {

										String countriesItem = countries.getString(k);

										if (countriesItem != null) {

											countries_str += countriesItem.toUpperCase() + ":";
										}
									}
								}
								// 去除最后一个:号
								if (!OverseaStringUtil.isBlank(countries_str)) {

									countries_str = countries_str.substring(0, countries_str.length() - 1);
								}

							} else {
								continue;
							}
							String name = item.getString("campaign_name");// "Legacy of Discord-FuriousWings"

							String description = item.getString("campaign_name");

							String icon = item.getString("campaign_icon_url");// "http://cdn.PullPersionlyOfferService.biz/s…Xzzyg=w3840_400x400.png"
							JSONObject subscription_caps = item.getJSONObject("subscription_caps");

							String caps_daily_remaining_str = subscription_caps.getString("daily_caps_left");// 9992
							Integer caps_daily_remaining = 99999;
							if (!OverseaStringUtil.isBlank(caps_daily_remaining_str)) {
								if ("-1".equals(caps_daily_remaining_str) || "0".equals(caps_daily_remaining_str)) {
									// continue;
									caps_daily_remaining = 99999;
								} else {

									caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
								}
							}
							String link = item.getString("tracking_url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if (roffer_id == null || payout == null || OverseaStringUtil.isBlank(countries_str) || OverseaStringUtil.isBlank(link) || OverseaStringUtil.isBlank(offer_model)) {

								continue;
							}
							String images_crative = null;

							JSONArray creatives1111 = item.getJSONArray("creatives");
							offer_model = offer_model.toUpperCase();
							// CPI类型，单价太低(0.06)，舍弃不入库
							if ("CPI".equals(offer_model) && payout < 0.06) {

								continue;
							}

							// CPI类型，pkg为空，舍弃不入库
							if ("CPI".equals(offer_model) && OverseaStringUtil.isBlank(pkg)) {

								continue;
							}

							// blacklist
							JSONObject blockChannelObject = item.getJSONObject("blacklist");

							JSONArray blockChannel = blockChannelObject.getJSONArray("subid2");
							if (blockChannel != null && blockChannel.size() > 0) {
								for (Object object : blockChannel) {
									if (object.toString().length() > 5) {
										if (object.toString().substring(4, 5).equals("_")) {
											block_pkg_affi.put(pkg.replace("_", "") + "&" + object.toString(), object.toString());
										}
									}
								}
							}

							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
							advertisersOffer.setName(name);

							if ("CPI".equals(offer_model)) {

								advertisersOffer.setCost_type(101);

								advertisersOffer.setOffer_type(101);
							} else if ("CPA".equals(offer_model)) {

								advertisersOffer.setCost_type(102);

								advertisersOffer.setOffer_type(102);// 订阅类型
							} else {

								advertisersOffer.setCost_type(104);

								advertisersOffer.setOffer_type(104);// 设置为其它类型
							}

							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							// advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(link);

							advertisersOffer.setCountry(countries_str);

							// advertisersOffer.setEcountry(ecountry);

							advertisersOffer.setDaily_cap(caps_daily_remaining);
							// advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							// advertisersOffer.setExpiration(category);
							advertisersOffer.setCreatives(images_crative);
							if ("CPI".equals(offer_model)) {

								advertisersOffer.setConversion_flow(101);
							} else {

								advertisersOffer.setConversion_flow(104);// 设置为其它类型
							}
							// advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(filterEmoji(description));
							advertisersOffer.setOs(os_str);

							advertisersOffer.setDevice_type(device_type);
							advertisersOffer.setOs_version(min_os_version);// 系统版本要求
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setIncent_type(incetive);// 是否激励
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);
						}
					}
					logger.info("block_pkg_affi" + block_pkg_affi);
					logger.info("after filter pull offer size := " + advertisersOfferList.size());

					// 黑名单
					Blacklist_affiliates_subidDao Blacklist_affiliates_subidDao = (Blacklist_affiliates_subidDao) TuringSpringHelper.getBean("Blacklist_affiliates_subidDao");
					List<BlackListSubAffiliates> block_list = Blacklist_affiliates_subidDao.getAllInfoByAdvertisers_id(advertisersId + "");
					Map<String, String> db_block_map = new HashMap<String, String>();
					for (BlackListSubAffiliates entity : block_list) {
						db_block_map.put(entity.getPkg() + "&" + entity.getAffiliates_id() + "_" + entity.getAffiliates_sub_id(), entity.getAffiliates_sub_id());
					}
					// 对比
					for (String key : block_pkg_affi.keySet()) {
						if (db_block_map.get(key) == null) {
							String sub[] = key.split("&");
							String pkg = sub[0];
							String affiliates_id = sub[1].substring(0, 4);
							String affiliates_sub_id = sub[1].substring(5, sub[1].length());
							Blacklist_affiliates_subidDao.add(advertisersId, pkg, affiliates_id, affiliates_sub_id);
						}
					}

					// 入网盟广告
					if (advertisersId != null && advertisersOfferList != null && advertisersOfferList.size() > 0) {

						PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper.getBean("pullOfferCommonService");

						pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
					}
				}
			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("http://dsp.persona.ly/api/campaigns?token={apikey}");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);

		PullPersionlyOfferService mmm = new PullPersionlyOfferService(tmp);
		mmm.run();
	}

}
