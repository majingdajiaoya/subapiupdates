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

public class PullTopInplayableOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullTopInplayableOfferService.class);

	private Advertisers advertisers;

	public PullTopInplayableOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullTopInplayableOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			logger.info("doPull PullTopInplayableOfferService Offer begin := " + new Date());
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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONArray offersArray = jsonPullObject.getJSONArray("datas");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {
								String offer_id = item.getString("id");
								String pkg = item.getString("app_pkg");
								String name = item.getString("app_name");
								String platform = item.getString("platform").toUpperCase();
								String icon = item.getString("app_icon");
								String country = item.getString("countries");
								logger.info(offer_id+"======= " + country);
								country = country.replace("[\"", "");
								country = country.replace("\"]", "");
								country = country.replace("\",\"", "");
								// //JP KR US CA ID TW DE GB FR MY

								if (!country.contains("MY") && !country.contains("GB") && !country.contains("FR") && !country.contains("CA") && !country.contains("DE") && !country.contains("ID") && !country.contains("IN") && !country.contains("HK") && !country.contains("MO") && !country.contains("KR") && !country.contains("JP") && !country.contains("US") && !country.contains("TW")) {
									continue;
								}

								Float payout = item.getFloat("price");

								if (payout > 15) {
									continue;
								}

								String description = item.getString("des");
								String tracklink = item.getString("click_url");
								String previewlink = item.getString("preview_url");
								String os_str = "";
								if (platform.contains("IOS")) {
									os_str = "1";
								} else if (platform.contains("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								Integer daily_cap = item.getInteger("daily_cap");

								JSONArray blockChannel = item.getJSONArray("blockChannel");
								if (blockChannel != null && blockChannel.size() > 0) {
									logger.info("blockChannel " + blockChannel);
									// .subSequence(4, 5).equals("_")
									for (Object object : blockChannel) {
										if (object.toString().subSequence(4, 5).equals("_")) {
											block_pkg_affi.put(pkg.replace("_", "") + "&" + object.toString(), object.toString());
										}
									}
								}

								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offer_id);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracklink);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(daily_cap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(description));
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
					logger.info("block_pkg_affi" + block_pkg_affi);
					logger.info("after filter pull offer size := " + advertisersOfferList.size());
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
		Advertisers tmp = new Advertisers();
		// http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret={apikey}
		tmp.setApiurl("http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret=db2be00d484a1d45a0dc23bed9d6385d");
		tmp.setApikey("db2be00d484a1d45a0dc23bed9d6385d");
		tmp.setId(1057L);

		PullTopInplayableOfferService mmm = new PullTopInplayableOfferService(tmp);
		mmm.run();
	}

}
