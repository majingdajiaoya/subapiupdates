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

public class PullHugOffersService implements PullOfferServiceTask{
	
protected static Logger logger = Logger.getLogger(PullAppFloodOfferService.class);
	
	private Advertisers advertisers;
	
	public PullHugOffersService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Adbink广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Adbink Offer begin := " + new Date());
			
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
				
				boolean status=jsonPullObject.getBooleanValue("success");
				Integer offers_num_this_page = jsonPullObject.getInteger("offers_num_this_page");
				if (!status) {
					return;
				}
				
				if (offers_num_this_page.intValue()<=0) {
					return;
				}
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
//				int apinum=0;
				if(offersArray != null
						&& offersArray.size() > 0){
//					apinum=offersArray.size();
					logger.info("begin pull offer size := " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer campaign_id = item.getInteger("campid");// 430633
							String platform = item.getString("platform");// "android"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
							Float payout_amt = item.getFloat("price");// 2.55
							String countriesArray = item.getString("geo");// "AU","US"
							String pkg = item.getString("app_id");
							String preview_url = item.getString("preview_link");
							String app_name = item.getString("offer_name");
							String payout_type = item.getString("price_model");// "CPA"
							String linkout_url = item.getString("tracking_link");
							
							String mainicon=item.getString("icon_link");
							
							/**
							 * 特殊处理字段
							 */
							
							String min_os_version = "";// "4.4"
							
							// 系统平台
							if(!OverseaStringUtil.isBlank(platform)){
								
								if("android".equals(platform.toLowerCase())){
									
									platform = "0";
									min_os_version=item.getString("min_android_version");
								}
								if("ios".equals(platform.toLowerCase())){
									
									platform = "1";
									min_os_version=item.getString("min_ios_version");
								}
								if("Other".equals(platform.toLowerCase())){
									continue;
								}
							}
							
//							{aff_sub}
							linkout_url=linkout_url.replace("[click_id]", "{aff_sub}");
							linkout_url=linkout_url.replace("[source]", "{channel}");
							linkout_url=linkout_url.replace("[idfa]", "{idfa}");
							linkout_url=linkout_url.replace("[advertising_id]", "{gaid}");
							
							
							// 国家
							String countries_str = countriesArray.replace(",", ":");
							
							if(campaign_id == null
									|| payout_amt == null
									|| OverseaStringUtil.isBlank(countries_str)
									|| OverseaStringUtil.isBlank(linkout_url)
									|| OverseaStringUtil.isBlank(platform)
									|| OverseaStringUtil.isBlank(pkg)
									|| OverseaStringUtil.isBlank(payout_type)){
								
								continue;
							}
							Integer daily_cap = item.getInteger("daily_cap");
							
							if (daily_cap<=0) {
								continue;
							}
							
						
							
//							//处理 icon，description,还有 creatives等
							String carrier = item.getString("carriers");
							String Description=item.getString("app_desc");
//							logger.info("offerid :"+campaign_id);
//							logger.info("name :"+app_name);
//							logger.info("advertisersId :"+advertisersId);
//							logger.info("pkg :"+pkg);
//							logger.info("previewlink :"+preview_url);
//							logger.info("tracking_link :"+linkout_url);
//							logger.info("countries :"+countries_str);
//							logger.info("dailyCap :"+daily_cap);
//							logger.info("payout :"+payout_amt);
//							logger.info("os_str :"+platform);
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
							advertisersOffer.setName(app_name);
							
							//
							if("CPI".equals(payout_type.toUpperCase())){
								
								advertisersOffer.setCost_type(101);	
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							} else if ("CPA".equals(payout_type.toUpperCase())) {
								advertisersOffer.setCost_type(102);	
								advertisersOffer.setOffer_type(102);
								advertisersOffer.setConversion_flow(102);
							}
							else{
								
								advertisersOffer.setCost_type(104);	
								advertisersOffer.setOffer_type(104);
								advertisersOffer.setConversion_flow(104);
							}
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(pkg);
							//advertisersOffer.setp();
							advertisersOffer.setMain_icon(mainicon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(linkout_url);
							advertisersOffer.setCountry(countries_str);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(daily_cap);//无cap限制，默认3000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout_amt);
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setSupported_carriers(carrier);
							advertisersOffer.setDescription(Description);
							advertisersOffer.setOs(platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							//system_version_android_
							//system_version_ios_
							min_os_version=min_os_version.replace("system_version_android_", "");
							min_os_version=min_os_version.replace("system_version_ios_", "");
							advertisersOffer.setOs_version(min_os_version);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							//advertisersOffer.setIncent_type(incentivized);// 是否激励
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
		Advertisers tmp=new Advertisers(); //{apikey}
		http://adbabys.hoapi0.com/v1?cid=adbabys&token=2f2e1345859846c6a61c3dbfda312f7b
		tmp.setApiurl("http://justdo.hoapi0.com/v1?cid=justdo&token=e288fa45ab0a469a85be38b6d23c4e96&service=HugOffers.com");
		tmp.setApikey("55b1dadf0b3347a79edeb0f7a90cf65b");
		tmp.setId(26L);
		
//		&aff_sub=[click_id]&aff_pub=[source]&idfa=[idfa]&site_id=[appname]
		
		PullHugOffersService mmm=new PullHugOffersService(tmp);
		mmm.run();
	}
}
