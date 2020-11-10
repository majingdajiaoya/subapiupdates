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

public class PullGlispaOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullGlispaOfferService.class);

	private Advertisers advertisers;

	public PullGlispaOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Glispa广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Glispa Offer begin := " + new Date());

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

				String str = HttpUtil.sendGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);

				String status = jsonPullObject.getString("status");

				if (!"ok".equals(status)) {

					logger.info("status is not ok return. status := " + status);

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("data");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							Integer campaign_id = item
									.getInteger("campaign_id");
							Float payout_amount = item
									.getFloat("payout_amount");// 0.31
							Integer daily_remaining_leads = item
									.getInteger("daily_remaining_leads");// 9992
							String mobile_app_id = item
									.getString("mobile_app_id");
							String mobile_platform = item
									.getString("mobile_platform");
							String mobile_min_version = item
									.getString("mobile_min_version");
							Integer incentivized = item
									.getInteger("incentivized");
							String name = item.getString("name");
							String category = item.getString("category");
							String click_url = item.getString("click_url");
							String preload_click_url = item
									.getString("preload_click_url");
							String icon = item.getString("icon");

							if (mobile_platform != null) {
								if ("android".equals(mobile_platform
										.toLowerCase())) {

									mobile_platform = "0";
								}
								if ("ios".equals(mobile_platform.toLowerCase())) {

									mobile_platform = "1";
								}
							}

							JSONArray countriesArray = item
									.getJSONArray("countries");

							String countries_str = "";
							if (countriesArray != null
									&& countriesArray.size() > 0) {

								for (int k = 0; k < countriesArray.size(); k++) {

									String countriesItem = countriesArray
											.getString(k);

									if (countriesItem != null) {

										countries_str += countriesItem
												.toUpperCase() + ":";
									}
								}
							}
							// 去除最后一个:号
							if (!OverseaStringUtil.isBlank(countries_str)) {

								countries_str = countries_str.substring(0,
										countries_str.length() - 1);
							}

							// 如果id、price、country、clickURL、mobile_platform、mobile_app_id、daily_remaining_leads为空，舍弃不入库
							if (campaign_id == null
									|| payout_amount == null
									|| countriesArray == null
									|| countriesArray.size() == 0
									|| OverseaStringUtil.isBlank(click_url)
									|| OverseaStringUtil
											.isBlank(mobile_platform)
									|| OverseaStringUtil.isBlank(mobile_app_id)
									|| daily_remaining_leads == null) {

								continue;
							}

							// 单价太低(0.06)，舍弃不入库
							if (payout_amount < 0.06) {

								continue;
							}

							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String
									.valueOf(campaign_id));
							advertisersOffer.setName(name);

							// CPI类型
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);

							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(mobile_app_id);
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preload_click_url);
							advertisersOffer.setClick_url(click_url);
							advertisersOffer.setCountry(countries_str);
							advertisersOffer
									.setDaily_cap(daily_remaining_leads);
							advertisersOffer.setPayout(payout_amount);
							advertisersOffer.setOs(mobile_platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(mobile_min_version);// 系统版本要求
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setIncent_type(incentivized);// 是否激励
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态

							advertisersOfferList.add(advertisersOffer);
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
			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}
	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		tmp.setApiurl("http://feed.platform.glispa.com/v1.1/native-feed/d1b354b4-f681-4171-afd8-2c472b529f49");
		tmp.setApikey("user=ye.ya@cappumedia.com&password=Mobrain123!");
		tmp.setId(2040L);

		com.network.task.service.PullGlispaOfferService mmm = new com.network.task.service.PullGlispaOfferService(
				tmp);
		mmm.run();
	}

}
