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

public class PullZoomysOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullZoomysOfferService.class);
	
	private Advertisers advertisers;
	
	public PullZoomysOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Zoomys Offer begin := " + new Date());
			
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
				
				String status = jsonPullObject.getString("result");
				Integer recordsTotal = jsonPullObject.getInteger("offer_total");
//				Integer recordsFiltered = jsonPullObject.getInteger("recordsFiltered");
				Integer pagecount = jsonPullObject.getInteger("offer_count");
				
//				Integer count = jsonPullObject.getInteger("total_count");
				
				if(!"success".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
				
				logger.info("recordsTotal:= " + recordsTotal+"     recordsFiltered:"+pagecount);
				if(pagecount == null
						|| pagecount == 0){
					
					logger.info("available or  count is empty. " + " recordsFiltered:=" + pagecount);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						try {
							if(item != null){
								
								Integer roffer_id = item.getInteger("offer_id");// 104573764
								Integer device_type = null;
								device_type=0;
							    String offer_model = item.getString("payout_type");// "CPI"
								Integer incetive = 0;// 是否激励(0:非激励 1:激励)
								String countriesArray=item.getString("countries");
								String os_str =item.getString("platform"); 
								if (os_str!= null) {
									if ("ANDROID".equals(os_str.toUpperCase())) {
										os_str = "0";
									} else if ("IOS".equals(os_str.toUpperCase())) {
										os_str = "1";
									}
								} else {
									os_str="";
								}
								String min_os_version =item.getString("min_os_version");
								String name = item.getString("name");// "Legacy of Discord-FuriousWings"
								String app_id = item.getString("package_name");// "com.gtarcade.lod"
								String target_url = item.getString("preview_url");;// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
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
								String description = item.getString("desc");
								String kpi = item.getString("kpi");
								description=description+" "+kpi;
								String icon = item.getString("icon_url");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

								String creative_link = item.getString("creatives");
								Integer caps_daily_remaining = 99999;
								String link = item.getString("tracking_link");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
								String carriers=item.getString("carriers");
								Float payout = item.getFloat("payout");// 0.31
								String countries_str = countriesArray.replace("|", ":");
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
								
								if("CPI".equals(offer_model)||"CPA".equals(offer_model)){
									
									advertisersOffer.setCost_type(101);
									
									advertisersOffer.setOffer_type(101);
								}
								else if("CPL".equals(offer_model)){
									
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
								advertisersOffer.setCreatives(creative_link);
								if("CPI".equals(offer_model)||"CPA".equals(offer_model)){
									
									advertisersOffer.setConversion_flow(101);
								} else{
									
									advertisersOffer.setConversion_flow(104);//设置为其它类型
								}
								advertisersOffer.setSupported_carriers(carriers);
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
						} catch(Exception e) {
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
	
	 public static String filterEmoji(String source) { 
		  if (source != null && source.length() > 0) { 
		   return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", ""); 
		  } else { 
		   return source; 
		  } 
		 }
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://api.clinkad.com/offer_api_v3?key=tizv1j8w9u7ykjck&pubid=6393&pagesize=200&type=CPI
		tmp.setApiurl("http://api.clinkad.com/offer_api_v3?{apikey}&pagesize=200&type=CPI");
		tmp.setApikey("key=tizv1j8w9u7ykjck&pubid=6393");
		tmp.setId(21L);
		
		PullZoomysOfferService mmm=new PullZoomysOfferService(tmp);
		mmm.run();
	}
	
}
