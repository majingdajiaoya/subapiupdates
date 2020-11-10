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
import com.network.task.util.HttpClientUtil;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullLemmonetOfferService implements PullOfferServiceTask {
	protected static Logger logger = Logger.getLogger(PullLemmonetOfferService.class);

	private Advertisers advertisers;

	public PullLemmonetOfferService(Advertisers advertisers) {

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
			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
			logger.info("doPull Adunity Offer begin := " + new Date());
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
				String charset = "utf-8";
				String httpOrgCreateTestRtn = HttpClientUtil.doPost(apiurl, null, charset);
				JSONObject json = JSON.parseObject(httpOrgCreateTestRtn);
				JSONArray offersArray = json.getJSONArray("result");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);
							if (item != null) {
								String name = item.getString("OfferName");
								String OfferID = item.getString("OfferID");
								String pkg = item.getString("PackageName");
								Float payout = item.getFloat("Payout");
								String AppIcon = item.getString("AppIcon");
								String Platform = item.getString("Platform").toUpperCase();
								String AppDescritpion = item.getString("AppDescritpion");
								String Notes = item.getString("Notes");
								Integer DailyCap = item.getInteger("DailyCap");
								String Countries = item.getString("Countries");
								Countries = Countries.replace("[", "");
								Countries = Countries.replace("]", "");
								Countries = Countries.replace("\"", "");
								Countries=Countries.replace(",", ":");
								String AppPreview = item.getString("AppPreview");
								String TrakingURL = item.getString("TrakingURL");
								TrakingURL = TrakingURL.replace("[INSERT_YOUR_PUBLISHER_ID]",
										"{channel}");
								TrakingURL = TrakingURL.replace("[INSERT_YOUR_CLICK]", "{aff_sub}");
								TrakingURL = TrakingURL.replace("[INSERT_IDFA]", "{idfa}");
								TrakingURL = TrakingURL.replace("[INSERT_GAID]", "{gaid}");
								String os_str = null;
								if (Platform.toUpperCase().equals("IOS")) {
									os_str = "1";
								} else if (Platform.toUpperCase().equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(OfferID);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(AppIcon);
								advertisersOffer.setPreview_url(AppPreview);
								advertisersOffer.setClick_url(TrakingURL);
								advertisersOffer.setCountry(Countries);
								advertisersOffer.setDaily_cap(DailyCap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer
										.setDescription(filterEmoji(Notes + AppDescritpion));
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

	public static void main(String[] args) {
		// 定义初始化变量
		String url = "https://lemmonetapi.azurewebsites.net/myoffers/";
		String charset = "utf-8";
		JSONArray array = null;
		String httpOrgCreateTestRtn = HttpClientUtil.doPost(url, null, charset);
		JSONObject json = JSON.parseObject(httpOrgCreateTestRtn);
		array = json.getJSONArray("result");
		if (array.size() > 0) {
			for (int i = 0; i < array.size(); i++) {
				JSONObject item = array.getJSONObject(i);
				if (item != null) {
					String name = item.getString("OfferName");
					String OfferID = item.getString("OfferID");
					String pkg = item.getString("PackageName");
					Float payout = item.getFloat("Payout");
					String AppIcon = item.getString("AppIcon");
					String Platform = item.getString("Platform").toUpperCase();
					String AppDescritpion = item.getString("AppDescritpion");
					String Notes = item.getString("Notes");
					Integer DailyCap = item.getInteger("DailyCap");
					String Countries = item.getString("Countries");
					Countries = Countries.replace("[", "");
					Countries = Countries.replace("]", "");
					Countries = Countries.replace("\"", "");
					String AppPreview = item.getString("AppPreview");
					String TrakingURL = item.getString("TrakingURL");
					TrakingURL = TrakingURL.replace("[INSERT_YOUR_PUBLISHER_ID]", "{channel}");
					TrakingURL = TrakingURL.replace("[INSERT_YOUR_CLICK]", "{aff_sub}");
					TrakingURL = TrakingURL.replace("[INSERT_IDFA]", "{idfa}");
					TrakingURL = TrakingURL.replace("[INSERT_GAID]", "{gaid}");
					System.out.println(TrakingURL);
				}
			}
		}
	}

}
