package com.network.task.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;

public class PullIgawOfferService implements PullOfferServiceTask {

	protected static Logger logger = Logger
			.getLogger(PullIgawOfferService.class);

	private Advertisers advertisers;

	public PullIgawOfferService(Advertisers advertisers) {

		this.advertisers = advertisers;
	}

	/**
	 * 拉取Leadbolt广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止 3.
	 * 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run() {
		// 黑名单
		Map<String, String> block_pkg_affi = new HashMap<String, String>();
		// 同步互斥
		synchronized (this) {

			logger.info("doPull Leadbolt Offer begin := " + new Date());

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

				JSONObject jsobj1 = new JSONObject();
				jsobj1.put("ssp_id", "Macan-native_API");
				jsobj1.put("token", "1ea9b60f-3d20-4fb6-a644-cfc4c9d1f63e");

				System.out.println(jsobj1);

				JSONArray offersArray = JSONArray.parseArray(post(jsobj1,
						apiurl));

				if (offersArray == null || offersArray.size() == 0) {

					logger.info("offersArray is empty");

					return;
				}

				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

				if (offersArray != null && offersArray.size() > 0) {

					logger.info("begin pull offer size := "
							+ offersArray.size());

					for (int i = 0; i < offersArray.size(); i++) {

						JSONObject item = offersArray.getJSONObject(i);

						if (item != null) {

							JSONObject app_detailsJSONObject = item
									.getJSONObject("app_details");
							String pkg = app_detailsJSONObject
									.getString("bundle_id");
							String preview_url = app_detailsJSONObject
									.getString("app_store_url");
							String platform = app_detailsJSONObject.getString(
									"platform").toUpperCase();
							JSONObject campaignsJSONObject = item
									.getJSONObject("campaigns");
							String campaign_id = campaignsJSONObject
									.getString("campaign_id");

							System.out.println(campaign_id);

							String country = campaignsJSONObject
									.getString("country");
							if (country.equals("KOR")) {
								country = "KR";
							} else {
								continue;
							}
							String kpi = campaignsJSONObject.getString("kpi");
							String points = campaignsJSONObject
									.getString("points");
							String daily_budget_cap = campaignsJSONObject
									.getString("daily_budget_cap");
							int cap = Math.round(Float
									.valueOf(daily_budget_cap)
									/ Float.valueOf(points));
							JSONArray native_creativesJSONArray = item
									.getJSONArray("native_creatives");
							
							if(native_creativesJSONArray.size()==0){
								continue;
							}
							
							
							JSONObject native_creativesJSONObject = (JSONObject) native_creativesJSONArray
									.get(0);
							// https://n.trk.tw.igaw.io/v1/ncpi/click?creative_id=81979&placement_type=10&adset_id=127121399&passcode=1ea9b60f-3d20-4fb6-a644-cfc4c9d1f63e&transaction_id=[CLICK_ID]&publisher_id=[AUDIENCE_ID]&adid=[DEVICE_AD_ID]
							String click_url = native_creativesJSONObject
									.getString("click_url");
							click_url = click_url.replace("", "");

							String banner_url = native_creativesJSONObject
									.getString("banner_url");
							String description = native_creativesJSONObject
									.getString("description");
							int os = 0;
							if (platform.equals("ANDROID")) {
								os = 0;
							} else if (platform.equals("IOS")) {
								os = 1;
							} else {
								continue;
							}
							String title = native_creativesJSONObject
									.getString("title");

							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();

							advertisersOffer.setAdv_offer_id(String
									.valueOf(campaign_id));
							advertisersOffer.setName(title);

							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);

							advertisersOffer.setAdvertisers_id(advertisersId);// 网盟ID
							advertisersOffer.setPkg(pkg);
							// advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(banner_url);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(click_url);
							advertisersOffer.setCountry(country);
							advertisersOffer.setDaily_cap(cap);// 无cap限制，默认3000
							advertisersOffer.setPayout(Float.valueOf(points));
							advertisersOffer.setDescription(filterEmoji(kpi));

							advertisersOffer.setOs(os + "");
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setSend_type(0);// 系统入库生成广告
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);// 设置为激活状态

							advertisersOfferList.add(advertisersOffer);
						}
					}

					logger.info("after filter pull PullIgawOfferService offer size := "
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

	public static String post(JSONObject json, String URL) {

		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(URL);
		post.setHeader("Content-Type", "application/json");
		post.addHeader("Authorization", "Basic YWRtaW46");
		String result = "";

		try {

			StringEntity s = new StringEntity(json.toString(), "utf-8");
			s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
					"application/json"));
			post.setEntity(s);

			// 发送请求
			HttpResponse httpResponse = client.execute(post);

			// 获取响应输入流
			InputStream inStream = httpResponse.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					inStream, "utf-8"));
			StringBuilder strber = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
				strber.append(line + "\n");
			inStream.close();

			result = strber.toString();

			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				System.out.println("请求服务器成功，做相应处理");

			} else {

				System.out.println("请求服务端失败");

			}

		} catch (Exception e) {
			System.out.println("请求异常");
			throw new RuntimeException(e);
		}

		return result;
	}

	public static void main(String[] args) {
		Advertisers tmp = new Advertisers();
		tmp.setApiurl("https://offers.trk.tw.igaw.io/v1/ncpi/bulk");
		tmp.setApikey("49548aeee8c97f4601db7738e8489fab");
		tmp.setId(1069L);

		PullIgawOfferService mmm = new PullIgawOfferService(tmp);
		mmm.run();
	}

}
