package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
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

public class PullZucksAffiliateOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullZucksAffiliateOfferService.class);

	private Advertisers advertisers;

	public PullZucksAffiliateOfferService(Advertisers advertisers) {

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

			logger.info("doPull Mobairs Android Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();
				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();
				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				List<AdvertisersOffer> adsList_ios = GetAll(apiurl, 1, advertisersId);
				List<AdvertisersOffer> adsList_android = GetAll(apiurl, 0, advertisersId);
				advertisersOfferList.addAll(adsList_ios);
				advertisersOfferList.addAll(adsList_android);
				logger.info("after filter pull offer size := " + advertisersOfferList.size());

				// 入网盟广告
				if (advertisersId != null && advertisersOfferList != null && advertisersOfferList.size() > 0) {

					PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper.getBean("pullOfferCommonService");

					pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
				}

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	private List<AdvertisersOffer> GetAll(String apiurl, int os, Long advertisersId) throws ClientProtocolException, IOException {
		List<AdvertisersOffer> advertisersOfferList = null;
		advertisersOfferList = new ArrayList<AdvertisersOffer>();
		if (os == 0) {
			apiurl = apiurl.replace("{os}", "android");
		} else if (os == 1) {
			apiurl = apiurl.replace("{os}", "ios");
		}
		String str = HttpUtil.sendGet(apiurl);
		JSONObject jsonPullObject = JSON.parseObject(str);
		System.out.println(jsonPullObject);
		JSONObject adSearch = jsonPullObject.getJSONObject("adSearch");
		JSONObject ads = adSearch.getJSONObject("ads");
		JSONArray offersArray = ads.getJSONArray("ad");
		if (offersArray != null && offersArray.size() > 0) {

			logger.info("begin pull offer " + offersArray.size());

			for (int i = 0; i < offersArray.size(); i++) {

				JSONObject item = offersArray.getJSONObject(i);
				// if
				// (!item.getString("conversion_mode").toUpperCase().equals("CPI"))
				// {
				// continue;
				// }

				String offerId = item.getString("acode");
				String name = item.getString("title");
				String url = item.getString("url");
				String redirecter = item.getString("redirecter");

				String pkg = item.getString("app_identifier");
				String icon = item.getString("img");
				String country = item.getString("country").toUpperCase();
				String description = item.getString("description");
				String result_condition = item.getString("result_condition");
				description = description + result_condition;
				Integer daily_cap = item.getInteger("remaining_conversion_count");
				Float payout = item.getFloat("unit_price");
				if (daily_cap == null) {
					daily_cap = 500;
				}
				if (pkg == null || pkg.length() == 0) {
					continue;
				}
				String app_name = "";
				List<String> app_name_list = new ArrayList<String>();
				String Previewlink = "";
				if (os == 0) {
					Previewlink = "https://play.google.com/store/apps/details?id=" + pkg;
					app_name_list.add("com.mt.mtxx.mtxx");
					app_name_list.add("com.meitu.poster");
					app_name_list.add("com.meitu.wheecam");
					app_name_list.add("vStudio.Android.Camera360");
					app_name_list.add("camera360.lite.beauty.selfie.camera");
					app_name_list.add("com.quvideo.xiaoying");
					app_name_list.add("com.wepie.snakeoff");
					app_name_list.add("com.leo.appmaster");
					app_name = app_name_list.get(RandomUtils.nextInt(7));
				} else {
					Previewlink = "https://itunes.apple.com/jp/app/id" + pkg + "?mt=8";
					app_name_list.add("id416048305");
					app_name_list.add("id875654777");
					app_name_list.add("id1014277964");
					app_name_list.add("id443354861");
					app_name_list.add("id545623778");
					app_name_list.add("id950519698");
					app_name_list.add("id1085015907");
					app_name_list.add("id1152952778");
					app_name_list.add("id1327137702");
					app_name = app_name_list.get(RandomUtils.nextInt(8));
				}

				AdvertisersOffer advertisersOffer = new AdvertisersOffer();

				advertisersOffer.setAdv_offer_id(offerId);
				advertisersOffer.setName(filterEmoji(name));
				advertisersOffer.setCost_type(101);
				advertisersOffer.setOffer_type(101);
				advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
				advertisersOffer.setPkg(pkg);
				// advertisersOffer.setpkg_size();
				advertisersOffer.setMain_icon(icon);
				advertisersOffer.setPreview_url(Previewlink);
				redirecter = redirecter + "&app_id={app_name}";
				redirecter = redirecter.replace("{app_name}", app_name);
				advertisersOffer.setClick_url(redirecter);
				advertisersOffer.setCountry(country);
				advertisersOffer.setDaily_cap(daily_cap);
				// advertisersOffer.setsend_cap();
				advertisersOffer.setPayout(payout);
				// advertisersOffer.setExpiration(category);
				advertisersOffer.setCreatives(null);
				advertisersOffer.setConversion_flow(101);
				// advertisersOffer.setSupported_carriers("");
				advertisersOffer.setDescription(filterEmoji(description));
				advertisersOffer.setOs(os + "");
				advertisersOffer.setSend_type(0);// 系统入库生成广告
				advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
				advertisersOffer.setStatus(0);// 设置为激活状态
				advertisersOfferList.add(advertisersOffer);
			}
		}
		return advertisersOfferList;
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
		// {apikey}
		// http://offer.wevemob.com/api?userId=95&token=zYb0W0amU32vU7gn
		tmp.setApiurl("https://get.mobu.jp/api/ads/3.3/?pcode=cappumedia&device={os}&count=100");
		tmp.setApikey("zYb0W0amU32vU7gn");
		tmp.setId(25L);

		PullZucksAffiliateOfferService mmm = new PullZucksAffiliateOfferService(tmp);
		mmm.run();

	}

}
