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
import org.apache.http.client.ClientProtocolException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PullOffersLookService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullOffersLookService.class);

	private Advertisers advertisers;

	public PullOffersLookService(Advertisers advertisers) {

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

			logger.info("doPull PullYMOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);
				int pages = getTotalPages(apiurl,apikey);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				for (int page = 1; page <= pages; page++) {
					advertisersOfferList.addAll(getPageOffers(advertisersId, apiurl,apikey, page));
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

	/**
	 *
	 * @param advertisersId
	 * @param apiurl
	 * @param apikey 拉取加密后key
	 * @param page
	 * @return
	 */
	private List<AdvertisersOffer> getPageOffers(Long advertisersId, String apiurl, String apikey,int page) {
		Map<String, String> header = new HashMap<String, String>();
		header.put("Authorization", "Basic "+apikey);
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		apiurl = apiurl.replace("{page}", page + "");
		try {
			String jsonStr = HttpUtil.sendGet(apiurl, header);
			JSONObject json = JSON.parseObject(jsonStr);
			JSONObject offersJSONObject = json.getJSONObject("data");
			JSONArray rowObject = offersJSONObject.getJSONArray("rowset");
			for (int i = 0; (rowObject != null) && (i < rowObject.size()); i++) {
				JSONObject rowItem = rowObject.getJSONObject(i);
				JSONObject offerItem = rowItem.getJSONObject("offer");
//				System.out.println("offerItem======="+offerItem);
				JSONObject countryJSONObject = rowItem.getJSONObject("offer_geo");
				JSONArray countrytarget = countryJSONObject.getJSONArray("target");

				String countries = "";
				for (int j = 0; (countrytarget != null) && (j < countrytarget.size()); j++) {
					JSONObject countryItem = countrytarget.getJSONObject(j);
					String country = countryItem.getString("country_code");
					if (countrytarget.size() == 1) {
						countries = country;
					} else {
						countries = countries + country + ":";
					}
				}
				if (countrytarget.size() > 1) {
					countries = countries.substring(0, countries.length() - 1);
				}
				String name = offerItem.getString("name");
				if (name.indexOf("test") > 0) {
					continue;
				}
				String pkg = offerItem.getString("app_id");
				String previewlink = offerItem.getString("preview_url");

				boolean ifNum =false;//判断pkg是否为纯数字
				//判断是否为纯数字，如果true为设备为ios，false安卓
				ifNum=isNumeric(pkg);
				String os_str = null;
				if (ifNum){
					previewlink="https://itunes.apple.com/app/id"+pkg;
				}else{
					previewlink="https://play.google.com/store/apps/details?id="+pkg;
				}
				if (previewlink.indexOf("play.google.com") > 0) {
					os_str = "0";
				} else if (previewlink.indexOf("apple") > 0) {
					os_str = "1";
				} else {
					continue;
				}
				Float payout = offerItem.getFloat("payout");
				String offerid = offerItem.getString("id");
				String tracking_link = offerItem.getString("tracking_link");
				if (tracking_link.indexOf("apply") > 0) {
					continue;
				}
				Object cap = rowItem.getJSONObject("offer_cap");
				Integer dailyCap =0;
				if (cap==null){
					dailyCap=100;
				}else {
					dailyCap = Integer.parseInt(cap.toString());
				}

				String status = offerItem.getString("status").toUpperCase();
				if (!status.equals("ACTIVE")) {
					continue;
				}

//				logger.info("offerid :"+offerid);
//				logger.info("name :"+name);
//				logger.info("advertisersId :"+advertisersId);
//				logger.info("pkg :"+pkg);
//				logger.info("previewlink :"+previewlink);
//				logger.info("tracking_link :"+tracking_link);
//				logger.info("countries :"+countries);
//				logger.info("dailyCap :"+dailyCap);
//				logger.info("payout :"+payout);
//				logger.info("os_str :"+os_str);



				AdvertisersOffer advertisersOffer = new AdvertisersOffer();
				advertisersOffer.setAdv_offer_id(offerid);
				advertisersOffer.setName(filterEmoji(name));
				advertisersOffer.setCost_type(101);

				advertisersOffer.setOffer_type(101);
				advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
				advertisersOffer.setPkg(pkg);
				advertisersOffer.setMain_icon("");
				advertisersOffer.setPreview_url(previewlink);
				advertisersOffer.setClick_url(tracking_link);
				advertisersOffer.setCountry(countries);
				advertisersOffer.setDaily_cap(dailyCap);
				advertisersOffer.setPayout(payout);
				advertisersOffer.setCreatives("");
				advertisersOffer.setConversion_flow(101);
				advertisersOffer.setDescription(filterEmoji(""));
				advertisersOffer.setOs(os_str);
				advertisersOffer.setDevice_type(0);// 设置为mobile类型
				advertisersOffer.setSend_type(0);// 系统入库生成广告
				advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
				advertisersOffer.setStatus(0);// 设置为激活状态
				advertisersOfferList.add(advertisersOffer);

			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return advertisersOfferList;
	}


	public boolean isNumeric(String str){
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if( !isNum.matches() ){
			return false;
		}
		return true;
	}
		private static int getTotalPages(String offerUrl,String apikey) {
		try {
			Map<String, String> header = new HashMap<String, String>();
			header.put("Authorization", "Basic "+apikey);
			String jsonStr = HttpUtil.sendGet(offerUrl, header);
			JSONObject json = JSON.parseObject(jsonStr);
			JSONObject offersJSONObject = json.getJSONObject("data");
			int page = offersJSONObject.getInteger("totalPages");
			return page;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) throws Exception {
//		try {
////			String sql = "SELECT tt.*, temp.* "
////					+ " FROM (SELECT * FROM advertisers_offer t WHERE id in ( SELECT min(id) from advertisers_offer ts where ts.advertisers_id=? Group By ts.adv_offer_id ) ) tt "
////					+ " RIGHT JOIN temp_advertisers_offer temp ON tt.adv_offer_id=temp.temp_adv_offer_id and tt.advertisers_id=temp.temp_advertisers_id "
////					+ " WHERE temp.temp_advertisers_id=? AND ( tt.adv_offer_id is NULL or tt.status<0 )";
////			System.out.println(sql);
//			Map<String, String> header = new HashMap<String, String>();
//			header.put("Authorization", "Basic bmFuaGFpLnhAZmVlbHRhcG1lZGlhLmNvbTozMjkyNmE3Mzk4NmY0ZGU3OWRiNTNjN2E3MmMxZmU1Yg==");
//			String jsonStr = HttpUtil.sendGet("http://domobi.api.offerslook.com/aff/v1/batches/offers?limit=100&type=personal&offset=1", header);
//			System.out.println(jsonStr);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Advertisers tmp = new Advertisers();
		// {apikey}
		// http://offer.wevemob.com/api?userId=95&token=zYb0W0amU32vU7gn
		tmp.setApiurl("http://domobi.api.offerslook.com/aff/v1/batches/offers?limit=100&type=personal&offset=1");
		tmp.setApikey("bmFuaGFpLnhAZmVlbHRhcG1lZGlhLmNvbTozMjkyNmE3Mzk4NmY0ZGU3OWRiNTNjN2E3MmMxZmU1Yg==");
		tmp.setId(25L);

		PullOffersLookService mmm = new PullOffersLookService(tmp);
		mmm.run();
	}

}
