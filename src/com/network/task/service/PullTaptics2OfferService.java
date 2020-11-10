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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullTaptics2OfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullTaptics2OfferService.class);
	
	private Advertisers advertisers;
	
	public PullTaptics2OfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Clickky广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Taptic Offer begin := " + new Date());
			
			try {
				
				Long advertisersId = advertisers.getId();
				
				String apiurl = advertisers.getApiurl();
				String apikey = advertisers.getApikey();
				
				if(advertisersId == null
						|| OverseaStringUtil.isBlank(apiurl)
						|| OverseaStringUtil.isBlank(apikey)){
					
					logger.info("advertisersId or apiurl or apikey is null return");
					
					return;
				}
				
				apiurl = apiurl.replace("{apikey}", apikey);
						
				String str = HttpUtil.CuttGet(apiurl);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				String status = jsonPullObject.getString("Error");
//				Integer available = jsonPullObject.getInteger("available");
//				Integer pageindex = jsonPullObject.getInteger("pageindex");
//				Integer pagecount = jsonPullObject.getInteger("pagecount");
				
				//Integer count = jsonPullObject.getInteger("total_count");
				
				if(!"OK".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
				
//				if(count == null
//						|| count == 0){
//					
//					logger.info("available or  count is empty. " + " count:=" + count);
//					
//					return;
//				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("Data");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer roffer_id = item.getInteger("OfferId");// 104573764
							//String type = item.getString("type");//	"App"
							
							Integer device_type = null;
							device_type=0;

//							if("App".equals(type)){
//								
//								device_type = 0;// 手机mobile
//							}
							
							//String lead_type = item.getString("lead_type");// "install"
							String offer_model = item.getString("PayoutType");// "CPI"
							
							boolean isincetive=item.getBooleanValue("IsIncent") ;
							Integer incetive = null;// 是否激励(0:非激励 1:激励)
							if(!isincetive){
								
								incetive = 0;
							}
							else {
								
								incetive = 1;
							}
							
							JSONObject targeting =  JSON.parseObject(item.getString("targeting"));
							JSONArray countriesArray=item.getJSONArray("SupportedCountriesV2");
							String countries_str = "";
							if (countriesArray!=null &&countriesArray.size()>0) {
								for (int k=0;k<countriesArray.size();k++) {
									JSONObject itemgeo=countriesArray.getJSONObject(k);
									String country=itemgeo.getString("country");
									countries_str += country.toUpperCase() + ":";
								}
							}
							
							String paltfrom=item.getString("Platforms");
							String target_url = item.getString("PreviewLink");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
							String os_str = "";
							if (target_url!=null &&target_url.contains("play.google.com")) {
								os_str = "0";
							} else if (target_url!=null &&target_url.contains("itunes.apple.com")) {
								os_str = "1";
							} else {
								os_str = "2";
							}
							
							
							String min_os_version =item.getString("MinOsVersion");
							

//							JSONObject product_info = null;
//							try{
//								product_info =JSON.parseObject(item.getString("product_info"));
//								
//							} catch (Exception e) {
//								e.printStackTrace();
//								logger.info(item.getString("product_info"));
//								continue;
//							}
							
							String name = item.getString("Name");// "Legacy of Discord-FuriousWings"

							//String free = item.getString("free");// "true"
							String app_id = item.getString("MarketAppId");// "com.gtarcade.lod"
							String description = item.getString("Description");
							//String category = product_info.getString("category");
							String icon = item.getString("AppIconUrl");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

							//String creative_link = item.getString("TrackingLink");
							
							JSONArray creatives = item.getJSONArray("Creatives");
							//String Description="";
							String images_crative=null;

							ArrayList<Map> arrayList = new ArrayList<Map>();
							if (creatives!=null &&creatives.size()>0) {
								for (int j=0;j<creatives.size();j++) {
									JSONObject creativesitem = creatives.getJSONObject(j);
									if (creativesitem!=null) {
										String link= creativesitem.getString( "CreativeLink");
										String szie=creativesitem.getString( "CreativeSize");
										if (arrayList.size() <4)  {
											Map jsonObject = new HashMap();
								            jsonObject.put("url",link);
								            jsonObject.put("size",szie);
								            arrayList.add(jsonObject);
										} else {
											break;
										}

									}
									
								}
								JSONArray images_JSONArray=new JSONArray();
								//
								if (arrayList.size()>0) {
									images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									images_crative = images_JSONArray.toString();
								}
								
							}
							
							
							
							if ( TuringStringUtil.isBlank(target_url)) {
								
								target_url =TuringStringUtil.getPriviewUrl(os_str, app_id);
							}
							
//							JSONArray categoryArr = item.getJSONArray("category");// 0 "Action" 1	"Role Playing"
//							String categoryStr = "";
//							
//							if(categoryArr != null
//									&& categoryArr.size() > 0){
//								
//								for(int j = 0; j < categoryArr.size(); j++){
//									
//									String categoryItem = categoryArr.getString(j);
//									
//									categoryStr += categoryItem + ";";
//								}
//							}
							
							//String description_lang = item.getString("description_lang");// ""
							//String instructions = item.getString("instructions");// ""
							//String expiration_date = item.getString("expiration_date");// ""
							Float payout = item.getFloat("Payout");// 0.31
							//unavailable

							String caps_daily_remaining_str = item.getString("DailyBudget");// 9992
							
							
							Integer caps_daily_remaining = null;
							if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
								if("UNAVAILABLE".equals(caps_daily_remaining_str.toUpperCase())){
									
									caps_daily_remaining = 90000;
								}
								else{
									Float DailyBudgetall=item.getFloat("DailyBudget");
									caps_daily_remaining = (int)(DailyBudgetall/payout) ;
								}
							}
							
							String link = item.getString("TrackingLink");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							
							if (os_str.equals("0")) {
								link=link+"&tt_app_name=com.brighthouse.whitetiles5s";
								
							} else if (os_str.equals("1")) {
								link=link+"&tt_app_name=694133630";
							}
							//去除最后一个:号
							if(!OverseaStringUtil.isBlank(countries_str)){
								
								countries_str = countries_str.substring(0, countries_str.length() - 1);
							}
							if(roffer_id == null
									|| payout == null
									|| countriesArray == null
									|| countriesArray.size() == 0
									|| OverseaStringUtil.isBlank(link)
									|| OverseaStringUtil.isBlank(offer_model)
									){
								
								continue;
							}
							
							offer_model = offer_model.toUpperCase();
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if("CPA".equals(offer_model)
									&& payout < 0.06){
								
								continue;
							}
							// CPI类型，pkg为空，舍弃不入库
							if("CPA".equals(offer_model)
									&& OverseaStringUtil.isBlank(app_id)){
								
								continue;
							}
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
							advertisersOffer.setName(filterEmoji(name));
							
							if("CPA".equals(offer_model)){
								
								advertisersOffer.setCost_type(101);
								
								advertisersOffer.setOffer_type(101);
							} else if ("CPE".equals(offer_model)) {
								advertisersOffer.setCost_type(105);
								advertisersOffer.setConversion_flow(105);
								advertisersOffer.setOffer_type(105);
							}
//							else if("CPA".equals(offer_model)){
//								
//								advertisersOffer.setCost_type(102);
//								
//								advertisersOffer.setOffer_type(102);//订阅类型
//							}
							else{
								
								advertisersOffer.setCost_type(104);
								
								advertisersOffer.setOffer_type(104);//设置为其它类型
							}
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(app_id);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(link);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(caps_daily_remaining);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							//advertisersOffer.setExpiration(category);
							advertisersOffer.setCreatives(images_crative);
							if("CPA".equals(offer_model)){
								
								advertisersOffer.setConversion_flow(101);
							} else if ("CPE".equals(offer_model)) {
								advertisersOffer.setConversion_flow(105);
							}
							else{
								
								advertisersOffer.setConversion_flow(104);//设置为其它类型
							}
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(filterEmoji(description));
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(device_type);
							advertisersOffer.setOs_version(min_os_version);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(incetive);// 是否激励
							advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
							advertisersOffer.setStatus(0);//设置为激活状态
							
							advertisersOfferList.add(advertisersOffer);
						}
					}
					
					logger.info("after filter pull offer size := " + advertisersOfferList.size());
					
					// 入网盟广告
					if(advertisersId != null
							&& advertisersOfferList != null
							&& advertisersOfferList.size() > 0){
						
						PullOfferCommonService pullOfferCommonService 
							= (PullOfferCommonService)TuringSpringHelper.getBean("pullOfferCommonService");
						
						pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
					}
				}
			}
			catch (Exception e) {
				
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//{apikey}
		//https://api.taptica.com/v2/bulk?token=7Bhisedf1PmH8bs7goa2jw%3d%3d&platforms=iPhone&version=2&format=json
		tmp.setApiurl("http://api.taptica.com/v2/bulk?token={apikey}&version=2&format=json&platforms=Android");
		tmp.setApikey("RZDT9xYtw0oqrhqyrc5SuA%3d%3d");
		tmp.setId(19L);
		
		PullTaptics2OfferService mmm=new PullTaptics2OfferService(tmp);
		mmm.run();
	}
	
}
