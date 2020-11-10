package com.network.task.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;

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
import com.network.task.util.Encrypt;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullMV_S3_OfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger.getLogger(PullMV_S3_OfferService.class);

	private Advertisers advertisers;

	public PullMV_S3_OfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullMV_S3_OfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			logger.info("doPull PullMV_S3_OfferService Offer begin := " + new Date());
			try {

				Long advertisersId = advertisers.getId();

				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();

				if (advertisersId == null || OverseaStringUtil.isBlank(apiurl) || OverseaStringUtil.isBlank(apikey)) {

					logger.info("advertisersId or apiurl or apikey is null return");

					return;
				}
				String client_secret_key = apikey.substring(0, apikey.indexOf("&"));
				String client_key = apikey.substring(apikey.indexOf("&") + 1, apikey.length());
				Encrypt Encrypt = new Encrypt();
				// 1535452492
				long time = System.currentTimeMillis() / 1000;
				// 13569
				String return_type = "json";
				String params = "client_key={client_key}&client_secret_key={client_secret_key}&return_type={return_type}&time={time}";
				params = params.replace("{time}", URLEncoder.encode(time + "", "utf-8"));
				params = params.replace("{client_key}", URLEncoder.encode(client_key, "utf-8"));
				params = params.replace("{client_secret_key}", URLEncoder.encode(client_secret_key, "utf-8"));
				params = params.replace("{return_type}", URLEncoder.encode(return_type, "utf-8"));
				params = Encrypt.SHA256(params);
				params = URLEncoder.encode(params, "utf-8");
				apiurl = "https://open.3s.mobvista.com/channel/v5?client_key=" + client_key + "&return_type=json&time={time}&token={token}";
				apiurl = apiurl.replace("{time}", time + "");
				apiurl = apiurl.replace("{token}", params);
				String str = HttpUtil.sendGet(apiurl);
				logger.info("str " + str);
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				JSONObject jsonPullObject = JSON.parseObject(str);
				JSONArray offersArray = jsonPullObject.getJSONArray("data");

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer " + offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {
						try {
							JSONObject item = offersArray.getJSONObject(i);
							if (item != null) {
								String status = item.getString("status");
								if (!status.equals("running")) {
									continue;
								}
								String campid = item.getString("campid");
								logger.info("campid " + campid);
								String app_name = item.getString("app_name");
								String platform = item.getString("platform").toUpperCase();
								String package_name = item.getString("package_name");
								String note = item.getString("note");
								String preview_link = item.getString("preview_link");
								String tracking_link = item.getString("tracking_link");
								String app_desc = item.getString("app_desc");
								String icon_link = item.getString("icon_link");
								String geo = item.getString("geo");
								geo = geo.replace("[\"", "");
								geo = geo.replace("\"]", "");
								geo = geo.replace(",", ":");
								geo = geo.replace("\"", "");
								Float payout = item.getFloat("price");

								if (payout > 15) {
									continue;
								}

								Integer cap = item.getInteger("daily_cap");
								String os_str = null;
								if (platform.toUpperCase().equals("IOS")) {
									os_str = "1";
									package_name = package_name.replace("id", "");
								} else if (platform.toUpperCase().equals("ANDROID")) {
									os_str = "0";
								} else {
									continue;
								}

								JSONArray blockChannel = item.getJSONArray("blacklist_subid");
								if (blockChannel != null && blockChannel.size() > 0) {
									for (Object object : blockChannel) {
										if (object.toString().length() > 5) {
											if (object.toString().substring(4, 5).equals("_")) {
												block_pkg_affi.put(package_name.replace("_", "") + "&" + JSON.parseObject(object.toString()).getString("subid"), JSON.parseObject(object.toString()).getString("subid"));
											}
										}

									}

								}

								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(campid);
								advertisersOffer.setName(filterEmoji(app_name));
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
								advertisersOffer.setPkg(package_name);
								advertisersOffer.setMain_icon(icon_link);
								advertisersOffer.setPreview_url(preview_link);
								advertisersOffer.setClick_url(tracking_link);
								advertisersOffer.setCountry(geo);
								advertisersOffer.setDaily_cap(cap);
								advertisersOffer.setPayout(payout);
								advertisersOffer.setCreatives(null);
								advertisersOffer.setConversion_flow(101);
								advertisersOffer.setDescription(filterEmoji(note + app_desc));
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
					logger.info("block_pkg_affi" + block_pkg_affi);

					// 黑名单
					Blacklist_affiliates_subidDao Blacklist_affiliates_subidDao = (Blacklist_affiliates_subidDao) TuringSpringHelper.getBean("Blacklist_affiliates_subidDao");
					List<BlackListSubAffiliates> block_list = Blacklist_affiliates_subidDao.getAllInfoByAdvertisers_id(advertisersId + "");
					Map<String, String> db_block_map = new HashMap<String, String>();
					for (BlackListSubAffiliates entity : block_list) {
						db_block_map.put(entity.getPkg() + "&" + entity.getAffiliates_id() + "_" + entity.getAffiliates_sub_id(), entity.getAffiliates_sub_id());
					}
					// 对比
					for (String key : block_pkg_affi.keySet()) {
						if (db_block_map.get(key) == null) {
							String sub[] = key.split("&");
							String pkg = sub[0];
							String affiliates_id = sub[1].substring(0, 4);
							String affiliates_sub_id = sub[1].substring(5, sub[1].length());
							Blacklist_affiliates_subidDao.add(advertisersId, pkg, affiliates_id, affiliates_sub_id);
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
		// DQSEUFZE447BV9YAZUU1&13569
		// String apikey="V5N3UR5NY9MET6YW0SBT&13642";
		// String client_secret_key=apikey.substring(0, apikey.indexOf("&"));
		// String client_key=apikey.substring(apikey.indexOf("&")+1,
		// apikey.length());
		// System.out.println(client_key);
		 Advertisers tmp = new Advertisers();
		 //
		// http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		 tmp.setApiurl("https://open.3s.mobvista.com/channel/v5");
		 tmp.setApikey("V5N3UR5NY9MET6YW0SBT&13642");
		 tmp.setId(2011L);
		
		 PullMV_S3_OfferService mmm = new PullMV_S3_OfferService(tmp);
		 mmm.run();

		
	}

}
