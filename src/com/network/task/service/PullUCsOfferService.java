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

public class PullUCsOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullUCsOfferService.class);
	
	private Advertisers advertisers;
	
	public PullUCsOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull UCs Offer begin := " + new Date());
			
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
						
				String str = HttpUtil.sendGet(apiurl);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
//				String status = jsonPullObject.getString("Statuscode");
				Integer available = jsonPullObject.getInteger("retcode");
//				Integer pageindex = jsonPullObject.getInteger("pageindex");
//				Integer pagecount = jsonPullObject.getInteger("pagecount");
				
				Integer count = jsonPullObject.getInteger("count");
				
//				if(!"200".equals(status)){
//					
//					logger.info("status is not ok return. status := " + status);
//					
//					return;
//				}
				logger.info("count is := " + count);
				if(count == null
						|| count == 0){
					
					logger.info("available or  count is empty. " + " count:=" + count);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						try {
							JSONObject item = offersArray.getJSONObject(i);
							
							if(item != null){
								
								Integer roffer_id = item.getInteger("campaign_id");// 104573764
								//String type = item.getString("type");//	"App"
								
								Integer device_type = null;
								device_type=0;

//								if("App".equals(type)){
//									
//									device_type = 0;// 手机mobile
//								}
								
								//String lead_type = item.getString("lead_type");// "install"
								String offer_model = "CPI";// "CPI"
								
								JSONObject note=  JSON.parseObject(item.getString("note"));
								String traffic_type = null;// "Non Incentive"
								if (note!=null) {
									traffic_type=note.getString("traffic");
								} else {
									traffic_type ="Non-Incent";
								}
								
								
								Integer incetive = null;// 是否激励(0:非激励 1:激励)
								if("Non-Incent".equals(traffic_type)){
									
									incetive = 0;
								}
								else if("Incent".equals(traffic_type)){
									
									incetive = 1;
								}
								
								//JSONObject targeting =  JSON.parseObject(item.getString("targeting"));
								//JSONArray countriesArray=targeting.getJSONArray("geo");
								
								
								String paltfrom=item.getString("os");
								String os_str = "";
								if (paltfrom.toUpperCase().contains("ANDROID")) {
									os_str = "0";
								} else if (paltfrom.toUpperCase().contains("IOS")) {
									os_str = "1";
								} 
								
								String min_os_version =item.getString("min_os");
								

//								JSONObject product_info = null;
//								try{
//									product_info =JSON.parseObject(item.getString("product_info"));
//									
//								} catch (Exception e) {
//									e.printStackTrace();
//									logger.info(item.getString("product_info"));
//									continue;
//								}
								
								String name = item.getString("app_name");// "Legacy of Discord-FuriousWings"

								//String free = item.getString("free");// "true"
								String app_id = item.getString("package_name");// "com.gtarcade.lod"
								//String target_url = product_info.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
								String description = item.getString("desc");
								//String category = product_info.getString("category");
								String icon = null;// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"
								JSONArray icons=item.getJSONArray("icons");
								if (icons!=null &&icons.size()>0) {
									JSONObject itemicon=icons.getJSONObject(0);
									icon=itemicon.getString("url");
								}
								
								
								
								//String creative_link = item.getString("click_url");
								
								JSONArray banners=item.getJSONArray("banners");
								String images_crative=null;
								if (banners!=null &&banners.size()>0) {
									int key=2;
									if (banners.size()<key) {
										key=banners.size();
									}
									JSONArray images_JSONArray=new JSONArray();

									ArrayList<Map> arrayList = new ArrayList<Map>();
									for (int h=0;h<key;h++) {
										JSONObject itemimgage=banners.getJSONObject(h);
										 Map jsonObject = new HashMap();
								            jsonObject.put("url", itemimgage.getString("url"));
								            jsonObject.put("size", itemimgage.getString("pixel"));
								            arrayList.add(jsonObject);
									}
									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									 images_crative = images_JSONArray.toString();
									
								}
								
//								JSONArray categoryArr = item.getJSONArray("category");// 0 "Action" 1	"Role Playing"
//								String categoryStr = "";
//								
//								if(categoryArr != null
//										&& categoryArr.size() > 0){
//									
//									for(int j = 0; j < categoryArr.size(); j++){
//										
//										String categoryItem = categoryArr.getString(j);
//										
//										categoryStr += categoryItem + ";";
//									}
//								}
								
								//String description_lang = item.getString("description_lang");// ""
								//String instructions = item.getString("instructions");// ""
								//String expiration_date = item.getString("expiration_date");// ""
								
							//	String caps_daily_remaining_str = item.getString("day_cap");// 1000
								Integer caps_daily_remaining = 1000;
//								if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
//									if("unlimited".equals(caps_daily_remaining_str)){
//										
//										caps_daily_remaining = 10000;
//									}
//									else{
//										
//										caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
//									}
//								}
								//String caps_daily = item.getString("caps_daily");// 9999
								//String caps_total = item.getString("caps_total");// "unlimited"
								//String caps_total_remaining	= item.getString("caps_total_remaining");// "unlimited"
								//Float avg_cr = item.getFloat("avg_cr");// 0.44
								//Integer rpc	= item.getInteger("rpc");// 0
								//boolean r = item.getBoolean("r");// false
								String link = item.getString("click_url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
								Float payout = item.getFloat("price");// 0.31
								
								//*	targeting	
								//*	allowed	
								//*		os	[1]
								//*		os_version	Object
								//*		models	[]
								//*		manufacturer	[]
								//*		countries	[1]
								//*		device_types	[]
//								JSONObject allowed = targeting.getJSONObject("allowed");
//								JSONArray osArray = allowed.getJSONArray("os");
//								
//								if(osArray != null
//										&& osArray.size() > 0){
//									
//									for(int k = 0; k < osArray.size(); k++){
//										
//										String osItem = osArray.getString(k);
//										
//										if(osItem != null){
//											if("android".equals(osItem.toLowerCase())){
//												
//												os_str += "0:";
//											}
//											if("ios".equals(osItem.toLowerCase())){
//												
//												os_str += "1:";
//											}
//										}
//									}
//								}
//								//去除最后一个:号
//								if(!OverseaStringUtil.isBlank(os_str)){
//									
//									os_str = os_str.substring(0, os_str.length() - 1);
//								}
								
								
								String countries_str = item.getString("countries");
								countries_str=countries_str.replace(",", ":");
								countries_str=countries_str.replace(":unknown", "");
								
//								if(countriesArray != null
//										&& countriesArray.size() > 0){
//									
//									for(int k = 0; k < countriesArray.size(); k++){
//										
//										String countriesItem = countriesArray.getString(k);
//										
//										if(countriesItem != null){
//											
//											countries_str += countriesItem.toUpperCase() + ":";
//										}
//									}
//								}
								//去除最后一个:号
//								if(!OverseaStringUtil.isBlank(countries_str)){
//									
//									countries_str = countries_str.substring(0, countries_str.length() - 1);
//								}
								
								//String app_name = item.getString("app_name");// "Legacy of Discord-FuriousWings"
								//String app_description = item.getString("app_description");// "Google Play Summer Sale …Lodsupport@gtarcade.com"
								
								
								// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
								if(roffer_id == null
										|| payout == null
										|| countries_str == null
										|| OverseaStringUtil.isBlank(link)
										|| OverseaStringUtil.isBlank(offer_model)
										){
									
									continue;
								}
								
								offer_model = offer_model.toUpperCase();
								
								// CPI类型，单价太低(0.06)，舍弃不入库
								if("CPI".equals(offer_model)
										&& payout < 0.06){
									
									continue;
								}
								// CPI类型，pkg为空，舍弃不入库
								if("CPI".equals(offer_model)
										&& OverseaStringUtil.isBlank(app_id)){
									
									continue;
								}
								
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								
								advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
								advertisersOffer.setName(name);
								
								if("CPI".equals(offer_model)){
									
									advertisersOffer.setCost_type(101);
									
									advertisersOffer.setOffer_type(101);
								}
								else if("CPA".equals(offer_model)){
									
									advertisersOffer.setCost_type(102);
									
									advertisersOffer.setOffer_type(102);//订阅类型
								}
								else{
									
									advertisersOffer.setCost_type(104);
									
									advertisersOffer.setOffer_type(104);//设置为其它类型
								}
								
								advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
								advertisersOffer.setPkg(app_id);
								//advertisersOffer.setpkg_size();
								advertisersOffer.setMain_icon(icon);
								//advertisersOffer.setPreview_url(target_url);
								advertisersOffer.setClick_url(link);
								
								advertisersOffer.setCountry(countries_str);
								
								//advertisersOffer.setEcountry(ecountry);
								
								advertisersOffer.setDaily_cap(caps_daily_remaining);
								//advertisersOffer.setsend_cap();
								advertisersOffer.setPayout(payout);
								//advertisersOffer.setExpiration(category);
								advertisersOffer.setCreatives(images_crative);
								if("CPI".equals(offer_model)){
									
									advertisersOffer.setConversion_flow(101);
								}
								else{
									
									advertisersOffer.setConversion_flow(104);//设置为其它类型
								}
								//advertisersOffer.setSupported_carriers("");
								advertisersOffer.setDescription(description);
								advertisersOffer.setOs(os_str);
								
								advertisersOffer.setDevice_type(device_type);
								advertisersOffer.setOs_version(min_os_version);//系统版本要求
								advertisersOffer.setSend_type(0);//系统入库生成广告
								advertisersOffer.setIncent_type(incetive);// 是否激励
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);//设置为激活状态
								
								advertisersOfferList.add(advertisersOffer);
							}
						} catch (Exception e) {
							e.printStackTrace();
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://data.union.ucweb.com/CampaignApiService/?pub_key=xw0ERQGdWeT_In47QqA8-A
		tmp.setApiurl("http://data.union.ucweb.com/CampaignApiService/?pub_key={apikey}");
		tmp.setApikey("xw0ERQGdWeT_In47QqA8-A");
		tmp.setId(19L);
		
		PullUCsOfferService mmm=new PullUCsOfferService(tmp);
		mmm.run();
	}
	
}
