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

public class PullStarOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullStarOfferService.class);

	private Advertisers advertisers;

	public PullStarOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Startapp广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull Startapp Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.CuttGet(apiurl);
				JSONObject json = JSON.parseObject(str);
				JSONObject jsonPullObject = json.getJSONObject("data");
				JSONArray offersArray = jsonPullObject.getJSONArray("content");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject offerItem = offersArray.getJSONObject(i);

						if (offerItem != null) {

							String offerid = offerItem.getString("id");
							String name = offerItem.getString("name");
							String logo = offerItem.getString("logo");
							String pkg = offerItem.getString("app_id");
							String tracking_link = offerItem.getString("tracking_link");
							String preview_url = offerItem.getString("preview_url");
							String description = offerItem.getString("description");
							String geo_countries = offerItem.getString("geo_countries");
							if (geo_countries == null || geo_countries.length() == 0) {
								continue;
							}
							geo_countries = geo_countries.replace(";", ":");
							Float payout = offerItem.getFloat("payout");

							String os_str = null;
							if (preview_url.indexOf("apple") > 0) {
								os_str = "1";
							} else if (preview_url.indexOf("google") > 0) {
								os_str = "0";
							} else {
								continue;
							}
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(offerid);
							advertisersOffer.setName(filterEmoji(name));
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							advertisersOffer.setMain_icon(logo);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(tracking_link);
							advertisersOffer.setCountry(geo_countries);
							advertisersOffer.setDaily_cap(500);
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

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	// https://api.startappservice.com/1.1/management/bulk/campaigns?partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&payout=0.1

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// https://api.startappservice.com/1.1/management/bulk/campaigns?partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&payout=0.1
		// {apikey}
		//

		tmp.setApiurl("http://api.startappservice.com/1.1/management/bulk/campaigns?{apikey}&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&minPayoutCPI=0.1");
		tmp.setApikey("partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784");
		tmp.setId(25L);

		PullStarOfferService mmm = new PullStarOfferService(tmp);
		mmm.run();
	}
}
