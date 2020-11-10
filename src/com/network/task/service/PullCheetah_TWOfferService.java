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
import common.Logger;

public class PullCheetah_TWOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullCheetah_TWOfferService.class);

	private Advertisers advertisers;

	public PullCheetah_TWOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullCheetah_TWOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullCheetah_TWOfferService Offer begin := " + new Date() + advertisers.getApiurl());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.sendGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);
				jsonPullObject = jsonPullObject.getJSONObject("data");
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {

								JSONObject offerItem = offersArray.getJSONObject(i);
								String offerId = offerItem.getString("id");
								String pkg = offerItem.getString("pkg_name");
								String description = offerItem.getString("description");
								String name = offerItem.getString("name");
								String previewlink = offerItem.getString("preview_url");
								JSONArray adSetsArray = offerItem.getJSONArray("adSets");
								JSONObject adSets = (JSONObject) adSetsArray.get(0);
								String platforms = adSets.getString("platform");
								JSONObject creativesJson = (JSONObject) adSets.getJSONArray("creatives").get(0);
								String icon = creativesJson.getString("icon");
								String click_url = creativesJson.getString("click_url");
								click_url = click_url.replace("{transaction_id}", "{aff_sub}").replace("{advertising_id}", "{gaid}").replace("{pub_id}", "{pub_id}").replace("{sub_id}", "{channel}").replace("{creative_name}", "{app_name}");
								;

								JSONObject cappingsJson = (JSONObject) adSets.getJSONArray("cappings").get(0);
								String country = cappingsJson.getString("country");
								country = country.replace("|", ":");
								Float payout = cappingsJson.getFloat("pay_out");

								String os_str = "";
								if (platforms.toUpperCase().equals("IOS")) {
									os_str = "1";
								} else if (platforms.toUpperCase().equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								logger.info("offerId " + offerId);
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerId);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);

								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(click_url);
								advertisersOffer.setCountry(country);
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
								advertisersOfferList.add(advertisersOffer);
							}

						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					logger.info("after filter pull offer size := " + advertisersOfferList.size());

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
		tmp.setApiurl("http://cheetahmobile.hoapi0.com/cmcm?cid=cheetahmobile&token=19297bc31ce44b5a845d1a61b73ed10a");
		tmp.setApikey("980726e43b06c20c12b878e3b1f49e3b9ae01fde");
		tmp.setId(1078L);
		PullCheetah_TWOfferService mmm = new PullCheetah_TWOfferService(tmp);
		mmm.run();
	}

}
