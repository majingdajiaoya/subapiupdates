package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

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

public class PullmobimaxOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullmobimaxOfferService.class);

	private Advertisers advertisers;

	public PullmobimaxOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullmobimaxOfferService广告信息，并入临时表中
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

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.sendGet(apiurl);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONArray offersArray = jsonPullObject.getJSONArray("ads");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject offerItem = offersArray.getJSONObject(i);
							if (offerItem != null) {
								String offerid = offerItem.getString(("adid"));
								String name = offerItem.getString(("title"));
								String trackinglink = offerItem.getString(("click_url"));
								String preview_url = offerItem.getString(("url"));
								String kpi = offerItem.getString(("kpi"));
								Float payout = offerItem.getFloat(("price"));
								Integer cap = 999;
								String icon = offerItem.getString("icon");
								String pkg = offerItem.getString("pkg");
								String country = offerItem.getString("country");
								String os = "0";
								if (preview_url.contains("google")) {
									os = "0";
								} else if (preview_url.contains("apple")) {
									os = "1";
								} else {
									continue;
								}
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerid);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(preview_url);
								advertisersOffer.setClick_url(trackinglink);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(cap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(kpi));
								advertisersOffer.setOs(os);
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

	public static void main(String[] args) throws ClientProtocolException, IOException {
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		String apiurl = "http://api.mobimax.cn/api_offline?appid=6093&time=1555565777&sign=9854e6df390d2ec4e8bb8647ca96f2c1";
		String str = HttpUtil.sendGet(apiurl);
		JSONObject jsonPullObject = JSON.parseObject(str);
		JSONArray offersArray = jsonPullObject.getJSONArray("ads");
		if (offersArray != null && offersArray.size() > 0) {
			logger.info("begin pull offer " + offersArray.size());
			for (int i = 0; i < offersArray.size(); i++) {
				JSONObject offerItem = offersArray.getJSONObject(i);
				String status = offerItem.getString(("status"));
				String offerid = offerItem.getString(("adid"));
				String name = offerItem.getString(("title"));
				String trackinglink = offerItem.getString(("click_url"));
				String preview_url = offerItem.getString(("url"));
				String kpi = offerItem.getString(("kpi"));
				Float payout = offerItem.getFloat(("price"));
				Integer cap = offerItem.getInteger(("daily_cap"));
				String icon = offerItem.getString("icon");
				String pkg = offerItem.getString("pkg");
				String country = offerItem.getString("country");
				String os = "0";
				if (preview_url.contains("google")) {
					os = "0";
				} else if (preview_url.contains("apple")) {
					os = "1";
				} else {
					continue;
				}

			}
		}

//		 Advertisers tmp = new Advertisers();
//		 tmp.setApiurl("http://api.mobimax.cn/api_offline?appid=6093&time=1555565777&sign=9854e6df390d2ec4e8bb8647ca96f2c1");
//		 tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
//		 tmp.setId(1069L);
//		 PullmobimaxOfferService mmm = new PullmobimaxOfferService(tmp);
//		 mmm.run();
	}

}
