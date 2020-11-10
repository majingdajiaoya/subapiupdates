package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

public class PullAdviewMobsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAdviewMobsOfferService.class);

	private Advertisers advertisers;

	public PullAdviewMobsOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullAdviewMobsOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Adview Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.CuttGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);

				String status = jsonPullObject.getString("success");
				Integer recordsTotal = jsonPullObject.getInteger("total_offers_num");

				if (!"true".equals(status)) {

					logger.info("status is not success return. status := " + status);

					return;
				}

				logger.info("recordsTotal:= " + recordsTotal);
				if (recordsTotal == null || recordsTotal == 0) {

					logger.info("available or  count is empty. ");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("offers");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							String roffer_id = item.getString("campid");// 104573764

							String performance_criteria = item.getString("performance_criteria");// 104573764

							Integer device_type = null;
							device_type = 0;

							String offer_model = item.getString("price_model").toUpperCase();// "CPI"

							String platform = item.getString("platform");//
							JSONArray countriesArray = null;
							String os_str = "";

							String min_os_version = "";
							JSONObject targeting = item.getJSONObject("target_info");
							if ("android".equals(os_str.toLowerCase())) {
								os_str = "0";
							} else if ("ios".equals(os_str.toLowerCase())) {
								os_str = "1";
							}

							String name = item.getString("offer_name");// "Legacy of Discord-FuriousWings"
							String icon = item.getString("icon_link");
							Integer daily_cap = item.getInteger("daily_cap");// 9992
							String link = item.getString("tracking_link");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							// &gaid={gaid}&idfa={idfa}&ptid={aff_sub}&siteid={sub_affid}
							// https://adview.hotrk0.com/offer?offer_id=91235&aff_id=8&aff_sub=[click_id]&aff_pub=[source]&advertising_id=[advertising_id]
							link = link.replace("[click_id]", "{aff_sub}").replace("[source]", "{sub_affid}").replace("[advertising_id]", "{gaid}").replace("[idfa]", "{idfa}");

							String pkg = item.getString("app_id");
							Float payout = item.getFloat("price");// 0.31
							String countries = item.getString("geo");// 0.31
							countries = countries.replace(",", ":");

							String preview_link = item.getString("preview_link");

							if (roffer_id == null || payout == null || countries == null || OverseaStringUtil.isBlank(link) || OverseaStringUtil.isBlank(offer_model)) {

								continue;
							}

							// CPI类型，单价太低(0.06)，舍弃不入库
							if ("CPI".equals(offer_model) && payout < 0.06) {

								continue;
							}
							// CPI类型，pkg为空，舍弃不入库
							if ("CPI".equals(offer_model) && OverseaStringUtil.isBlank(pkg)) {

								continue;
							}

							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
							advertisersOffer.setName(name);

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
							advertisersOffer.setPkg(pkg);
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_link);
							advertisersOffer.setClick_url(link);
							advertisersOffer.setCountry(countries);
							advertisersOffer.setDaily_cap(daily_cap);
							advertisersOffer.setPayout(payout);
							if ("CPI".equals(offer_model)) {

								advertisersOffer.setConversion_flow(101);
							} else {

								advertisersOffer.setConversion_flow(104);// 设置为其它类型
							}
							advertisersOffer.setDescription(filterEmoji(performance_criteria));
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(device_type);
							advertisersOffer.setOs_version(min_os_version);// 系统版本要求
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
		Advertisers tmp = new Advertisers();
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		// http://server.adview.com/api/public/campaigns?token=da98812b-8ae1-40db-baf4-e07bf1b62851

		// //http://server.adview.com/api/public/campaigns?token={apikey}
		// http://adview.hoapi0.com/v1?cid=adview&token=5dfc7d1b5b52446c9cc05afce43bb46e
		tmp.setApiurl("http://adview.hoapi0.com/v1?cid=adview&token=5dfc7d1b5b52446c9cc05afce43bb46e");
		tmp.setApikey("5dfc7d1b5b52446c9cc05afce43bb46e");
		// da98812b-8ae1-40db-baf4-e07bf1b62851
		tmp.setId(25L);

		// &{sid}={aff_sub}&{subid}={sub_affid}&gaid={gaid}&idfa={idfa}
		PullAdviewMobsOfferService mmm = new PullAdviewMobsOfferService(tmp);
		mmm.run();
	}

}
