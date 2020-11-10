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
import com.network.task.bean.BlackListSubAffiliates;
import com.network.task.dao.Blacklist_affiliates_subidDao;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.SslUtils;
import com.network.task.util.TuringSpringHelper;
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullAppierOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullAppierOfferService.class);

	private Advertisers advertisers;

	public PullAppierOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取PullAppierOfferService广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {

		// 同步互斥
		synchronized (this) {

			logger.info("doPull PullAppierOfferService Offer begin := "
					+ new Date());
			// 黑名单
			Map<String, String> block_pkg_affi = new HashMap<String, String>();
			// 白名单
			Map<String, String> white_pkg_affi = new HashMap<String, String>();
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
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				String next = getadvertisersOfferList(apiurl, advertisersId,
						advertisersOfferList);

			} catch (Exception e) {

				e.printStackTrace();

				NetworkLog.exceptionLog(e);
			}
		}
	}

	private String getadvertisersOfferList(String apikey, Long advertisersId,
			List<AdvertisersOffer> advertisersOfferList) throws Exception {
		String next = null;

		System.out.println(apikey);
		advertisersOfferList.addAll(getOffer(apikey, advertisersId));
		try {
			Map<String, String> header = new HashMap<String, String>();
			header.put(
					"Authorization",
					"1h7lm1Jog3KVQjV2Ffg90VE84E-mhOO32YLWBfSlgfbPD1AliiYyawmQgNDQvdiJO_mRXYHlysd9sW2-h6WehkKfGUKgcTeSgfu1wAegn7_btCzxvI3KKagDX7iSW7Qo1LQb8nJZYNv2y9a8Tt9qsjSZRVPq5gNHoNtXh1CvDD0");
			SslUtils st = new SslUtils();
			String str = st
					.getRequest(
							apikey,
							130000,
							"1h7lm1Jog3KVQjV2Ffg90VE84E-mhOO32YLWBfSlgfbPD1AliiYyawmQgNDQvdiJO_mRXYHlysd9sW2-h6WehkKfGUKgcTeSgfu1wAegn7_btCzxvI3KKagDX7iSW7Qo1LQb8nJZYNv2y9a8Tt9qsjSZRVPq5gNHoNtXh1CvDD0");
			JSONObject jsonPullObject = JSON.parseObject(str);
			next = jsonPullObject.getJSONObject("paging").getString("next");
			if (next != null && next.length() > 0 && !next.equals("null")) {
				getadvertisersOfferList(next, advertisersId,
						advertisersOfferList);
			}
			if (next == null) {
				System.out.println(advertisersOfferList.size());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return next;
	}

	private List<AdvertisersOffer> getOffer(String apikey, Long advertisersId)
			throws Exception {
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
		SslUtils st = new SslUtils();
		String str = st
				.getRequest(
						apikey,
						130000,
						"1h7lm1Jog3KVQjV2Ffg90VE84E-mhOO32YLWBfSlgfbPD1AliiYyawmQgNDQvdiJO_mRXYHlysd9sW2-h6WehkKfGUKgcTeSgfu1wAegn7_btCzxvI3KKagDX7iSW7Qo1LQb8nJZYNv2y9a8Tt9qsjSZRVPq5gNHoNtXh1CvDD0");
		JSONObject jsonPullObject = JSON.parseObject(str);
		System.out.println(jsonPullObject);
		JSONArray offersArray = jsonPullObject.getJSONArray("data");
		if (offersArray != null && offersArray.size() > 0) {

			for (int i1 = 0; i1 < offersArray.size(); i1++) {
				JSONObject item = offersArray.getJSONObject(i1);
				if (item != null) {

					String name = item.getString("name");// 430633
					String id = item.getString("id");// "android"

					String click_url = item.getString("click_url");//
					String preview_url = item.getString("store_url");
					String pkg = TuringStringUtil.getpkg(preview_url);
					JSONArray countriesArray = item.getJSONArray("countries");

					String countries_str = "";
					if (countriesArray != null && countriesArray.size() > 0) {

						for (int k = 0; k < countriesArray.size(); k++) {

							String countriesItem = countriesArray.getString(k);

							if (countriesItem != null) {

								countries_str += countriesItem.toUpperCase()
										+ ":";
							}
						}
					}
					// 去除最后一个:号
					if (!OverseaStringUtil.isBlank(countries_str)) {

						countries_str = countries_str.substring(0,
								countries_str.length() - 1);
					}

					String os = "0";
					if (preview_url.contains("google")) {
						os = "0";
					} else if (preview_url.contains("apple")) {
						os = "1";
					} else {
						continue;
					}

					Float payout = item.getFloat("bid");
					
					if(payout<0.1){
						continue;
					}
					
					String des = item.getString("kpi_goal_description");
					click_url = click_url.replace("{PubID}", "")
							.replace("{SubID}", "{channel}")
							.replace("{DeviceIds[IFA]}", "{idfa}")
							.replace("{DeviceIds[AndroidID]}", "{gaid}")
							.replace("{DynamicParameter}", "{aff_sub}");
					Float daily_budget = item.getFloat("daily_budget");
					Integer cap = Integer.valueOf((Math.ceil(daily_budget
							/ payout) + "").replace(".0", ""));
					AdvertisersOffer advertisersOffer = new AdvertisersOffer();
					advertisersOffer.setAdv_offer_id(id);
					advertisersOffer.setName(filterEmoji(name));
					advertisersOffer.setCost_type(101);
					advertisersOffer.setOffer_type(101);
					advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
					advertisersOffer.setPkg(pkg);
					advertisersOffer.setMain_icon(null);
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
			}
		}
		return advertisersOfferList;
	}

	public static String filterEmoji(String source) {
		if (source != null && source.length() > 0) {
			return source.replaceAll(
					"[\ud800\udc00-\udbff\udfff\ud800-\udfff]", "");
		} else {
			return source;
		}
	}

	public static void main(String[] args) throws Exception {
		Advertisers tmp = new Advertisers();
		// http://api.inplayable.com/adfetch/v1/s2s/campaign/fetch?platform=android
		// http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=222&secret={apikey}
		tmp.setApiurl("https://adfeed.appier.net/v3.0/offers");
		tmp.setApikey("1h7lm1Jog3KVQjV2Ffg90VE84E");
		tmp.setId(21L);

		PullAppierOfferService mmm = new PullAppierOfferService(tmp);
		mmm.run();

	}

}
