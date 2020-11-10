package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

public class PullPokktOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullPokktOfferService.class);
	
	private Advertisers advertisers;
	
	public PullPokktOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Pokkt Offer begin := " + new Date());
			
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
				JSONObject data= jsonPullObject.getJSONObject("data");
				
				//String status = jsonPullObject.getString("status");
				Integer recordsTotal = data.getInteger("totalItems");
				//Integer recordsFiltered = jsonPullObject.getInteger("recordsFiltered");
//				Integer pagecount = jsonPullObject.getInteger("pagecount");
				
//				Integer count = jsonPullObject.getInteger("total_count");
				
				if(recordsTotal.intValue() <=0){
					
					logger.info("status is not success return. status := " + recordsTotal);
					
					return;
				}
				
				//logger.info("recordsTotal:= " + recordsTotal+"     recordsFiltered:"+recordsFiltered);
//				if(recordsTotal == null
//						|| recordsTotal == 0){
//					
//					logger.info("available or  count is empty. " + " recordsTotal:=" + recordsFiltered);
//					
//					return;
//				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = data.getJSONArray("content");
				
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
							String offer_model = item.getString("payout_type");// "CPI"
							
							//String traffic_type =item.getString("incent");
							
							Integer incetive = 0;// 是否激励(0:非激励 1:激励)
//							if("0".equals(traffic_type)){
//								
//								incetive = 0;
//							}
//							else if("1".equals(traffic_type)){
//								
//								incetive = 1;
//							}
							
							String status = item.getString("status");
							if (status == null||!"Active".equals(status)) {
								continue;
							}
							
							String currency = item.getString("currency");
							if (!currency.contains("Dollars")) {
								logger.info("currency: " + currency);
								continue;
							}
//							SONObject targeting =  JSON.parseObject(item.getString("targeting"));
							String countriesArray=item.getString("geo_countries");
							
//							String paltfrom=targeting.getString("platform");
							String target_url = item.getString("preview_url");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

							String os_str =TuringStringUtil.getPlatoms(target_url); 
//							if ("android".equals(os_str)) {
//								os_str = "0";
//							} else if ("ios".equals(os_str)) {
//								os_str = "1";
//							}
//							else if ("Web".equals(paltfrom)) {
//								os_str = "2";
//							}
							String min_os_version ="";

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
							
							String name = item.getString("name");// "Legacy of Discord-FuriousWings"

							//String free = item.getString("free");// "true"
							String app_id = TuringStringUtil.getpkg(target_url);// "com.gtarcade.lod"
							//String target_url = product_info.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

							String description = item.getString("description");
							//String kpi = item.getString("kpi");
							//description =description +" "+kpi;
//							String category = item.getString("appcategory");
							String icon = item.getString("logo");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

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
							
							String caps_daily_remaining_str = item.getString("day_cap");// 9992
							Integer caps_daily_remaining = 99999;
//							if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
//								if("unlimited".equals(caps_daily_remaining_str)){
//									
//									caps_daily_remaining = 10000;
//								}
//								else{
//									
//									caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
//								}
//							}
							//String caps_daily = item.getString("caps_daily");// 9999
							//String caps_total = item.getString("caps_total");// "unlimited"
							//String caps_total_remaining	= item.getString("caps_total_remaining");// "unlimited"
							//Float avg_cr = item.getFloat("avg_cr");// 0.44
							//Integer rpc	= item.getInteger("rpc");// 0
							//boolean r = item.getBoolean("r");// false
							String link = item.getString("tracking_link");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							//http://click.szkuka.com/index.php?m=advert&p=click&app_id=140&offer_id=766738&clickid={clickid}&gaid={gaid}&android={android}&idfa={idfa}&subid={subid}
							
//							link =link.replace("{clickid}", "{aff_sub}");
//							link =link.replace("{subid}", "{sub_affid}");
//							
//							
//
//							
//							if (os_str.equals("0")) { //android
//								link=link.replace("{idfa}", "");
//							} else if (os_str.equals("1")) { //ios
//								link=link.replace("{gaid}", "");
//								link=link.replace("{android}", "");
//							}
							
							
							
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
							//String images = item.getString("images");
							//                "1200x627":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_2_prod_708_ea93b7381f854a0bcb78ce0b762fd52d.jpg",
			               // "x":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_55_prod_708_59b23dc121a71.jpg"
							String images_crative=null;
//							if (images!= null&&!images.equals("{}")&&!images.equals("[]")) {
//								try {
//									JSONArray images_JSONArray=new JSONArray();
//
//									ArrayList<Map> arrayList = new ArrayList<Map>();
//									JSONObject images_temp = JSON.parseObject(images);
//									
//									 for (Map.Entry<String, Object> entry : images_temp.entrySet()) {
//								            System.out.println(entry.getKey() + ":" + entry.getValue());
//								            Map jsonObject = new HashMap();
//								            jsonObject.put("url", entry.getValue());
//								            jsonObject.put("size", entry.getKey());
//								            arrayList.add(jsonObject);
//								        }
//									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
//									 images_crative = images_JSONArray.toString();
//									 
//								} catch(Exception e) {
//									logger.info("images is:"+images);
//									e.printStackTrace();
//								}
//
//							}
							
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
							advertisersOffer.setCreatives(images_crative);
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
		
		//http://pokkt.fuseclick.com//api/v1/getOffers?key=66976B433749ECE54D5B76248C8E7853&a=143&page=1
		tmp.setApiurl("http://pokkt.fuseclick.com//api/v1/getOffers?key={apikey}&a=143&page=1&limit=1000");
		tmp.setApikey("66976B433749ECE54D5B76248C8E7853");
		tmp.setId(25L);
		
		PullPokktOfferService mmm=new PullPokktOfferService(tmp);
		mmm.run();
	}
	
}
