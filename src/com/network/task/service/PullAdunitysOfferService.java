package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class PullAdunitysOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullAdunitysOfferService.class);
	
	private Advertisers advertisers;
	
	public PullAdunitysOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Adunity Offer begin := " + new Date());
			
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
				
				String status = jsonPullObject.getString("status");
//				Integer available = jsonPullObject.getInteger("available");
//				Integer pageindex = jsonPullObject.getInteger("pageindex");
//				Integer pagecount = jsonPullObject.getInteger("pagecount");
				JSONObject offers = jsonPullObject.getJSONObject("offers");
				
				if(!"OK".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
				if (offers!=null) {
					Integer count = offers.getInteger("total_offers");
					if(count == null
							|| count.intValue() == 0){
						
						logger.info("available or  count is empty. " + " count:=" + count);
						return;
					}
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = offers.getJSONArray("offer");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						try {
							JSONObject item = offersArray.getJSONObject(i);
							
							if(item != null){
								
								Integer roffer_id = item.getInteger("offerid");// 104573764
								//String type = item.getString("type");//	"App"
								
								Integer device_type = null;
								device_type=0;

//								if("App".equals(type)){
//									
//									device_type = 0;// 手机mobile
//								}
								
								//String lead_type = item.getString("lead_type");// "install"
								String offer_model = item.getString("convflow");// "CPI"
								if (!"101".equals(offer_model)) {
									continue;
								}
								
								String incent=item.getString("incent");
								Integer incetive = null;// 是否激励(0:非激励 1:激励)
								if("no".equals(incent)){
									
									incetive = 0;
								}
								else {
									
									incetive = 1;
								}
							
								
								String paltfrom=item.getString("os");
								String os_str = "";
								if ("android".equals(paltfrom)) {
									os_str = "0";
								} else if ("ios".equals(paltfrom)) {
									os_str = "1";
								} else if ("Web".equals(paltfrom)) {
									os_str = "2";
								}
								
								String min_os_version =item.getString("min_os_version");
								
								String name = item.getString("title");// "Legacy of Discord-FuriousWings"

								//String free = item.getString("free");// "true"
								String app_id = item.getString("package");// "com.gtarcade.lod"
								String target_url = item.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
								
								String description = item.getString("desc");
								String kpi = item.getString("kpi");
								
								if (kpi!=null) {
									description= kpi+" "+description;
								}
								String category = item.getString("category");
								String icon = item.getString("icon_url");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

								//String creative_link = item.getString("creative_link");
								
								if ( TuringStringUtil.isBlank(target_url)) {
									
									target_url =TuringStringUtil.getPriviewUrl(os_str, app_id);
								}
								Integer caps_daily_remaining = null;

								if (kpi!=null&&(kpi.contains("Cap:")|| kpi.contains("cap:"))) {
									//把key 信息 取出来
									 int intIndex = kpi.indexOf("Cap:");
									 if (intIndex==-1) {
										 intIndex=kpi.indexOf("cap:");
									 }
									 if (intIndex!=-1) {
										 String substr=kpi.substring(intIndex,intIndex+12);
										 String regEx="[^0-9]";   
										 Pattern p = Pattern.compile(regEx);   
										 Matcher m = p.matcher(substr);   
										 String cccp=m.replaceAll("").trim();
										 if (cccp!=null&&!"".equals(cccp)) {
											 caps_daily_remaining = Integer.valueOf(cccp);
										 } else {
											 caps_daily_remaining = 99999;
										 }
										 
									 }
									 
									
								} else {
									caps_daily_remaining = 99999;
								}
							

								String link = item.getString("click_url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
								if (os_str=="0") {
									link=link+"&device_id={gaid}&enforcedv={gaid}";
								} else if (os_str=="1") {
									link=link+"&device_id={idfa}&enforcedv={idfa}";
								}
								
								String price= item.getString("price");
								price=price.replace("$", "");
								Float payout = Float.valueOf(price);// 0.31
								
								String countries_str = item.getString("geos").replace("|", ":");

								
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
								if("101".equals(offer_model)
										&& payout < 0.06){
									
									continue;
								}
								// CPI类型，pkg为空，舍弃不入库
								if("101".equals(offer_model)
										&& OverseaStringUtil.isBlank(app_id)){
									
									continue;
								}
								
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								
								advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
								advertisersOffer.setName(filterEmoji(name));
								
								if("101".equals(offer_model)){
									
									advertisersOffer.setCost_type(101);
									
									advertisersOffer.setOffer_type(101);
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
								if("101".equals(offer_model)){
									
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
		//http://api.c.adunity.mobi/ads2s?sourceid=31717&pagenum=100
		tmp.setApiurl("http://api.c.adunity.mobi/ads2s?sourceid={apikey}&pagenum=100");
		tmp.setApikey("31717");
		tmp.setId(19L);
		
		PullAdunitysOfferService mmm=new PullAdunitysOfferService(tmp);
		mmm.run();
	}
	
}
