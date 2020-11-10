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
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullCpxiOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullCpxiOfferService.class);

	private Advertisers advertisers;

	public PullCpxiOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullCpxiOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullCpxiOfferService Offer begin := "
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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				apiurl = apiurl.replace("{apikey}", apikey);
				String str = HttpUtil.sendGet(apiurl);
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONArray offer_list = jsonPullObject.getJSONArray("items");
				if (offer_list != null && offer_list.size() > 0) {

					logger.info("begin pull offer " + offer_list.size());

					for (int i = 0; i < offer_list.size(); i++) {

						JSONObject item = offer_list.getJSONObject(i);

						if (item != null) {
							String status = item.getString("status")
									.toUpperCase();
							if (status.equals("ACTIVE")) {
								String name = item.getString("name");
								String offerid = item.getString("id");
								String preview_url = item.getString("preview_url");
								String description = item.getString("description");
								String tracking_url = item.getString("tracking_url");
								JSONArray goalsArray=item.getJSONArray("goals");
								JSONObject offerObject=goalsArray.getJSONObject(0);
								JSONArray countriesArray=offerObject.getJSONArray("countries");
								String country=(String) countriesArray.get(0);
								Float payout =offerObject.getJSONObject("publisher_payout").getFloat("amount");
								String pkg=TuringStringUtil.getpkg(preview_url);
								String platform =TuringStringUtil.getPlatoms(preview_url);
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(offerid);
								advertisersOffer.setName(filterEmoji(name));
								advertisersOffer.setCost_type(101);

								advertisersOffer.setOffer_type(101);
								advertisersOffer
										.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(pkg);
								advertisersOffer.setMain_icon(null);
								advertisersOffer.setPreview_url(preview_url);
								advertisersOffer.setClick_url(tracking_url);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(500);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer
										.setDescription(filterEmoji(description));
								advertisersOffer.setOs(platform);
								advertisersOffer.setDevice_type(0);// 设置为mobile类型
								advertisersOffer.setSend_type(0);// 系统入库生成广告
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);// 设置为激活状态
								advertisersOfferList.add(advertisersOffer);
							}
						}
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
		Advertisers tmp=new Advertisers();
		//{apikey}
		//https://api.taptica.com/v2/bulk?token=7Bhisedf1PmH8bs7goa2jw%3d%3d&platforms=iPhone&version=2&format=json
		tmp.setApiurl("https://item-api.aff-adeals.com/api/auth/v2?_site=361&_l=cappumedia&_p=917eee5d41f181ba018166e726ee636b4aee75e228bec403039dd007c036e003");
		tmp.setApikey("917eee5d41f181ba018166e726ee636b4aee75e228bec403039dd007c036e003");
		tmp.setId(19L);
		
		PullAdealsOfferService mmm=new PullAdealsOfferService(tmp);
		mmm.run();
	}

}
