package com.network.task.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
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

public class PullTapgerineOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullTapgerineOfferService.class);
	
	private Advertisers advertisers;
	
	public PullTapgerineOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Tapgerine广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){

			logger.info("doPull tapgerine Offer begin := " + new Date());
					
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
				
				Boolean success = jsonPullObject.getBoolean("success");
				String error_messages = jsonPullObject.getString("error_messages");
				
				if(success == null
						|| !success){
					
					logger.info("status is not ok return. success := " + success);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONObject offers = jsonPullObject.getJSONObject("offers");
				
				if(offers == null
						|| offers.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				if(offers != null
						&& offers.size() > 0){
					
					logger.info("begin pull offer " + offers.size());
					
					for(java.util.Map.Entry<String,Object> entry : offers.entrySet()){
	
						JSONObject item = (JSONObject)entry.getValue();
						
						if(item != null){
								
							String ID = item.getString("ID");// "1545238"
							String APP_ID = item.getString("APP_ID");// "com.app.chatozz"
							String Name = item.getString("Name");// "ChatOZZ messenger for chats (Android) PL - Incent"
							String Original_name = item.getString("Original_name");// "ChatOZZ messenger for chats"
							String Description = item.getString("Description");// "<p><br></p>"
							String Type = item.getString("Type");// "Incent"
							String Preview_url = item.getString("Preview_url");// "https://play.google.com/store/apps/details?id=com.app.chatozz"
							String Tracking_url = item.getString("Tracking_url");// "http://track.tapgerine.net/?aff_id=586192&offer_id=1545238"
							String Icon_url = item.getString("Icon_url");// "http://tapgerine.net/creative/HpzkXMkjRgDyf
							String Currency = item.getString("Currency");// "USD"
							String Tags = item.getString("Tags");// "Communication,Incent,Android,CPI,No KPI"
							String Platforms = item.getString("Platforms");// "Android,iPhone,iPad"
							String Countries = item.getString("Countries");// "PL,US"
							String Payout = item.getString("Payout");// "0.35"
							String Status = item.getString("Status");// "active"
							String Expiration_date = item.getString("Expiration_date");// "2018-07-03 12:11:54"
							String Daily_cap = item.getString("Daily_cap");// "3000"
							String Monthly_cap = item.getString("Monthly_cap");// "0"
							String Approved = item.getString("Approved");// "0"
							
							//TODO
							//Restrictions;
							
							/**
							 * 排除处理
							 */
							
							// 如果id、pkg、price、country、Platforms、clickURL、remaining_cap为空，舍弃不入库
							if(OverseaStringUtil.isBlank(ID)
									|| OverseaStringUtil.isBlank(APP_ID)
									|| OverseaStringUtil.isBlank(Payout)
									|| OverseaStringUtil.isBlank(Countries)
									|| OverseaStringUtil.isBlank(Platforms)
									|| OverseaStringUtil.isBlank(Tracking_url)
									|| OverseaStringUtil.isBlank(Daily_cap)){
								
								continue;
							}
							
							// 如果非active，排除
							if(OverseaStringUtil.isBlank(Status)
									|| !"active".equals(Status)){
								
								continue;
							}
							
							// Approved=0状态舍弃，保留Approved=1的
							if(OverseaStringUtil.isBlank(Approved)
									|| !"1".equals(Approved)){
								
								continue;
							}
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if(Float.valueOf(Payout) < 0.06){
								
								continue;
							}
							
							/**
							 * 特殊字段处理
							 */
							//处理国家
							Countries = Countries.replace(",", ":");
							
							//处理平台(0:andriod:1:ios:2:pc)
							String os_str = "";
							
							String[] platformsArr = Platforms.split(",");
							
							Map<String, String> platformsMap = new HashMap<String, String>();
							
							if(platformsArr != null
									&& platformsArr.length > 0){
								
								for(String platform : platformsArr){
									
									if("ANDROID".equals(platform.toUpperCase())){
										
										platformsMap.put("0", "0");
									}
									else if("IPHONE".equals(platform.toUpperCase())){
										
										platformsMap.put("1", "1");
									}
									else if("IPAD".equals(platform.toUpperCase())){
										
										platformsMap.put("1", "1");
									}
								}
							}
							
							if(!platformsMap.isEmpty()){
								
								for(String key : platformsMap.keySet()){
									
									os_str += key + ":";
								}
							}
							
							//去除最后一个字符
							os_str = os_str.substring(0, os_str.length() - 1);
							
							//激励类型(0:非激励 1:激励)
							Integer incetive = null;
							
							if(!OverseaStringUtil.isBlank(Type)){
								
								if("INCENT".equals(Type.toUpperCase())){
									
									incetive = 1;
								}
								else if("NON INCENT".equals(Type.toUpperCase())){
									
									incetive = 0;
								}
							}
							
							/**
							 * 封装网盟广告对象
							 */
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(ID));
							advertisersOffer.setName(Name);
							
							
							//都设置为CPI类型广告	
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(APP_ID);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(Icon_url);
							advertisersOffer.setPreview_url(Preview_url);
							advertisersOffer.setClick_url(Tracking_url);
							
							advertisersOffer.setCountry(Countries);
						
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(Integer.parseInt(Daily_cap));
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(Float.valueOf(Payout));
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(Tags);
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);//设置为mobile类型
							//advertisersOffer.setOs_version(min_version);//系统版本要求
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
	
}
