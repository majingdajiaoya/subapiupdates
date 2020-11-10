package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullGameberryMobsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullGameberryMobsOfferService.class);

	private Advertisers advertisers;

	public PullGameberryMobsOfferService(Advertisers advertisers) {

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

			logger.info("doPull Gameberry Offer begin := " + new Date());

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

				int totalpage = getAllofferPage(advertisersOfferList, advertisersId, apiurl, 1);
				if (totalpage > 1) {
					for (int i = 2; i <= totalpage; i++) {
						totalpage = getAllofferPage(advertisersOfferList, advertisersId, apiurl, 2);
					}
				}

				if (advertisersOfferList != null && advertisersOfferList.size() > 0) {

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

	public int getAllofferPage(List<AdvertisersOffer> advertisersOfferList, Long advertisersId,
			String apiurl, int page) {
		int totalpage = 0;
		try {
			apiurl = apiurl.replace("{page}", String.valueOf(page));
			String str = HttpUtil.CuttGet(apiurl);

			JSONObject jsonPullObject = JSON.parseObject(str);

			String status = jsonPullObject.getString("status");
			// Integer recordsTotal = jsonPullObject.getInteger("total");
			// Integer recordsFiltered =
			// jsonPullObject.getInteger("recordsFiltered");
			// Integer pagecount = jsonPullObject.getInteger("pagecount");

			// Integer count = jsonPullObject.getInteger("total_count");

			JSONObject pagination = jsonPullObject.getJSONObject("pagination");
			Integer recordsTotal = pagination.getInteger("total_count");
			Integer perpage = pagination.getInteger("per_page");
			totalpage = (int) Math.ceil((double) recordsTotal / (double) perpage);

			if (!"1".equals(status)) {

				logger.info("status is not success return. status := " + status);

				return -2;
			}

			// logger.info("recordsTotal:= " +
			// recordsTotal+"     recordsFiltered:"+recordsFiltered);
			// if(recordsTotal == null
			// || recordsTotal == 0){
			//
			// logger.info("available or  count is empty. " + " recordsTotal:="
			// + recordsFiltered);
			//
			// return;
			// }

			JSONArray offersArray = jsonPullObject.getJSONArray("offers");

			if (offersArray != null && offersArray.size() > 0) {

				logger.info("begin pull offer " + offersArray.size());

				for (int i = 0; i < offersArray.size(); i++) {

					JSONObject item = offersArray.getJSONObject(i);

					if (item != null) {

						String roffer_id = item.getString("id");// 104573764
						Integer device_type = null;
						device_type = 0;
						boolean is_cpi = item.getBooleanValue("is_cpi");
						String offer_model = "cpi";// "CPI"
						if (!is_cpi) {
							offer_model = "no";
						}
						Integer incetive = 0;// 是否激励(0:非激励 1:激励)
						JSONArray payments = item.getJSONArray("payments");
						JSONObject payments_item = null;
						if (payments != null && payments.size() > 0) {
							payments_item = payments.getJSONObject(0);
						} else {
							continue;
						}
						JSONArray countries = payments_item.getJSONArray("countries");
						String os_str = "";
						String min_os_version = null;
						String name = item.getString("title");// "Legacy of Discord-FuriousWings"

						String preview_url = item.getString("preview_url");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
						//https://itunes.apple.com/pl/app/sts-online/id904595321?l=pl&mt=8"
						String pkg = "";
						pkg=TuringStringUtil.getpkg(preview_url);
						os_str=TuringStringUtil.getPlatoms(preview_url);
						if(pkg.length()==0){
							continue;
						}
						if(pkg.length()>250){
							continue;
						}
						String description = item.getString("description");
						
						String icon = item.getString("logo");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

						String caps_daily_remaining_str = item.getString("cap");// 9992
						Integer caps_daily_remaining = 500;
						if (!OverseaStringUtil.isBlank(caps_daily_remaining_str)) {
							if ("0".equals(caps_daily_remaining_str)) {

								caps_daily_remaining = 99999;
							} else {

								caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
							}
						}
						// String caps_daily = item.getString("caps_daily");//
						// 9999
						// String caps_total = item.getString("caps_total");//
						// "unlimited"
						// String caps_total_remaining =
						// item.getString("caps_total_remaining");// "unlimited"
						// Float avg_cr = item.getFloat("avg_cr");// 0.44
						// Integer rpc = item.getInteger("rpc");// 0
						// boolean r = item.getBoolean("r");// false

						String link = item.getString("link");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
						// http://click.szkuka.com/index.php?m=advert&p=click&app_id=140&offer_id=766738&clickid={clickid}&gaid={gaid}&android={android}&idfa={idfa}&subid={subid}

						// link =link.replace("{clickid}", "{aff_sub}");
						// link =link.replace("{subid}", "{sub_affid}");

						// if (os_str.equals("0")) { //android
						// link=link.replace("{idfa}", "");
						// } else if (os_str.equals("1")) { //ios
						// link=link.replace("{gaid}", "");
						// link=link.replace("{android}", "");
						// }

						Float payout = payments_item.getFloat("revenue");// 0.31

						// * targeting
						// * allowed
						// * os [1]
						// * os_version Object
						// * models []
						// * manufacturer []
						// * countries [1]
						// * device_types []
						// JSONObject allowed =
						// targeting.getJSONObject("allowed");
						// JSONArray osArray = allowed.getJSONArray("os");
						//
						// if(osArray != null
						// && osArray.size() > 0){
						//
						// for(int k = 0; k < osArray.size(); k++){
						//
						// String osItem = osArray.getString(k);
						//
						// if(osItem != null){
						// if("android".equals(osItem.toLowerCase())){
						//
						// os_str += "0:";
						// }
						// if("ios".equals(osItem.toLowerCase())){
						//
						// os_str += "1:";
						// }
						// }
						// }
						// }
						// //去除最后一个:号
						// if(!OverseaStringUtil.isBlank(os_str)){
						//
						// os_str = os_str.substring(0, os_str.length() - 1);
						// }

						String countries_str = "";
						if (countries != null && countries.size() > 0) {

							for (int k = 0; k < countries.size(); k++) {

								String countriesItem = countries.getString(k);

								if (countriesItem != null) {

									countries_str += countriesItem.toUpperCase() + ":";
								}
							}
						}
						// 去除最后一个:号
						if (!OverseaStringUtil.isBlank(countries_str)) {

							countries_str = countries_str.substring(0, countries_str.length() - 1);
						}

						// String app_name = item.getString("app_name");//
						// "Legacy of Discord-FuriousWings"
						// String app_description =
						// item.getString("app_description");//
						// "Google Play Summer Sale …Lodsupport@gtarcade.com"

						// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
						if (roffer_id == null || payout == null || countries == null
								|| OverseaStringUtil.isBlank(link)
								|| OverseaStringUtil.isBlank(offer_model)) {

							continue;
						}

						String images_crative = item.getString("creatives");
						
						// "1200x627":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_2_prod_708_ea93b7381f854a0bcb78ce0b762fd52d.jpg",
						// "x":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_55_prod_708_59b23dc121a71.jpg"

						offer_model = offer_model.toUpperCase();

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
						// advertisersOffer.setpkg_size();
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(preview_url);
						advertisersOffer.setClick_url(link);

						advertisersOffer.setCountry(countries_str);

						// advertisersOffer.setEcountry(ecountry);

						advertisersOffer.setDaily_cap(caps_daily_remaining);
						// advertisersOffer.setsend_cap();
						advertisersOffer.setPayout(payout);
						// advertisersOffer.setExpiration(category);
						advertisersOffer.setCreatives(images_crative);
						if ("CPI".equals(offer_model)) {

							advertisersOffer.setConversion_flow(101);
						} else {

							advertisersOffer.setConversion_flow(104);// 设置为其它类型
						}
						// advertisersOffer.setSupported_carriers("");
						advertisersOffer.setDescription(filterEmoji(description));
						advertisersOffer.setOs(os_str);

						advertisersOffer.setDevice_type(device_type);
						advertisersOffer.setOs_version(min_os_version);// 系统版本要求
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						advertisersOffer.setIncent_type(incetive);// 是否激励
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态

						advertisersOfferList.add(advertisersOffer);
					}
				}
			} else {
				return -2;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return totalpage;
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
		// http://api.junggglex.affise.com/3.0/partner/offers?page=1&limit=500&api-key=2a341afa8b5692a698b88698a0cc4385ae5fde0f
		// &sub1=1603&sub2={sub_affid}&sub3={aff_sub}&sub4={gaid}&sub5={idfa}
		//https://api-sinumvendo.affise.com/ 
		tmp.setApiurl("http://api-sinumvendo.affise.com/3.0/partner/offers?api-key=b8fed406891f0ceb6fc511e7d5a0345491e59057&limit=10000");
		tmp.setApikey("b8fed406891f0ceb6fc511e7d5a0345491e59057");
		tmp.setId(1060L);

		PullGameberryMobsOfferService mmm = new PullGameberryMobsOfferService(tmp);
		mmm.run();
		String s="com.lenskart.app&referrer=af_tranid%3DwaTXHQ1flaGm6qX8FzPIDA%26pid%3Dappier_int%26c%3DApp-Installs%26clickid%3D%24{managed_ids}%26android_id%3D%24{androididraw}%26sha1_android_id%3D%24{androididsha1}%26advertising_id%3D%24{idfaraw}%26af_siteid%3D%24{partner_id}_%24{channel_id}";
		System.out.println(s.length());
	}

}
