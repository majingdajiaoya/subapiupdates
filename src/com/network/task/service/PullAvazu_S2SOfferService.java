package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

public class PullAvazu_S2SOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullAvazu_S2SOfferService.class);

	private Advertisers advertisers;

	public PullAvazu_S2SOfferService(Advertisers advertisers) {

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
				int pages = getRecords(apiurl);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				for (int page = 1; page <= pages; page++) {
					advertisersOfferList.addAll(getPageOffers(advertisersId,
							apiurl, page));
				}
				logger.info("after filter pull offer size := "
						+ advertisersOfferList.size());
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

	private List<AdvertisersOffer> getPageOffers(Long advertisersId,
			String apiurl, int page) {
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		apiurl = apiurl.replace("{page}", page + "");
		logger.info("apiurl := " + apiurl);
		try {
			JSONObject json1;
			JSONArray array = null;
			String str = HttpUtil.sendGet(apiurl);
			json1 = JSON.parseObject(str);
			json1 = json1.getJSONObject("ads");
			array = json1.getJSONArray("ad");
			
			logger.info("array := " + array);
			
			for (int i = 0; i < array.size(); i++) {
				JSONObject item = array.getJSONObject(i);
				String pkg = item.getString("pkgname");
				String name = item.getString("title");
				String payout = item.getString("payout");
				payout = payout.replace("$", "");
				String icon = item.getString("icon");
				String offerid = item.getString("campaignid");
				// https://itunes.apple.com/app/id436672029
				String tracklink = item.getString("clkurl");
				String countries = item.getString("countries");
				countries = countries.replace("|", ":");

				String campaigndesc = item.getString("campaigndesc");
				String description = item.getString("description");
				description = campaigndesc + description;
				String os = item.getString("os").toUpperCase();
				String previewlink = "";
				String os_str = null;
				if (os.equals("IOS")) {
					previewlink = "https://itunes.apple.com/app/id" + pkg;
					os_str = "1";
				}
				if (os.equals("ANDROID")) {
					previewlink = "https://play.google.com/store/apps/details?id="
							+ pkg;
					os_str = "0";
				}

				AdvertisersOffer advertisersOffer = new AdvertisersOffer();
				advertisersOffer.setAdv_offer_id(offerid);
				advertisersOffer.setName(filterEmoji(name));
				advertisersOffer.setCost_type(101);
				advertisersOffer.setOffer_type(101);
				advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
				advertisersOffer.setPkg(pkg);
				advertisersOffer.setMain_icon(icon);
				advertisersOffer.setPreview_url(previewlink);
				advertisersOffer.setClick_url(tracklink);
				advertisersOffer.setCountry(countries);
				advertisersOffer.setDaily_cap(500);
				advertisersOffer.setPayout(Float.valueOf(payout));
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
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return advertisersOfferList;
	}

	/**
	 * 获取总offer数
	 * 
	 * @param i
	 * @return
	 */
	private int getRecords(String apiurl) {
		Integer total_records = 0;
		try {
			JSONObject json1;
			apiurl = apiurl.replace("{page}", "1");
			String str = HttpUtil.sendGet(apiurl);
			json1 = JSON.parseObject(str);
			json1 = json1.getJSONObject("ads");
			total_records = (Integer) json1.get("total_records");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int page = 1;
		if (total_records % 500 == 0) {
			page = total_records / 500;
		} else {
			page = total_records / 500 + 1;
		}
		return page;
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
		Advertisers tmp = new Advertisers();
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		// http://api.c.avazunativeads.com/s2s?pagenum=500&page=1&campaigndesc=1&enforcedv=device_id&sourceid=34021
		// http://api.c.avazunativeads.com/s2s?pagenum=500&page=1&campaigndesc=1&enforcedv=device_id&sourceid=34021
		tmp.setApiurl("http://api.c.avazunativeads.com/s2s?sourceid=35060&pagenum=500&page={page}");
		
		//http://api.c.avazunativeads.com/s2s?sourceid=35060&pagenum=9999&campaigndesc=1&enforcedv=device_id,appname
		tmp.setApikey("34021");
		tmp.setId(2047L);

		PullAvazu_S2SOfferService mmm = new PullAvazu_S2SOfferService(tmp);
		mmm.run();
	}

}
