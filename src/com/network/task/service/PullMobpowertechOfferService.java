package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import com.alibaba.fastjson.JSON;
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

public class PullMobpowertechOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullMobpowertechOfferService.class);

	private Advertisers advertisers;

	public PullMobpowertechOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullPullMobpowertechOfferServiceOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullMobpowertechOfferService Offer begin := "
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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				String str = HttpUtil.sendGet(apiurl);
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONObject offersObject = jsonPullObject
						.getJSONObject("campaigns");
				Iterator<String> iterator = offersObject.keySet().iterator();

				if (iterator != null) {
					while (iterator.hasNext()) {
						try {
							String offerIdkey = (String) iterator.next();
							String offerValue = offersObject
									.getString(offerIdkey);
							JSONObject offerValueJsonObject = JSON
									.parseObject(offerValue);
							String offerId = offerValueJsonObject
									.getString("campaign_id");
							String pkg = offerValueJsonObject.getString(
									"trace_app_id").replace("id", "");
							String preview_url = offerValueJsonObject
									.getString("preview_url");
							String trackinglink = offerValueJsonObject
									.getString("trackurl");
							String impression_url = offerValueJsonObject
									.getString("impression_url");
							String payout = offerValueJsonObject.getString(
									"payout").replace("$", "");
							String icon_url = offerValueJsonObject
									.getString("icon_url");
							Integer cap = offerValueJsonObject
									.getInteger("cap");
							String kpi = offerValueJsonObject
									.getString("appdesc");
							String name = offerValueJsonObject
									.getString("title");
							String country = offerValueJsonObject.getString(
									"allow_country").replace(",", ":");
							if (!country.contains("JP")
									&& !country.contains("TW")
									&& !country.contains("US")) {
								continue;
							}

							String os = "0";
							if (preview_url.contains("google")) {
								os = "0";
							} else if (preview_url.contains("apple")) {
								os = "1";
							} else {
								continue;
							}

							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(offerId);
							advertisersOffer.setName(filterEmoji(name));
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							advertisersOffer.setMain_icon(icon_url);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(trackinglink);
							advertisersOffer.setCountry(country);
							advertisersOffer.setDaily_cap(cap);
							advertisersOffer.setPayout(Float.valueOf(payout));
							advertisersOffer.setCreatives(null);
							advertisersOffer.setConversion_flow(101);
							advertisersOffer.setDescription(filterEmoji(kpi));
							advertisersOffer.setOs(os);
							advertisersOffer.setDevice_type(0);// 设置为mobile类型
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);

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

	public static void main(String[] args) throws ClientProtocolException,
			IOException {
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("http://affiliate.mobpowertech.com/offline/sync?appid=5000230&apikey=f032943778de93f5d7ce0c03be443029");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);
		PullMobpowertechOfferService mmm = new PullMobpowertechOfferService(tmp);
		mmm.run();
	}

}
