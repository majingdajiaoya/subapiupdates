package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

public class CPA_ClickmobadOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(CPA_ClickmobadOfferService.class);

	private Advertisers advertisers;

	public CPA_ClickmobadOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取CPA_ClickmobadOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull CPA_ClickmobadOfferService Offer begin := " + new Date());

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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				JSONArray offersArray = jsonPullObject.getJSONArray("data");
				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {
							String payout_model = item.getString("payout_model");
							if (!payout_model.equals("CPA")) {
								continue;
							}
							String id = item.getString("id");
							String name = item.getString("name");
							Float payout = item.getFloat("payout");
							String preview_url = item.getString("preview_url");
							// 1 Click、2 Click、Pin Submit、MO
							// 转化类型(101:cpi, 102:click, 103:two click,104:Pin
							// Submit,105:MO)
							String conversion_flow = item.getString("conversion_flow");
							if (conversion_flow.equals("1 Click")) {
								conversion_flow = "102";
							} else if (conversion_flow.equals("2 Click")) {
								conversion_flow = "103";
							} else if (conversion_flow.equals("Pin Submit")) {
								conversion_flow = "104";
							} else if (conversion_flow.equals("MO")) {
								conversion_flow = "105";
							}
							String tracking_url = item.getString("tracking_url");
							
							logger.info("begin pull offer " + conversion_flow);
							// Integer roffer_id = item.getInteger("id");//
							// 104573764
							// Integer device_type = null;
							// device_type = 0;
							//
							//
							//
							// Integer incetive = 0;// 是否激励(0:非激励 1:激励)
							// String name = item.getString("title");//
							// "Legacy of Discord-FuriousWings"
							// String target_url =
							// item.getString("preview_url");//
							// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
							// if (null == target_url) {
							// continue;
							// }
							//
							// String app_id =
							// TuringStringUtil.getpkg(target_url);//
							// "com.gtarcade.lod"
							// String os_str =
							// TuringStringUtil.getPlatoms(target_url);
							//
							// JSONArray countriesArray =
							// item.getJSONArray("countries");
							//
							// String min_os_version = "";
							//
							// String description =
							// item.getString("description");
							// if (null == description) {
							// description = name;
							// }
							// try {
							// JSONObject kpi = item.getJSONObject("kpi");
							// if (kpi != null) {
							// if (kpi.containsKey("en")) {
							// String enn = kpi.getString("en");
							// if (null != enn && !enn.equals("")) {
							// description = enn + " " + description;
							// }
							// }
							// }
							// } catch (Exception e) {
							//
							// }
							//
							// // String category =
							// item.getString("appcategory");
							// String icon = item.getString("logo");//
							// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"
							//
							// Integer caps_daily_remaining = 200;
							//
							// try {
							// JSONArray caps = item.getJSONArray("caps");
							// if (caps != null && caps.size() > 0) {
							// JSONObject capitem = caps.getJSONObject(0);
							// if (capitem != null) {
							// String value = capitem.getString("value");
							// if (value != null) {
							// caps_daily_remaining = Integer.parseInt(value);
							// }
							// }
							// }
							// } catch (Exception e) {
							//
							// }
							//
							// String link = item.getString("link");//
							// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							//
							// Float payout = 0.0f;// 0.31
							//
							// JSONArray payments =
							// item.getJSONArray("payments");
							// if (payments != null && payments.size() > 0) {
							// JSONObject paymentsitem =
							// payments.getJSONObject(0);
							// String currency =
							// paymentsitem.getString("currency");
							// if (currency == null || (!currency.equals("usd")
							// && !currency.equals("eur"))) {
							// continue;
							// }
							// payout = paymentsitem.getFloat("revenue");
							//
							// } else {
							// continue;
							// }
							//
							// String countries_str = "";
							// if (countriesArray != null &&
							// countriesArray.size() > 0) {
							//
							// for (int k = 0; k < countriesArray.size(); k++) {
							//
							// String countriesItem =
							// countriesArray.getString(k);
							//
							// if (countriesItem != null) {
							//
							// countries_str += countriesItem.toUpperCase() +
							// ":";
							// }
							// }
							// } else {
							// continue;
							// }
							// // 去除最后一个:号
							// if (!OverseaStringUtil.isBlank(countries_str)) {
							//
							// countries_str = countries_str.substring(0,
							// countries_str.length() - 1);
							// }
							//
							// if (roffer_id == null || payout == null ||
							// countriesArray == null ||
							// OverseaStringUtil.isBlank(link) ||
							// OverseaStringUtil.isBlank(offer_model)) {
							//
							// continue;
							// }
							// String images_crative = null;
							//
							// offer_model = offer_model.toUpperCase();
							//
							// // CPI类型，单价太低(0.06)，舍弃不入库
							// if ("CPI".equals(offer_model) && payout < 0.06) {
							//
							// continue;
							// }
							// // CPI类型，pkg为空，舍弃不入库
							// if ("CPI".equals(offer_model) &&
							// OverseaStringUtil.isBlank(app_id)) {
							//
							// continue;
							// }
							//
							// if (payout < 0.2) {
							// continue;
							// }
							// AdvertisersOffer advertisersOffer = new
							// AdvertisersOffer();
							//
							// advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
							// advertisersOffer.setName(name);
							//
							// if ("CPI".equals(offer_model)) {
							//
							// advertisersOffer.setCost_type(101);
							//
							// advertisersOffer.setOffer_type(101);
							// } else if ("CPA".equals(offer_model)) {
							//
							// advertisersOffer.setCost_type(102);
							//
							// advertisersOffer.setOffer_type(102);// 订阅类型
							// } else {
							//
							// advertisersOffer.setCost_type(104);
							//
							// advertisersOffer.setOffer_type(104);// 设置为其它类型
							// }
							//
							// advertisersOffer.setAdvertisers_id(advertisersId);//
							// 网盟ID
							// advertisersOffer.setPkg(app_id);
							// // advertisersOffer.setpkg_size();
							// advertisersOffer.setMain_icon(icon);
							// advertisersOffer.setPreview_url(target_url);
							// advertisersOffer.setClick_url(link);
							//
							// advertisersOffer.setCountry(countries_str);
							//
							// // advertisersOffer.setEcountry(ecountry);
							//
							// advertisersOffer.setDaily_cap(caps_daily_remaining);
							// // advertisersOffer.setsend_cap();
							// advertisersOffer.setPayout(payout);
							// // advertisersOffer.setExpiration(category);
							// advertisersOffer.setCreatives(images_crative);
							// if ("CPI".equals(offer_model)) {
							//
							// advertisersOffer.setConversion_flow(101);
							// } else {
							//
							// advertisersOffer.setConversion_flow(104);//
							// 设置为其它类型
							// }
							// // advertisersOffer.setSupported_carriers("");
							// advertisersOffer.setDescription(filterEmoji(description));
							// advertisersOffer.setOs(os_str);
							//
							// advertisersOffer.setDevice_type(device_type);
							// advertisersOffer.setOs_version(min_os_version);//
							// 系统版本要求
							// advertisersOffer.setSend_type(0);// 系统入库生成广告
							// advertisersOffer.setIncent_type(incetive);// 是否激励
							// advertisersOffer.setSmartlink_type(1);//
							// 非Smartlink类型
							// advertisersOffer.setStatus(0);// 设置为激活状态
							//
							// advertisersOfferList.add(advertisersOffer);
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
		tmp.setApiurl("http://api.clickmobad.com/api/gateway/offers?token=77cf6d04439fd52a8e83c72528ef44f34f3b56e1&page=1&limit=15");
		tmp.setApikey("980726e43b06c20c12b878e3b1f49e3b9ae01fde");
		tmp.setId(1078L);
		CPA_ClickmobadOfferService mmm = new CPA_ClickmobadOfferService(tmp);
		mmm.run();
	}

}
