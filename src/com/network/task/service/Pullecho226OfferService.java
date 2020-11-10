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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class Pullecho226OfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(Pullecho226OfferService.class);

	private Advertisers advertisers;

	public Pullecho226OfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Pullecho226OfferService广告信息，并入临时表中
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
				JSONArray offersArray = jsonPullObject.getJSONArray("rows");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject offerItem = offersArray.getJSONObject(i);
							String preview_url = offerItem.getString(("previewUrl"));
							String pkg = TuringStringUtil.getpkg(preview_url);
							String applicationStatus = offerItem.getString(("applicationStatus"));
							if (applicationStatus.contains("Approved")) {
								if (pkg != null && pkg.length() > 0) {
									String kpi = offerItem.getString(("termsAndConditions"));
									String offerid = offerItem.getString(("id"));
									String name = offerItem.getString(("name"));
									String trackinglink = offerItem.getString(("trackingLink"));
									// &sub_id={aff_sub}&sub_id2={channel}&sub_id5={app_name}&sub_id4={app_name}
									trackinglink = trackinglink.replace("&sub_id={aff_sub}", "").replace("&sub_id4={app_name}", "").replace("&sub_id2={channel}", "").replace("&sub_id5={app_name}", "");
									Float payout = offerItem.getFloat(("payout"));
									Integer cap = 50;
									if (offerItem.getInteger(("dailyCapping")) != null) {
										cap = offerItem.getInteger(("dailyCapping"));
									}
									String platform = offerItem.getString("os");
									String os_str = "0";
									if (preview_url.contains("google")) {
										os_str = "0";
										trackinglink = trackinglink.replace("&sub_id3={gaid/idfa}", "&sub_id3={gaid}");
									} else if (preview_url.contains("apple")) {
										os_str = "1";
										trackinglink = trackinglink.replace("&sub_id3={gaid/idfa}", "&sub_id3={idfa}");
									} else {
										continue;
									}
									String icon = offerItem.getString("icon");
									String country = offerItem.getString("countries").toUpperCase().replace("|", ":");
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
									advertisersOffer.setOs(os_str);
									advertisersOffer.setDevice_type(0);// 设置为mobile类型
									advertisersOffer.setSend_type(0);// 系统入库生成广告
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
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		String apiurl = "http://leverage.echo226.com/2015-03-01/bulk?affiliate=75438086&auth=4b3994c5e25149dcbbeaa791276b54cf&applicationStatus=Approved";
		String str = HttpUtil.sendGet(apiurl);
		JSONObject jsonPullObject = JSON.parseObject(str);
		JSONArray offersArray = jsonPullObject.getJSONArray("rows");
		logger.info("begin pull offer " + offersArray.size());
		for (int i = 0; i < offersArray.size(); i++) {
			JSONObject offerItem = offersArray.getJSONObject(i);
			String preview_url = offerItem.getString(("previewUrl"));
			String pkg = TuringStringUtil.getpkg(preview_url);
			String applicationStatus = offerItem.getString(("applicationStatus"));
			if (applicationStatus.contains("Approved")) {
				if (pkg != null && pkg.length() > 0) {
					String des = offerItem.getString(("termsAndConditions"));
					String offerid = offerItem.getString(("id"));
					String name = offerItem.getString(("name"));
					String trackinglink = offerItem.getString(("trackingLink"));
					// &sub_id={aff_sub}&sub_id2={channel}&sub_id5={app_name}&sub_id4={app_name}
					trackinglink = trackinglink.replace("&sub_id={aff_sub}", "").replace("&sub_id4={app_name}", "").replace("&sub_id2={channel}", "").replace("&sub_id5={app_name}", "");
					Float payout = offerItem.getFloat(("payout"));
					Integer cap = offerItem.getInteger(("dailyCapping"));
					String platform = offerItem.getString("os");
					String os_str = "0";
					if (preview_url.contains("google")) {
						os_str = "0";
						trackinglink = trackinglink.replace("&sub_id3={gaid/idfa}", "&sub_id3={gaid}");
					} else if (preview_url.contains("apple")) {
						os_str = "1";
						trackinglink = trackinglink.replace("&sub_id3={gaid/idfa}", "&sub_id3={idfa}");
					} else {
						continue;
					}
					String icon = offerItem.getString("icon");
					String country = offerItem.getString("countries").toUpperCase().replace("|", ":");
					System.out.println(platform);
					System.out.println(os_str);

				}

			}

		}

		// Advertisers tmp = new Advertisers();
		// tmp.setApiurl("http://ads.mobring.co/api?token=b99453d30229c00b70ac2dd8d5afb786");
		// tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		// tmp.setId(1069L);
		// Pullecho226OfferService mmm = new Pullecho226OfferService(tmp);
		// mmm.run();
	}

}
