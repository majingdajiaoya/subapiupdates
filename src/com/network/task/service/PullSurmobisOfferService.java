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
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullSurmobisOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullSurmobisOfferService.class);
	
	private Advertisers advertisers;
	
	public PullSurmobisOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Surmobi Offer begin := " + new Date());
			
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
				
	//			String status = jsonPullObject.getString("status");
				Integer recordsTotal = jsonPullObject.getInteger("recordsTotal");
				Integer recordsFiltered = jsonPullObject.getInteger("recordsFiltered");
//				Integer pagecount = jsonPullObject.getInteger("pagecount");
				
//				Integer count = jsonPullObject.getInteger("total_count");
				
//				if(!"OK".equals(status)){
//					
//					logger.info("status is not ok return. status := " + status);
//					
//					return;
//				}
				
				logger.info("recordsTotal:= " + recordsTotal+"     recordsFiltered:"+recordsFiltered);
				if(recordsFiltered == null
						|| recordsFiltered == 0){
					
					logger.info("available or  count is empty. " + " recordsFiltered:=" + recordsFiltered);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer roffer_id = item.getInteger("id");// 104573764
							//String type = item.getString("type");//	"App"
							
							Integer device_type = null;
							device_type=0;

//							if("App".equals(type)){
//								
//								device_type = 0;// 手机mobile
//							}
							
							//String lead_type = item.getString("lead_type");// "install"
							//String offer_model = item.getString("payout_type");// "CPI"
							String offer_model = "cpi";// "CPI"
							
							String traffic_type =item.getString("is_incent");
							
							Integer incetive = null;// 是否激励(0:非激励 1:激励)
							if("0".equals(traffic_type)){
								
								incetive = 0;
							}
							else if("1".equals(traffic_type)){
								
								incetive = 1;
							}
							
							
//							SONObject targeting =  JSON.parseObject(item.getString("targeting"));
							String countriesArray=item.getString("country");
							
//							String paltfrom=targeting.getString("platform");
							String os_str =item.getString("os"); 
							if ("1".equals(os_str)) {
								os_str = "0";
							} else if ("2".equals(os_str)) {
								os_str = "1";
							}
//							else if ("Web".equals(paltfrom)) {
//								os_str = "2";
//							}
							String min_os_version =item.getString("min_os_version");

//							JSONArray include_min_os_version=item.getJSONArray("include_min_os_version");
//							if (include_min_os_version!=null&&include_min_os_version.size()>0) {
//								min_os_version = include_min_os_version.getString(0);
//							}
							

//							JSONObject product_info = null;
//							try{
//								product_info =JSON.parseObject(item.getString("product_info"));
//								
//							} catch (Exception e) {
//								e.printStackTrace();
//								logger.info(item.getString("product_info"));
//								continue;
//							}
							
							String name = item.getString("app_name");// "Legacy of Discord-FuriousWings"

							//String free = item.getString("free");// "true"
							String app_id = item.getString("app_id");// "com.gtarcade.lod"
							//String target_url = product_info.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
							String target_url = null;// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

							if ( TuringStringUtil.isBlank(target_url)) {
								
								target_url =TuringStringUtil.getPriviewUrl(os_str, app_id);
							}
							String description = item.getString("app_name");
//							String category = item.getString("appcategory");
							String icon = item.getString("app_icon");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

//							String creative_link = product_info.getString("creative_link");
							
							
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
							
							String caps_daily_remaining_str = item.getString("cap");// 9992
							Integer caps_daily_remaining = 1000;
							if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
//								if("unlimited".equals(caps_daily_remaining_str)){
//									
//									caps_daily_remaining = 10000;
//								}
//								else
								{
									
									caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
								}
							}
							//String caps_daily = item.getString("caps_daily");// 9999
							//String caps_total = item.getString("caps_total");// "unlimited"
							//String caps_total_remaining	= item.getString("caps_total_remaining");// "unlimited"
							//Float avg_cr = item.getFloat("avg_cr");// 0.44
							//Integer rpc	= item.getInteger("rpc");// 0
							//boolean r = item.getBoolean("r");// false
							String link = item.getString("tracking_url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							//替换link中的 字段 &cid=[cid]&data1=[data1]&data2=[data2]&data3=[data3]&data4=[data4]&affsub1=[affsub1]&device_id=[device_id]&idfa=[idfa]&gaid=[gaid]
//					        $click = str_replace("[gaid]","{gaid}",$v['clickurl']);
////					        $click = str_replace("[affsub1]","{aff_sub}",$click);
//					        $click = str_replace("[cid]","{aff_sub}",$click);
//					        $click = str_replace("[idfa]","{idfa}",$click);
							
							if (os_str.equals("0")) { //android
								link=link+"&device_id={gaid}";
							} else if (os_str.equals("1")) { //ios
								link=link+"&device_id={idfa}";
							}
							
							Float payout = item.getFloat("payout");// 0.31
							
							//*	targeting	
							//*	allowed	
							//*		os	[1]
							//*		os_version	Object
							//*		models	[]
							//*		manufacturer	[]
							//*		countries	[1]
							//*		device_types	[]
//							JSONObject allowed = targeting.getJSONObject("allowed");
//							JSONArray osArray = allowed.getJSONArray("os");
//							
//							if(osArray != null
//									&& osArray.size() > 0){
//								
//								for(int k = 0; k < osArray.size(); k++){
//									
//									String osItem = osArray.getString(k);
//									
//									if(osItem != null){
//										if("android".equals(osItem.toLowerCase())){
//											
//											os_str += "0:";
//										}
//										if("ios".equals(osItem.toLowerCase())){
//											
//											os_str += "1:";
//										}
//									}
//								}
//							}
//							//去除最后一个:号
//							if(!OverseaStringUtil.isBlank(os_str)){
//								
//								os_str = os_str.substring(0, os_str.length() - 1);
//							}
							
							
							String countries_str = countriesArray.replace(",", ":");
//							if(countriesArray != null
//									&& countriesArray.size() > 0){
//								
//								for(int k = 0; k < countriesArray.size(); k++){
//									
//									String countriesItem = countriesArray.getString(k);
//									
//									if(countriesItem != null){
//										
//										countries_str += countriesItem.toUpperCase() + ":";
//									}
//								}
//							}
//							//去除最后一个:号
//							if(!OverseaStringUtil.isBlank(countries_str)){
//								
//								countries_str = countries_str.substring(0, countries_str.length() - 1);
//							}
							
							//String app_name = item.getString("app_name");// "Legacy of Discord-FuriousWings"
							//String app_description = item.getString("app_description");// "Google Play Summer Sale …Lodsupport@gtarcade.com"
							
							
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if(roffer_id == null
									|| payout == null
									|| countriesArray == null
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
							advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(link);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(caps_daily_remaining);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							//advertisersOffer.setExpiration(category);
							advertisersOffer.setCreatives(null);
							if("CPI".equals(offer_model)){
								
								advertisersOffer.setConversion_flow(101);
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
		//http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		tmp.setApiurl("http://adpresso.io/adp/api/offer/v2.0/?token={apikey}&os=1,2&is_incent=0,1&limit=50");
		tmp.setApikey("7b9f7dac17e54b7e8f86a789bfc9a60e");
		tmp.setId(21L);
		
		PullSurmobisOfferService mmm=new PullSurmobisOfferService(tmp);
		mmm.run();
	}
	
}
