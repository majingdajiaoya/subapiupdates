package com.network.task.service;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullAltamob2NewOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAltamob2NewOfferService.class);

	private Advertisers advertisers;

	public PullAltamob2NewOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull altamob new v2  Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				Map<String, String> header = new HashMap<String, String>();

				header.put("token", apikey);

				String str = HttpUtil.sendGet(apiurl, header);

				JSONObject jsonPullObject = JSON.parseObject(str);

				JSONArray offersArr = jsonPullObject.getJSONArray("data");

				if (offersArr == null || offersArr.size() == 0) {

					logger.info("offersArray is empty");

					return;
				}

				logger.info("begin pull offer " + offersArr.size());

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				for (int i = 0; i < offersArr.size(); i++) {

					JSONObject item = offersArr.getJSONObject(i);

					if (item != null) {

						Integer offer_id = item.getInteger("offerId");
						String title = item.getString("appName");
						String icon_url = item.getString("appIconUrl");
						String pkg_name = item.getString("pkg_name");
						Float maxpayout = item.getFloat("payout");
						String click_url = item.getString("trackingLink");
						String app_store_url = item.getString("previewLink");
						// String countries = item.getString("geo");
						boolean isIncentbool = item.getBooleanValue("isIncent");
						Integer isIncent = 0;
						if (isIncentbool) {
							isIncent = 1;
						} else {
							isIncent = 0;
						}

						String cover_url = item.getString("cover_url");
						JSONArray countryObject = item.getJSONArray("supportedCountries");
						String min_os_version = item.getString("minOsVersion");
						String description = "";
						description = item.getString("description");
						if ("".equals(description)) {
							description = title;
						}

						String countries = "";
						if (countryObject != null && countryObject.size() > 0) {
							for (int k = 0; k < countryObject.size(); k++) {
								JSONObject countryitem = countryObject.getJSONObject(k);
								if (countryitem != null) {
									String country = countryitem.getString("country");
									if ("".equals(countries)) {
										countries = country;
									} else {
										countries = countries + ":" + country;
									}
								}

							}

						}
						// http://pixel.admobclick.com/v1/ad/click?subsite_id=30829&
						// transaction_id={transaction_id}&id=1271&offer_id=142611501&geo={geo}&aid={aid}&client_version={client_version}&gaid={gaid}&tmark=1522295239141&p={p}&d=ZLABKDAFB

						click_url = click_url.replace("{transaction_id}", "{aff_sub}");
						click_url = click_url.replace("{p}", "");
						click_url = click_url.replace("{aid}", "{andid}");
						click_url = click_url.replace("{geo}", "{country}");
						click_url = click_url.replace("{client_version}", "");

						if (offer_id == null || OverseaStringUtil.isBlank(pkg_name) || maxpayout == null || OverseaStringUtil.isBlank(countries) || OverseaStringUtil.isBlank(click_url)) {

							logger.info("offer_id := " + offer_id + " or pkg_name := " + pkg_name + " or maxpayout := " + maxpayout + " or countries := " + countries + " or click_url := " + click_url + " is empty, return");

							continue;
						}

						if (maxpayout <= 0.06) {

							logger.info("payout is lower := " + maxpayout + " return");

							continue;
						}
						String platforms = item.getString("platforms").toUpperCase().replace("[\"", "").replace("\"]", "");
						String os_str = "";
						if (platforms.equals("IOS")) {
							os_str = "1";
						} else if (platforms.equals("ANDROID")) {
							os_str = "0";
						} else {
							continue;
						}

						// 非激励量
						// Integer incetive = Integer.valueOf(0);
						String images_crative = null;
						if (cover_url != null) {
							JSONArray images_JSONArray = new JSONArray();

							ArrayList<Map> arrayList = new ArrayList<Map>();
							Map jsonObject = new HashMap();
							jsonObject.put("url", cover_url);
							jsonObject.put("size", "1200x620");
							arrayList.add(jsonObject);
							images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
							images_crative = images_JSONArray.toString();
						}

						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));// 以offer_id+geo组合做主键
						advertisersOffer.setName(title);

						advertisersOffer.setCost_type(Integer.valueOf(101));
						advertisersOffer.setOffer_type(Integer.valueOf(101));
						advertisersOffer.setConversion_flow(Integer.valueOf(101));

						advertisersOffer.setAdvertisers_id(advertisersId);

						advertisersOffer.setPkg(pkg_name);
						advertisersOffer.setMain_icon(icon_url);
						advertisersOffer.setPreview_url(app_store_url);
						advertisersOffer.setClick_url(click_url);

						advertisersOffer.setCountry(countries);

						advertisersOffer.setDaily_cap(Integer.valueOf(99999));

						advertisersOffer.setPayout(maxpayout);

						advertisersOffer.setDescription(filterEmoji(description));
						advertisersOffer.setOs(os_str);

						advertisersOffer.setCreatives(images_crative);
						advertisersOffer.setDevice_type(Integer.valueOf(0));
						advertisersOffer.setOs_version(min_os_version);
						advertisersOffer.setSend_type(Integer.valueOf(0));
						advertisersOffer.setIncent_type(isIncent);
						advertisersOffer.setSmartlink_type(Integer.valueOf(1));
						advertisersOffer.setStatus(Integer.valueOf(0));

						advertisersOfferList.add(advertisersOffer);
					}
				}

				logger.info("after filter pull offer size := " + advertisersOfferList.size());

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
		Advertisers tmp = new Advertisers();
		// {apikey}
		// http://api.altamob.com/adfetch/v2/s2s/campaign/fetch?platform=android
		tmp.setApiurl("http://api.altamob.com/adfetch/v2/s2s/campaign/fetch?platform=android");
		tmp.setApikey("5f356628-5910-c117-43f7-19bda98a0733");
		tmp.setId(19L);

		PullAltamob2NewOfferService mmm = new PullAltamob2NewOfferService(tmp);
		mmm.run();
	}

}
