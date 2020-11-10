package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullFilexmediaOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullFilexmediaOfferService.class);

	private Advertisers advertisers;

	public PullFilexmediaOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullFilexmediaOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullFilexmediaOfferService Offer begin := "
					+ new Date());

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

				JSONObject jsonPullObject = JSON.parseObject(str);

				String status = jsonPullObject.getString("httpStatus");

				if (!"200".equals(status)) {

					logger.info("status is not ok return. status := " + status);

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONObject("data")
						.getJSONArray("content");
				int apinum = 0;
				if (offersArray != null && offersArray.size() > 0) {
					apinum = offersArray.size();
					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							Integer roffer_id = item.getInteger("id");// 104573764
							int incetive = 0;
							String name = item.getString("name");// "Legacy of Discord-FuriousWings"
							String preview_url = item.getString("preview_url");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
							String app_id = TuringStringUtil
									.getpkg(preview_url);// "com.gtarcade.lod"
							String description = item.getString("description");
							String tracking_link = item
									.getString("tracking_link");
							String os_str = "";
							if (preview_url.indexOf("google") > 0) {
								os_str = "0";
								tracking_link = tracking_link
										+ "&device_id={gaid}";
							} else if (preview_url.indexOf("apple.com") > 0) {
								os_str = "1";
								tracking_link = tracking_link
										+ "&device_id={idfa}";
							} else {
								continue;
							}
							Float payout = item.getFloat("payout");

							String geo_countries = item
									.getString("geo_countries")
									.replace(",", ":").replace(";", ":");
							if (payout <= 0.1) {
								continue;
							}

							if (app_id.length() == 0) {
								continue;
							}
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String
									.valueOf(roffer_id));
							advertisersOffer.setName(filterEmoji(name));

							advertisersOffer.setCost_type(101);

							advertisersOffer.setOffer_type(101);

							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(app_id);
							// advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon("");
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(tracking_link);

							advertisersOffer.setCountry(geo_countries);

							// advertisersOffer.setEcountry(ecountry);

							advertisersOffer.setDaily_cap(999);
							// advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							// advertisersOffer.setExpiration(category);

							advertisersOffer.setConversion_flow(101);
							advertisersOffer
									.setDescription(filterEmoji(description));
							advertisersOffer.setOs(os_str);

							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setIncent_type(incetive);// 是否激励
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);
						}
					}

					logger.info("after filter pull PullFilexmediaOfferService offer size := "
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
		tmp.setApiurl("http://xyads.fuseclick.com/api/v2/getOffers?key=15EB899C5BA9123395CA33BB3B992690&a=382&limit=1000&page=1");
		tmp.setApikey("2e50942d-9c1b-444c-86b3-24fada0a5344");
		tmp.setId(19L);

		PullFilexmediaOfferService mmm = new PullFilexmediaOfferService(tmp);
		mmm.run();
	}

}
