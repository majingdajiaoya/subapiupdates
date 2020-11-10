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
import common.Logger;

public class PullAmgOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAltamobOfferService.class);

	private Advertisers advertisers;

	public PullAmgOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	@Override
	public void run() {
		synchronized (this) {
			logger.info("doPull AMG Offer begin := " + new Date());

			Long advertisersId = advertisers.getId();

			String apiurl = advertisers.getApiurl();
			String apikey = advertisers.getApikey();

			if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

				logger.info("advertisersId or apiurl or apikey is null return");

				return;
			}

			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

			int beginPage = 1;

			int total = doPagePullOffer(advertisersOfferList, advertisersId, apiurl, apikey, beginPage);

			int totalPage = 0;

			int Remainder = total % 5000;
			if (Remainder > 0) {
				totalPage = total / 5000 + 1;
			} else {
				totalPage = total / 5000;
			}

			for (int i = 2; i <= totalPage; i++) {

				doPagePullOffer(advertisersOfferList, advertisersId, apiurl, apikey, i);
			}

			logger.info("after filter pull offer size := " + advertisersOfferList.size());

			// 入网盟广告
			if (advertisersId != null && advertisersOfferList != null && advertisersOfferList.size() > 0) {

				PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper.getBean("pullOfferCommonService");

				pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
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
	private int doPagePullOffer(List<AdvertisersOffer> advertisersOfferList, Long advertisersId, String apiurl, String apikey, int page) {

		Integer total_page = 0;

		try {

			apiurl = apiurl.replace("{apikey}", apikey);
			apiurl = apiurl.replace("{page}", String.valueOf(page));

			String str = HttpUtil.sendGet(apiurl);

			JSONObject jsonPullObject = JSON.parseObject(str);

			String status = jsonPullObject.getString("status");// "errno":0

			if (status == null || !status.equals("success")) {

				logger.info("status is success := " + status);

				return 0;
			}

			Integer total = jsonPullObject.getInteger("total");

			JSONArray adList = jsonPullObject.getJSONArray("offers");

			if (adList == null || adList.size() == 0) {

				logger.info("adList is empty");

				return 0;
			}

			if (adList != null && adList.size() > 0) {

				logger.info("begin pull offer " + adList.size());

				for (int i = 0; i < adList.size(); i++) {

					JSONObject item = adList.getJSONObject(i);

					if (item != null) {

						String id = item.getString("id");// 8763
						String pkg = item.getString("packet_name");// "com.uc.iflow",
						Float payout = item.getFloat("price");// 0.88,
						Integer conversions_cap = item.getInteger("daily_cap");// 0,
						String preview_url = item.getString("preview_url");// "https:\/\/play.google.com\/store\/apps\/details?id=com.uc.iflow",
						String title = item.getString("name");// "UC News - Latest News, Live Cricket Score, Videos",
						JSONArray countrys = item.getJSONArray("countries");// "IN",
						String platform = item.getString("platform");// "Android",
						String description = item.getString("description");// "com.uc.iflow",
						String click_url = item.getString("click_url");// "http:\/\/duclick.baidu.com\/click\/affClick?aff_id=512&offer_id=8763",
						String icon = item.getString("icon");// icon url 列表

						if ((description == null || description.equals("")) && title != null) {
							description = title;
						}
						/**
						 * 排除处理
						 */

						// 如果id、pkg、price、country、Platforms、clickURL、cap为空，舍弃不入库
						if (id == null || OverseaStringUtil.isBlank(pkg) || payout == null || countrys == null || OverseaStringUtil.isBlank(platform) || OverseaStringUtil.isBlank(click_url) || conversions_cap == null || conversions_cap < 0) {

							continue;
						}

						// CPI类型，单价太低(0.06)，舍弃不入库
						if (payout < 0.06) {

							continue;
						}

						/**
						 * 特殊字段处理
						 */
						// 处理国家
						String country = "";
						if (countrys.size() > 0) {
							for (int j = 0; j < countrys.size(); j++) {
								String cotryess = countrys.getString(j);
								if ("".equals(cotryess)) {
									country = cotryess;
								} else {
									country = cotryess + ":" + country;
								}
							}
						} else {
							continue;
						}

						// 处理平台(0:andriod:1:ios:2:pc)
						String os_str = "";

						if ("ANDROID".equals(platform.toUpperCase())) {
							os_str = "0";
						} else if ("IOS".equals(platform.toUpperCase())) {
							os_str = "1";
						}

						// 去除最后一个字符
						country = country.substring(0, country.length() - 1);

						/**
						 * 封装网盟广告对象
						 */

						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(title);

						// 都设置为CPI类型广告
						advertisersOffer.setCost_type(101);
						advertisersOffer.setOffer_type(101);
						advertisersOffer.setConversion_flow(101);

						advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
						advertisersOffer.setPkg(pkg);
						// advertisersOffer.setpkg_size();
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(preview_url);
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setCountry(country);
						if (conversions_cap == 0) {

							advertisersOffer.setDaily_cap(10000);// 不限制cap
						} else {

							advertisersOffer.setDaily_cap(conversions_cap);
						}
						advertisersOffer.setIncent_type(0);
						advertisersOffer.setPayout(payout);
						advertisersOffer.setDescription(description);
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
						// advertisersOffer.setOs_version(minOS);//系统版本要求
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						// advertisersOffer.setIncent_type(incetive);// 是否激励
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态

						advertisersOfferList.add(advertisersOffer);
					}
				}
			}
			return total;
		} catch (Exception e) {

			e.printStackTrace();

			NetworkLog.exceptionLog(e);
		}

		return 0;
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
		//http://api.amgepic.com/index.php?m=advert&p=getoffer&app_id=24&app_key={apikey}&page={page}
		//http://api.amgepic.com/index.php?m=advert&p=getoffer&app_id=34&app_key=ba7fa79de0f55529adbe9d9964804442
		tmp.setApiurl("http://api.amgepic.com/index.php?m=advert&p=getoffer&app_id=34&app_key=ba7fa79de0f55529adbe9d9964804442&page={page}");
		tmp.setApikey("5702711e6318112517deb7563e9c4968");
		tmp.setId(19L);

		PullAmgOfferService mmm = new PullAmgOfferService(tmp);
		mmm.run();
	}

}
