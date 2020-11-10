package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullVoloOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullVoloOfferService.class);

	private Advertisers advertisers;

	public PullVoloOfferService(Advertisers advertisers) {

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

			// 黑名单
			logger.info("doPull PullYouAppiOfferService Offer begin := "
					+ new Date());
			try {
				JSONObject json;
				JSONArray array = null;
				Long advertisersId = advertisers.getId();
				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				apiurl = apiurl.replace("{apikey}", apikey);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				String str = HttpUtil.sendGet(apiurl);
				json = JSON.parseObject(str);
				JSONObject responseObject = json.getJSONObject("response");
				responseObject = responseObject.getJSONObject("data");
				Map<String, Object> dataMap = responseObject;
				for (String key : dataMap.keySet()) {
					responseObject = (JSONObject) dataMap.get(key);
					JSONObject OfferObject = responseObject
							.getJSONObject("Offer");
					String status = OfferObject.getString("status");
					if (!status.equals("active")) {
						continue;
					}
					JSONObject countryObject = responseObject
							.getJSONObject("Country");
					Map<String, Object> countryMap = countryObject;
					String country = (countryMap.keySet() + "")
							.replace("[", "").replace("]", "").replace(" ", "")
							.replace(",", ":");
					Float payout = OfferObject.getFloat("default_payout");
					Integer cap = OfferObject.getInteger("conversion_cap");
					String preview_url = OfferObject.getString("preview_url");
					String pkg = TuringStringUtil.getpkg(preview_url);
					String os = TuringStringUtil.getPlatoms(preview_url);
					String name = OfferObject.getString("name");
					String description = OfferObject.getString("description");
					// Thumbnail
					JSONObject ThumbnailObject = responseObject
							.getJSONObject("Thumbnail");
					String icon="";
					if(ThumbnailObject!=null){
						 icon = ThumbnailObject.getString("url");
					}
					JSONObject TrackingLinkObject = responseObject
							.getJSONObject("TrackingLink");
					String click_url = TrackingLinkObject
							.getString("click_url");
					AdvertisersOffer advertisersOffer = new AdvertisersOffer();
					advertisersOffer.setAdv_offer_id(key);
					advertisersOffer.setName(filterEmoji(name));
					advertisersOffer.setCost_type(101);
					advertisersOffer.setOffer_type(101);
					advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
					advertisersOffer.setPkg(pkg);
					advertisersOffer.setMain_icon(icon);
					advertisersOffer.setPreview_url(preview_url);
					advertisersOffer.setClick_url(click_url);
					advertisersOffer.setCountry(country);
					advertisersOffer.setDaily_cap(cap);
					advertisersOffer.setPayout(payout);
					advertisersOffer.setCreatives(null);
					advertisersOffer.setConversion_flow(101);
					advertisersOffer.setDescription(filterEmoji(description));
					advertisersOffer.setOs(os);
					advertisersOffer.setDevice_type(0);// 设置为mobile类型
					advertisersOffer.setSend_type(0);// 系统入库生成广告
					advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
					advertisersOffer.setStatus(0);// 设置为激活状态
					advertisersOfferList.add(advertisersOffer);

				}

				// 入网盟广告
				if (advertisersId != null && advertisersOfferList != null
						&& advertisersOfferList.size() > 0) {

					PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
							.getBean("pullOfferCommonService");

					pullOfferCommonService.doPullOffer(advertisers,
							advertisersOfferList);
				}
			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll(
					"[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) {
		// 定义初始化变量
		JSONObject json;
		JSONArray array = null;
		try {
			String apiurl = "http://voloads.com/api/v1/offers?key=8839a34e10bed2b31d7506c3204b91696ca2dd2e8713064d5b3b826375eba046";
			String str = HttpUtil.sendGet(apiurl);
			json = JSON.parseObject(str);
			JSONObject responseObject = json.getJSONObject("response");
			responseObject = responseObject.getJSONObject("data");
			Map<String, Object> dataMap = responseObject;
			for (String key : dataMap.keySet()) {
				responseObject = (JSONObject) dataMap.get(key);
				JSONObject OfferObject = responseObject.getJSONObject("Offer");
				String status = OfferObject.getString("status");
				if (!status.equals("active")) {
					continue;
				}
				System.out.println(key);
				JSONObject countryObject = responseObject
						.getJSONObject("Country");
				Map<String, Object> countryMap = countryObject;
				String country = (countryMap.keySet() + "").replace("[", "")
						.replace("]", "").replace(" ", "").replace(",", ":");
				Float payout = OfferObject.getFloat("default_payout");
				Integer cap = OfferObject.getInteger("conversion_cap");
				String preview_url = OfferObject.getString("preview_url");
				String pkg = TuringStringUtil.getpkg(preview_url);
				String os = TuringStringUtil.getPlatoms(preview_url);
				String name = OfferObject.getString("name");
				String description = OfferObject.getString("description");
				// Thumbnail
				JSONObject ThumbnailObject = responseObject
						.getJSONObject("Thumbnail");
				String icon="";
				if(ThumbnailObject!=null){
					 icon = ThumbnailObject.getString("url");
				}
				
				JSONObject TrackingLinkObject = responseObject
						.getJSONObject("TrackingLink");
				String click_url = TrackingLinkObject.getString("click_url");
				System.out.println();
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
