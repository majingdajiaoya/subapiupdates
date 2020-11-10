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
import com.network.task.util.MyX509TrustManager;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullAdealsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullAdealsOfferService.class);

	private Advertisers advertisers;

	public PullAdealsOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullAdealsOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullAdealsOfferService Offer begin := "
					+ new Date());

			try {
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				Long advertisersId = advertisers.getId();
				String tokenUrl = advertisers.getApiurl();
				Map<String, String> header = new HashMap<String, String>();
				byte[] jsonStr = MyX509TrustManager.doPost(tokenUrl, "");
				JSONObject json = JSON.parseObject(new String(jsonStr, "utf-8"));
				header.put("Authorization", "Bearer " + json.get("token") + "");
				String apiurl = "https://item-api.aff-adeals.com/api/v2/get_campaigns?_page=1&_per=50";
				String offerJosn = HttpUtil.execCurl(apiurl, header);
				JSONObject offersObject = JSON.parseObject(offerJosn);
				
				System.out.println(offersObject);
				
				JSONArray offersArray = JSON.parseArray(offersObject
						.get("campaigns") + "");
				
				if (offersArray != null && offersArray.size() > 0) {

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {
								String name = item.getString("campaign_name");
								JSONArray itemArray = (JSONArray) item
										.get("items");
								String actionsStr = itemArray.get(0).toString();
								JSONObject actionsObj = JSON
										.parseObject(actionsStr);
								JSONArray unit_price_usdObject = (JSONArray) actionsObj
										.get("actions");
								Float payout = JSON.parseObject(
										unit_price_usdObject.get(0).toString())
										.getFloat("unit_price_usd");
								String pkg = actionsObj.getString("bundle")
										.replace("id", "");
								String offerId = actionsObj.getString("id");
								String platform = actionsObj.getString("os");
								String os_str = null;
								if (platform.toUpperCase().equals("IOS")) {
									os_str = "1";
								} else if (platform.toUpperCase().equals(
										"ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								Integer cap = actionsObj
										.getInteger("campaign_budget_daily");
								if (cap == null) {
									cap = 100;
								}
								String description = actionsObj
										.getString("note");
								String previewlink = actionsObj
										.getString("store_url");
								String offer_model = JSON.parseObject(
										unit_price_usdObject.get(0).toString())
										.getString("requirement");

								String trackinglink = actionsObj
										.getString("click_url");

								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerId);
								advertisersOffer.setName(filterEmoji(name));
								if ("CPI".equals(offer_model)) {

									advertisersOffer.setCost_type(101);

									advertisersOffer.setOffer_type(101);
								} else if ("CPA".equals(offer_model)) {

									advertisersOffer.setCost_type(102);

									advertisersOffer.setOffer_type(102);// 订阅类型
								}
								advertisersOffer
										.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon("");
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(trackinglink);
								advertisersOffer.setCountry("JP");
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

						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					logger.info("after filter  pull  PullAdealsOfferService offer size := "
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

	public static void main(String[] args) throws Exception {
		Map<String, String> header = new HashMap<String, String>();
		String tokenUrl = "https://item-api.aff-adeals.com/api/auth/v2?_site=364&_l=cappumedia&_p=917eee5d41f181ba018166e726ee636b4aee75e228bec403039dd007c036e003";
		byte[] jsonStr = MyX509TrustManager.doPost(tokenUrl, "");
		JSONObject json = JSON.parseObject(new String(jsonStr, "utf-8"));
		header.put("Authorization", "Bearer " + json.get("token") + "");
		String apiurl = "https://item-api.aff-adeals.com/api/v2/get_campaigns?_page=1&_per=50";
		String offerJosn = HttpUtil.execCurl(apiurl, header);
		JSONObject offersObject = JSON.parseObject(offerJosn);
		
		System.out.println(offersObject);
		
		JSONArray offersArray = JSON.parseArray(offersObject.get("campaigns")
				+ "");
		for (int i = 0; i < offersArray.size(); i++) {
			JSONObject item = offersArray.getJSONObject(i);
			String name = item.getString("campaign_name");
			JSONArray itemArray = (JSONArray) item.get("items");
			String actionsStr = itemArray.get(0).toString();
			JSONObject actionsObj = JSON.parseObject(actionsStr);
			JSONArray unit_price_usdObject = (JSONArray) actionsObj
					.get("actions");
			Float payout = JSON.parseObject(
					unit_price_usdObject.get(0).toString()).getFloat(
					"unit_price_usd");

			String requirement = JSON.parseObject(
					unit_price_usdObject.get(0).toString()).getString(
					"requirement");

			String pkg = actionsObj.getString("bundle").replace("id", "");
			String offerId = actionsObj.getString("id");
			String platform = actionsObj.getString("os");
			String os_str = null;
			if (platform.toUpperCase().equals("IOS")) {
				os_str = "1";
			} else if (platform.toUpperCase().equals("ANDROID")) {
				os_str = "0";
			} else {
				continue;
			}
			Integer cap = actionsObj.getInteger("campaign_budget_daily");
			if (cap == null) {
				cap = 100;
			}
			String note = actionsObj.getString("note");
			String store_url = actionsObj.getString("store_url");
		}

	}
}
