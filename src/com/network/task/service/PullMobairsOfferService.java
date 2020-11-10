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

public class PullMobairsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullMobairsOfferService.class);

	private Advertisers advertisers;

	public PullMobairsOfferService(Advertisers advertisers) {

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
				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

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
				if (advertisersId != null && advertisersOfferList != null
						&& advertisersOfferList.size() > 0) {

					PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
							.getBean("pullOfferCommonService");

					pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
				}

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	private List<AdvertisersOffer> GetAll(String apiurl, int os, Long advertisersId) {
		List<AdvertisersOffer> advertisersOfferList = null;
		try {
			advertisersOfferList = new ArrayList<AdvertisersOffer>();
			if (os == 0) {
				apiurl = apiurl.replace("{os}", "ANDROID");
			} else if (os == 1) {
				apiurl = apiurl.replace("{os}", "IOS");
			}
			String str = HttpUtil.sendGet(apiurl);
			JSONObject jsonPullObject = JSON.parseObject(str);

			String status = jsonPullObject.getString("status");
			if (!"OK".equals(status)) {

				logger.info("status is not ok return. status := " + status);

				return advertisersOfferList;
			}
			JSONArray offersArray = jsonPullObject.getJSONArray("ads");

			if (offersArray != null && offersArray.size() > 0) {

				logger.info("begin pull offer " + offersArray.size());

				for (int i = 0; i < offersArray.size(); i++) {

					JSONObject item = offersArray.getJSONObject(i);

					if (item != null) {

						Integer roffer_id = item.getInteger("offerid");// 104573764
						Integer device_type = null;
						device_type = 0;
						String offer_model = item.getString("payout_type");// "CPI"
						String traffic_type = item.getString("incent");

						Integer incetive = null;// 是否激励(0:非激励 1:激励)
						if ("no".equals(traffic_type)) {

							incetive = 0;
						} else if ("yes".equals(traffic_type)) {

							incetive = 1;
						}
						String countriesArray = item.getString("countries");
						String min_os_version = null;
						JSONArray include_min_os_version = item
								.getJSONArray("include_min_os_version");
						if (include_min_os_version != null && include_min_os_version.size() > 0) {
							min_os_version = include_min_os_version.getString(0);
						}
						String name = item.getString("title");// "Legacy of Discord-FuriousWings"
						String app_id = item.getString("appid");// "com.gtarcade.lod"
						String description = item.getString("description");
						String icon = item.getString("icon");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"
						Integer caps_daily_remaining = 1000;
						String link = item.getString("clickurl");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
						link = link.replace("[cid]", "{aff_sub}");
						link = link.replace("[gaid]", "{gaid}");
						link = link.replace("[device_id]", "{andid}");
						link = link.replace("[affsub1]", "{channels}");
						link = link.replace("[idfa]", "{idfa}");
						link = link.replace("[data1]", "");
						link = link.replace("[data2]", "");
						link = link.replace("[data3]", "");
						link = link.replace("[data4]", "");
						Float payout = item.getFloat("payout");// 0.31
						String countries_str = countriesArray.replace(",", ":");
						if (roffer_id == null || payout == null || countriesArray == null
								|| OverseaStringUtil.isBlank(link)
								|| OverseaStringUtil.isBlank(offer_model)) {

							continue;
						}

						offer_model = offer_model.toUpperCase();

						// CPI类型，单价太低(0.06)，舍弃不入库
						if ("CPI".equals(offer_model) && payout < 0.06) {

							continue;
						}
						// CPI类型，pkg为空，舍弃不入库
						if ("CPI".equals(offer_model) && OverseaStringUtil.isBlank(app_id)) {

							continue;
						}

						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
						advertisersOffer.setName(filterEmoji(name));

						if ("CPI".equals(offer_model)) {

							advertisersOffer.setCost_type(101);

							advertisersOffer.setOffer_type(101);
						} else if ("CPA".equals(offer_model)) {

							advertisersOffer.setCost_type(102);

							advertisersOffer.setOffer_type(102);// 订阅类型
						} else {

							advertisersOffer.setCost_type(104);

							advertisersOffer.setOffer_type(104);// 设置为其它类型
						}

						advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
						advertisersOffer.setPkg(app_id);
						// advertisersOffer.setpkg_size();
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url("");
						advertisersOffer.setClick_url(link);

						advertisersOffer.setCountry(countries_str);

						// advertisersOffer.setEcountry(ecountry);

						advertisersOffer.setDaily_cap(caps_daily_remaining);
						// advertisersOffer.setsend_cap();
						advertisersOffer.setPayout(payout);
						// advertisersOffer.setExpiration(category);
						advertisersOffer.setCreatives(null);
						if ("CPI".equals(offer_model)) {

							advertisersOffer.setConversion_flow(101);
						} else {

							advertisersOffer.setConversion_flow(104);// 设置为其它类型
						}
						// advertisersOffer.setSupported_carriers("");
						advertisersOffer.setDescription(filterEmoji(description));
						advertisersOffer.setOs(os+"");

						advertisersOffer.setDevice_type(device_type);
						advertisersOffer.setOs_version(min_os_version);// 系统版本要求
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						advertisersOffer.setIncent_type(incetive);// 是否激励
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态

						advertisersOfferList.add(advertisersOffer);
					}
				}
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		tmp.setApiurl("http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL");
		tmp.setApikey("IN6ZZJR8xX0tG7cI7RaBsc9w4Ea5x8A4");
		tmp.setId(20L);

		PullMobairsOfferService mmm = new PullMobairsOfferService(tmp);
		mmm.run();
	}

}
