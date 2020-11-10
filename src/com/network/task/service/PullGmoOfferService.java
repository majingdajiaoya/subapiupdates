package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.xml.XMLSerializer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.bean.BlackListSubAffiliates;
import com.network.task.dao.Blacklist_affiliates_subidDao;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullGmoOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullGmoOfferService.class);

	private Advertisers advertisers;

	public PullGmoOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullGmoOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullGmoOfferService Offer begin := " + new Date());
			// 黑名单
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			// 白名单
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
				str = new String(str.getBytes("ISO-8859-1"), "utf-8");
				XMLSerializer xmlSerializer = new XMLSerializer();
				// 将xml转为json（注：如果是元素的属性，会在json里的key前加一个@标识）
				String result = xmlSerializer.read(str).toString();
				JSONObject jsonPullObject = JSON.parseObject(result.replace("@", ""));

				logger.info("jsonPullObject " + jsonPullObject);

				JSONArray offersArray = jsonPullObject.getJSONArray("Items");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);

							if (item != null) {

								String name = item.getString("Title");// 430633
								String id = item.getString("AdId");// "android"
								String click_url = item.getString("Link");//
								String pkg = item.getString("AppId");//
								String countries_str = "JP";
								String Os = item.getString("Os").toUpperCase();//
								String os = "0";
								if (Os.contains("ANDROID")) {
									os = "0";
								} else if (Os.contains("IOS")) {
									os = "1";
								} else {
									continue;
								}
								String preview_url = TuringStringUtil.getPriviewUrl(os, pkg);

								Float payout = item.getFloat("Net");
								String des = item.getString("Note");
								String ImageUrl = item.getString("ImageUrl");
								String Description = item.getString("Description");
								Description = des + Description;

								Integer cap = 9999;

								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(id);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(ImageUrl);
								advertisersOffer.setPreview_url(preview_url);
								advertisersOffer.setClick_url(click_url);
								advertisersOffer.setCountry(countries_str);
								advertisersOffer.setDaily_cap(cap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(des));
								advertisersOffer.setOs(os);
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

	public static void main(String[] args) throws Exception {
		Advertisers tmp = new Advertisers();
		// http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret={apikey}
		tmp.setApiurl("https://media.smaad.net/ad_api?zone_id=234231475&feature=both");
		tmp.setApikey("1h7lm1Jog3KVQjV2Ffg90VE84E");
		tmp.setId(21L);

		PullGmoOfferService mmm = new PullGmoOfferService(tmp);
		mmm.run();

	}

}
