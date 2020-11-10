package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

public class PullInmobi2OfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullInmobi2OfferService.class);

	private Advertisers advertisers;

	public PullInmobi2OfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull Inmobi2 Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();// 获取请求的url
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				String generate = "https://api.inmobi.com/v1.0/generatesession/generate";

				Map<String, String> header2 = new HashMap<String, String>();
				header2.put("secretKey", "c5b17928f1c34f4c8413ba527386f2ea");
				header2.put("userName", "michelle.hu@cappumedia.com");
				header2.put("password", "Cappumedia123@");
				String str2 = HttpUtil.sendGet(generate, header2);
				JSONObject jsonObject2 = JSON.parseObject(str2);
				String sessionId = "";
				String accountId = "";
				JSONArray respList = jsonObject2.getJSONArray("respList");
				if (respList != null && respList.size() > 0) {
					JSONObject item = respList.getJSONObject(0);
					if (item != null) {
						sessionId = item.getString("sessionId");
						accountId = item.getString("accountId");
					} else {
						logger.info("get sessionId error");
						return;
					}

				}
				Map<String, String> header = new HashMap<String, String>();

				header.put("secretKey", apikey);

				header.put("sessionId", sessionId);
				header.put("accountId", accountId);

				String str = HttpUtil.sendGet(apiurl, header);

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

					AdvertisersOffer advertisersOffer = new AdvertisersOffer();

					String offerIdkey = (String) iterator.next();

					String offerValue = offerData.getString(offerIdkey);

					JSONObject offerValueJsonObject = JSON.parseObject(offerValue);

					// 获得offer相关信息
					String offer = offerValueJsonObject.getString("Offer");
					String trackingLink = offerValueJsonObject.getString("TrackingLink");
					String country = offerValueJsonObject.getString("Country");
					String Creative = offerValueJsonObject.getString("Creative");

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
					JSONObject CreativeObject = JSON.parseObject(Creative);

					String id = offerObject.getString("id");
					String name = offerObject.getString("name");
					String preview_url = offerObject.getString("preview_url");// preview_Url
					Float price = offerObject.getFloat("default_payout");// 价格
					String description = offerObject.getString("description");// 描述
					String conversion_cap = offerObject.getString("conversion_cap");// "1500"
					String click_url = offerObject.getString("click_url");
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

					String os = offerObject.getString("os");
					if ("ANDROID".equals(os.toUpperCase())) {
						osStr = "0";
					} else if ("IOS".equals(os.toUpperCase())) {
						osStr = "1";
					} else {
						continue;
					}
					pkg = "";

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

					} else if (preview_url.contains("itunes.apple.com")) {

						if (preview_url.contains("?mt")) {

							pkg = (String) preview_url.substring(preview_url.indexOf("/id") + 3, preview_url.indexOf("?mt"));
						} else {

							pkg = (String) preview_url.substring(preview_url.indexOf("/id") + 3);
						}

					}
					Iterator<String> CreativeIterator = CreativeObject.keySet().iterator();
					ArrayList<Map> arrayList = new ArrayList<Map>();
					JSONArray images_JSONArray = new JSONArray();
					String images_crative = null;

					while (CreativeIterator.hasNext()) {
						if (arrayList.size() > 3) {
							break;
						}

						String creativekey = (String) CreativeIterator.next();
						String creativeValue = CreativeObject.getString(creativekey);
						JSONObject creativeObject = JSON.parseObject(creativeValue);

						String url = creativeObject.getString("url");
						Map jsonObject11 = new HashMap();
						jsonObject11.put("url", url);
						jsonObject11.put("size", "*");
						arrayList.add(jsonObject11);

					}
					if (arrayList.size() > 0) {
						images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
						images_crative = images_JSONArray.toString();
					}
					logger.info("---------------------");
					logger.info("offer_"+id);
					logger.info("offer_"+country);
					logger.info("offer_"+pkg);
					logger.info("offer_"+click_url);
					logger.info("offer_"+conversion_cap);
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

					if ("0".equals(conversion_cap)) {

						continue;
					}

					// 判断是否是非激励类型
					
					
					Integer incent_type = 0;

					advertisersOffer.setCost_type(101);
					advertisersOffer.setOffer_type(101);
					advertisersOffer.setConversion_flow(101);

					advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
					advertisersOffer.setAdv_offer_id(id);
					advertisersOffer.setClick_url(click_url);
					advertisersOffer.setName(name);// 网盟offer名称
					advertisersOffer.setPreview_url(preview_url);// 预览链接
					advertisersOffer.setCountry(countryStr);
					advertisersOffer.setPayout(price);
					advertisersOffer.setOs(osStr);

					advertisersOffer.setCreatives(images_crative);
					advertisersOffer.setPkg(pkg);// 设置包名
					advertisersOffer.setDaily_cap(Integer.parseInt(conversion_cap));
					advertisersOffer.setDevice_type(0);// 设置为mobile类型
					advertisersOffer.setDescription(filterEmoji(description));// 描述
					advertisersOffer.setIncent_type(incent_type);
					advertisersOffer.setSend_type(0);// 系统入库生成广告
					advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
					advertisersOffer.setStatus(0);// 设置为激活状态
					advertisersOfferList.add(advertisersOffer);
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

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) throws ClientProtocolException, IOException {
		Advertisers tmp = new Advertisers();
		// https://api.inmobi.com/iap/v0/json?api_key=997a6a54-1cad-4e41-a57f-228ae0560664&target=affiliate_offer&method=findMyApprovedOffers
		tmp.setApiurl("http://api.inmobi.com/iap/v0/json?api_key=997a6a54-1cad-4e41-a57f-228ae0560664&target=affiliate_offer&method=findMyApprovedOffers");
		tmp.setApikey("c5b17928f1c34f4c8413ba527386f2ea");
		tmp.setId(21L);

		PullInmobi2OfferService mmm = new PullInmobi2OfferService(tmp);
		mmm.run();

	}

}
