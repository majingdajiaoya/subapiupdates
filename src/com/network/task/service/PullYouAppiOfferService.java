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

public class PullYouAppiOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullYouAppiOfferService.class);

	private Advertisers advertisers;

	public PullYouAppiOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	@SuppressWarnings("rawtypes")
	public void run() {

		// 同步互斥
		synchronized (this) {

			// 黑名单
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			// 白名单
			logger.info("doPull PullYouAppiOfferService Offer begin := " + new Date());
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
				String str = HttpUtil.sendGet(apiurl);
				JSONObject json = JSON.parseObject(str);
				JSONObject offersJSONObject = json.getJSONObject("data");
				for (Object map : offersJSONObject.entrySet()) {
					JSONObject offerObject = (JSONObject) ((Map.Entry) map).getValue();
					Float payout = offerObject.getFloat("cpi");
					// impression_url
					String impression_url = null;
					impression_url = offerObject.getString("impression_url");

					// &ipaddress=&useragent=
					if (impression_url != null) {
						impression_url = impression_url.replace("&subid=", "&subid={aff_sub}");
						impression_url = impression_url.replace("&publishertoken=", "&publishertoken={channel}");
						impression_url = impression_url.replace("&publishername=", "&publishername={app_name}");
						impression_url = impression_url.replace("&deviceAndroidId=", "&deviceAndroidId={gaid}");
						impression_url = impression_url.replace("&deviceIfa=", "&deviceIfa={idfa}");
						impression_url = impression_url.replace("&bundle_id=", "&bundle_id={app_name}");
						impression_url=impression_url.replace("&useragent=", "&useragent={ua}");
						impression_url=impression_url.replace("&ipaddress=", "&ipaddress={ip}");
					}
					String offerId = offerObject.getString("campaign_id");
					String platform = offerObject.getString("platform").toUpperCase();
					String previewlink = offerObject.getString("app_url");
					// https://track.56txs4.com/dlclicks?accesstoken=a3d4c97c-54da-474b-a5ad-0e7728c7e3ba&appid=62458&campaignpartnerid=1252203&ot=dir&creative_set_id=-1&subid=&publishertoken=&publishername=&usertoken=&deviceAndroidId=&deviceIfa=&age=&gender=&publisher_type=&format=&consent=
					String tracklink = offerObject.getString("redirect_url");
					tracklink = tracklink.replace("&subid=", "&subid={aff_sub}");
					tracklink = tracklink.replace("&publishertoken=", "&publishertoken={channel}");
					tracklink = tracklink.replace("&publishername=", "&publishername={app_name}");
					tracklink = tracklink.replace("&deviceAndroidId=", "&deviceAndroidId={gaid}");
					tracklink = tracklink.replace("&deviceIfa=", "&deviceIfa={idfa}");
					tracklink = tracklink.replace("&bundle_id=", "&bundle_id={app_name}");
					String countries = offerObject.getString("countries").toUpperCase();
					countries = countries.replace("[\"", "");
					countries = countries.replace("\"]", "");
					countries = countries.replace(",", ":");
					countries = countries.replace("\"", "");
					if (countries.substring(countries.length() - 1, countries.length()).equals(":")) {
						countries = countries.substring(0, countries.length() - 1);
					}
					Integer cap = offerObject.getInteger("max_daily");
					JSONObject app_detailsObject = offerObject.getJSONObject("app_details");
					String name = app_detailsObject.getString("app_name");
					String pkg = app_detailsObject.getString("app_id");
					String app_description = app_detailsObject.getString("app_description");
					String app_icon = app_detailsObject.getString("app_icon");
					JSONArray black_array = offerObject.getJSONArray("blacklist_publisherTokens");

					if (black_array != null && black_array.size() > 0) {
						for (int j = 0; j < black_array.size(); j++) {
							if (black_array.getString(j).length() > 5) {
								if (black_array.getString(j).substring(4, 5).equals("_")) {
									block_pkg_affi.put(pkg + "&" + black_array.getString(j), pkg.replace("_", ""));
								}
							}

						}
					}
					String os_str = null;
					if (platform.toUpperCase().equals("IPHONE")) {
						os_str = "1";
					} else if (platform.toUpperCase().equals("ANDROID")) {
						os_str = "0";
					} else {
						continue;
					}
					if (cap == null) {
						cap = 50;
					}
					if (cap == 0) {
						cap = 99999;
					}

					// 版本
					String minOs = offerObject.getString("minOs");
					if (minOs != null && minOs.contains("+")) {
						minOs = minOs.replace("+", "");
					}
					AdvertisersOffer advertisersOffer = new AdvertisersOffer();
					advertisersOffer.setAdv_offer_id(offerId);
					advertisersOffer.setName(filterEmoji(name));
					advertisersOffer.setCost_type(101);
					advertisersOffer.setOs_version(minOs);
					advertisersOffer.setOffer_type(101);
					advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
					advertisersOffer.setPkg(pkg);
					advertisersOffer.setMain_icon(app_icon);
					advertisersOffer.setPreview_url(previewlink);
					advertisersOffer.setClick_url(tracklink);
					advertisersOffer.setCountry(countries);
					advertisersOffer.setDaily_cap(cap);
					advertisersOffer.setPayout(payout);
					advertisersOffer.setCreatives(null);
					advertisersOffer.setConversion_flow(101);
					advertisersOffer.setDescription(filterEmoji(app_description));
					advertisersOffer.setOs(os_str);
					advertisersOffer.setDevice_type(0);// 设置为mobile类型
					advertisersOffer.setSend_type(0);// 系统入库生成广告
					advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
					advertisersOffer.setImpressionUrl(impression_url);
					advertisersOffer.setStatus(0);// 设置为激活状态
					advertisersOfferList.add(advertisersOffer);
				}

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
		// JSONObject json;
		// JSONArray array = null;
		// try {
		// logger.info("get avazu offer start");
		// String str = HttpUtil
		// .sendGet("https://service.youappi.com/cmp/campaigninfo?accesstoken=a3d4c97c-54da-474b-a5ad-0e7728c7e3ba");
		// json = JSON.parseObject(str);
		// JSONObject offersJSONObject = json.getJSONObject("data");
		// for (Object map : offersJSONObject.entrySet()) {
		// JSONObject offerObject = (JSONObject) ((Map.Entry) map).getValue();
		// String offerId = offerObject.getString("offer_id");
		// String platform = offerObject.getString("platform").toUpperCase();
		// String prewlink = offerObject.getString("app_url");
		// //
		// https://track.56txs4.com/dlclicks?accesstoken=a3d4c97c-54da-474b-a5ad-0e7728c7e3ba&appid=62458&campaignpartnerid=1252203&ot=dir&creative_set_id=-1&subid=&publishertoken=&publishername=&usertoken=&deviceAndroidId=&deviceIfa=&age=&gender=&publisher_type=&format=&consent=
		// String tracklink = offerObject.getString("redirect_url");
		// tracklink = tracklink.replace("&subid=", "&subid={aff_sub}");
		// tracklink = tracklink.replace("&publishertoken=",
		// "&publishertoken={channel}");
		// tracklink = tracklink.replace("&publishername=",
		// "&publishername={app_name}");
		// tracklink = tracklink.replace("&deviceAndroidId=",
		// "&deviceAndroidId={gaid}");
		// tracklink = tracklink.replace("&deviceIfa=", "&deviceIfa={idfa}");
		// String countries = offerObject.getString("countries").toUpperCase();
		// countries = countries.replace("[\"", "");
		// countries = countries.replace("\"]", "");
		// countries = countries.replace(",", ":");
		// countries = countries.replace("\"", "");
		// Integer cap = offerObject.getInteger("max_daily");
		// JSONObject app_detailsObject =
		// offerObject.getJSONObject("app_details");
		// String name = app_detailsObject.getString("app_name");
		// String pkg = app_detailsObject.getString("app_id");
		// String app_description =
		// app_detailsObject.getString("app_description");
		// String app_icon = app_detailsObject.getString("app_icon");
		// Float payout = offerObject.getFloat("cpi");
		// System.out.println(payout);
		// }
		//
		// } catch (ClientProtocolException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("https://service.youappi.com/cmp/campaigninfo?accesstoken=c70bf565-8210-4c37-8975-e92017e9e8fd");
		tmp.setApikey("c70bf565-8210-4c37-8975-e92017e9e8fd");
		tmp.setId(1078L);
		PullYouAppiOfferService mmm = new PullYouAppiOfferService(tmp);
		mmm.run();
	}

}
