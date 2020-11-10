package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

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

public class PullTrafficRedOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullTrafficRedOfferService.class);

	private Advertisers advertisers;

	public PullTrafficRedOfferService(Advertisers advertisers) {

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

			logger.info("doPull PullCheetah_TWOfferService Offer begin := "
					+ new Date());

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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("data");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {
								String offerId = item.getString("id");
								String campaign_name = item
										.getString("campaign_name");
								String platforms = item.getString("platform")
										.toUpperCase();
								Float payout = item.getFloat("payout");
								Integer cap = item.getInteger("cap");
								String previewlink = item
										.getString("preview_link");
								String tracking_link = item
										.getString("tracking_link");
								String package_name = item
										.getString("package_name");
								if (package_name == null) {
									package_name = TuringStringUtil
											.getpkg(previewlink);
								}
								String kpi = item.getString("kpi");
								String geo = item.getString("geo").replace(",",
										":");

								String os_str = "";
								if (platforms.equals("IOS")) {
									os_str = "1";
								} else if (platforms.equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerId);
								advertisersOffer
										.setName(filterEmoji(campaign_name));
								advertisersOffer.setCost_type(101);

								advertisersOffer.setOffer_type(101);
								advertisersOffer
										.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(package_name);
								advertisersOffer.setMain_icon("");
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracking_link);
								advertisersOffer.setCountry(geo);
								advertisersOffer.setDaily_cap(cap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer
										.setDescription(filterEmoji(kpi));
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

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll(
					"[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) {
		// 定义初始化变量
		JSONObject json1;
		JSONArray array = null;
		try {
			logger.info("get avazu offer start");
			String str = HttpUtil
					.sendGet("http://api.traffic.red/v3/offers?token=159&pub=cappumedia&page_size=100&page=1");
			json1 = JSON.parseObject(str);
			array = json1.getJSONArray("data");
			if (array.size() > 0) {
				for (int i = 0; i < array.size(); i++) {
					JSONObject item = array.getJSONObject(i);
					if (item != null) {
						String offerid = item.getString("id");
						String campaign_name = item.getString("campaign_name");
						String platform = item.getString("platform")
								.toUpperCase();
						Float payout = item.getFloat("payout");
						Integer cap = item.getInteger("cap");
						String preview_link = item.getString("preview_link");
						String tracking_link = item.getString("tracking_link");
						String package_name = item.getString("package_name");
						if (package_name == null) {
							package_name = TuringStringUtil
									.getpkg(preview_link);
						}
						String kpi = item.getString("kpi");
						String geo = item.getString("geo").replace(",", "");
						System.out.println(offerid);
					}
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
