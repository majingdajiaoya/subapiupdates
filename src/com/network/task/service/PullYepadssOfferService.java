package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class PullYepadssOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullYepadssOfferService.class);
	
	private Advertisers advertisers;
	
	public PullYepadssOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Inplayable Offer begin := " + new Date());
			
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
				
				//apiurl = apiurl.replace("{apikey}", apikey);
						
				//String str = HttpUtil.sendGet(apiurl);
				
				Map<String, String> header = new HashMap<String ,String>();
				
				header.put("Authorization", "Token "+apikey);
				
				String str = HttpUtil.sendGet(apiurl, header);
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				//JSONArray offersArray2 = JSONArray.parseArray(str);
				//使用hashset 来先保存可以的id，然后再去遍历另外一个接口
				//Set<Integer> idsd=new HashSet<Integer>();
				if (true) {
				//	logger.info("begin pull offer " + offersArray2.size());
//					for(int i = 0; i < offersArray2.size(); i++){
//						JSONObject item = offersArray2.getJSONObject(i);
//						Integer id=item.getInteger("id");
//						Boolean is_healthy=item.getBoolean("is_healthy");
//						if (is_healthy.booleanValue()) {
//							idsd.add(id);
//						}
//					}
					if (true) {
						//apiurl="http://api.pbydom.com/api-ext/v1/ag/campaigns/?limit=500&offset=0&status=08&vertical_id=2,8";
						//Map<String, String> header2 = new HashMap<String ,String>();
						
						//header2.put("Authorization", "Token "+apikey);
						
						//String str2 = HttpUtil.sendGet(apiurl, header2);
						JSONArray offersArray = JSONArray.parseArray(str);
						try {
							if(offersArray != null
									&& offersArray.size() > 0){
								
								logger.info("begin pull offer " + offersArray.size());
								
								for(int i = 0; i < offersArray.size(); i++){
									
									JSONObject item = offersArray.getJSONObject(i);
									
									if(item != null){
										JSONObject offer=item.getJSONObject("offer");
										
										Integer roffer_id = offer.getInteger("id");// 104573764
										//String type = item.getString("type");//	"App"
//										if (!idsd.contains(roffer_id)) {
//											continue;
//										}
										Boolean is_healthy=item.getBoolean("is_healthy");
										if (!is_healthy.booleanValue()) {
											continue;
										}
										
										Integer device_type = null;
										device_type=0;

//										if("App".equals(type)){
//											
//											device_type = 0;// 手机mobile
//										}
										
										//String lead_type = item.getString("lead_type");// "install"
									    String offer_model = "CPI";// "CPI"
										//String offer_model = "cpi";// "CPI"
										
										//String traffic_type =item.getString("is_incent");
										
										Integer incetive = 0;// 是否激励(0:非激励 1:激励)
//										if("0".equals(traffic_type)){
//											
//											incetive = 0;
//										}
//										else if("1".equals(traffic_type)){
//											
//											incetive = 1;
//										}
										
										String countries_str ="";

//										SONObject targeting =  JSON.parseObject(item.getString("targeting"));
										JSONObject tags=offer.getJSONObject("tags");
										String os_str =""; 
										if (tags!=null) {
											JSONArray platforms=tags.getJSONArray("platforms");
											if (platforms!=null&&platforms.size()>0) {
													
													JSONObject platformlite = platforms.getJSONObject(0);
													String platname=platformlite.getString("name");
													if ("ANDROID".equals(platname.toUpperCase())) {
														os_str = "0";
													} else if ("IOS".equals(platname.toUpperCase())) {
														os_str = "1";
													}
											}
											
											JSONArray countries=tags.getJSONArray("countries");
											if (countries!=null&&countries.size()>0) {
												for(int k=0;k<countries.size();k++) {
													JSONObject countrieslite=countries.getJSONObject(k);
													String code=countrieslite.getString("code");
													countries_str += code.toUpperCase() + ":";
												}
											}
											if(!OverseaStringUtil.isBlank(countries_str)){
												
												countries_str = countries_str.substring(0, countries_str.length() - 1);
											}
										}
										
										String min_os_version =null;
										
										String name = offer.getString("name");// "Legacy of Discord-FuriousWings"

										String app_id = offer.getString("package_name");// "com.gtarcade.lod"
										//String target_url = product_info.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
										String target_url = offer.getString("preview_link");;// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

										String description = offer.getString("description");
										String kpi_type = offer.getString("kpi_type");
										if (!"00".equals(kpi_type)) {
											description=offer.getString("kpi_requirements")+description;
										}
										//description=description+" "+kpi;
//										String category = item.getString("appcategory");
										String icon = item.getString("icon_url");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

										if ("".equals(os_str)) {
											if (target_url!=null ) {
												if (target_url.contains("play.google.com")) {
													os_str="0";
												} else if (target_url.contains("itunes.apple.com")) {
													os_str="1";
												}
											} else {
												logger.info("roffer_id:"+roffer_id +"  has no os_str and target_url");
												continue;
											}
										}
										JSONArray creatives=offer.getJSONArray("creatives");
										if (creatives!=null&&creatives.size()>0) {
											JSONObject creativeslite=creatives.getJSONObject(0);
											String file_name=creativeslite.getString("file_name");
											String link=null;
											if (OverseaStringUtil.isBlank(file_name)) {
												link= creativeslite.getString("link");
											}
											ArrayList<Map> arrayList = new ArrayList<Map>();
											String images_crative=null;
											if (link!=null) {
									            Map jsonObject = new HashMap();
									            jsonObject.put("url", link);
									            jsonObject.put("size", "*");
									            arrayList.add(jsonObject);
									            JSONArray images_JSONArray=new JSONArray();
												 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
												 images_crative = images_JSONArray.toString();
											}
										}
		
										//String caps_daily_remaining_str = item.getString("caps");// 9992
										JSONObject caps_daily_arry=item.getJSONObject("caps");
										Integer caps_daily_remaining = 10000;
										String caps_daily_remaining_str=caps_daily_arry.getString("campaign_limit_daily");
										if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
//											if("unlimited".equals(caps_daily_remaining_str)){
//												
//												caps_daily_remaining = 10000;
//											}
//											else
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
										String link = item.getString("url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
										//替换link中的 字段 &cid=[cid]&data1=[data1]&data2=[data2]&data3=[data3]&data4=[data4]&affsub1=[affsub1]&device_id=[device_id]&idfa=[idfa]&gaid=[gaid]
//								        $click = str_replace("[gaid]","{gaid}",$v['clickurl']);
////								        $click = str_replace("[affsub1]","{aff_sub}",$click);
//								        $click = str_replace("[cid]","{aff_sub}",$click);
//								        $click = str_replace("[idfa]","{idfa}",$click);
										
//										link=link.replace("{tid}", "{click_id}");
//										link=link.replace("{param}", "{aff_sub}");
//										link=link.replace("{aid}", "{andriod_id}");
//										link=link.replace("{region}", "{country}");
										
										
//										if (os_str.equals("0")) { //android
//											link=link+"&device_id={gaid}";
//										} else if (os_str.equals("1")) { //ios
//											link=link+"&device_id={idfa}";
//										}
										//String carriers=item.getString("carriers");
										
										Float payout = item.getFloat("campaign_payout");// 0.31
										
										//*	targeting	
										//*	allowed	
										//*		os	[1]
										//*		os_version	Object
										//*		models	[]
										//*		manufacturer	[]
										//*		countries	[1]
										//*		device_types	[]
//										JSONObject allowed = targeting.getJSONObject("allowed");
//										JSONArray osArray = allowed.getJSONArray("os");
//										
//										if(osArray != null
//												&& osArray.size() > 0){
//											
//											for(int k = 0; k < osArray.size(); k++){
//												
//												String osItem = osArray.getString(k);
//												
//												if(osItem != null){
//													if("android".equals(osItem.toLowerCase())){
//														
//														os_str += "0:";
//													}
//													if("ios".equals(osItem.toLowerCase())){
//														
//														os_str += "1:";
//													}
//												}
//											}
//										}
//										//去除最后一个:号
//										if(!OverseaStringUtil.isBlank(os_str)){
//											
//											os_str = os_str.substring(0, os_str.length() - 1);
//										}
										
									
										
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
										} else if ("CPA".equals(offer_model)) {
											advertisersOffer.setConversion_flow(102);
										} else{
											
											advertisersOffer.setConversion_flow(104);//设置为其它类型
										}
										advertisersOffer.setSupported_carriers(null);
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
							
						} catch(Exception e) {
							e.printStackTrace();
						}
						
						
					} else {
						return;
					}
					
					
				} else {
					return;
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
		//http://api.pbydom.com/api-ext/v1/offers/?limit=500&offset=0&status=08&vertical_id=2,7
		tmp.setApiurl("http://api.pbydom.com/api-ext/v1/campaigns/?limit=500&offset=0&status=08&vertical_id=2,8");
		tmp.setApikey("2f8e78b69d82d3dbf41ca72b20cbd80af496e864");
		tmp.setId(49L);
		
		PullYepadssOfferService mmm=new PullYepadssOfferService(tmp);
		mmm.run();
	}
	
}
