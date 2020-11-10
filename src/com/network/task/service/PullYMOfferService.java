package com.network.task.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import common.Logger;

public class PullYMOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullYMOfferService.class);

	private Advertisers advertisers;

	public PullYMOfferService(Advertisers advertisers) {

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

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);
				int pages = getTotalPages(apiurl);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				for (int page = 1; page <= pages; page++) {
					advertisersOfferList.addAll(getPageOffers(advertisersId, apiurl, page));
				}
				logger.info("after filter pull offer size := " + advertisersOfferList.size());
				// 入网盟广告
				if (advertisersId != null && advertisersOfferList != null
						&& advertisersOfferList.size() > 0) {

					PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
							.getBean("pullOfferCommonService");

					pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
				}

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	private List<AdvertisersOffer> getPageOffers(Long advertisersId, String apiurl, int page) {
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		apiurl = apiurl.replace("{page}", page+""); 
		try {
			String str = HttpUtil.sendGet(apiurl);
			JSONObject json = JSON.parseObject(str);
			json = json.getJSONObject("data");
			json = json.getJSONObject("data");
			if (json != null) {
				Set<Entry<String, Object>> entrySet = json.entrySet();
				Iterator<Entry<String, Object>> iterator = entrySet.iterator();
				while (iterator.hasNext()) {
					Map.Entry<java.lang.String, java.lang.Object> entry = (Map.Entry<java.lang.String, java.lang.Object>) iterator
							.next();
					JSONObject offerItem = (JSONObject) entry.getValue();
					String type = offerItem.getString(("type")).toUpperCase();
					if (type.equals("OTHER")) {
						continue;
					}
					String offerid = entry.getKey();
					String countries = offerItem.getString(("countries"));
					countries = countries.replace("[", "");
					countries = countries.replace("]", "");
					countries = countries.replace("\"", "");
					countries = countries.replace(",", ":");
					if (countries.length() > 16) {
						continue;
					}
					String name = offerItem.getString(("name"));
					String pkg = offerItem.getString(("pkgname"));
					pkg=pkg.replace("id", "");
					if (pkg.length() == 0) {
						continue;
					}
					String icon = offerItem.getString(("icon"));
					String previewlink = offerItem.getString(("preview_url"));
					Float payout = offerItem.getFloat("payout");
					if (payout == 0.0) {
						continue;
					}
					String creative_link = offerItem.getString(("creative_link"));
					String tracklink = offerItem.getString(("tracklink"));
					String cap = offerItem.getString(("remaining_daily_cap"));
					if (cap.equals("-1")) {
						cap = "9999";
					}
					String offer_description = offerItem.getString(("offer_description"));

					String os_str = null;
					if (type.toUpperCase().equals("ANDROID")) {
						os_str = "0";
					} else if (type.toUpperCase().equals("IOS")) {
						os_str = "1";
					} else {
						continue;
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
					advertisersOffer.setDaily_cap(Integer.valueOf(cap));
					advertisersOffer.setPayout(payout);
					advertisersOffer.setCreatives(creative_link);
					advertisersOffer.setConversion_flow(101);
					advertisersOffer.setDescription(filterEmoji(offer_description));
					advertisersOffer.setOs(os_str);
					advertisersOffer.setDevice_type(0);// 设置为mobile类型
					advertisersOffer.setSend_type(0);// 系统入库生成广告
					advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
					advertisersOffer.setStatus(0);// 设置为激活状态
					advertisersOfferList.add(advertisersOffer);

				}
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
			offerUrl=offerUrl.replace("{page}", "1");
			String str = HttpUtil.sendGet(offerUrl);
			JSONObject json = JSON.parseObject(str);
			json = json.getJSONObject("data");
			int page = json.getInteger("totalpage");
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

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// http://api.aubemobile.com/api/offer/v2.0/?token=eba56face3724ce2bd9995e882d2f9f6&os=1,2&is_incent=0,1&limit=50
		// http://api.aubemobile.com/api/offer/v2.0/?token={apikey}&os=1,2&is_incent=0,1&limit=50

		tmp.setApiurl("http://sync.yeahmobi.com/sync/offer/get?api_id=351755562@qq.com&api_token={apikey}&limit=500&filters[type][$eq]=android,ios&page=1");
		tmp.setApikey("9c76ad56921efb1365959985d2bd24aa");
		tmp.setId(2005L);

		// &placement={sub_affid}&subid1={aff_sub}
		PullYMOfferService mmm = new PullYMOfferService(tmp);
		mmm.run();
	}

}
