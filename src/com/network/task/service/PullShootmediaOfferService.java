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

public class PullShootmediaOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullShootmediaOfferService.class);
	
	private Advertisers advertisers;
	
	public PullShootmediaOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Shootmedia广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){

			logger.info("doPull shootmedia Offer begin := " + new Date());
					
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
				
				Boolean success = jsonPullObject.getBoolean("success");// "status":"OK"
				
				if(success == null
						|| !success){
					
					logger.info("return, success := " + success);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray appArr = jsonPullObject.getJSONArray("apps");
				
				if(appArr == null
						|| appArr.size() == 0){
					
					logger.info("appArr is empty");
					
					return;
				}
				
				if(appArr != null
						&& appArr.size() > 0){
					
					logger.info("begin pull appArr " + appArr.size());
					
					for(int i = 0; i < appArr.size(); i++){
	
						JSONObject appItem = appArr.getJSONObject(i);
						
						if(appItem != null){
							
							String package_name = appItem.getString("package_name");// "com.icantw.wings",
							
							JSONArray iconsArray = appItem.getJSONArray("icons");
							
							String icon = null;
									
							if(iconsArray != null
									&& iconsArray.size() > 0){
								
								JSONObject iconItem = iconsArray.getJSONObject(0);
								icon = iconItem.getString("url");// "http:\/\/images.startappservice.com",
							}
							
							JSONArray offers = appItem.getJSONArray("offers");
							
							if(offers != null
									&& offers.size() > 0){
								
								for(int j = 0; j < offers.size(); j++){
								
									JSONObject item = offers.getJSONObject(j);
									
									Integer offer_id = item.getInteger("offer_id");// "1716071",
									String offer_name = item.getString("offer_name");// "MakeMyTrip-Flights Hotels Cabs-CA",
									Float  payout = item.getFloat("payout");// 0.88,
									String payout_type = item.getString("payout_type");// "CPI"
									String download_type = item.getString("download_type");// "GP"
									Integer remaining_cap_daily = item.getInteger("remaining_cap_daily");// 500,
									String preview_link = item.getString("preview_link");
									String tracking_link = item.getString("tracking_link");
									String incentive = item.getString("incentive");// "Non Incentive"
									String currency = item.getString("currency");// "USD",
									JSONArray platformsArray = item.getJSONArray("platforms");
									JSONArray countriesArray = item.getJSONArray("countries");
									String status = item.getString("status");// "active"
									
									/**
									 * 排除处理
									 */
									
									// 如果id、pkg、price、country、Platforms、clickURL、remaining_cap为空，舍弃不入库
									if(offer_id == null
											|| OverseaStringUtil.isBlank(package_name)
											|| payout == null
											|| countriesArray == null
											|| platformsArray == null
											|| OverseaStringUtil.isBlank(tracking_link)
											|| remaining_cap_daily == null){
										
										logger.info("offer_id := " + offer_id
												+ "package_name := " + package_name
												+ "payout := " + payout
												+ "countriesArray := " + countriesArray
												+ "platformsArray := " + platformsArray
												+ "tracking_link := " + tracking_link
												+ "remaining_cap_daily := " + remaining_cap_daily
												+ ", filter");
										
										continue;
									}
									
									if(!"active".equals(status)){
										
										logger.info("status := " + status + ", filter");
										
										continue;
									}
									
									// CPI类型，单价太低(0.06)，舍弃不入库
									if(payout < 0.06){
										
										logger.info("payout := " + payout + ", filter");
										
										continue;
									}
									
									/**
									 * 特殊字段处理
									 */
									
									// 剩余日cap
									if(remaining_cap_daily == -1){
										
										remaining_cap_daily = 10000;
									}
									
									//处理国家
									String countries_str = "";
									
									if(countriesArray != null
											&& countriesArray.size() > 0){
										
										for(int k = 0; k < countriesArray.size(); k++){
											
											String countriesItem = countriesArray.getString(k);
											
											if(countriesItem != null){
												
												countries_str += countriesItem.toUpperCase() + ":";
											}
										}
									}
									//去除最后一个:号
									if(!OverseaStringUtil.isBlank(countries_str)){
										
										countries_str = countries_str.substring(0, countries_str.length() - 1);
									}
									
									//处理平台(0:andriod:1:ios:2:pc)
									String os_str = "";
									
									if(platformsArray != null
											&& platformsArray.size() > 0){
										
										for(int k = 0; k < platformsArray.size(); k++){
											
											String platformItem = platformsArray.getString(k);
											
											if(platformItem != null){
												
												if("ANDROID".equals(platformItem.toUpperCase())){
													
													os_str +="0:";
												}
												else if("IOS".equals(platformItem.toUpperCase())){
													
													os_str +="1:";
												}
											}
										}
									}
									//去除最后一个:号
									if(!OverseaStringUtil.isBlank(os_str)){
										
										os_str = os_str.substring(0, os_str.length() - 1);
									}
									
									//激励类型(0:非激励 1:激励)
									Integer incetive = null;
									
									if(!OverseaStringUtil.isBlank(incentive)){
										
										if("Non Incentive".equals(incentive)){
											
											incetive = 0;
										}
										else if("Incentive".equals(incentive)){
											
											incetive = 1;
										}
									}
									
									/**
									 * 封装网盟广告对象
									 */
									
									AdvertisersOffer advertisersOffer = new AdvertisersOffer();
									
									advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));
									advertisersOffer.setName(offer_name);
									
									//都设置为CPI类型广告
									if("CPI".equals(payout_type)){
										
										advertisersOffer.setCost_type(101);
										advertisersOffer.setOffer_type(101);
										advertisersOffer.setConversion_flow(101);
									}
									else if("CPA".equals(payout_type)
											&& !OverseaStringUtil.isBlank(download_type)){
										
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
									
									advertisersOffer.setCountry(countries_str);
								
									//advertisersOffer.setEcountry(ecountry);
									
									advertisersOffer.setDaily_cap(remaining_cap_daily);
									//advertisersOffer.setsend_cap();
									advertisersOffer.setPayout(payout);
									//advertisersOffer.setExpiration(expriation);
									//advertisersOffer.setcreatives();
									
									//advertisersOffer.setSupported_carriers("");
									//advertisersOffer.setDescription(completionAction);
									advertisersOffer.setOs(os_str);
									
									advertisersOffer.setDevice_type(0);//设置为mobile类型
									//advertisersOffer.setOs_version(minOS);//系统版本要求
									advertisersOffer.setSend_type(0);//系统入库生成广告
									advertisersOffer.setIncent_type(incetive);// 是否激励
									advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
									advertisersOffer.setStatus(0);//设置为激活状态
									
									advertisersOfferList.add(advertisersOffer);
								}
							}
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
	
}
