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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullInmobiOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullInmobiOfferService.class);

	private Advertisers advertisers;

	public PullInmobiOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull Inmobi1 Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();// 获取请求的url
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.sendGet(apiurl);

				JSONObject jsonObject = JSON.parseObject(str);

				JSONObject response = JSON.parseObject(jsonObject.getString("response"));
				String status = response.getString("status");

				if (!"1".equals(status)) {

					logger.info("status is not 1 return. status := " + status);

					return;
				}

				JSONObject jsonTotalData = JSON.parseObject(response.getString("data"));// 第一个data

				if (jsonTotalData == null) {

					logger.info("totalData is null, return");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONObject offerData = JSON.parseObject(jsonTotalData.getString("data"));// 第二个data

				if (offerData == null) {

					logger.info("offerData is null, return");
				}

				// 遍历data中每一个数据
				Iterator<String> iterator = offerData.keySet().iterator();

				while (iterator.hasNext()) {
					try {
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

						String offerIdkey = (String) iterator.next();

						String offerValue = offerData.getString(offerIdkey);

						JSONObject offerValueJsonObject = JSON.parseObject(offerValue);

						// 获得offer相关信息
						String offer = offerValueJsonObject.getString("Offer");
						String trackingLink = offerValueJsonObject.getString("TrackingLink");
						String country = offerValueJsonObject.getString("Country");

						if (OverseaStringUtil.isBlank(offer) || OverseaStringUtil.isBlank(trackingLink) || OverseaStringUtil.isBlank(country) || "[]".equals(country)) {

							logger.info("offerObject or trackingLinkObject or countryObject is null, continue");

							continue;
						}

						if (!country.startsWith("{")) {

							continue;
						}

						JSONObject offerObject = JSON.parseObject(offer);
						JSONObject trackingLinkObject = JSON.parseObject(trackingLink);
						JSONObject countryObject = JSON.parseObject(country);

						String id = offerObject.getString("id");
						String name = offerObject.getString("name");
						String preview_url = offerObject.getString("preview_url");// preview_Url
						Float price = offerObject.getFloat("default_payout");// 价格
						String description = offerObject.getString("description");// 描述
						// String
						// expiration=offerObject.getString("expiration_date");//过期时间
						String payout_type = offerObject.getString("payout_type");// 类型
																					// "cpa_flat"
						String conversion_cap = offerObject.getString("conversion_cap");// "1500"
						if ("0".equals(conversion_cap)) {
							conversion_cap = "99999";
						}

						String click_url = trackingLinkObject.getString("click_url");// 获得click_url

						/**
						 * 特殊处理字段
						 */

						// 获取国家
						String countryStr = "";

						Iterator<String> countryIterator = countryObject.keySet().iterator();

						while (countryIterator.hasNext()) {

							String countrykey = (String) countryIterator.next();

							countryStr += countrykey + ":";
						}

						// 去除最后一个:号
						if (!OverseaStringUtil.isBlank(countryStr)) {

							countryStr = countryStr.substring(0, countryStr.length() - 1);
						}

						// 获取pkg包名
						String pkg = "";
						String osStr = "";

						if (preview_url.contains("play.google.com")) {

							if (preview_url.contains("&hl")) {

								pkg = (String) preview_url.substring(preview_url.indexOf("?id=") + 4, preview_url.indexOf("&hl"));
							} else {
								if (preview_url.contains("&")) {
									pkg = (String) preview_url.substring(preview_url.indexOf("?id=") + 4, preview_url.indexOf("&"));
								} else {
									pkg = (String) preview_url.substring(preview_url.indexOf("?id=") + 4);

								}
							}
							osStr = "0";
						} else if (preview_url.contains("itunes.apple.com")) {

							if (preview_url.contains("?mt")) {

								pkg = (String) preview_url.substring(preview_url.indexOf("/id") + 3, preview_url.indexOf("?mt"));
							} else {

								pkg = (String) preview_url.substring(preview_url.indexOf("/id") + 3);
							}
							osStr = "1";
						}

						/**
						 * 排除处理
						 */
						// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
						if (OverseaStringUtil.isBlank(id) || price == null || OverseaStringUtil.isBlank(country) || OverseaStringUtil.isBlank(pkg)// 转换类型
								|| OverseaStringUtil.isBlank(click_url)// 点击url
								|| OverseaStringUtil.isBlank(conversion_cap)) {

							continue;
						}

						// pkg名称太长
						if (pkg.length() > 195) {

							continue;
						}
						String nameIncent = name.toUpperCase().trim();

						if ("CPA_FLAT".equals(payout_type.toUpperCase())) {

							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
						} else {

							advertisersOffer.setCost_type(104);
							advertisersOffer.setOffer_type(104);// 设置为其它类型
							advertisersOffer.setConversion_flow(104);
						}

						advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setName(name);// 网盟offer名称
						advertisersOffer.setPreview_url(preview_url);// 预览链接
						advertisersOffer.setCountry(countryStr);
						advertisersOffer.setPayout(price);
						advertisersOffer.setOs(osStr);
						// advertisersOffer.setExpiration(expiration);//设置过期时间
						advertisersOffer.setPkg(pkg);// 设置包名
						advertisersOffer.setDaily_cap(Integer.parseInt(conversion_cap));
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
						advertisersOffer.setDescription(description);// 描述
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);// 设置为激活状态
						advertisersOfferList.add(advertisersOffer);
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

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

	public static void main(String[] args) throws ClientProtocolException, IOException {
		String apiurl = "https://api.hasoffers.com/Apiv3/json?NetworkId=wmadv&Target=Affiliate_Offer&Method=findMyApprovedOffers&api_key=9180ecfd192ac4d21e15cf3d0f1a1db88e25052fd67690e4fb2bcb3e776399f5&contain%5B%5D=TrackingLink&contain%5B%5D=Country&contain%5B%5D=OfferCategory&limit=10000&filters%5Bpreview_url%5D%5BLIKE%5D=%25play.google.com%25";
		String str = HttpUtil.sendGet(apiurl);
		JSONObject jsonPullObject = JSON.parseObject(str);
		JSONObject JsonObjectresponse = jsonPullObject.getJSONObject("response");
		JSONObject offerDate = JsonObjectresponse.getJSONObject("data");
		offerDate = offerDate.getJSONObject("data");

		Iterator<String> iterator = offerDate.keySet().iterator();

		while (iterator.hasNext()) {

			String offerIdkey = (String) iterator.next();

			String offerValue = offerDate.getString(offerIdkey);
			JSONObject offerValueJsonObject = JSON.parseObject(offerValue);

			// 获得offer相关信息
			// 获得offer相关信息
			String offer = offerValueJsonObject.getString("Offer");
			String trackingLink = offerValueJsonObject.getString("TrackingLink");
			String country = offerValueJsonObject.getString("Country");

			if (OverseaStringUtil.isBlank(offer) || OverseaStringUtil.isBlank(trackingLink) || OverseaStringUtil.isBlank(country) || "[]".equals(country)) {

				logger.info("offerObject or trackingLinkObject or countryObject is null, continue");

				continue;
			}

			if (!country.startsWith("{")) {

				continue;
			}
			JSONObject offerObject = JSON.parseObject(offer);
			JSONObject trackingLinkObject = JSON.parseObject(trackingLink);
			JSONObject countryObject = JSON.parseObject(country);

			String id = offerObject.getString("id");
			String name = offerObject.getString("name");
			String preview_url = offerObject.getString("preview_url");// preview_Url
			Float price = offerObject.getFloat("default_payout");// 价格
			Integer cap = offerObject.getInteger("conversion_cap");
			if (cap == 0) {
				continue;
			}
			// 获取国家
			String countryStr = "";

			Iterator<String> countryIterator = countryObject.keySet().iterator();

			while (countryIterator.hasNext()) {

				String countrykey = (String) countryIterator.next();

				countryStr += countrykey + ":";
			}

			// 去除最后一个:号
			if (!OverseaStringUtil.isBlank(countryStr)) {

				countryStr = countryStr.substring(0, countryStr.length() - 1);
			}
			
			String click_url = trackingLinkObject.getString("click_url");// 获得click_url
			System.out.println(click_url);
		}

	}

}
