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
import com.network.task.bean.BlackListSubAffiliates;
import com.network.task.dao.Blacklist_affiliates_subidDao;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullLeadboltAndroidOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullLeadboltAndroidOfferService.class);

	private Advertisers advertisers;

	public PullLeadboltAndroidOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Leadbolt广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {
		// 黑名单
		Map<String, String> block_pkg_affi = new HashMap<String, String>();
		// 同步互斥
		synchronized (this) {

			logger.info("doPull Leadbolt Offer begin := " + new Date());

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

				JSONArray offersArray = JSONArray.parseArray(str);

				if (offersArray == null || offersArray.size() == 0) {

					logger.info("offersArray is empty");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer size := " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							Integer campaign_id = item.getInteger("campaign_id");// 430633
							String platform = item.getString("platform");// "android"
							Float payout_amt = item.getFloat("payout_amt");// 2.55
							JSONArray countriesArray = item.getJSONArray("countries_supported");// ["AU","US"]
							String pkg = item.getString("app_marketplace_code");
							String preview_url = item.getString("app_marketplace_url");
							String app_name = item.getString("app_name");
							String min_os_version = item.getString("min_os_version");// "4.4"
							String payout_type = item.getString("payout_type");// "CPA"
							String linkout_url = item.getString("linkout_url");
							// group_ids_excluded
							JSONArray black_array = item.getJSONArray("group_ids_excluded");
							if (black_array.size() > 0) {
								if (black_array != null) {
									for (int j = 0; j < black_array.size(); j++) {
										if (black_array.getString(j).length() > 5) {
											if (black_array.getString(j).substring(4, 5).equals("_")) {
												block_pkg_affi.put(pkg + "&" + black_array.getString(j), pkg.replace("_", ""));
											}
										}

									}
								}
							}

							/**
							 * 特殊处理字段
							 */

							// 系统平台
							if (!OverseaStringUtil.isBlank(platform)) {

								if ("android".equals(platform.toLowerCase())) {

									platform = "0";
								}
								if ("ios".equals(platform.toLowerCase())) {

									platform = "1";
									continue;
								}
							}

							// 国家
							String countries_str = "";

							if (countriesArray != null && countriesArray.size() > 0) {

								for (int k = 0; k < countriesArray.size(); k++) {

									String countriesItem = countriesArray.getString(k);

									if (countriesItem != null) {

										countries_str += countriesItem.toUpperCase() + ":";
									}
								}
							}
							// 去除最后一个:号
							if (!OverseaStringUtil.isBlank(countries_str)) {

								countries_str = countries_str.substring(0, countries_str.length() - 1);
							}

							// 如果id、price、country、clickURL、mobile_platform、pkg、payout_type为空，舍弃不入库
							if (campaign_id == null || payout_amt == null || OverseaStringUtil.isBlank(countries_str) || OverseaStringUtil.isBlank(linkout_url) || OverseaStringUtil.isBlank(platform) || OverseaStringUtil.isBlank(pkg) || OverseaStringUtil.isBlank(payout_type)) {

								continue;
							}

							// 单价太低(0.06)，舍弃不入库
							if (payout_amt < 0.06) {

								continue;
							}
							// 处理 icon，description,还有 creatives等

							JSONArray creatives = item.getJSONArray("creatives");// ["AU","US"]
							String Description = "";
							String mainicon = null;
							String images_crative = null;

							ArrayList<Map> arrayList = new ArrayList<Map>();

							if (creatives != null && creatives.size() > 0) {
								for (int j = 0; j < creatives.size(); j++) {
									JSONObject creativesitem = creatives.getJSONObject(j);

									if (creativesitem != null) {
										if (creativesitem.containsKey("creative_type") && "Text".equals(creativesitem.getString("creative_type"))) {
											String temp = creativesitem.getString("title") + " " + creativesitem.getString("description");
											if (temp.length() > Description.length()) {
												Description = temp;
											}
										}
										if (creativesitem.containsKey("creative_type") && "Image".equals(creativesitem.getString("creative_type"))) {
											if (creativesitem.getInteger("width").intValue() == 120 && creativesitem.getInteger("height").intValue() == 120) {
												mainicon = creativesitem.getString("src_url");
											} else {
												if (arrayList.size() < 4) {
													Map jsonObject = new HashMap();
													jsonObject.put("url", creativesitem.getString("src_url"));
													jsonObject.put("size", creativesitem.get("width") + "x" + creativesitem.get("height"));
													arrayList.add(jsonObject);
												}
											}

										}

									}
								}
								JSONArray images_JSONArray = new JSONArray();
								//
								if (arrayList.size() > 0) {
									images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									images_crative = images_JSONArray.toString();
								}
							}

							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
							advertisersOffer.setName(app_name);

							// CPA类型(leadbolt 不做订阅，app安装都归类为CPA类型)
							if ("CPA".equals(payout_type)) {

								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							} else {

								advertisersOffer.setCost_type(104);
								advertisersOffer.setOffer_type(104);
								advertisersOffer.setConversion_flow(104);
							}

							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							// advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(mainicon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(linkout_url);
							advertisersOffer.setCountry(countries_str);
							// advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(99999);// 无cap限制，默认3000
							// advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout_amt);
							// advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(images_crative);
							// advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(filterEmoji(Description));

							advertisersOffer.setOs(platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(min_os_version);// 系统版本要求
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							// advertisersOffer.setIncent_type(incentivized);//
							// 是否激励
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态

							advertisersOfferList.add(advertisersOffer);
						}
					}

					logger.info("after filter pull offer size := " + advertisersOfferList.size());

					logger.info("block_pkg_affi := " + block_pkg_affi);
					// 黑名单
					Blacklist_affiliates_subidDao Blacklist_affiliates_subidDao = (Blacklist_affiliates_subidDao) TuringSpringHelper.getBean("Blacklist_affiliates_subidDao");
					List<BlackListSubAffiliates> block_list = Blacklist_affiliates_subidDao.getAllInfoByAdvertisers_id(advertisersId + "");
					Map<String, String> db_block_map = new HashMap<String, String>();
					for (BlackListSubAffiliates entity : block_list) {
						db_block_map.put(entity.getPkg() + "&" + entity.getAffiliates_id() + "_" + entity.getAffiliates_sub_id(), entity.getAffiliates_sub_id());
					}
					// 对比
					for (String key : block_pkg_affi.keySet()) {
						if (db_block_map.get(key) == null) {
							// com.grubhub.android_27
							String sub[] = key.split("&");
							String pkg = sub[0];
							String affiliates_id = sub[1].substring(0, 4);
							String affiliates_sub_id = sub[1].substring(5, sub[1].length());
							Blacklist_affiliates_subidDao.add(advertisersId, pkg, affiliates_id, affiliates_sub_id);
						}
					}

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
		Advertisers tmp = new Advertisers(); // {apikey}
		// http://api.leadbolt.net/api/native_offline?api_key=Kctvetgo2cmVC2ndXdh49lwFm2mQMTHu
		tmp.setApiurl("http://api.leadbolt.net/api/native_offline?api_key={apikey}");
		tmp.setApikey("4kx5bIDEwuzbr9S1TKIBNW0lxlkMbuX2");
		tmp.setId(26L);

		PullLeadboltAndroidOfferService mmm = new PullLeadboltAndroidOfferService(tmp);
		mmm.run();
	}

}
