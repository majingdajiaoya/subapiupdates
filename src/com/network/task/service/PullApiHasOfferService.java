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

public class PullApiHasOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullApiHasOfferService.class);

	private Advertisers advertisers;

	public PullApiHasOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	public void run() {

		synchronized (this) {

			logger.info("doPull PullApiHasOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = this.advertisers.getId();

				String apiurl = this.advertisers.getApiurl();// 获取请求的url
				String apikey = this.advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.sendGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONObject JsonObjectresponse = jsonPullObject.getJSONObject("response");
				JSONObject offerDate = JsonObjectresponse.getJSONObject("data");
				offerDate = offerDate.getJSONObject("data");

				Iterator<String> iterator = offerDate.keySet().iterator();

				while (iterator.hasNext()) {
					try {
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();

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
						String payout_type = offerObject.getString("payout_type");// 类型
						String pkg = TuringStringUtil.getpkg(preview_url);
						String description = offerObject.getString("description");// 描述
						if (cap == 0) {
							cap = 99999;
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
						
						if(countryStr.indexOf("TW")>=0||countryStr.indexOf("HK")>=0){
							logger.info("countryStr"+countryStr);
							continue;
						}

						String click_url = trackingLinkObject.getString("click_url");// 获得click_url

						String impression_pixel = null;
						if (trackingLinkObject.getString("impression_pixel") != null) {
							impression_pixel = trackingLinkObject.getString("impression_pixel");
						}

						String os = "0";
						if (preview_url.contains("google")) {
							os = "0";
						} else if (preview_url.contains("apple")) {
							os = "1";
						} else {
							continue;
						}

						/**
						 * 排除处理
						 */
						// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
						if (OverseaStringUtil.isBlank(id) || price == null || OverseaStringUtil.isBlank(country) || OverseaStringUtil.isBlank(pkg)// 转换类型
								|| OverseaStringUtil.isBlank(click_url)// 点击url
						) {

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
						advertisersOffer.setOs(os);
						// advertisersOffer.setExpiration(expiration);//设置过期时间
						advertisersOffer.setPkg(pkg);// 设置包名
						advertisersOffer.setDaily_cap(cap);
						advertisersOffer.setDevice_type(0);// 设置为mobile类型
						advertisersOffer.setDescription(description);// 描述
						advertisersOffer.setSend_type(0);// 系统入库生成广告
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setImpressionUrl(impression_pixel);
						advertisersOffer.setStatus(0);// 设置为激活状态
						advertisersOfferList.add(advertisersOffer);
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

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}

		}

	}

	public static void main(String[] args) throws ClientProtocolException, IOException {
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("https://api.hasoffers.com/Apiv3/json?NetworkId=wmadv&Target=Affiliate_Offer&Method=findMyApprovedOffers&api_key=2d2801fea695bce9b677ad4880553d0bf4ab206a4ed4c8372fabe8f91e230107");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);
		PullApiHasOfferService mmm = new PullApiHasOfferService(tmp);
		mmm.run();

	}

}
