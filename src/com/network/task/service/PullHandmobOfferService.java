package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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

public class PullHandmobOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullHandmobOfferService.class);

	private Advertisers advertisers;

	public PullHandmobOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullHandmobOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullMobpowertechOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				String str = HttpUtil.sendGet(apiurl);
				JSONArray offersArray = JSON.parseArray(str);
				if (offersArray != null && offersArray.size() > 0) {
					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						JSONObject item = offersArray.getJSONObject(i);
						String offerId = item.getString("id");
						String title = item.getString("title");
						String pkg = item.getString("pkg");
						String appStoreUrl = item.getString("appStoreUrl");
						String clk_url = item.getString("clk_url");
						String country = item.getString("country");
						String os = item.getString("os");
						Float payout = item.getFloat("payout");
						String icon = item.getString("icon");
						
						
						
						
						
						
						
						
						
						
						
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

	public static void main(String[] args) throws ClientProtocolException, IOException {
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		String apiurl = "http://13.229.213.140/apps/ssp/offers?unitid=01c5ebe5186d92d82e5fdd7049480a60";
		String str = HttpUtil.sendGet(apiurl);
		JSONArray offersArray = JSON.parseArray(str);
		if (offersArray != null && offersArray.size() > 0) {
			logger.info("begin pull offer " + offersArray.size());

			for (int i = 0; i < offersArray.size(); i++) {
				JSONObject item = offersArray.getJSONObject(i);
				String offerId = item.getString("id");
				String title = item.getString("title");
				String pkg = item.getString("pkg");
				String appStoreUrl = item.getString("appStoreUrl");
				String clk_url = item.getString("clk_url");
				String country = item.getString("country");
				country=country.replace(",", ":");
				String os = item.getString("os");
				Float payout = item.getFloat("payout");
				String icon = item.getString("icon");
				System.out.println(offerId);
			}
		}

		// Advertisers tmp = new Advertisers();
		// tmp.setApiurl("http://api.PullMobpowertechOfferService.cn/api_offline?appid=6093&time=1555565777&sign=9854e6df390d2ec4e8bb8647ca96f2c1");
		// tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		// tmp.setId(1069L);
		// PullMobpowertechOfferService mmm = new
		// PullMobpowertechOfferService(tmp);
		// mmm.run();
	}

}
