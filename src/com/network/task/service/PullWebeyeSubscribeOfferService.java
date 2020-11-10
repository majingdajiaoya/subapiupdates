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

public class PullWebeyeSubscribeOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullWebeyeSubscribeOfferService.class);
	
	private Advertisers advertisers;
	
	public PullWebeyeSubscribeOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Webeye订阅广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull webeye subscribe Offer begin := " + new Date());
			
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
				
				Integer status = jsonPullObject.getInteger("status");
				String message = jsonPullObject.getString("message");
				
				
				if(status == null
						|| status != 200){
					
					logger.info("status is not 200 return. status := " + status);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size()&& i < 1000; i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer campaign_id = item.getInteger("campaign_id");// 104573764
							String title = item.getString("title");// "SmartNews: Breaking News Headlines"
							String preview_url = item.getString("preview_url");
							String click_url = item.getString("click_url");
							Float payout_amount = item.getFloat("payout_amount");// 0.31
							String platform = item.getString("platform");// "android"
							JSONArray countriesArray = item.getJSONArray("countries");
							Integer cap = item.getInteger("cap");// 0
							String carriers = item.getString("carriers");// ""
							String category = item.getString("category");// "News & Magazines"
							String description = item.getString("description");// ""
							Boolean is_incent = item.getBoolean("is_incent");
							String payout_type = item.getString("payout_type");// ""
							
							if (cap==0) {
								cap=1000;
							}
							
							
							/**
							 * 排除处理
							 */
							
							// 如果id、price、country、clickURL、carriers、remaining_cap为空，舍弃不入库
							if(campaign_id == null
									|| payout_amount == null
									|| countriesArray == null
									|| countriesArray.size() == 0
									|| OverseaStringUtil.isBlank(click_url)
									|| OverseaStringUtil.isBlank(carriers)
									|| cap == null){
								
								continue;
							}
							
							// CPAI类型，单价太低(0.06)，舍弃不入库
							if(payout_amount < 0.06){
								
								continue;
							}
							
							//geo={geo}&sub={sub}
							click_url=click_url.replace("{geo}", "");
							click_url=click_url.replace("{sub}", "{aff_sub}");
							/**
							 * 特殊字段处理
							 */
							
							// cap
							if(cap != null
									&& cap == 0){
								
								cap = 10000;
							}
							
							// category处理，添加在title后
							if(!OverseaStringUtil.isBlank(title)
									&& !OverseaStringUtil.isBlank(category)){
								
								title += "(" + category + ")";
							}
							
							
							// 是否激励
							Integer incetive = null;// 是否激励(0:非激励 1:激励)
							if(is_incent != null
									&& is_incent){
								
								incetive = 1;
							}
							else{
								
								incetive = 0;
							}
							
							// 平台os
							String os_str = "";
							
							if(!OverseaStringUtil.isBlank(platform)){
								
								if("android".equals(platform.toLowerCase())){
									
									os_str += "0:";
								}
								if("ios".equals(platform.toLowerCase())){
									
									os_str += "1:";
								}
								if (os_str.equals("")) {
									os_str="0:1:";
								}
							}
							else{//为空代表不限制平台Android和IOS
								
								os_str = "0:1:";
							}
							
							// 去除最后一个:号
							if(!OverseaStringUtil.isBlank(os_str)){
								
								os_str = os_str.substring(0, os_str.length() - 1);
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
							String creative_files=item.getString("creative_files");
							String images_crative=null;
							if (creative_files!= null) {
								try{
									JSONArray creative_files_JSONArray=item.getJSONArray("creative_files");
									JSONArray images_JSONArray=new JSONArray();
									ArrayList<Map> arrayList = new ArrayList<Map>();

									if (creative_files_JSONArray!= null&&creative_files_JSONArray.size()>0) {
										int leng=creative_files_JSONArray.size();
										if (leng>6){
											leng=6;
										}
										for(int j = 0; j < leng; j++) {
											JSONObject imageitem = creative_files_JSONArray.getJSONObject(j);
											if (imageitem!= null) {
									            Map jsonObject = new HashMap();
									            jsonObject.put("url",imageitem.get("link"));
									            jsonObject.put("size",imageitem.get("width")+"x"+imageitem.get("height") );
									            arrayList.add(jsonObject);
											}
											
										}
										images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
										images_crative = images_JSONArray.toString();
								}

								} catch (Exception e) {
									logger.info("creative_files is: " + creative_files);
								}
							}
							
							/**
							 * 封装网盟广告对象
							 */
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
							advertisersOffer.setName(title);
							
							//CPA类型
							if("CPA".equals(payout_type)){
								
								advertisersOffer.setCost_type(102);	
								advertisersOffer.setOffer_type(102);
								advertisersOffer.setConversion_flow(102);
							}
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							//advertisersOffer.setPkg(package_name);
							//advertisersOffer.setpkg_size();
							//advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(click_url);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(cap);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout_amount);
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(images_crative);
							advertisersOffer.setSupported_carriers(carriers);
							advertisersOffer.setDescription(description);
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);// mobile
							//advertisersOffer.setOs_version(min_os_version);//系统版本要求
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
		//http://subapi.webeyemob.com/v1/sub/bulk?api_key=U9YXRJHGSN17ASHIHQXQZ3MR96BX7XQN&pub_id=90255
		tmp.setApiurl("http://subapi.webeyemob.com/v1/sub/bulk?api_key={apikey}&pub_id=90255");
		tmp.setApikey("U9YXRJHGSN17ASHIHQXQZ3MR96BX7XQN");
		tmp.setId(26L);
		
		PullWebeyeSubscribeOfferService mmm=new PullWebeyeSubscribeOfferService(tmp);
		mmm.run();
	}
	
}

