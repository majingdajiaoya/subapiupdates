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

public class PullMobisummerOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullMobisummerOfferService.class);
	
	private Advertisers advertisers;
	
	public PullMobisummerOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Mobisummer广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Mobisummer Offer begin := " + new Date());
			
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
				
				Boolean success = jsonPullObject.getBoolean("success");// 2449
				Integer offer_total = jsonPullObject.getInteger("offer_total");// "204"
				
				if(success == null
						|| !success){
					
					logger.info("success is false, return := " + success);
					
					return;
				}
				
				if(offer_total == null
						|| offer_total == 0){
					
					logger.info("offer_total is empty, return. offer_total:= " + offer_total);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							//String app_name = item.getString("app_name");
							String country = item.getString("country");
							Integer daily_cap = item.getInteger("daily_cap");
							if (daily_cap.intValue()==0) {
								daily_cap=99999;
							}
							JSONArray iconsArray = item.getJSONArray("icons");
							String incentive = item.getString("incentive");
							Integer offer_id = item.getInteger("offer_id");
							String offer_name = item.getString("offer_name");
							String package_name = item.getString("package_name");
							String payout = item.getString("payout");
							String payout_type = item.getString("payout_type");
							String platform = item.getString("platform");
							String preview_link = item.getString("preview_link");
							String required_os_version = item.getString("required_os_version");
							String status = item.getString("status");
							String tracking_link = item.getString("tracking_link");
							String termskpi=item.getString("terms");
							String description="";
							description=item.getString("description");
							if (termskpi!=null) {
								description=termskpi+" "+description;
							}
							JSONArray bannerArray = item.getJSONArray("banners");
							tracking_link=tracking_link.replace("{device_id}", "");

				            
				            /**
				             * 排除处理
				             */
				            
				            //如果id、price、country、clickURL、mobile_platform、pkg为空，舍弃不入库
							if(offer_id == null
									|| OverseaStringUtil.isBlank(payout)
									|| OverseaStringUtil.isBlank(country)
									|| daily_cap == null
									|| OverseaStringUtil.isBlank(tracking_link)
									|| OverseaStringUtil.isBlank(platform)
									|| OverseaStringUtil.isBlank(package_name)){
								
								continue;
							}
							
							if(!"active".equals(status)){
								
								continue;
							}
							
							// 单价太低(0.06)，舍弃不入库
							if(Float.valueOf(payout) < 0.06){
								
								continue;
							}
							
							/**
							 * 特殊处理字段
							 */
							String os_str = "";
							
							if("ANDROID".equals(platform.toUpperCase())){
								
								os_str = "0";
							}
							else if("IOS".equals(platform.toUpperCase())){
								
								os_str = "1";
							}
							
							// 国家
							String countries = country.replace(",", ":");
							
							//icon
							String icon = "";
							
							if(iconsArray != null
									&& iconsArray.size() > 0){
								
								JSONObject iconJSONObject = iconsArray.getJSONObject(0);
								
								icon = iconJSONObject.getString("url");
							}
				            ArrayList<Map> arrayList = new ArrayList<Map>();
							JSONArray images_JSONArray=new JSONArray();
							String images_crative=null;
							if (bannerArray!=null &&bannerArray.size()>0) {
								for (int k=0;k<bannerArray.size();k++) {
					 	       		if (arrayList.size()>3) {
					 	       			break;
					 	       		}
									JSONObject bannerJSONObject = bannerArray.getJSONObject(k);
						            Map jsonObject11 = new HashMap();
						            String url222=bannerJSONObject.getString("url");
						            if (url222!=null&&!"".equals(url222)) {
							            jsonObject11.put("url", bannerJSONObject.getString("url"));
							            jsonObject11.put("size", "*");
							            arrayList.add(jsonObject11);
						            }
								}
								
					 	       	if (arrayList.size()>0) {
									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									 images_crative = images_JSONArray.toString();
					 	       	}
							}
							
							
							//激励类型(0:非激励 1:激励)
							Integer incetive = null;
							
							if(!OverseaStringUtil.isBlank(incentive)){
								
								if("Non-incent".equals(incentive)){
									
									incetive = 0;
								}
								else if("Incentive".equals(incentive)){
									
									incetive = 1;
								}
							}	
							
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));
							advertisersOffer.setName(offer_name);
							
							//CPI类型
							if("CPI".equals(payout_type)){
								
								advertisersOffer.setCost_type(101);	
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							}
							else{
								
								advertisersOffer.setCost_type(104);	
								advertisersOffer.setOffer_type(104);
								advertisersOffer.setConversion_flow(104);
							}
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(package_name);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_link);
							advertisersOffer.setClick_url(tracking_link);
							advertisersOffer.setCountry(countries);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(daily_cap);//无cap限制，默认10000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(Float.valueOf(payout));
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(images_crative);
							advertisersOffer.setDescription(description);
							//advertisersOffer.setSupported_carriers("");
							//advertisersOffer.setDescription(category);
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(required_os_version);//系统版本要求
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//{apikey}
		//http://api.howdoesin.net/api/v1/get?code=c98e535a-627c-45c3-8abf-173847a7ad54&pageSize=1000
		tmp.setApiurl("http://api.howdoesin.net/api/v1/get?code={apikey}&pageSize=1000");
		tmp.setApikey("c98e535a-627c-45c3-8abf-173847a7ad54");
		tmp.setId(19L);
		
		PullMobisummerOfferService mmm=new PullMobisummerOfferService(tmp);
		mmm.run();
	}
	
}
