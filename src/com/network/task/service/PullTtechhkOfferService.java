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

public class PullTtechhkOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullTtechhkOfferService.class);

	private Advertisers advertisers;

	public PullTtechhkOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullTtechhkOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			logger.info("doPull PullTtechhkOfferService Offer begin := "
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
				String str = HttpUtil.sendGet(apiurl);
				JSONObject jsonPullObject = JSON.parseObject(str);
				String status = jsonPullObject.getString("status");

				if (status.equals("ok")) {

					List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

					JSONArray dataArray = jsonPullObject.getJSONArray("data");

					if (dataArray != null && dataArray.size() > 0) {

						logger.info("begin pull offer " + dataArray.size());

						for (int i = 0; i < dataArray.size(); i++) {

							JSONObject item = dataArray.getJSONObject(i);

							if (item != null) {

								String pkg = item.getString("appid");
								String country = item.getString("geo");
								String description = item
										.getString("description");
								String kpi = item.getString("kpi");
								String id = item.getString("id");
								String preview_link = item
										.getString("previewurl");
								String name = item.getString("offername");
								Integer cap = item.getInteger("cap");
								Float payout = item.getFloat("payout");
								String platform = item.getString("os");
								String trackurl = item.getString("trackurl");
								// http://tracking.ttechhk.com/a/c?a=10145&c=262100955&device_id={device_id}&click_id={click_id}&affsub={pub_subid}
								trackurl = trackurl
										.replace("{device_id}", "{devad_id}")
										.replace("{click_id}", "{aff_sub}")
										.replace("{pub_subid}", "{channel}");
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();

								advertisersOffer.setAdv_offer_id(id);
								advertisersOffer.setName(name);

								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer
										.setAdvertisers_id(advertisersId);// 10代表PullTtechhkOfferService
								advertisersOffer.setPkg(pkg);
								// advertisersOffer.setpkg_size();
								advertisersOffer.setMain_icon("");
								advertisersOffer.setPreview_url(preview_link);
								advertisersOffer.setClick_url(trackurl);
								advertisersOffer.setCountry(country);
								advertisersOffer.setDaily_cap(cap);
								advertisersOffer.setPayout(payout);

								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(kpi);

								if (platform.toUpperCase().equals("IOS")) {

									advertisersOffer.setOs("0");
									advertisersOffer.setDevice_type(0);
								} else if (platform.toUpperCase().equals(
										"ANDROID")) {

									advertisersOffer.setOs("1");
									advertisersOffer.setDevice_type(0);
								} else {
									continue;
								}
								advertisersOffer.setSend_type(0);// 系统入库生成广告
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);// 设置为激活状态

								advertisersOfferList.add(advertisersOffer);
							}
						}

						logger.info("after filter pull offer size := "
								+ advertisersOfferList.size());

						// 入网盟广告
						if (advertisersId != null
								&& advertisersOfferList != null
								&& advertisersOfferList.size() > 0) {

							PullOfferCommonService pullOfferCommonService = (PullOfferCommonService) TuringSpringHelper
									.getBean("pullOfferCommonService");

							pullOfferCommonService.doPullOffer(advertisers,
									advertisersOfferList);
						}
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
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=124&secret=0edb866cabafd269a4c977d7f33f90b4
		tmp.setApiurl("http://i.ttechhk.com/v1/affiliate/myoffer?API-Key=36436ebb290aa2c8251d62d7b43f4220");
		tmp.setApikey("0edb866cabafd269a4c977d7f33f90b4");
		tmp.setId(21L);

		PullTtechhkOfferService mmm = new PullTtechhkOfferService(tmp);
		mmm.run();
	}

}
