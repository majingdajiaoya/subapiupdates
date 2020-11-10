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

public class PullvisionNewapiOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullvisionNewapiOfferService.class);
	
	private Advertisers advertisers;
	
	public PullvisionNewapiOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Leadbolt广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull visionNewapi Offer begin := " + new Date());
			
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
				int total=jsonPullObject.getIntValue("total_count");
				if (total <= 0) {
					logger.info("total is 0 return");
					return;
				}
				
				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns_info");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer size := " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							String campaign_id = item.getString("campaign_id");// 430633
							Float payout_amt = item.getFloat("payout");// 2.55
							String app_name = item.getString("campaign_name");
							String payout_type = item.getString("payout_type");//CPI/CPL/CPC/CPS
							Integer day_cap =item.getInteger("day_cap");
							String traffic=item.getString("traffic");
							String kpi=item.getString("kpi");
							String linkout_url = item.getString("tracking_link");
							
							Integer incetive = null;// 是否激励(0:非激励 1:激励)
							if ("Non-Incent".equals(traffic)) {
								incetive=0;
							} else {
								incetive=1;
							}
							JSONObject target_into = item.getJSONObject("target_into");
							JSONObject product_info = item.getJSONObject("product_info");
							
							String platform=null;
							JSONArray countriesArray = null;
							String min_os_version = null;
							if (target_into!=null) {
								platform = target_into.getString("platform");// "android"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
								countriesArray = target_into.getJSONArray("geo");// ["AU","US"]
				                //"min_os_version":"4.0.0",
				                //"deviceid_must":"gaid"
								min_os_version = target_into.getString("min_os_version");// "4.4"
							    String deviceid_must = target_into.getString("deviceid_must");
							}
							String pkg = null;
							String description=null;
							String icon_url=null;
							String preview_link = null;
							String creative_link = null;
							if (product_info!=null) {
								pkg = product_info.getString("package_id");
								description = product_info.getString("description");
								icon_url = product_info.getString("icon_url");
								preview_link= product_info.getString("preview_link");
								creative_link = product_info.getString("creative_link");
							}
							if (kpi!=null) {
								if (description!=null) {
									description =description+" "+ kpi;
								} else {
									description = kpi;
								}
							}
							
							/**
							 * 特殊处理字段
							 */
							
							// 系统平台
							if(!OverseaStringUtil.isBlank(platform)){
								
								if("android".equals(platform.toLowerCase())){
									
									platform = "0";
								} else if("ios".equals(platform.toLowerCase())){
									platform = "1";
									linkout_url=linkout_url+"&app=PixPaint+-+Number+Coloring";
								}
							}
							
							// 国家
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
							
							// 如果id、price、country、clickURL、mobile_platform、pkg、payout_type为空，舍弃不入库
							if(campaign_id == null
									|| payout_amt == null
									|| OverseaStringUtil.isBlank(countries_str)
									|| OverseaStringUtil.isBlank(linkout_url)
									|| OverseaStringUtil.isBlank(platform)
									|| OverseaStringUtil.isBlank(pkg)
									|| OverseaStringUtil.isBlank(payout_type)){
								
								continue;
							}
							
							// 单价太低(0.06)，舍弃不入库
							if(payout_amt < 0.06){
								
								continue;
							}
							//处理 icon，description,还有 creatives等
							if (day_cap.intValue() == 0 ) {
								day_cap=99999;
							}
							
//							JSONArray creatives = item.getJSONArray("creatives");// ["AU","US"]
//							String Description="";
//							String mainicon=null;
//							String images_crative=null;
//
//							ArrayList<Map> arrayList = new ArrayList<Map>();
//
//							if (creatives!=null&&creatives.size()>0) {
//								for (int j=0;j<creatives.size();j++) {
//									JSONObject creativesitem = creatives.getJSONObject(j);
//									
//									if (creativesitem!=null) {
//										if (creativesitem.containsKey("creative_type")
//												&&"Text".equals(creativesitem.getString("creative_type"))) {
//											String temp=creativesitem.getString("title")+" "+creativesitem.getString("description");
//										    if (temp.length()>Description.length()) {
//										    	Description=temp;
//										    }
//										}
//										if (creativesitem.containsKey("creative_type")
//												&&"Image".equals(creativesitem.getString("creative_type"))) {
//											if (creativesitem.getInteger("width").intValue()==120
//													&&creativesitem.getInteger("height").intValue()==120) {
//												mainicon=creativesitem.getString("src_url");
//											} else {
//												if (arrayList.size() <4) {
//										            Map jsonObject = new HashMap();
//										            jsonObject.put("url",creativesitem.getString("src_url"));
//										            jsonObject.put("size",creativesitem.get("width")+"x"+creativesitem.get("height") );
//										            arrayList.add(jsonObject);
//												}
//											}
//											
//										}
//										
//										
//									}
//								}
//								JSONArray images_JSONArray=new JSONArray();
//								//
//								if (arrayList.size()>0) {
//									images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
//									images_crative = images_JSONArray.toString();
//								}
//							}
							
							
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(campaign_id);
							advertisersOffer.setName(app_name);
							
							//CPA类型(leadbolt 不做订阅，app安装都归类为CPA类型)
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
							advertisersOffer.setPkg(pkg);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon_url);
							advertisersOffer.setPreview_url(preview_link);
							advertisersOffer.setClick_url(linkout_url);
							advertisersOffer.setCountry(countries_str);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(day_cap);//无cap限制，默认3000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout_amt);
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(creative_link);
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(description);
							advertisersOffer.setOs(platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers(); //{apikey}
		//http://offer.vision-vision.com/api/public/campaigns?token=3e9f305d-bed8-4b85-a0ca-63e6c86a39a7
		tmp.setApiurl("http://offer.vision-vision.com/api/public/campaigns?token={apikey}");
		tmp.setApikey("3e9f305d-bed8-4b85-a0ca-63e6c86a39a7");
		tmp.setId(26L);
		
		PullvisionNewapiOfferService mmm=new PullvisionNewapiOfferService(tmp);
		mmm.run();
	}
	
}
