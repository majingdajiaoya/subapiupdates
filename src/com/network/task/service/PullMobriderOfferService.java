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
import common.Logger;

public class PullMobriderOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullMobriderOfferService.class);

	private Advertisers advertisers;

	public PullMobriderOfferService(Advertisers advertisers) {

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

			logger.info("doPull PullYMOfferService Offer begin := "
					+ new Date());

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
				int pages = getTotalPages(apiurl);
				
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
		try {
			String str = HttpUtil.sendGet(apiurl);
			JSONObject json = JSON.parseObject(str);
			JSONObject offersJSONObject = json.getJSONObject("offers");
			for (Object map : offersJSONObject.entrySet()) {
				JSONObject offerObject = (JSONObject) ((Map.Entry) map)
						.getValue();
				String Status = offerObject.getString("Status");
				if (!Status.equals("active")) {
					continue;
				}
				String offerid = offerObject.getString("ID");
				String pkg = offerObject.getString("APP_ID");
				if (pkg == null) {
					continue;
				}
				pkg = pkg.replace("id", "");
				String Preview_url = offerObject.getString("Preview_url");
				String Name = offerObject.getString("Name");
				String Icon_url = offerObject.getString("Icon_url");
				String Tracking_url = offerObject.getString("Tracking_url");
				String Description = offerObject.getString("Description");
				if (Tracking_url == null) {
					continue;
				}
				JSONArray GoalsArray = offerObject.getJSONArray("Goals");
				if (GoalsArray == null && GoalsArray.size() == 0) {
					continue;
				}
				JSONObject goal = GoalsArray.getJSONObject(0);
				String countries = goal.getString("Countries");
				Integer Daily_Install = goal.getInteger("Daily_Install");
				if (Daily_Install == null) {
					Daily_Install = 50;
				}
				Float Payout = goal.getFloat("Payout");
				String Platforms = goal.getString("Platforms").toUpperCase();

				String os_str = null;
				if (Platforms.indexOf("IPHONE") > 0) {
					os_str = "1";
				}
				if (Platforms.indexOf("ANDROID") > 0) {
					os_str = "0";
				}else{
					continue;
				}
				AdvertisersOffer advertisersOffer = new AdvertisersOffer();
				advertisersOffer.setAdv_offer_id(offerid);
				advertisersOffer.setName(filterEmoji(Name));
				advertisersOffer.setCost_type(101);

				advertisersOffer.setOffer_type(101);
				advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
				advertisersOffer.setPkg(pkg);
				advertisersOffer.setMain_icon(Icon_url);
				advertisersOffer.setPreview_url(Preview_url);
				advertisersOffer.setClick_url(Tracking_url);
				advertisersOffer.setCountry(countries);
				advertisersOffer.setDaily_cap(Daily_Install);
				advertisersOffer.setPayout(Payout);
				advertisersOffer.setCreatives("");
				advertisersOffer.setConversion_flow(101);
				advertisersOffer.setDescription(filterEmoji(Description));
				logger.info("os_str := "+os_str
						);
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

	private static int getTotalPages(String offerUrl) {
		try {
			offerUrl = offerUrl.replace("{page}", 1 + "");
			String str = HttpUtil.sendGet(offerUrl);
			JSONObject json = JSON.parseObject(str);
			int page = json.getInteger("count_pages");
			return page;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
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
		// http://api.aubemobile.com/api/offer/v2.0/?token=eba56face3724ce2bd9995e882d2f9f6&os=1,2&is_incent=0,1&limit=50
		// http://api.aubemobile.com/api/offer/v2.0/?token={apikey}&os=1,2&is_incent=0,1&limit=50

		tmp.setApiurl("http://api.mobrider.com/v2/affiliate/offer/findAll?token=eoo5T1wwu7cEi6iomFQoJr8mU5Z4EqPq&page={page}&limit=100");
		tmp.setApikey("eoo5T1wwu7cEi6iomFQoJr8mU5Z4EqPq");
		tmp.setId(2005L);

		// &placement={sub_affid}&subid1={aff_sub}
		PullMobriderOfferService mmm = new PullMobriderOfferService(tmp);
		mmm.run();
	}

}
