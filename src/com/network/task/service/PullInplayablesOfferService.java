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
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullInplayablesOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullInplayablesOfferService.class);

	private Advertisers advertisers;

	public PullInplayablesOfferService(Advertisers advertisers) {

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

			logger.info("doPull Inplayable Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				// apiurl = apiurl.replace("{apikey}", apikey);

				// String str = HttpUtil.sendGet(apiurl);

				Map<String, String> header = new HashMap<String, String>();

				header.put("token", apikey);

				String str = HttpUtil.sendGet(apiurl, header);

				// JSONObject jsonPullObject = JSON.parseObject(str);

				// String status = jsonPullObject.getString("result");
				// Integer recordsTotal =
				// jsonPullObject.getInteger("offer_total");
				// Integer recordsFiltered =
				// jsonPullObject.getInteger("recordsFiltered");
				// Integer pagecount = jsonPullObject.getInteger("offer_count");

				// Integer count = jsonPullObject.getInteger("total_count");

				// if(!"success".equals(status)){
				//
				// logger.info("status is not ok return. status := " + status);
				//
				// return;
				// }
				//
				// logger.info("recordsTotal:= " +
				// recordsTotal+"     recordsFiltered:"+pagecount);
				// if(pagecount == null
				// || pagecount == 0){
				//
				// logger.info("available or  count is empty. " +
				// " recordsFiltered:=" + pagecount);
				//
				// return;
				// }

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = JSONArray.parseArray(str);

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {
							String offer_id = item.getString("id");
							String pkg = item.getString("app_pkg");
							String name = item.getString("app_name");
							String icon = item.getString("app_icon");
							Float payout = item.getFloat("price");
							String country = item.getString("countries");
							country = country.replace("[\"", "");
							country = country.replace("\"]", "");
							//JP KR US CA ID TW DE GB FR MY
							
							
							
							
							
							String kpitype = item.getString("kpitype");
							String tracklink = item.getString("click_url");
							Integer cap = item.getInteger("daily_cap");
							String previewlink = item.getString("preview_url");
							String des = item.getString("des");
							String platform = item.getString("platform").toUpperCase();
							String os_str = null;
							if (platform.toUpperCase().equals("ANDROID")) {
								os_str = "0";
							} else if (platform.toUpperCase().equals("IOS")) {
								os_str = "1";
							} else {
								continue;
							}
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(offer_id);
							advertisersOffer.setName(filterEmoji(pkg));
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(name);
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(previewlink);
							advertisersOffer.setClick_url(tracklink);
							advertisersOffer.setCountry(country);
							advertisersOffer.setDaily_cap(cap);
							advertisersOffer.setPayout(payout);
							advertisersOffer.setCreatives(null);
							advertisersOffer.setConversion_flow(101);
							advertisersOffer.setDescription(filterEmoji(kpitype + des));
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(0);// 设置为mobile类型
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);
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
		Advertisers tmp = new Advertisers();
		// http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret={apikey}
		tmp.setApiurl("http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android");
		tmp.setApikey("2cfbad46-f207-08e8-222f-59570c121301");
		tmp.setId(21L);

		PullInplayablesOfferService mmm = new PullInplayablesOfferService(tmp);
		mmm.run();
	}

}
