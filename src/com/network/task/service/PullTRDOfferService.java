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

public class PullTRDOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullTRDOfferService.class);

	private Advertisers advertisers;

	public PullTRDOfferService(Advertisers advertisers) {

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

				String str = HttpUtil
						.sendPost(apiurl,
								"{\"token\":\"10b156a8-14a2-4a74-b933-f49015d37559\",\"ssp_id\":\"Cappuccino_API\"}");
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				JSONArray offersArray = JSON.parseArray(str);

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);
							if (item != null) {
								String previewlink=item.getJSONObject("app_details").getString("app_store_url");
								String pkg=item.getJSONObject("app_details").getString("bundle_id");
								String platform=item.getJSONObject("app_details").getString("platform");
								String campaigndesc=item.getJSONObject("campaigns").getString("KPI");
								String offerId=item.getJSONObject("campaigns").getString("campaign_id");
								String country=item.getJSONObject("campaigns").getString("country");
								if(country.contains("KOR")){
									country="KR";
								}
								String payout=item.getJSONObject("campaigns").getString("points");
								JSONArray array1=JSON.parseArray(item.getString("native_creatives"));
								JSONObject item1=(JSONObject) array1.get(0);
								String icon=item1.getString("banner_url");
								String tracklink=item1.getString("click_url");
								String name=item1.getString("title");
								String os_str = null;
								if (platform.toUpperCase().equals("IOS")) {
									os_str = "0";
								} else if (platform.toUpperCase().equals("ANDROID")) {
									os_str = "1";
								} else {
									continue;
								}
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerId);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracklink);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(500);
								advertisersOffer.setPayout(Float.valueOf(payout));
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(campaigndesc));
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
