package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

public class PullIchestnutOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullIchestnutOfferService.class);

	private Advertisers advertisers;

	public PullIchestnutOfferService(Advertisers advertisers) {

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

				String str = HttpUtil.sendGet(apiurl);

				JSONObject jsonObject = JSON.parseObject(str);
				JSONArray offersArray = jsonObject.getJSONArray("apps");
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {

								String name = item.getString("app_name");
								String description = item.getString("description");
								String creatives = item.getString("creatives");
								String pkg = item.getString("package_name");
								JSONArray iconsArray = item.getJSONArray("icons");
								String icon = "";
								if (iconsArray != null && iconsArray.size() > 0) {
									for (int j = 0; j < iconsArray.size(); j++) {
										if (iconsArray.get(j).toString().indexOf("75*75") > 0) {
											JSONObject iconObject = JSON.parseObject(iconsArray.get(j)
													.toString());
											icon = iconObject.getString("url");
										}
									}
								}
								JSONArray offersarray = item.getJSONArray("offers");
								JSONObject offersObject = JSON.parseObject(offersarray.getString(0));
								String offerId = offersObject.getString("offer_id");
								String countries = offersObject.getString("countries");
								countries = countries.replace("[\"", "");
								countries = countries.replace("\"]", "");
								Integer cap_daily = offersObject.getInteger("cap_daily");
								String preview_link = offersObject.getString("preview_link");
								String tracking_link = offersObject.getString("tracking_link");
								String status = offersObject.getString("status");
								if (!status.equals("active")) {
									continue;
								}
								String payout_type = offersObject.getString("payout_type");
								if (!payout_type.equals("CPI")) {
									continue;
								}
								description=offersObject.getString("description")+description;
								Float payout = offersObject.getFloat("payout");
								String platforms = offersObject.getString("platforms").toUpperCase();
								String os_str="";
								if(platforms.indexOf("IOS")>0){
									os_str="1";
								}else if(platforms.indexOf("ANDROID")>0){
									os_str="0";
								}else{
									continue;
								}
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerId);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(preview_link);
								advertisersOffer.setClick_url(tracking_link);
								advertisersOffer.setCountry(countries);
								advertisersOffer.setDaily_cap(cap_daily);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(creatives);
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
		Advertisers tmp=new Advertisers();
		//http://api.hopemobi.net/v2/offer/pull?token=bba767c03e624b4cabb3a03d70ffa5db
		//{apikey}
		tmp.setApiurl("http://api.ichestnut.net/v1/apps/get?code={apikey}");
		tmp.setApikey("6fae1fb3c47ea4cb4a153638ed4fc619");
		tmp.setId(1079L);
		
		com.network.task.service.PullIchestnutOfferService mmm=new com.network.task.service.PullIchestnutOfferService(tmp);
		mmm.run();
	}

}
