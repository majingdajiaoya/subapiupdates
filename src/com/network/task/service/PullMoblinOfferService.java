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

public class PullMoblinOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullMoblinOfferService.class);

	private Advertisers advertisers;

	public PullMoblinOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull PullMoblinOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();// 获取请求的url
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				Map<String, String> header2 = new HashMap<String, String>();
				String posturl = "http://offers.mct.moblin.com/token";
				String str = HttpUtil
						.sendPost(posturl,
								"grant_type=password&username=wisky.zh@cappumedia.com&password=Pass12345!");
				JSONObject jsonObject = JSON.parseObject(str);
				String generate = "http://offers.mct.moblin.com/api/moblinOW/available_offers";
				String token = "bearer " + jsonObject.getString("access_token");
				Map<String, String> header = new HashMap<String, String>();
				header.put("Authorization", token);
				String str2 = HttpUtil.sendGet(generate, header);
				JSONObject jsonObject1 = JSON.parseObject(str2);
				jsonObject1 = jsonObject1.getJSONObject("offersModel");
				JSONArray offersArray = jsonObject1.getJSONArray("offer");
				if (offersArray != null && offersArray.size() > 0) {
					logger.info("begin pull offer " + offersArray.size());
					for (int i = 0; i < offersArray.size(); i++) {
						JSONObject offerValueJsonObject = offersArray
								.getJSONObject(i);
						String id = offerValueJsonObject
								.getString("campaignid");
						String title = offerValueJsonObject.getString("title");
						String pkgname = offerValueJsonObject
								.getString("pkgname");
						String icon = offerValueJsonObject.getString("icon");
						String description = offerValueJsonObject
								.getString("description");
						String kpi = offerValueJsonObject.getString("kpi");
						description = kpi + description;
						Float payout = offerValueJsonObject.getFloat("payout");
						Integer cap = offerValueJsonObject
								.getInteger("dailycap");
						String os = offerValueJsonObject.getString("os")
								.toUpperCase();
						String countries = offerValueJsonObject.getString(
								"countries").toUpperCase();
						String tracklink = "https://track.mct.moblin.com/ck?cid={id}&did={id}&sid=1467&crid=1&p1={aff_sub}&ssid={channel}&idfa={gaid}&p3={app_name}";
						tracklink = tracklink.replace("{id}", id);
						if (os.equals("ANDROID")) {
							tracklink = tracklink.replace("{gaid}", "{idfa}");
						}
						tracklink = tracklink.replace("{id}", id);
						String os_str = "";
						if (os.equals("IOS")) {
							os_str = "1";
						} else if (os.equals("ANDROID")) {
							os_str = "0";
						} else {
							continue;
						}
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(filterEmoji(title));
						advertisersOffer.setCost_type(101);

						advertisersOffer.setOffer_type(101);
						advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
						advertisersOffer.setPkg(pkgname);
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(tracklink);
						advertisersOffer.setClick_url(tracklink);
						advertisersOffer.setCountry(countries);
						advertisersOffer.setDaily_cap(cap);
						advertisersOffer.setPayout(payout);
						advertisersOffer.setCreatives(null);
						advertisersOffer.setConversion_flow(101);
						advertisersOffer
								.setDescription(filterEmoji(description));
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
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
		// user=ye.ya@cappumedia.com&password=Mobrain123!
		Map<String, String> header2 = new HashMap<String, String>();
		String posturl = "http://offers.mct.moblin.com/token";
		String str = HttpUtil
				.sendPost(posturl,
						"grant_type=password&username=wisky.zh@cappumedia.com&password=Pass12345!");
		JSONObject jsonObject = JSON.parseObject(str);
		String generate = "http://offers.mct.moblin.com/api/moblinOW/available_offers";
		String token = "bearer " + jsonObject.getString("access_token");
		Map<String, String> header = new HashMap<String, String>();
		header.put("Authorization", token);
		String str2 = HttpUtil.sendGet(generate, header);
		JSONObject jsonObject1 = JSON.parseObject(str2);
		jsonObject1 = jsonObject1.getJSONObject("offersModel");
		JSONArray offersArray = jsonObject1.getJSONArray("offer");
		if (offersArray != null && offersArray.size() > 0) {
			logger.info("begin pull offer " + offersArray.size());
			for (int i = 0; i < offersArray.size(); i++) {
				JSONObject offerValueJsonObject = offersArray.getJSONObject(i);
				String id = offerValueJsonObject.getString("campaignid");
				String title = offerValueJsonObject.getString("title");
				String pkgname = offerValueJsonObject.getString("pkgname");
				String icon = offerValueJsonObject.getString("icon");
				String description = offerValueJsonObject
						.getString("description");
				String kpi = offerValueJsonObject.getString("kpi");
				description = kpi + description;
				Float payout = offerValueJsonObject.getFloat("payout");
				Integer cap = offerValueJsonObject.getInteger("dailycap");
				String os = offerValueJsonObject.getString("os").toUpperCase();
				String countries = offerValueJsonObject.getString("countries")
						.toUpperCase();
				String tracklink = "https://track.mct.moblin.com/ck?cid={id}&did={id}&sid=1467&crid=1&p1={aff_sub}&ssid={channel}&idfa={gaid}&p3={app_name}";
				tracklink = tracklink.replace("{id}", id);
				if (os.equals("ANDROID")) {
					tracklink = tracklink.replace("{gaid}", "{idfa}");
				}
				tracklink = tracklink.replace("{id}", id);
			}
		}
	}

}
