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

public class PullMISKYKROfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullMISKYKROfferService.class);

	private Advertisers advertisers;

	public PullMISKYKROfferService(Advertisers advertisers) {

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
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject offerItem = offersArray.getJSONObject(i);
							if (offerItem != null) {
								String active = offerItem.getString("status");
								if (!active.contains("active")) {
									continue;
								}
								String offerid = offerItem.getString("id");
								String name = offerItem.getString("name");
								String previewlink = offerItem.getString("preview_url");
								String tracking_link = offerItem.getString("tracking_link");
								String icon_url = offerItem.getString("icon_url");
								String description = offerItem.getString("description");
								// payouts
								JSONObject payouts = offerItem.getJSONObject("payouts");
								if (payouts.getString("KR") != null) {
								} else {
									continue;
								}
								JSONArray devices = offerItem.getJSONArray("devices");
								JSONObject dev = (JSONObject) devices.get(0);
								String platform = dev.getString("name");
								String pkg = "";
								if (previewlink.contains("itunes.apple.com")) {
									pkg = previewlink.substring(previewlink.indexOf("/id") + 3);
								} else if (previewlink.contains("play.google.com")) {
									pkg = previewlink.substring(previewlink.indexOf("id=") + 3);
								} else {
									continue;
								}
								if (pkg.indexOf("?") >= 0) {
									pkg = pkg.substring(0, pkg.indexOf("?"));
								}
								if (pkg.indexOf("&") >= 0) {
									pkg = pkg.substring(0, pkg.indexOf("&"));
								}
								String os_str = null;
								if (platform.toUpperCase().equals("IOS")) {
									os_str = "1";
								} else if (platform.toUpperCase().equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								Float payout = payouts.getFloat("KR");
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerid);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon_url);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracking_link);
								advertisersOffer.setCountry("US");
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
		// 定义初始化变量
		JSONObject json1;
		JSONArray array = null;
		try {
			logger.info("get avazu offer start");
			String str = HttpUtil
					.sendGet("http://api.c.avazutracking.net/performance/v2/getcampaigns.php?uid=24619&sourceid=28460&policy=1,2,3,4&pagesize=10000&country=KR,JP");
			System.out.println(str);
			json1 = JSON.parseObject(str);
			array = json1.getJSONArray("campaigns");
			if (array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					JSONObject item = array.getJSONObject(i);
					if (item != null) {
						JSONArray lpsArray = item.getJSONArray("lps");
						JSONObject ad = lpsArray.getJSONObject(0);
						String countrys = ad.getString("country");
						countrys = countrys.replace("|", ":");
						if (countrys.length() == 0 || countrys.length() > 20) {
							continue;
						}
						String offerid = ad.getString("lpid");
						String name = ad.getString("lpname");
						String previewlink = ad.getString("previewlink");
						if (previewlink.length() == 0) {
							continue;
						}
						if (!previewlink.contains("play.google.com")
								&& !previewlink.contains("itunes.apple.com")) {
							continue;
						}
						String pkg = ad.getString("pkgname");
						// dv1={aff_sub}&sub_pub={channel}
						String trackinglink = ad.getString("trackinglink");
						Double payout = ad.getDouble("payout");
						String description = item.getString("description");
						System.out.println(pkg);
					}
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
