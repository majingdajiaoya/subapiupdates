package com.network.task.service;

import java.text.DecimalFormat;
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
import com.network.task.util.CountryMappingUtil;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import com.network.task.util.TuringStringUtil;

import common.Logger;

public class PullPubnativeOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullPubnativeOfferService.class);
	
	private Advertisers advertisers;
	
	public PullPubnativeOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Pubnative广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Pubnative Offer begin := " + new Date());
			
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
			
				JSONArray offersArray = JSONArray.parseArray(str);
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						try {
							if(item != null){
								
								JSONObject app_details = item.getJSONObject("app_details");
								JSONObject creatives = item.getJSONObject("creatives");
								JSONArray campaigns = item.getJSONArray("campaigns");
								
								String bundle_id = app_details.getString("bundle_id");//pkg or appid
								String platform = app_details.getString("platform");
								String category = app_details.getString("category");
								String icon_url = creatives.getString("icon_url");
								String title = creatives.getString("title");
								
								if(campaigns != null
										&& campaigns.size() > 0){
									
									for(int j = 0; j < campaigns.size(); j++){
										
										JSONObject campaign = campaigns.getJSONObject(j);
										
										String cid = campaign.getString("cid");
										String click_url = campaign.getString("click_url");
										JSONArray countries = campaign.getJSONArray("countries");
										Integer points = campaign.getInteger("points");
										String min_os_version = campaign.getString("min_os_version");
										
										// 如果id、price、country、clickURL、platform为空，舍弃不入库
										if(OverseaStringUtil.isBlank(cid)
												|| points == null
												|| countries == null
												|| countries.size() == 0
												|| OverseaStringUtil.isBlank(click_url)
												|| OverseaStringUtil.isBlank(platform)){
											
											continue;
										}
										
										DecimalFormat df = new DecimalFormat("0.000");
										float payout =  Float.valueOf(df.format(Float.valueOf((float)points / (float)1000)));
										
										if(payout < 0.06){
											
											continue;
										}
										
										if(OverseaStringUtil.isBlank(bundle_id)){
											
											continue;
										}
										
										/**
										 * 特殊处理字段
										 */
										
										//国家
										String country_str = "";
										
										if(countries != null
												&& countries.size() > 0){
											
											for(int k = 0; k < countries.size(); k++){
												
												String country3ch = String.valueOf(countries.get(k));
												
												country_str += CountryMappingUtil.getCountryByCountry3ch(country3ch) + ":";
											}
										}
										
										//去除最后一个:号
										if(!OverseaStringUtil.isBlank(country_str)){
											
											country_str = country_str.substring(0, country_str.length() - 1);
										} else {
											continue;
										}
										
										//平台
										String os_str = "";
										
										if("android".equals(platform.toLowerCase())){
											
											os_str += "0:";
											click_url=click_url+"&gid={gaid}";
										}
										if("ios".equals(platform.toLowerCase())){
											
											os_str += "1:";
											click_url=click_url+"&gid={idfa}";
										}
										
										//去除最后一个:号
										if(!OverseaStringUtil.isBlank(os_str)){
											
											os_str = os_str.substring(0, os_str.length() - 1);
										}
										String target_url=null;
										target_url = TuringStringUtil.getPriviewUrl(os_str, bundle_id);
										
										String images_crative=null;
										String banner_url = creatives.getString("banner_url");
										if (banner_url!=null &&!"".equals(banner_url)) {
											ArrayList<Map> arrayList = new ArrayList<Map>();
								            Map jsonObject = new HashMap();
								            jsonObject.put("url", banner_url);
								            jsonObject.put("size", "1200*627");
								            arrayList.add(jsonObject);
											
											JSONArray images_JSONArray=new JSONArray();
											 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
											 images_crative = images_JSONArray.toString();

										}
										
										
										AdvertisersOffer advertisersOffer = new AdvertisersOffer();
										
										advertisersOffer.setAdv_offer_id(cid);
										advertisersOffer.setName(title);
										
										//CPI类型	
										advertisersOffer.setCost_type(101);
										advertisersOffer.setOffer_type(101);
										advertisersOffer.setConversion_flow(101);
										
										advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
										advertisersOffer.setPkg(bundle_id);
										//advertisersOffer.setpkg_size();
										advertisersOffer.setMain_icon(icon_url);
									    advertisersOffer.setPreview_url(target_url);
										advertisersOffer.setClick_url(click_url);
										
										advertisersOffer.setCountry(country_str);
										
										//advertisersOffer.setEcountry(ecountry);
										
										advertisersOffer.setDaily_cap(99999);//不限制
										//advertisersOffer.setsend_cap();
										advertisersOffer.setPayout(payout);
										//advertisersOffer.setExpiration(expriation);
										//advertisersOffer.setcreatives();
										advertisersOffer.setCreatives(images_crative);
										//advertisersOffer.setSupported_carriers("");
										advertisersOffer.setDescription(title);
										advertisersOffer.setOs(os_str);
										
										advertisersOffer.setDevice_type(0);//mobile
										advertisersOffer.setOs_version(min_os_version);//系统版本要求
										advertisersOffer.setSend_type(0);//系统入库生成广告
										advertisersOffer.setIncent_type(0);// 0 : 否，是否激励
										advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
										advertisersOffer.setStatus(0);//设置为激活状态
										
										advertisersOfferList.add(advertisersOffer);
									}
								}
							}
						} catch (Exception e) {
							
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
		//http://bulk.pubnative.net/api/bulk/v1/promotions?app_token=1561bd0afa5b421d8a38db72eae2dc4d
		//http://bulk.pubnative.net/api/bulk/v1/promotions?app_token={app_token}&platform={platform}&countries[]={country_code}&countries[]={country_code}
		tmp.setApiurl("http://bulk.pubnative.net/api/bulk/v1/promotions?app_token={apikey}");
		tmp.setApikey("1561bd0afa5b421d8a38db72eae2dc4d");
		tmp.setId(25L);
		//&pub_sub_id={sub_affid}&aff_sub={aff_sub}
		PullPubnativeOfferService mmm=new PullPubnativeOfferService(tmp);
		mmm.run();
	}
	
}
