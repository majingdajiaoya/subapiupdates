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

public class PullCCOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullCCOfferService.class);

	private Advertisers advertisers;

	public PullCCOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullCCOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullCCOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				Map<String, String> header = new HashMap<String, String>();
				header.put("Authorization", apikey);

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				String str = HttpUtil.sendGet(apiurl, header);

				JSONArray offersArray = JSON.parseArray(str);

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + str);

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {
								Float payout = item.getFloat("campaign_payout");
								String name = item.getJSONObject("offer").getString("name");
								String pkg = item.getJSONObject("offer").getString("package_name");
								String offerId = item.getJSONObject("offer").getString("id");
								String previewlink = item.getJSONObject("offer").getString("preview_link");
								String description = item.getJSONObject("offer").getString("kpi_requirements");
								JSONArray JSONArraycountries = item.getJSONObject("offer").getJSONObject("tags").getJSONArray("countries");
								String countries = "";
								if (JSONArraycountries == null || JSONArraycountries.size() == 0) {
									continue;
								}
								for (int j = 0; j < JSONArraycountries.size(); ++j) {
									JSONObject countryItem = JSONArraycountries.getJSONObject(j);
									countries = countries + countryItem.getString("code") + ":";
								}
								JSONArray JSONArrayOs = item.getJSONObject("offer").getJSONObject("tags").getJSONArray("platforms");
								JSONObject ObjectOs = (JSONObject) JSONArrayOs.get(0);
								String platform = ObjectOs.getString("name");
								countries = countries.substring(0, countries.length() - 1);
								String trackinglink = item.getString("url");
								String icon = "";
								String os_str = null;
								if (platform.toUpperCase().equals("IOS")) {
									os_str = "1";
								} else if (platform.toUpperCase().equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}

								if (item.getJSONObject("offer").getJSONArray("thumbnails").size() > 0) {
									JSONObject iconObject = (JSONObject) item.getJSONObject("offer").getJSONArray("thumbnails").get(0);
									icon = iconObject.getString("link");
									AdvertisersOffer advertisersOffer = new AdvertisersOffer();
									advertisersOffer.setAdv_offer_id(offerId);
									advertisersOffer.setName(filterEmoji(name));
									advertisersOffer.setCost_type(101);

									advertisersOffer.setOffer_type(101);
									advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
									advertisersOffer.setPkg(pkg);
									advertisersOffer.setMain_icon(icon);
									advertisersOffer.setPreview_url(previewlink);
									advertisersOffer.setClick_url(trackinglink);
									advertisersOffer.setCountry(countries);
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

						} catch (Exception e) {
							e.printStackTrace();
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
		Advertisers tmp = new Advertisers();
		// {apikey}
		// https://admin.appnext.com/offerApi.aspx?pimp=1&id=b72a0d89-232a-46ea-b613-7762691e0a01&city=1&ua=1&sub_accounts=1
		tmp.setApiurl("https://api.trackkor.com/api-ext/v1/campaigns/?status=08");
		// tmp.setApikey("b72a0d89-232a-46ea-b613-7762691e0a01");//android
		// 7b613f43-9e09-4295-a09f-301e22f8f08a
		tmp.setApikey("Token 821679691fe88f9c42a0204ad3fdaaa36dd14c6f");
		tmp.setId(25L);

		PullCCOfferService mmm = new PullCCOfferService(tmp);
		mmm.run();
	}

}
