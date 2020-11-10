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
import com.network.task.dao.AdvertisersOfferDao;
import com.network.task.dao.AffiliatesOfferDao;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullTABOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullTABOfferService.class);

	private Advertisers advertisers;

	public PullTABOfferService(Advertisers advertisers) {

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

			logger.info("doPull PullTABOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				Map<String, String> header = new HashMap<String, String>();
				header.put("X-Eflow-API-Key", apikey);
				String str = HttpUtil.sendGet(apiurl, header);
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				if (offersArray != null && offersArray.size() > 0) {
					logger.info("begin pull offer PullTABOfferService" + offersArray.size());
					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);
							if (item != null) {
								String offer_status = item.getString("offer_status");
								if (!offer_status.contains("active")) {
									continue;
								}
								String offerid = item.getString("network_offer_id");
								
								String name = item.getString("name");
								Integer cap = item.getInteger("daily_conversion_cap");
								if (cap == 0) {
									cap = 100;
								}
								String previewlink = item.getString("preview_url");
								String tracking_url = item.getString("tracking_url");
								JSONObject relationship = item.getJSONObject("relationship");
								JSONObject ruleset = relationship.getJSONObject("ruleset");
								JSONArray countries = ruleset.getJSONArray("countries");
								JSONArray platforms = ruleset.getJSONArray("platforms");
								if (platforms == null || platforms.size() == 0) {
									continue;
								}
								
								if (countries == null || countries.size() == 0) {
									continue;
								}
								
								if (previewlink.length() == 0) {
									continue;
								}
								
								if (tracking_url.length() == 0) {
									continue;
								}
								JSONObject itemCountry = (JSONObject) countries.get(0);
								JSONObject itemplatforms = (JSONObject) platforms.get(0);
								String os = itemplatforms.getString("label");
								String country = itemCountry.getString("country_code");
								String description = item.getString("html_description");
								JSONObject payouts = relationship.getJSONObject("payouts");
								JSONArray entries = payouts.getJSONArray("entries");
								if (entries == null || entries.size() == 0) {
									continue;
								}
					
								JSONObject payout_amount = (JSONObject) entries.get(0);
								Float payout = payout_amount.getFloat("payout_amount");
								String thumbnail_url = item.getString("thumbnail_url");
								String os_str = null;
								String pkg = "";
								if (previewlink.indexOf("itunes.apple.com") > 0) {
									if (previewlink.indexOf("id") > 0) {
										pkg = previewlink.substring(previewlink.indexOf("id"), previewlink.length());
										if (pkg.indexOf("?") > 0) {
											pkg = pkg.substring(pkg.indexOf("id"), pkg.indexOf("?"));
										} else {
											pkg = pkg.substring(pkg.indexOf("id"), pkg.length());
										}
										pkg = pkg.replace("id", "");
										os_str = "1";
									}

								} else if (previewlink.indexOf("play.google.com") > 0) {
									if (previewlink.indexOf("id") > 0) {
										pkg = previewlink.substring(previewlink.indexOf("?id") + 4, previewlink.length());
										if (pkg.indexOf("&") > 0) {
											pkg = pkg.substring(0, pkg.indexOf("&"));
										}
										os_str = "0";
									} else {
									}
								} else {
								}
								if (pkg.indexOf("/") > 0) {
									pkg = pkg.substring(pkg.indexOf("/") + 1, pkg.length());
								}
								
								logger.info("jsonPullObject" + offerid);
								logger.info("jsonPullObject" + payout);
								
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerid);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(thumbnail_url);
								advertisersOffer.setPreview_url(previewlink);
								advertisersOffer.setClick_url(tracking_url);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(cap);
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

						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					logger.info("after filter pull offer size := " + advertisersOfferList.size());

					// 入网盟广告
					if (advertisersId != null && advertisersOfferList != null && advertisersOfferList.size() > 0) {

						PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper.getBean("pullOfferCommonService");

						pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
					} else {
						AdvertisersOfferDao advertisersOfferDao = (AdvertisersOfferDao) TuringSpringHelper.getBean("advertisersOfferDao");
						advertisersOfferDao.updateStatus(advertisers.getId(), -2);
						AffiliatesOfferDao affiliatesOfferDao = (AffiliatesOfferDao) TuringSpringHelper.getBean("affiliatesOfferDao");
						affiliatesOfferDao.updateStatus(advertisers.getId(), -2);
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
		// https://api.startappservice.com/1.1/management/bulk/campaigns?partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&payout=0.1
		// {apikey}
		//

		tmp.setApiurl("https://api.eflow.team/v1/affiliates/offers");
		tmp.setApikey("t3KvndCdTdmX5ljx5Tp0A");
		tmp.setId(1053L);

		PullTABOfferService mmm = new PullTABOfferService(tmp);
		mmm.run();
	}

}
