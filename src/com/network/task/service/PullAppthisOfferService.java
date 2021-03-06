package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

public class PullAppthisOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAppthisOfferService.class);

	private Advertisers advertisers;

	public PullAppthisOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Clickky广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Adunity Offer begin := " + new Date());

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

				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONObject jsonObject = jsonPullObject.getJSONObject("offers");

				if (jsonObject != null) {

					Set<String> jsonSet = jsonObject.keySet();
					for (Iterator<String> iterator = jsonSet.iterator(); iterator.hasNext();) {
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						String offerid = (String) iterator.next();
						JSONObject item = jsonObject.getJSONObject(offerid);
						String name = item.getString("name");
						String tracklink = item.getString("tracking_url");
						String icon = item.getString("icon_url");
						String description = item.getString("description");
						// campaigns
						JSONArray campaignsitem = item.getJSONArray("campaigns");
						for (int i = 0; i < campaignsitem.size(); i++) {
							JSONObject ad = campaignsitem.getJSONObject(i);
							String platform = ad.getString("platform");
							if (platform.contains("iPhone") || platform.contains("Android")) {
								Float payout = ad.getFloat("payout");
								if (payout < 0.5) {
									continue;
								}
								String countries = ad.getString("countries");
								countries = countries.replace("[\"", "");
								countries = countries.replace("\"]", "");
								String pkg = null;
								String previewlink = null;
								String os_str = null;
								if (platform.contains("iPhone")) {
									pkg = item.getString("ios_bundle_id");
									previewlink = "https://itunes.apple.com/app/id" + pkg;
									os_str = "1";
								}
								if (platform.contains("Android")) {
									pkg = item.getString("android_package_name");
									previewlink = "https://play.google.com/store/apps/details?id="
											+ pkg;
									os_str = "0";
								}
								advertisersOffer.setAdv_offer_id(offerid);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracklink);
								advertisersOffer.setCountry(countries);
								advertisersOffer.setDaily_cap(500);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(description));
								advertisersOffer.setOs(os_str);
								advertisersOffer.setDevice_type(0);// 设置为mobile类型
								advertisersOffer.setSend_type(0);// 系统入库生成广告
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);// 设置为激活状态
							}
						}
						advertisersOfferList.add(advertisersOffer);
					}

					logger.info("after filter pull offer size := " + advertisersOfferList.size());

					// 入网盟广告
					if (advertisersId != null && advertisersOfferList != null
							&& advertisersOfferList.size() > 0) {

						PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
								.getBean("pullOfferCommonService");

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
		// http://api.clinkad.com/offer_api_v3?key=tizv1j8w9u7ykjck&pubid=6393&pagesize=200&type=CPI
		tmp.setApiurl("hhttps://feed.appthis.com/v2?api_key=5bba645800dbf7f3c45f67fe6f8c3b07");
		tmp.setApikey("key=tizv1j8w9u7ykjck&pubid=6393");
		tmp.setId(1055L);

		PullAppthisOfferService mmm = new PullAppthisOfferService(tmp);
		mmm.run();
	}

}
