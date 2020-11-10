package com.network.task.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.Encrypt;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaMd5;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;

public class PullMovista2OfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullMovista2OfferService.class);

	private Advertisers advertisers;

	public PullMovista2OfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull Movista2  Offer begin := " + new Date());
			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				String offset = doPagePullOffer(advertisersOfferList, advertisersId, apiurl,
						apikey, null);
				if (offset != null) {
					for (int i = 0; i < 20; i++) {
						offset = doPagePullOffer(advertisersOfferList, advertisersId, apiurl,
								apikey, offset);
						if (offset == null) {
							break;
						}

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
			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	public static String doPagePullOffer(List<AdvertisersOffer> advertisersOfferList,
			Long advertisersId, String apiurl, String apikey, String offset) {
		String offsetyuanshi = null;
		try {

			if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
					|| OverseaStringUtil.isBlank(apikey)) {
				logger.info("advertisersId or apiurl or apikey is null return");
				return null;
			}
			apiurl = apiurl.replace("{apikey}", apikey);

			long time = System.currentTimeMillis() / 1000;
			String token = OverseaMd5.md5("NJIM9VACK41HAG10SHPY"
					+ OverseaMd5.md5(String.valueOf(time)));

			apiurl = apiurl + "&time=" + time + "&token=" + token;
			if (offset != null) {
				apiurl = apiurl + "&time=" + time + "&token=" + token + "&offset=" + offset;
			}

			String str = HttpUtil.sendGet(apiurl);

			JSONObject jsonPullObject = JSON.parseObject(str);
			int balance_offers_num = jsonPullObject.getIntValue("balance_offers_num");
			if (balance_offers_num <= 0) {
				return null;
			} else {
				offsetyuanshi = jsonPullObject.getString("offset");
			}

			JSONArray offersArr = jsonPullObject.getJSONArray("offers");

			if (offersArr == null || offersArr.size() == 0) {

				logger.info("offersArray is empty");

				return null;
			}

			logger.info("begin pull offer " + offersArr.size());

			for (int i = 0; i < offersArr.size(); i++) {

				JSONObject item = offersArr.getJSONObject(i);

				if (item != null) {

					Integer offer_id = item.getInteger("campid");
					String title = item.getString("offer_name");
					String icon_url = item.getString("icon_link");
					String pkg_name = item.getString("package_name");
					Float maxpayout = item.getFloat("price");
					String click_url = item.getString("tracking_link");
					String app_store_url = item.getString("preview_link");
					// String countries = item.getString("geo");
					String Incentbool = item.getString("traffic_source");

					String traffic_network = item.getString("traffic_network");
					Integer isIncent = 0;
					if ("non-incent".equals(Incentbool)) {
						isIncent = 0;
					} else {
						isIncent = 1;
					}

					String cover_url = item.getString("cover_url");
					JSONArray countryObject = item.getJSONArray("geo");
					String platforms = item.getString("platform");

					String min_os_version = item.getString("min_version");
					String description = "";
					description = item.getString("app_desc");
					if ("".equals(description)) {
						description = title;
					}

					String countries = "";
					if (countryObject != null && countryObject.size() > 0) {
						for (int k = 0; k < countryObject.size(); k++) {
							// JSONObject countryitem =
							// countryObject.getJSONObject(k);
							String countryitem = countryObject.getString(k);

							if (countryitem != null) {
								if ("".equals(countries)) {
									countries = countryitem;
								} else {
									countries = countries + ":" + countryitem;
								}
							}

						}

					}
					// http://pixel.admobclick.com/v1/ad/click?subsite_id=30829&
					// transaction_id={transaction_id}&id=1271&offer_id=142611501&geo={geo}&aid={aid}&client_version={client_version}&gaid={gaid}&tmark=1522295239141&p={p}&d=ZLABKDAFB
					//
					// click_url=click_url.replace("{transaction_id}",
					// "{aff_sub}");
					// click_url=click_url.replace("{p}", "");
					// click_url=click_url.replace("{aid}", "{andriod_id}");
					// click_url=click_url.replace("{geo}", "{country}");
					// click_url=click_url.replace("{client_version}", "");

					if (offer_id == null || OverseaStringUtil.isBlank(pkg_name)
							|| maxpayout == null || OverseaStringUtil.isBlank(countries)
							|| OverseaStringUtil.isBlank(click_url)) {

						logger.info("offer_id := " + offer_id + " or pkg_name := " + pkg_name
								+ " or maxpayout := " + maxpayout + " or countries := " + countries
								+ " or click_url := " + click_url + " is empty, return");

						continue;
					}

					if (maxpayout <= 0.06) {

						logger.info("payout is lower := " + maxpayout + " return");

						continue;
					}

					/**
					 * 特殊字段处理
					 */

					// countries = countries.replace(",", ":");

					// 处理平台(0:andriod:1:ios:2:pc)
					String os_str = "";
					if ("ANDROID".equals(platforms.toUpperCase())) {
						os_str = "0";
						click_url = click_url + "&idfa={gaid}";
					} else if ("IOS".equals(platforms.toUpperCase())) {
						click_url = click_url + "&idfa={idfa}";
						os_str = "1";
					}

					// 非激励量
					// Integer incetive = Integer.valueOf(0);
					String images_crative = null;
					JSONArray creatives = item.getJSONArray("creatives");
					if (creatives != null && creatives.size() > 0) {
						JSONArray images_JSONArray = new JSONArray();

						ArrayList<Map> arrayList = new ArrayList<Map>();

						for (int k = 0; k < creatives.size(); k++) {
							String imageurl = creatives.getString(k);
							if (arrayList.size() > 3) {
								break;
							}
							Map jsonObject = new HashMap();
							jsonObject.put("url", imageurl);
							jsonObject.put("size", "*");
							arrayList.add(jsonObject);
						}
						images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
						images_crative = images_JSONArray.toString();

					}
					String daily_cap = item.getString("daily_cap");
					int caps = 99999;
					if ("open cap".equals(daily_cap)) {

					} else {
						caps = Integer.valueOf(daily_cap);
						if (caps == 0) {
							continue;
						}

					}

					String price_model = item.getString("price_model");

					AdvertisersOffer advertisersOffer = new AdvertisersOffer();

					advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));// 以offer_id+geo组合做主键
					advertisersOffer.setName(title);

					if ("CPI".equals(price_model.toUpperCase())) {
						advertisersOffer.setCost_type(Integer.valueOf(101));
						advertisersOffer.setOffer_type(Integer.valueOf(101));
						advertisersOffer.setConversion_flow(Integer.valueOf(101));
					} else if ("CPA".equals(price_model.toUpperCase())) {
						advertisersOffer.setCost_type(Integer.valueOf(102));
						advertisersOffer.setOffer_type(Integer.valueOf(102));
						advertisersOffer.setConversion_flow(Integer.valueOf(102));
					} else {
						advertisersOffer.setCost_type(Integer.valueOf(104));
						advertisersOffer.setOffer_type(Integer.valueOf(104));
						advertisersOffer.setConversion_flow(Integer.valueOf(104));
					}

					advertisersOffer.setAdvertisers_id(advertisersId);

					advertisersOffer.setPkg(pkg_name);
					advertisersOffer.setMain_icon(icon_url);
					advertisersOffer.setPreview_url(app_store_url);
					advertisersOffer.setClick_url(click_url);

					advertisersOffer.setCountry(countries);

					advertisersOffer.setDaily_cap(caps);

					advertisersOffer.setPayout(maxpayout);

					advertisersOffer.setDescription(filterEmoji(description));
					advertisersOffer.setOs(os_str);
					advertisersOffer.setSupported_carriers(traffic_network);

					advertisersOffer.setCreatives(images_crative);
					advertisersOffer.setDevice_type(Integer.valueOf(0));
					advertisersOffer.setOs_version(min_os_version);
					advertisersOffer.setSend_type(Integer.valueOf(0));
					advertisersOffer.setIncent_type(isIncent);
					advertisersOffer.setSmartlink_type(Integer.valueOf(1));
					advertisersOffer.setStatus(Integer.valueOf(0));

					advertisersOfferList.add(advertisersOffer);
				}
			}

			// logger.info("after filter pull offer size := " +
			// advertisersOfferList.size());
			//
			// // 入网盟广告
			// if(advertisersId != null
			// && advertisersOfferList != null
			// && advertisersOfferList.size() > 0){
			//
			// PullOfferCommonService pullOfferCommonService
			// =
			// (PullOfferCommonService)TuringSpringHelper.getBean("pullOfferCommonService");
			//
			// pullOfferCommonService.doPullOffer(advertisers,
			// advertisersOfferList);
			// }
		} catch (Exception e) {

			e.printStackTrace();

			NetworkLog.exceptionLog(e);
		}

		return offsetyuanshi;
	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) throws ClientProtocolException, IOException {
		// 1535452492
		// 1535555204
		// https://open.3s.mobvista.com/channel/v5?client_key=13569&return_type=json&time=1535452492&token=7094f7c15cc2c669892826c411e659f17e2972a59d8c0fbe7d20ada0e1e7b4bb
		// client_key: 13569
		// client_secret_key: DQSEUFZE447BV9YAZUU1
		Encrypt Encrypt = new Encrypt();
		// 1535452492
		long time = System.currentTimeMillis() / 1000;
		// 13569
		String client_key = "13569";
		// DQSEUFZE447BV9YAZUU1
		String client_secret_key = "DQSEUFZE447BV9YAZUU1";
		String return_type = "json";
		String params = "client_key={client_key}&client_secret_key={client_secret_key}&return_type={return_type}&time={time}";
		params = params.replace("{time}", URLEncoder.encode(time + "", "utf-8"));
		params = params.replace("{client_key}", URLEncoder.encode(client_key, "utf-8"));
		params = params.replace("{client_secret_key}",
				URLEncoder.encode(client_secret_key, "utf-8"));
		params = params.replace("{return_type}", URLEncoder.encode(return_type, "utf-8"));
		System.out.println(params);
		params = Encrypt.SHA256(params);
		params = URLEncoder.encode(params, "utf-8");
		System.out.println(params);
		String apiurl = "https://open.3s.mobvista.com/channel/v5?client_key=13569&return_type=json&time={time}&token={token}";
		apiurl = apiurl.replace("{time}", time + "");
		apiurl = apiurl.replace("{token}", params);
		String str = HttpUtil.sendGet(apiurl);
		JSONObject jsonPullObject = JSON.parseObject(str);
		JSONArray offersArr = jsonPullObject.getJSONArray("data");
		for (int i = 0; i < offersArr.size(); i++) {
			JSONObject item = offersArr.getJSONObject(i);
			if (item != null) {
				String status =item.getString("status");
				if(!status.equals("running")){
					continue;
				}
				String campid =item.getString("campid");
				String app_name =item.getString("app_name");
				String platform =item.getString("platform").toUpperCase();
				String package_name =item.getString("package_name");
				String note =item.getString("note");
				String preview_link =item.getString("preview_link");
				String app_desc =item.getString("app_desc");
				String icon_link =item.getString("icon_link");
				String geo =item.getString("geo");
				geo=geo.replace("[\"", "");
				geo=geo.replace("\"]", "");
				geo=geo.replace(",", ":");
				Float payout =item.getFloat("price");
				Integer cap =item.getInteger("daily_cap");
				System.out.println(geo);
			}
		}
	}
}
