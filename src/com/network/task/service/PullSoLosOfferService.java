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

public class PullSoLosOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullSoLosOfferService.class);

	private Advertisers advertisers;

	public PullSoLosOfferService(Advertisers advertisers) {

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

			logger.info("doPull SOLO Offer begin := " + new Date());

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

				String status = jsonPullObject.getString("Statuscode");
				// Integer available = jsonPullObject.getInteger("available");
				// Integer pageindex = jsonPullObject.getInteger("pageindex");
				// Integer pagecount = jsonPullObject.getInteger("pagecount");

				Integer count = jsonPullObject.getInteger("total_count");

				if (!"200".equals(status)) {

					logger.info("status is not ok return. status := " + status);

					return;
				}

				if (count == null || count == 0) {

					logger.info("available or  count is empty. " + " count:=" + count);

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");

				if (offersArray != null && offersArray.size() > 0) {
					logger.info("begin pull offer " + offersArray.size());
					for (int i = 0; i < offersArray.size(); i++) {
						JSONObject offerItem = offersArray.getJSONObject(i);
						String name = offerItem.getString(("campaign_name"));
						String offerid = offerItem.getString(("id"));
						Float payout = offerItem.getFloat(("payout"));
						// JP KR HK TW，1
						if (payout <= 1) {
							continue;
						}
						Integer day_cap = offerItem.getInteger(("day_cap"));
						String tracklink = offerItem.getString(("tracking_link"));
						tracklink = tracklink.replace("&app_name=", "&app_name={app_name}");
						JSONObject note = offerItem.getJSONObject("note");
						String kpi = note.getString("kpi");
						JSONObject targeting = offerItem.getJSONObject("targeting");
						String countries = targeting.getString("geo");
						countries = countries.replace("[\"", "");
						countries = countries.replace("\"]", "");
						countries = countries.replace("\"", "");
						countries = countries.replace(",", ":");
						String platform = targeting.getString("platform").toUpperCase();
						JSONObject product_info = offerItem.getJSONObject("product_info");
						String icon_url = product_info.getString("icon_url");
						String previewlink = product_info.getString("preview_link");
						String pkg = product_info.getString("package_id");

						String os_str = "0";
						if (platform.contains("ANDROID")) {
							os_str = "0";
						} else if (platform.contains("IOS")) {
							os_str = "1";
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
						advertisersOffer.setMain_icon(icon_url);
						advertisersOffer.setPreview_url(previewlink);
						advertisersOffer.setClick_url(tracklink);
						advertisersOffer.setCountry(countries);
						advertisersOffer.setDaily_cap(day_cap);
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
		// List<AdvertisersOffer> advertisersOfferList = new
		// ArrayList<AdvertisersOffer>();
		// String apiurl =
		// "http://pspm.pingstart.com/api/v2/campaigns?token=3af5f6ff-465e-4317-9d29-32aacc494c69&publisher_id=1880";
		// String str = HttpUtil.sendGet(apiurl);
		// JSONObject jsonPullObject = JSON.parseObject(str);
		// JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");
		// if (offersArray != null && offersArray.size() > 0) {
		// logger.info("begin pull offer " + offersArray.size());
		// for (int i = 0; i < offersArray.size(); i++) {
		// JSONObject offerItem = offersArray.getJSONObject(i);
		// String name = offerItem.getString(("campaign_name"));
		// String id = offerItem.getString(("id"));
		// String payout = offerItem.getString(("payout"));
		// String day_cap = offerItem.getString(("day_cap"));
		// String tracking_link = offerItem.getString(("tracking_link"));
		// tracking_link = tracking_link.replace("&app_name=",
		// "&app_name={app_name}");
		// JSONObject note = offerItem.getJSONObject("note");
		// String kpi = note.getString("kpi");
		// JSONObject targeting = offerItem.getJSONObject("targeting");
		// String country = targeting.getString("geo");
		// country = country.replace("[\"", "");
		// country = country.replace("\"]", "");
		// country = country.replace("\"", "");
		// country = country.replace(",", ":");
		// String platform = targeting.getString("platform").toUpperCase();
		// JSONObject product_info = offerItem.getJSONObject("product_info");
		// String icon_url = product_info.getString("icon_url");
		// String preview_link = product_info.getString("preview_link");
		// String package_id = product_info.getString("package_id");
		//
		// }
		// }

		Advertisers tmp = new Advertisers();
		tmp.setApiurl("https://api.ads.newborntown.com/api/v2/campaigns?token=3af5f6ff-465e-4317-9d29-32aacc494c69&publisher_id=1880");
		tmp.setApikey("2a82ee3e-f8db-485f-bea3-83fa8a7ec4db");
		tmp.setId(19L);

		PullSoLosOfferService mmm = new PullSoLosOfferService(tmp);
		mmm.run();
	}

}
