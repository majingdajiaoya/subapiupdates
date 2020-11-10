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

public class PullHopemobiOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullHopemobiOfferService.class);

	private Advertisers advertisers;

	public PullHopemobiOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Baidu广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Hopemobi Offer begin := " + new Date());

			Long advertisersId = advertisers.getId();

			String apiurl = advertisers.getApiurl();
			String apikey = advertisers.getApikey();

			if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
					|| OverseaStringUtil.isBlank(apikey)) {

				logger.info("advertisersId or apiurl or apikey is null return");

				return;
			}

			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

			int beginPage = 1;

			boolean nextPage = doPagePullOffer(advertisersOfferList,
					advertisersId, apiurl, apikey, beginPage);
			if (nextPage) {
				for (int i = 2; i <= 10; i++) {
					if (nextPage) {
						nextPage = doPagePullOffer(advertisersOfferList,
								advertisersId, apiurl, apikey, i);
					} else {
						break;
					}
				}
			}

			logger.info("after filter pull offer size := "
					+ advertisersOfferList.size());

			// 入网盟广告
			if (advertisersId != null && advertisersOfferList != null
					&& advertisersOfferList.size() > 0) {

				PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
						.getBean("pullOfferCommonService");

				pullOfferCommonService.doPullOffer(advertisers,
						advertisersOfferList);
			}
		}
	}

	/**
	 * 翻页处理
	 * 
	 * @param advertisersId
	 * @param apiurl
	 * @param apikey
	 * @param page
	 */
	private boolean doPagePullOffer(
			List<AdvertisersOffer> advertisersOfferList, Long advertisersId,
			String apiurl, String apikey, int page) {

		Integer total_page = 0;
		boolean next_page = false;

		try {

			apiurl = apiurl.replace("{apikey}", apikey);
			apiurl = apiurl.replace("{page}", String.valueOf(page));

			String str = HttpUtil.sendGet(apiurl);

			JSONObject jsonPullObject = JSON.parseObject(str);

			Integer errno = jsonPullObject.getInteger("code");// "errno":0
			System.out.println(jsonPullObject);
			if (errno == null || errno != 0) {

				logger.info("errno is not ok return. errno := " + errno);

				return false;
			}
			next_page = jsonPullObject.getBooleanValue("next_page");

			// 获取分页等参数值
			// JSONObject params = data.getJSONObject("params");

			// total_page = params.getInteger("total_page");

			JSONArray adList = jsonPullObject.getJSONArray("data");

			if (adList == null || adList.size() == 0) {

				logger.info("data is empty");

				return false;
			}

			if (adList != null && adList.size() > 0) {

				logger.info("begin pull offer " + adList.size());

				for (int i = 0; i < adList.size(); i++) {

					JSONObject item = adList.getJSONObject(i);

					if (item != null) {

						String id = item.getString("id");// 8763
						String pkg = item.getString("app_pack_name");// "com.uc.iflow",
						Float payout = item.getFloat("payout");// 0.88,
						Integer conversions_cap = item.getInteger("cap_daily");// 0,
						String preview_url = item.getString("preview_url");// "https:\/\/play.google.com\/store\/apps\/details?id=com.uc.iflow",
						String title = item.getString("name");// "UC News - Latest News, Live Cricket Score, Videos",
						if (conversions_cap == null || conversions_cap == 0) {
							conversions_cap = 99999;
						}
						String payout_type = item.getString("payout_type");
						String incentive = item.getString("incentive");
						int incentiveint = 0;
						if ("Non Incentive".equals(incentive)) {
							incentiveint = 0;
						} else if ("Incentive".equals(incentive)) {
							incentiveint = 1;
						}

						// String country = item.getString("countries");// "IN",
						JSONArray countriess = item.getJSONArray("countries");
						String countries = "";
						if (countriess != null && countriess.size() > 0) {
							// 处理国家

							for (int j = 0; j < countriess.size(); j++) {

								String countryItem = countriess.getString(j);

								countries += countryItem + ":";
							}
							// 去除最后一个:号
							if (!OverseaStringUtil.isBlank(countries)) {

								countries = countries.substring(0,
										countries.length() - 1);
							}

						}

						String platform = item.getString("os");// "Android",
						String description = item.getString("desc");// "com.uc.iflow",
						String click_url = item.getString("click_url");// "http:\/\/duclick.baidu.com\/click\/affClick?aff_id=512&offer_id=8763",
						JSONArray iconArr = item.getJSONArray("icons");// icon_url  列表

						if ((description == null || description.equals(""))
								&& title != null) {
							description = title;
						}
						/**
						 * 排除处理
						 */

						// 如果id、pkg、price、country、Platforms、clickURL、cap为空，舍弃不入库
						if (id == null || OverseaStringUtil.isBlank(pkg)
								|| payout == null
								|| OverseaStringUtil.isBlank(countries)
								|| OverseaStringUtil.isBlank(platform)
								|| OverseaStringUtil.isBlank(click_url)
								|| conversions_cap == null
								|| conversions_cap < 0) {

							continue;
						}

						// CPI类型，单价太低(0.06)，舍弃不入库
						// if(payout < 0.06){
						//
						// continue;
						// }

						/**
						 * 特殊字段处理
						 */
						// 处理国家

						// 处理平台(0:andriod:1:ios:2:pc)
						String os_str = "";

						// 去除最后一个字符
						// os_str = os_str.substring(0, os_str.length() - 1);
						if ("ANDROID".equals(platform.toUpperCase())) {
							os_str = "0";
						} else if ("IOS".equals(platform.toUpperCase())) {
							os_str = "1";
						}

						// icon设置
						String icon = "";

						if (iconArr != null && iconArr.size() > 0) {

							JSONObject iconJSONObject = iconArr
									.getJSONObject(0);

							icon = iconJSONObject.getString("url");// 获取第一个icon连接
						}
						// String images111 = item.getString("images");
						// "1200x627":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_2_prod_708_ea93b7381f854a0bcb78ce0b762fd52d.jpg",
						// "x":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_55_prod_708_59b23dc121a71.jpg"
						String images_crative = null;
						JSONArray images = item.getJSONArray("images");
						if (images != null && images.size() > 0) {
							try {
								JSONArray images_JSONArray = new JSONArray();
								ArrayList<Map> arrayList = new ArrayList<Map>();
								for (int j = 0; j < images.size(); j++) {
									JSONObject item22 = images.getJSONObject(j);
									if (arrayList.size() >= 3) {
										break;
									}
									Map jsonObject = new HashMap();
									jsonObject.put("url", item22.get("url"));
									jsonObject.put("size", item22.get("pixel"));
									arrayList.add(jsonObject);

								}
								images_JSONArray = JSONArray.parseArray(JSON
										.toJSONString(arrayList));
								images_crative = images_JSONArray.toString();
							} catch (Exception e) {

							}

						}
//						logger.info("offerid :"+id);
//						logger.info("name :"+title);
//						logger.info("advertisersId :"+advertisersId);
//						logger.info("pkg :"+pkg);
//						logger.info("previewlink :"+preview_url);
//						logger.info("tracking_link :"+click_url);
//						logger.info("countries :"+countries);
//						logger.info("dailyCap :"+conversions_cap);
//						logger.info("payout :"+payout);
//						logger.info("os_str :"+os_str);
						/**
						 * 封装网盟广告对象
						 */

						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(title);

						// 都设置为CPI类型广告
						if ("CPI".equals(payout_type)) {
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
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(preview_url);
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setCountry(countries);
						if (conversions_cap == 0) {

							advertisersOffer.setDaily_cap(10000);// 不限制cap
						} else {

							advertisersOffer.setDaily_cap(conversions_cap);
						}
						advertisersOffer.setIncent_type(incentiveint);
						advertisersOffer.setPayout(payout);
						advertisersOffer.setDescription(description);
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
						// advertisersOffer.setOs_version(minOS);//系统版本要求
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						// advertisersOffer.setIncent_type(incetive);// 是否激励
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态
						advertisersOffer.setCreatives(images_crative);

						advertisersOfferList.add(advertisersOffer);
					}
				}
			}
		} catch (Exception e) {

			e.printStackTrace();

			NetworkLog.exceptionLog(e);
		}

		return next_page;
	}

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// http://api.hopemobi.net/v2/offer/pull?token=bba767c03e624b4cabb3a03d70ffa5db
		// {apikey}
//		tmp.setApiurl("http://api.occuad.com/v2/offer/pull?token=04b3ab6112c441f2b0b148b69cfd39ef&page={page}");
//		tmp.setApikey("04b3ab6112c441f2b0b148b69cfd39ef");
		tmp.setApiurl("http://api.flymobi.biz/v2/offer/pull?token=c54582a912c247a192a32d43be799448&page={page}");
		tmp.setApikey("c54582a912c247a192a32d43be799448");
		tmp.setId(19L);

		PullHopemobiOfferService mmm = new PullHopemobiOfferService(tmp);
		mmm.run();
	}

}
