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

public class PullMobringOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullMobringOfferService.class);

	private Advertisers advertisers;

	public PullMobringOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullMobringOfferService广告信息，并入临时表中
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
				JSONArray offersArray = jsonPullObject.getJSONArray("data");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject offerItem = offersArray.getJSONObject(i);
							if (offerItem != null) {
								String status = offerItem.getString(("status"));
								if (status.equals("0")) {
									String offerid = offerItem.getString(("adid"));
									String name = offerItem.getString(("name"));
									String trackinglink = offerItem.getString(("click_url"));
									String preview_url = offerItem.getString(("preview_url"));
									String kpi = offerItem.getString(("kpi"));
									Float payout = offerItem.getFloat(("payout"));
									Integer cap = offerItem.getInteger(("daily_cap"));
									String platform = offerItem.getString("platform");
									String icon = offerItem.getString("icon");
									String pkg = offerItem.getString("pkg");
									String country = offerItem.getString("cn").toUpperCase().replace("|", ":");
									// 1CPI 2CPA 3CPS
									Integer bid_type = offerItem.getInteger("bid_type");
									Integer cost_type = 101;
									Integer offer_type = 101;
									if (bid_type == 2) {
										cost_type = 102;
										offer_type = 102;
									}
									if (bid_type == 3) {
										cost_type = 103;
										offer_type = 103;
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
									advertisersOffer.setOs(platform);
									advertisersOffer.setDevice_type(0);// 设置为mobile类型
									advertisersOffer.setSend_type(0);// 系统入库生成广告
									advertisersOffer.setCost_type(cost_type);
									advertisersOffer.setOffer_type(offer_type);
									advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
									advertisersOffer.setStatus(0);// 设置为激活状态
									advertisersOfferList.add(advertisersOffer);
								}
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
//		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
//		String apiurl = "http://ads.mobring.co/api?token=b99453d30229c00b70ac2dd8d5afb786";
//		String str = HttpUtil.sendGet(apiurl);
//		JSONObject jsonPullObject = JSON.parseObject(str);
//		JSONArray offersArray = jsonPullObject.getJSONArray("data");
//		if (offersArray != null && offersArray.size() > 0) {
//
//			logger.info("begin pull offer " + offersArray.size());
//
//			for (int i = 0; i < offersArray.size(); i++) {
//				JSONObject offerItem = offersArray.getJSONObject(i);
//				String status = offerItem.getString(("status"));
//				if (status.equals("0")) {
//					String offerid = offerItem.getString(("adid"));
//					String name = offerItem.getString(("name"));
//					String trackinglink = offerItem.getString(("click_url"));
//					String preview_url = offerItem.getString(("preview_url"));
//					String kpi = offerItem.getString(("kpi"));
//					Float payout = offerItem.getFloat(("payout"));
//					Integer cap = offerItem.getInteger(("daily_cap"));
//					String platform = offerItem.getString("platform");
//					String icon = offerItem.getString("icon");
//					String pkg = offerItem.getString("pkg");
//					String country = offerItem.getString("cn").toUpperCase().replace("|", ":");
//					// 1CPI 2CPA 3CPS
//					Integer bid_type = offerItem.getInteger("bid_type");
//					Integer cost_type = 101;
//					Integer offer_type = 101;
//					if (bid_type == 2) {
//						cost_type = 102;
//						offer_type = 102;
//					}
//					if (bid_type == 3) {
//						cost_type = 103;
//						offer_type = 103;
//					}
//
//				}
//
//			}
//		}
		
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("http://ads.mobring.co/api?token=b99453d30229c00b70ac2dd8d5afb786");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);
		PullMobringOfferService mmm = new PullMobringOfferService(tmp);
		mmm.run();
	}

}
