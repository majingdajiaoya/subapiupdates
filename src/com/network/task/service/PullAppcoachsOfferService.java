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
import common.Logger;

public class PullAppcoachsOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAppcoachsOfferService.class);

	private Advertisers advertisers;

	public PullAppcoachsOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullAppcoachsOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullAppcoachsOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				apiurl = apiurl.replace("{apikey}", apikey);
				String str = HttpUtil.sendGet(apiurl);
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONArray offer_list = jsonPullObject.getJSONArray("offer_list");

				if (offer_list != null && offer_list.size() > 0) {

					logger.info("begin pull offer " + offer_list.size());

					for (int i = 0; i < offer_list.size(); i++) {

						JSONObject item = offer_list.getJSONObject(i);

						if (item != null) {
							String payout_type = item.getString("payout_type").toUpperCase();
							if (payout_type.equals("CPI")) {
								String name = item.getString("name");
								String offerid = item.getString("id");
								Float payout = item.getFloat("payout");
								JSONArray country_list = item.getJSONArray("country_list");
								String country = country_list.getString(0);
								JSONObject appObject = item.getJSONObject("app");
								String pkg = appObject.getString("package_name");
								String preview_url = appObject.getString("preview_url");
								String platform = appObject.getString("platform").toUpperCase();
								String icon_url = appObject.getString("icon_url");
								String kpi = item.getString("terms");
								String click_url = item.getString("click_url");
								String os_str = null;
								if (platform.equals("ANDROID")) {
									os_str = "0";
								} else if (platform.equals("IOS")) {
									os_str = "1";
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
								advertisersOffer.setMain_icon(icon_url);
								advertisersOffer.setPreview_url(preview_url);
								advertisersOffer.setClick_url(click_url);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(500);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(kpi));
								advertisersOffer.setOs(os_str);
								advertisersOffer.setDevice_type(0);// 设置为mobile类型
								advertisersOffer.setSend_type(0);// 系统入库生成广告
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);// 设置为激活状态
								advertisersOfferList.add(advertisersOffer);
							}
						}
					}

					logger.info("after filter pull offer size := " + advertisersOfferList.size());

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
		String str;
		try {
			str = HttpUtil.sendGet("https://api.affiliate.appcoachs.com/v1.5/getoffers?api_key=72d16c06d2cb040de4cbd0147c6a9fef1b4e8ed600dd1e9441df1e512cdbdda6&pa%20ge_index=1&page_size=1000");
			JSONObject jsonPullObject = JSON.parseObject(str);
			JSONArray offer_list = jsonPullObject.getJSONArray("offer_list");
			for (int i = 0; i < offer_list.size(); i++) {
				JSONObject item = offer_list.getJSONObject(i);
				if (item != null) {
					String payout_type = item.getString("payout_type").toUpperCase();
					if (payout_type.equals("CPI")) {
						String name = item.getString("name");
						String id = item.getString("id");
						Float payout = item.getFloat("payout");
						JSONArray country_list = item.getJSONArray("country_list");
						String country = country_list.getString(0);
						JSONObject appObject = item.getJSONObject("app");
						String pkg = appObject.getString("package_name");
						String preview_url = appObject.getString("preview_url");
						String platform = appObject.getString("platform").toUpperCase();
						String icon_url = appObject.getString("icon_url");
						String kpi = item.getString("name");
						String click_url = item.getString("terms");

					}
				}
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
