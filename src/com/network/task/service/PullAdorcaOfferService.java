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
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.SslUtils;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullAdorcaOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullAdorcaOfferService.class);

	private Advertisers advertisers;

	public PullAdorcaOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取oceanbys广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			logger.info("doPull PullAdorcaOfferService Offer begin := " + new Date());

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
				SslUtils st = new SslUtils();
				String str = st.getRequest(apiurl, 130000,"");
				JSONObject jsonPullObject = JSON.parseObject(str);

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				JSONArray dataArray = jsonPullObject.getJSONArray("data");

				if (dataArray != null && dataArray.size() > 0) {

					logger.info("begin pull offer " + dataArray.size());

					for (int i = 0; i < dataArray.size(); i++) {

						JSONObject item = dataArray.getJSONObject(i);

						if (item != null) {
							String icon = item.getString("icon");
							String name = item.getString("name");
							String country2B = item.getString("country2B")
									.replace(",", ":");
							String offerId = item.getString("offerId");
							Float payout = item.getFloat("bid");
							String os = item.getString("os").toUpperCase();
							String description = item.getString("description");
							String pkg = item.getString("pack");
							String clickUrl = item.getString("clickUrl");
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							advertisersOffer.setAdv_offer_id(offerId);
							advertisersOffer.setName(name);
							String offer_type = item
									.getString("conversionFlow").toUpperCase();

							if ("CPI".equals(offer_type)) {

								advertisersOffer.setCost_type(101);

								advertisersOffer.setOffer_type(101);
							} else if ("CPA".equals(offer_type)) {

								advertisersOffer.setCost_type(102);

								advertisersOffer.setOffer_type(102);// 订阅类型
							} else {

								advertisersOffer.setCost_type(104);

								advertisersOffer.setOffer_type(104);// 设置为其它类型
							}
							String preview_link = "";
							Integer osStr = null;
							if (os.equals("ANDROID")) {
								preview_link = "https://play.google.com/store/apps/details?"
										+ pkg;
								osStr = 0;
							} else if (os.equals("IOS")) {
								preview_link = "https://itunes.apple.com/app/id"
										+ pkg + "?mt=8";
								osStr = 1;
							} else {
								continue;
							}
							advertisersOffer.setAdvertisers_id(advertisersId);// 10代表oceanbys
							advertisersOffer.setPkg(pkg);
							// advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_link);
							advertisersOffer.setClick_url(clickUrl);
							advertisersOffer.setCountry(country2B);
							advertisersOffer.setDaily_cap(999);
							advertisersOffer.setPayout(payout);
							if ("CPI".equals(offer_type)) {

								advertisersOffer.setConversion_flow(101);
							} else {

								advertisersOffer.setConversion_flow(104);// 设置为其它类型
							}
							advertisersOffer.setDescription(description);
							advertisersOffer.setOs(osStr+"");
							advertisersOffer.setDevice_type(0);
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态
							advertisersOfferList.add(advertisersOffer);
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
	
	
	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		// http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret={apikey}
		tmp.setApiurl("https://portal.adorca.com/export/offer?access=371&token={apikey}");
		tmp.setApikey("54eb1d20c818e3e006dfa6c41eff7064");
		tmp.setId(21L);

		PullAdorcaOfferService mmm = new PullAdorcaOfferService(tmp);
		mmm.run();
	}

}
