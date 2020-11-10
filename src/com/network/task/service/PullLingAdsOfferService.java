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

public class PullLingAdsOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullLingAdsOfferService.class);
	
	private Advertisers advertisers;
	
	public PullLingAdsOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Glispa广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull LingAdss Offer begin := " + new Date());
			
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
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				int status = jsonPullObject.getIntValue("total");
				
				if(status<=0){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						try {
							JSONObject item = offersArray.getJSONObject(i);
							
							if(item != null){
								
								String campaign_id = item.getString("id");
								Float payout_amount = item.getFloat("price");// 0.31
								//String payout_currency = item.getString("payout_currency");
								Integer daily_remaining_leads = item.getInteger("daily_cap");// 9992
								String mobile_app_id = item.getString("packet_name");
								String mobile_platform = item.getString("platform");
								String mobile_min_version = item.getString("minOS");
								Integer incentivized = item.getInteger("incent");
								String name = item.getString("name");
								//String category = item.getString("category");
								//String short_description = item.getString("short_description");
								String click_url = item.getString("click_url");
								//http://click.lingads.mobi/index.php?m=advert&p=click&app_id=1&offer_id=1708&
								//aff_sub={aff_sub}&gaid={gaid}&android={android}&idfa={idfa}&channel={channel}
								click_url = click_url.replace("{android}", "{android_id}");
								click_url = click_url.replace("{channel}", "{sub_affid}");
								
								String preload_click_url = item.getString("preview_url");
								String icon = item.getString("icon");
								
								String description = item.getString("description");
								String kpi= item.getString("kpi");
								if (description ==null||description.equals("")) {
									description= name;
								}
								if (kpi!= null &&!kpi.equals("")) {
									description = kpi + " "+description;
								}
								
								if(mobile_platform != null){
									if("android".equals(mobile_platform.toLowerCase())){
										
										mobile_platform = "0";
									}
									if("ios".equals(mobile_platform.toLowerCase())){
										
										mobile_platform = "1";
									}
								}
								
								JSONArray countriesArray = item.getJSONArray("countries");
								
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
								
								// 如果id、price、country、clickURL、mobile_platform、mobile_app_id、daily_remaining_leads为空，舍弃不入库
								if(campaign_id == null
										|| payout_amount == null
										|| countriesArray == null
										|| countriesArray.size() == 0
										|| OverseaStringUtil.isBlank(click_url)
										|| OverseaStringUtil.isBlank(mobile_platform)
										|| OverseaStringUtil.isBlank(mobile_app_id)
										|| daily_remaining_leads == null
										|| daily_remaining_leads.intValue()==0){
									
									continue;
								}
								
								// 单价太低(0.06)，舍弃不入库
								if(payout_amount < 0.06){
									
									continue;
								}
//								JSONObject images = item.getJSONObject("images");
								String images_crative=null;
//
//								if (images!= null) {
//									JSONArray images_JSONArray=new JSONArray();
//									ArrayList<Map> arrayList = new ArrayList<Map>();
//									 for (Map.Entry<String, Object> entry : images.entrySet()) {
//								            //System.out.println(entry.getKey() + ":" + entry.getValue());
//										 if (arrayList.size()<=3) {
//									            Map jsonObject = new HashMap();
//									            jsonObject.put("url", entry.getValue());
//									            jsonObject.put("size", entry.getKey());
//									            arrayList.add(jsonObject);
//										 } else {
//											break; 
//										 }
//								        }
//									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
//									 images_crative = images_JSONArray.toString();
//								}
								
								
								
								/**
								 * 生成广告池对象
								 */
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								
								advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
								advertisersOffer.setName(name);
								
								//CPI类型
								advertisersOffer.setCost_type(101);	
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
								
								advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
								advertisersOffer.setPkg(mobile_app_id);
								//advertisersOffer.setpkg_size();
								advertisersOffer.setMain_icon(icon);
								advertisersOffer.setPreview_url(preload_click_url);
								advertisersOffer.setClick_url(click_url);
								advertisersOffer.setCountry(countries_str);
								//advertisersOffer.setEcountry(ecountry);
								advertisersOffer.setDaily_cap(daily_remaining_leads);
								//advertisersOffer.setsend_cap();
								advertisersOffer.setPayout(payout_amount);
								//advertisersOffer.setExpiration(expriation);
								//advertisersOffer.setcreatives();
								//advertisersOffer.setSupported_carriers("");
								advertisersOffer.setCreatives(images_crative);
								advertisersOffer.setDescription(description);
								advertisersOffer.setOs(mobile_platform);
								advertisersOffer.setDevice_type(0);// 0代表手机mobile
								advertisersOffer.setOs_version(mobile_min_version);//系统版本要求
								advertisersOffer.setSend_type(0);//系统入库生成广告
								advertisersOffer.setIncent_type(incentivized);// 是否激励
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
	
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		//http://api.lingads.mobi/index.php?request_uri=/Offer/Getoffer/getOffer&app_id=1&app_key=b60af0be385aefd6b3420203664ad602
		tmp.setApiurl("http://api.lingads.mobi/index.php?request_uri=/Offer/Getoffer/getOffer&{apikey}");
		tmp.setApikey("app_id=1&app_key=b60af0be385aefd6b3420203664ad602");
		tmp.setId(25L);
		
		PullLingAdsOfferService mmm=new PullLingAdsOfferService(tmp);
		mmm.run();
	}
	
}
