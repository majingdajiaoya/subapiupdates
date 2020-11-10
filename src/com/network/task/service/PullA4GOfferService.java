package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
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

public class PullA4GOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullA4GOfferService.class);

	private Advertisers advertisers;

	public PullA4GOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullA4GOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	@SuppressWarnings("unchecked")
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullA4GOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				apiurl = apiurl.replace("{apikey}", apikey);
				String str = HttpUtil.sendGet(apiurl);
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONObject offersObject = jsonPullObject.getJSONObject("Offers");
				Map<String, JSONObject> itemMap = JSONObject.toJavaObject(offersObject, Map.class);

				if (itemMap != null && itemMap.size() > 0) {
					for (String offerid : itemMap.keySet()) {
						JSONObject offerItem = itemMap.get(offerid);
						String Platform = offerItem.getString("Platform").toUpperCase();
						if (!Platform.contains("IOS") && !Platform.contains("ANDROID")) {
							continue;
						}

						String name = offerItem.getString("OfferName");
						String icon = offerItem.getString("PreviewImage");
						String preview_url = offerItem.getString("PreviewUrl");
						String TrackingUrl = offerItem.getString("TrackingUrl");
						String pkg = offerItem.getString("ProductId");
						if (pkg == null || pkg.length() == 0) {
							continue;
						}
						String kpi = offerItem.getString("Restrictions");
						String Description = offerItem.getString("Description");
						Integer cap = offerItem.getInteger("DailyCap");
						Description = kpi + Description;

						String os_str = null;
						if (Platform.equals("IOS")) {
							os_str = "1";
							TrackingUrl = TrackingUrl + "&subid4={idfa}";
						}
						if (Platform.contains("ANDROID")) {
							os_str = "0";
							TrackingUrl = TrackingUrl + "&subid4={gaid}";
						}

						JSONArray countriesArray = offerItem.getJSONArray("countries");
						for (int i = 0; i < countriesArray.size(); i++) {
							String country = countriesArray.getJSONObject(i).getString("CountryName");
							Float payout = countriesArray.getJSONObject(i).getFloat("Rate");
							if (!country.equals("US") && !country.equals("JP")) {
								continue;
							}
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(country + offerid);
							advertisersOffer.setName(filterEmoji(name));
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(TrackingUrl);
							advertisersOffer.setCountry(country);
							advertisersOffer.setDaily_cap(cap);
							advertisersOffer.setPayout(Float.valueOf(payout));
							advertisersOffer.setCreatives(null);
							advertisersOffer.setConversion_flow(101);
							advertisersOffer.setDescription(filterEmoji(Description));
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(0);// 设置为mobile类型
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);

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
		String Platform="Android Smartphone & Tablet";
		Platform=Platform.toUpperCase();
		if(Platform.contains("ANDROID")){
			System.out.println("=================");
		}
		
		
		
	}

}
