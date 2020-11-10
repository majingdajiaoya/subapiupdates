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
import com.network.task.util.DateUtil;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullAppLiftOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAppLiftOfferService.class);

	private Advertisers advertisers;

	public PullAppLiftOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Applift广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Applift Offer begin := " + new Date());

			try {
				
				String clicktime = DateUtil.getNowDataStr("mm");
				int minute = Integer.valueOf(clicktime);
				if( (minute >= 15&&minute<30) || (minute>=45&&minute < 59)){
					return;
				}

				Long advertisersId = advertisers.getId();
				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.CuttGet(apiurl);
				JSONObject obbte = JSON.parseObject(str);
				JSONArray resultsArray = obbte.getJSONArray("results");

				if (resultsArray == null || resultsArray.size() == 0) {

					logger.info("resultsArray is empty");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				if (resultsArray != null && resultsArray.size() > 0) {

					logger.info("begin pull offer " + resultsArray.size());
					try {
						for (int i = 0; i < resultsArray.size(); i++) {
							JSONObject resultsitem = resultsArray.getJSONObject(i);
							JSONObject app_details = resultsitem.getJSONObject("app_details");
							JSONArray offersArray = resultsitem.getJSONArray("offers");
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							//for (int a = 0; a < offersArray.size(); a++) 
							{
								JSONObject offersitem = offersArray.getJSONObject(0);
								if (offersitem != null) {
									String offer_id = offersitem.getString("offer_id");
									String offer_name = offersitem.getString("offer_name");
									String click_url = offersitem.getString("click_url");
									click_url = click_url.replace("{click_id}", "{aff_sub}");
									JSONArray goal_payouts = offersitem
											.getJSONArray("goal_payouts");
									Float payout = null;
									if (goal_payouts != null) {
										payout = goal_payouts.getJSONObject(0).getFloat("payout");
										payout = payout / 1000;
									}
									String platform = app_details.getString("platform");
									int os = 0;
									if (platform.toUpperCase().equals("ANDROID")) {
										os = 0;
									} else if (platform.toUpperCase().equals("IOS")) {
										os = 1;
									}
									String pkg = app_details.getString("bundle_id");
									String countries = "";
									JSONArray countriesArray = offersitem
											.getJSONArray("geo_targeting");
									if (countriesArray!=null&&countriesArray.size() > 0) {
										for (int b = 0; b < countriesArray.size(); b++) {
											countries = countries
													+ ":"
													+ countriesArray.getJSONObject(b).getString(
															"country_code");
										}
										countries = countries.substring(1, countries.length());
									} else {
										continue;
									}
									String icon = "";
									JSONArray creativesArray = offersitem.getJSONArray("creatives");
									for (int c = 0; c < creativesArray.size(); c++) {
										String creative_type = creativesArray.getJSONObject(c)
												.getString("creative_type");
										if (creative_type != null) {
											if (creative_type.equals("icon")) {
												icon = creativesArray.getJSONObject(c).getString(
														"url");
											}
										}
									}
									String preview_url = TuringStringUtil.getPriviewUrl(os + "",
											pkg);
									String goal_type = offersitem.getString("goal_type");
									String creatives = offersitem.getString("creatives");
									if (offer_id == null || payout == null
											|| countries.length() == 0
											|| OverseaStringUtil.isBlank(click_url)
											|| OverseaStringUtil.isBlank(goal_type)) {

										continue;
									}
									// CPI类型，单价太低(0.06)，舍弃不入库
									if ("CPI".equals(goal_type.toUpperCase()) && payout < 0.06) {
										continue;
									}
									int daily_cap = 99999;
									// 系统最低版本
									String min_os_version = null;
									JSONArray preferencesArray = offersitem
											.getJSONArray("preferences");
									if (preferencesArray != null) {
										for (int c = 0; c < preferencesArray.size(); c++) {
											String preference_type = preferencesArray
													.getJSONObject(c).getString("preference_type");
											if (preference_type != null) {
												if (preference_type.equals("min_os_version")) {
													min_os_version = preferencesArray
															.getJSONObject(c).getString("value");
												}
											}
										}
									}

									Integer incetive = 0;// 是否激励(0:非激励 1:激励)
									String incent = offersitem.getString("incent");
									if (incent.equals("false")) {
										incetive = 0;
									} else if (incent.equals("true")) {
										incetive = 1;
									}
									/**
									 * 生成广告池对象
									 */
									advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));
									advertisersOffer.setName(offer_name);

									//
									if ("CPI".equals(goal_type.toUpperCase())) {

										advertisersOffer.setCost_type(101);
										advertisersOffer.setOffer_type(101);
										advertisersOffer.setConversion_flow(101);
									} else if ("CPA".equals(goal_type.toUpperCase())) {
										advertisersOffer.setCost_type(102);
										advertisersOffer.setOffer_type(102);
										advertisersOffer.setConversion_flow(102);
									} else {

										advertisersOffer.setCost_type(104);
										advertisersOffer.setOffer_type(104);
										advertisersOffer.setConversion_flow(104);
									}

									advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
									advertisersOffer.setPkg(pkg);
									// advertisersOffer.setp();
									advertisersOffer.setMain_icon(icon);
									advertisersOffer.setPreview_url(preview_url);
									advertisersOffer.setClick_url(click_url);
									if (countries==null||countries.equals("")) {
										logger.info("countries := " + countries);
									}
									advertisersOffer.setCountry(countries);
									advertisersOffer.setDaily_cap(daily_cap);// 无cap限制，
									advertisersOffer.setPayout(payout);
									advertisersOffer.setCreatives(null);
									// 无描述设置为name
									advertisersOffer.setDescription(offer_name);
									advertisersOffer.setOs(os + "");
									advertisersOffer.setDevice_type(0);// 0代表手机mobile
									advertisersOffer.setOs_version(min_os_version);// 系统版本要求
									advertisersOffer.setSend_type(0);// 系统入库生成广告
									advertisersOffer.setIncent_type(incetive);// 是否激励
									advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
									advertisersOffer.setStatus(0);// 设置为激活状态
								}
							}
							advertisersOfferList.add(advertisersOffer);
						}
					} catch (Exception e) {

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

	public static void main(String[] args) {
//		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
//		String str = HttpUtil
//				.CuttGet("https://bapi.applift.com/bapi_v2?token=MOT4t3c-ngMgli8OVu6-7w&format=json&v=2");
//		JSONObject obbte = JSON.parseObject(str);
//		JSONArray resultsArray = obbte.getJSONArray("results");
//		for (int i = 0; i < resultsArray.size(); i++) {
//			JSONObject resultsitem = resultsArray.getJSONObject(i);
//			JSONArray offersArray = resultsitem.getJSONArray("offers");
//			JSONObject app_details = resultsitem.getJSONObject("app_details");
//			AdvertisersOffer advertisersOffer = new AdvertisersOffer();
//			for (int a = 0; a < offersArray.size(); a++) {
//				JSONObject offersitem = offersArray.getJSONObject(a);
//				String offer_id = offersitem.getString("offer_id");
//				String offer_name = offersitem.getString("offer_name");
//				String click_url = offersitem.getString("click_url");
//				click_url = click_url.replace("{click_id}", "{aff_sub}");
//				JSONArray goal_payouts = offersitem.getJSONArray("goal_payouts");
//				Float payout = null;
//				if (goal_payouts != null) {
//					payout = goal_payouts.getJSONObject(0).getFloat("payout");
//					payout = payout / 1000;
//				}
//				String platform = app_details.getString("platform");
//				int os = 0;
//				if (platform.toUpperCase().equals("ANDROID")) {
//					os = 0;
//				} else if (platform.toUpperCase().equals("IOS")) {
//					os = 1;
//				}
//				String pkg = app_details.getString("bundle_id");
//				String countries = "";
//				JSONArray countriesArray = offersitem.getJSONArray("geo_targeting");
//				if (countriesArray.size() > 0) {
//					for (int b = 0; b < countriesArray.size(); b++) {
//						countries = countries + ":"
//								+ countriesArray.getJSONObject(b).getString("country_code");
//					}
//					countries = countries.substring(1, countries.length());
//				} else {
//					continue;
//				}
//				String icon = "";
//				JSONArray creativesArray = offersitem.getJSONArray("creatives");
//				if (creativesArray != null) {
//					for (int c = 0; c < creativesArray.size(); c++) {
//						String creative_type = creativesArray.getJSONObject(c).getString(
//								"creative_type");
//						if (creative_type != null) {
//							if (creative_type.equals("icon")) {
//								icon = creativesArray.getJSONObject(c).getString("url");
//							}
//						}
//					}
//				}
//
//				String preview_url = TuringStringUtil.getPriviewUrl(os + "", pkg);
//				String goal_type = offersitem.getString("goal_type");
//				String creatives = offersitem.getString("creatives");
//				if (offer_id == null || payout == null || countries.length() == 0
//						|| OverseaStringUtil.isBlank(click_url)
//						|| OverseaStringUtil.isBlank(goal_type)) {
//
//					continue;
//				}
//				// CPI类型，单价太低(0.06)，舍弃不入库
//				if ("CPI".equals(goal_type.toUpperCase()) && payout < 0.06) {
//					continue;
//				}
//				int daily_cap = 99999;
//				//
//				String min_os_version = null;
//				JSONArray preferencesArray = offersitem.getJSONArray("preferences");
//				if (preferencesArray != null) {
//					for (int c = 0; c < preferencesArray.size(); c++) {
//						String preference_type = preferencesArray.getJSONObject(c).getString(
//								"preference_type");
//						if (preference_type != null) {
//							if (preference_type.equals("min_os_version")) {
//								min_os_version = preferencesArray.getJSONObject(c).getString(
//										"value");
//							}
//						}
//					}
//				}
//				Integer incetive = 0;// 是否激励(0:非激励 1:激励)
//				String incent = offersitem.getString("incent");
//				if (incent.equals("false")) {
//					incetive = 0;
//				} else if (incent.equals("true")) {
//					incetive = 1;
//				}
//				/**
//				 * 生成广告池对象
//				 */
//				advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));
//				advertisersOffer.setName(offer_name);
//
//				//
//				if ("CPI".equals(goal_type.toUpperCase())) {
//
//					advertisersOffer.setCost_type(101);
//					advertisersOffer.setOffer_type(101);
//					advertisersOffer.setConversion_flow(101);
//				} else if ("CPA".equals(goal_type.toUpperCase())) {
//					advertisersOffer.setCost_type(102);
//					advertisersOffer.setOffer_type(102);
//					advertisersOffer.setConversion_flow(102);
//				} else {
//
//					advertisersOffer.setCost_type(104);
//					advertisersOffer.setOffer_type(104);
//					advertisersOffer.setConversion_flow(104);
//				}
//
//				advertisersOffer.setAdvertisers_id(101L);// 网盟ID
//				advertisersOffer.setPkg(pkg);
//				// advertisersOffer.setp();
//				advertisersOffer.setMain_icon(icon);
//				advertisersOffer.setPreview_url(preview_url);
//				advertisersOffer.setClick_url(click_url);
//				advertisersOffer.setCountry(countries);
//				advertisersOffer.setDaily_cap(daily_cap);// 无cap限制，
//				advertisersOffer.setPayout(payout);
//				advertisersOffer.setCreatives(creatives);
//				// advertisersOffer.setDescription(Description);
//				advertisersOffer.setOs(os + "");
//				advertisersOffer.setDevice_type(0);// 0代表手机mobile
//				advertisersOffer.setOs_version(min_os_version);// 系统版本要求
//				advertisersOffer.setSend_type(0);// 系统入库生成广告
//				advertisersOffer.setIncent_type(incetive);// 是否激励
//				advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
//				advertisersOffer.setStatus(0);// 设置为激活状态
//			}
//			advertisersOfferList.add(advertisersOffer);
//		}
//		System.out.println(resultsArray.size());
//		System.out.println(advertisersOfferList.get(0).getPreview_url());
//		System.out.println(advertisersOfferList.get(0).getOs());
//		System.out.println(advertisersOfferList.get(0).getName());
//		System.out.println(advertisersOfferList.get(0).getPkg());
//		System.out.println(advertisersOfferList.get(0).getCountry());
//		System.out.println(advertisersOfferList.get(0).getIncent_type());
		 Advertisers tmp = new Advertisers(); // {apikey}
		 tmp.setApiurl("https://bapi.applift.com/bapi_v2?token={apikey}&format=json&v=2");
		 tmp.setApikey("MOT4t3c-ngMgli8OVu6-7w");
		 tmp.setId(101L);
		 PullAppLiftOfferService mmm = new PullAppLiftOfferService(tmp);
		 mmm.run();
	}

}
