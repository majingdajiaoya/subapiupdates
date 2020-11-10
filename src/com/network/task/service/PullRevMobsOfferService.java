package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullRevMobsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullRevMobsOfferService.class);

	private Advertisers advertisers;

	public PullRevMobsOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullRevMobsOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull RevMob Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.sendGet(apiurl);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = JSONArray.parseArray(str);

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							String roffer_id = item.getString("id");// 104573764
							Integer device_type = null;
							device_type = 0;
							String offer_model = item.getString("payoutType");// "CPI"

							Integer incetive = 0;// 是否激励(0:非激励 1:激励)
							JSONArray countries = item
									.getJSONArray("countries");
							String os_str = item.getString("os");
							if ("android".equals(os_str.toLowerCase())) {
								os_str = "0";
							} else if ("ios".equals(os_str.toLowerCase())) {
								os_str = "1";
							}
							String min_os_version = item
									.getString("minOSVersion");
							String name = item.getString("name");// "Legacy of Discord-FuriousWings"

							String app_id = item.getString("bundleId");// "com.gtarcade.lod"
							String target_url = null;// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

							target_url = TuringStringUtil.getPriviewUrl(os_str,
									app_id);

							String description = name;
							String icon = null;// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"
							Integer caps_daily_remaining = 9999;
							String link = item.getString("clickUrl");
							link = link.replace("%{clickId}", "{aff_sub}");
							link = link.replace("%{subId}", "{channel}");

							if (os_str.equals("0")) { // android
								link = link.replace("%{ifa}", "{gaid}");
								link = link.replace("%{appName}", "{app_name}");

							} else if (os_str.equals("1")) { // ios
								link = link.replace("%{ifa}", "{idfa}");
								// 1146128499,1331831564,1073000685
								link = link.replace("%{appName}", "{app_name}");
							}
							Float payout = item.getFloat("price");// 0.31
							String countries_str = "";
							if (countries != null && countries.size() > 0) {

								for (int k = 0; k < countries.size(); k++) {

									String countriesItem = countries
											.getString(k);

									if (countriesItem != null
											&& !countriesItem.contains("-")) {

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
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if (roffer_id == null || payout == null
									|| countries == null
									|| OverseaStringUtil.isBlank(link)
									|| OverseaStringUtil.isBlank(offer_model)) {

								continue;
							}
							String images_crative = null;
							offer_model = offer_model.toUpperCase();
							// CPI类型，pkg为空，舍弃不入库
							if ("CPI".equals(offer_model)
									&& OverseaStringUtil.isBlank(app_id)) {

								continue;
							}

							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String
									.valueOf(roffer_id));
							advertisersOffer.setName(filterEmoji(name));

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
							advertisersOffer.setPkg(app_id);
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(link);

							advertisersOffer.setCountry(countries_str);

							advertisersOffer.setDaily_cap(caps_daily_remaining);
							advertisersOffer.setPayout(payout);
							advertisersOffer.setCreatives(images_crative);
							if ("CPI".equals(offer_model)) {

								advertisersOffer.setConversion_flow(101);
							} else {

								advertisersOffer.setConversion_flow(104);// 设置为其它类型
							}
							advertisersOffer
									.setDescription(filterEmoji(description));
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

					logger.info("after filter pull offer size := "
							+ advertisersOfferList.size());

					// 入网盟广告
					if (advertisersId != null && advertisersOfferList != null
							&& advertisersOfferList.size() > 0) {

						PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
								.getBean("pullOfferCommonService");

						pullOfferCommonService.doPullOffer(advertisers,
								advertisersOfferList);
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
			return source.replaceAll(
					"[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		// Revmobinterestmob18!!
		// https://interestmob:{apikey}@apioffers.revmobmobileadnetwork.com/revmob-affiliate/fetchAvailableOffers
		tmp.setApiurl("https://cappumedia:CappumediaRM2018!@apioffers.revmobmobileadnetwork.com/revmob-affiliate/fetchAvailableOffers");
		tmp.setApikey("cappumedia:CappumediaRM2018!@");
		tmp.setId(25L);
		// https://cappumedia:CappumediaRM2018!@apioffers.revmobmobileadnetwork.com/revmob-affiliate/fetchAvailableOffers
		// postback
		// http://pb.interforgame.com/mobpb/pb.do?sub={clickId}&payout={payout}&sr=revmob3198

		PullRevMobsOfferService mmm = new PullRevMobsOfferService(tmp);
		mmm.run();
	}

}
