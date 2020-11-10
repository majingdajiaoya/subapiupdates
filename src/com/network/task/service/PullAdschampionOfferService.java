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

public class PullAdschampionOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullAdschampionOfferService.class);

	private Advertisers advertisers;

	public PullAdschampionOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullAdschampionOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			logger.info("doPull PullAdschampionOfferService Offer begin := " + new Date());

			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}

				apiurl = apiurl.replace("{apikey}", apikey);

				String str = HttpUtil.CuttGet(apiurl);

				JSONObject jsonPullObject = JSON.parseObject(str);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray offersArray = jsonPullObject.getJSONArray("offer_list");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							String id = item.getString("id");// 104573764
							String terms = item.getString("terms");// 104573764
							String name = item.getString("name");//
							JSONArray countriesArray = item.getJSONArray("country_list");
							String countries_str = "";
							if (countriesArray != null && countriesArray.size() > 0) {

								for (int k = 0; k < countriesArray.size(); k++) {

									String countriesItem = countriesArray.getString(k);

									if (countriesItem != null) {

										countries_str += countriesItem.toUpperCase() + ":";
									}
								}
							}
							// 去除最后一个:号
							if (!OverseaStringUtil.isBlank(countries_str)) {

								countries_str = countries_str.substring(0, countries_str.length() - 1);
							}

							Float payout = item.getFloat("payout");//

							Integer cap = item.getInteger("daily_conversion_cap");
							String click_url = item.getString("click_url");//
							JSONObject JSONObjectItem = item.getJSONObject("app");
							String platform = JSONObjectItem.getString("platform").toUpperCase();
							String package_name = JSONObjectItem.getString("package_name");
							String preview_url = JSONObjectItem.getString("preview_url");

							String os = "0";
							if (preview_url.contains("google")) {
								os = "0";
							} else if (preview_url.contains("apple")) {
								os = "1";
							} else {
								continue;
							}

							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(id);
							advertisersOffer.setName(filterEmoji(name));
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(package_name);
							advertisersOffer.setMain_icon(null);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(click_url);
							advertisersOffer.setCountry(countries_str);
							advertisersOffer.setDaily_cap(cap);
							advertisersOffer.setPayout(payout);
							advertisersOffer.setCreatives(null);
							advertisersOffer.setConversion_flow(101);
							advertisersOffer.setDescription(filterEmoji(terms));
							advertisersOffer.setOs(os);
							advertisersOffer.setDevice_type(0);// 设置为mobile类型
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);

						}
					}

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

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("http://api.affiliate.adschampion.com/v1.0/getoffers?api_key=3507f75af598ec2d80204112a82407cad7af55bd22d09376a6a9bd72a55d9c54");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);

		PullAdschampionOfferService mmm = new PullAdschampionOfferService(tmp);
		mmm.run();
	}

}
