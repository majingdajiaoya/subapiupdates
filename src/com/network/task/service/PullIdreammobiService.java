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

public class PullIdreammobiService implements PullOfferServiceTask{
	
protected static Logger logger = Logger.getLogger(PullIdreammobiService.class);
	
	private Advertisers advertisers;
	
	public PullIdreammobiService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	

	/**
	 * 拉取Idreammobi广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	@Override
	public void run() {
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Idreammobi Offer begin := " + new Date());

			try{
				
				 Long advertisersId = this.advertisers.getId();
				 
				 String apiurl = this.advertisers.getApiurl();//获取请求的url
				 String apikey = this.advertisers.getApikey();
				    
				 if (advertisersId == null
				    	|| OverseaStringUtil.isBlank(apiurl)
				    	|| OverseaStringUtil.isBlank(apikey)){
				    	
				    logger.info("advertisersId or apiurl or apikey is null return");
				    return;
				 }
				apiurl = apiurl.replace("{apikey}", apikey); 
				//获取所有的数据并且转换为JSONArray对象
				String str = HttpUtil.sendGet(apiurl);
				
				JSONObject jsonObject = JSON.parseObject(str);
				
				Boolean status = jsonObject.getBoolean("success");// "statusMessage":"OK"
				
				if(status == null || !status){
					
					logger.info("status is not true return. status := " + status);
					
					return;
				}
				
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonObject.getJSONArray("offers");
				
				if(offersArray != null && offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					for(int i = 0; i < offersArray.size(); i++){
						JSONObject item = offersArray.getJSONObject(i);
					    try {
							if(item!=null){
								
								String offerstatus=item.getString("status").toUpperCase();
								if(!offerstatus.equals("RUNNING")){
									continue;
								}
								String adv_offer_id=item.getString("campid");
								String offer_name=item.getString("offer_name");
								String pkg=item.getString("app_id");
								String icon=item.getString("icon_link");
								Float payout = item.getFloat("price");
								String preview_url=item.getString("preview_link");
								
								String countries_str =item.getString("geo");
								
								if(OverseaStringUtil.isBlank(countries_str)){
									continue;
								}
								countries_str=countries_str.replace(",", ":");
								
								Integer daily_cap=item.getInteger("daily_cap");
								String description=item.getString("performance_criteria");
								String plantform=item.getString("platform");
								
								String kpitype = item.getString("price_model");
								
								if ("".equals(description)) {
									description = offer_name;
								}
								
								String click_url=item.getString("tracking_link");
								
								click_url=click_url.replace("[click_id]", "{aff_sub}");
								click_url=click_url.replace("[source]", "{sub_affid}");
								click_url=click_url.replace("[idfa]", "{idfa}");
								click_url=click_url.replace("[advertising_id]", "{gaid}");
								
								String os_version="";
								if(plantform != null){
									if("android".equals(plantform.toLowerCase())){
										plantform = "0";
										os_version=item.getString("min_android_version");
									} else if("ios".equals(plantform.toLowerCase())){
										plantform = "1";
										os_version=item.getString("min_ios_version");
									}
								}
								
								String incent_type=item.getString("requirement_traffic");
								Integer incent=0;
								if(!incent_type.contains("non-incent")){
									incent=1;
								}
								
								JSONArray creatives = item.getJSONArray("creative_link");
								String images_crative=null;
								if(creatives != null && creatives.size() > 0){
									ArrayList<Map> arrayList = new ArrayList<Map>();
									JSONArray images_JSONArray=new JSONArray();
									for(int k = 0; k < creatives.size(); k++){
										if (arrayList.size() <3) {
											JSONObject creativesItem = creatives.getJSONObject(k);
											if(creativesItem!=null&&!"".equals(creativesItem)){
												String url=creativesItem.getString("url");
												String height=creativesItem.getString("height");
												String width=creativesItem.getString("width");
												Map jsonMap = new HashMap();
												jsonMap.put("url",url);
												jsonMap.put("size", width+"x"+height);
										        arrayList.add(jsonMap);
											}
										}
									}
									images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									images_crative = images_JSONArray.toString();
								}						
								
								// 如果adv_offer_id payout countriesArray click_url plantform pkg daily_cap为空，舍弃不入库
								if(adv_offer_id == null
										|| payout == null
										|| OverseaStringUtil.isBlank(click_url)
										|| OverseaStringUtil.isBlank(plantform)
										|| OverseaStringUtil.isBlank(pkg)
										|| daily_cap == null){
									
									continue;
								}
								
								// 单价太低(0.06)，舍弃不入库
								if(payout < 0.06){
									continue;
								}
								// CPI类型，pkg为空，舍弃不入库
								if("CPI".equals(kpitype)
										&& OverseaStringUtil.isBlank(pkg)){
									
									continue;
								}
								if(daily_cap==null || daily_cap==0){
									daily_cap=99999;
								}
								
								
//								System.out.println(adv_offer_id+"::"+offer_name+"::"+pkg+"::"+icon+"::"+preview_url+"::"+click_url+"::"+countries_str+"::"
//										+daily_cap+"::"+payout+"::"+description+"::"+plantform+"::"+os_version+"::"+incent_type+"::"+images_crative);
								
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								advertisersOffer.setAdv_offer_id(String.valueOf(adv_offer_id));
								advertisersOffer.setName(TuringStringUtil.filterEmoji(offer_name));
								
								//CPI类型
								if ("CPI".equals(kpitype.toUpperCase())) {
									advertisersOffer.setCost_type(Integer.valueOf(101));
									advertisersOffer.setOffer_type(Integer.valueOf(101));
									advertisersOffer.setConversion_flow(Integer.valueOf(101));
								} else if ("CPA".equals(kpitype.toUpperCase())) {
									advertisersOffer.setCost_type(Integer.valueOf(102));
									advertisersOffer.setOffer_type(Integer.valueOf(102));
									advertisersOffer.setConversion_flow(Integer.valueOf(102));
								} else {
									advertisersOffer.setCost_type(Integer.valueOf(104));
									advertisersOffer.setOffer_type(Integer.valueOf(104));
									advertisersOffer.setConversion_flow(Integer.valueOf(104));
								}
								
								advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
								advertisersOffer.setPkg(pkg);
								//advertisersOffer.setpkg_size();
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(preview_url);
								advertisersOffer.setClick_url(click_url);
								advertisersOffer.setCountry(countries_str);
								//advertisersOffer.setEcountry(ecountry);
								advertisersOffer.setDaily_cap(daily_cap);
								//advertisersOffer.setsend_cap();
								advertisersOffer.setPayout(payout);
								//advertisersOffer.setExpiration(expriation);
								advertisersOffer.setCreatives(images_crative);
								//advertisersOffer.setSupported_carriers("");
								advertisersOffer.setDescription(TuringStringUtil.filterEmoji(description));
								advertisersOffer.setOs(plantform);
								advertisersOffer.setDevice_type(0);// 0代表手机mobile
								advertisersOffer.setOs_version(os_version);//系统版本要求
								advertisersOffer.setSend_type(0);//系统入库生成广告
								advertisersOffer.setIncent_type(incent);// 是否激励
								advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
								advertisersOffer.setStatus(0);//设置为激活状态
								
								advertisersOfferList.add(advertisersOffer);
								
							}
							} catch(Exception e) {
								e.printStackTrace();
							}
					    

					}
					logger.info("after Idreammobi pull offer size := " + advertisersOfferList.size());
					
					// 入网盟广告
					if(advertisersId != null
							&& advertisersOfferList != null
							&& advertisersOfferList.size() > 0){
						
						PullOfferCommonService pullOfferCommonService 
							= (PullOfferCommonService)TuringSpringHelper.getBean("pullOfferCommonService");
						
						pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
					}
				}
				
				

			}catch(Exception e){
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
	
	public static void main(String[] args){
		Advertisers tmp=new Advertisers();
		
		tmp.setApiurl("http://idreammobi.hoapi0.com/v1?cid=idreammobi&token={apikey}");
		tmp.setApikey("0b4f5f1175cd414988694ef7b6aa79af");
		tmp.setId(25L);
		
		PullIdreammobiService mmm=new PullIdreammobiService(tmp);
		mmm.run();
	}

}
