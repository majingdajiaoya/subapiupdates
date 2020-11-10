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

public class PullSuperlinkOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullSuperlinkOfferService.class);
	  
	private Advertisers advertisers;
	  
	public PullSuperlinkOfferService(Advertisers advertisers){
		  this.advertisers = advertisers;
	}
	  
	public void run(){
		  
		logger.info("doPull Superlink Offer begin := " + new Date());
		  
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
			
			Integer status = jsonObject.getInteger("status");// "status":"OK"
			
			if(status == null
					|| status != 1){
				
				logger.info("status is not ok return. status := " + status);
				
				return;
			}
			
			JSONObject jsonDataObject = jsonObject.getJSONObject("data");
			
			JSONArray offersArr = jsonDataObject.getJSONArray("list");
			
			if(offersArr == null
					|| offersArr.size() == 0){
				
				logger.info("offersArray is empty");
				
				return;
			}
			
			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
			
			if(offersArr != null
					&& offersArr.size() > 0){
				
				logger.info("begin pull offer " + offersArr.size());
				
				for(int i = 0; i < offersArr.size(); i++){

					JSONObject item = offersArr.getJSONObject(i);
					
					if(item != null){
						
						String id = item.getString("id");
						String name = item.getString("name");
						String icon = item.getString("icon");
						String package_name = item.getString("package_name");
						String  price = item.getString("price");
						String countries = item.getString("country_code");
						String preview_url = item.getString("preview_link");
						String min_os_version = item.getString("min_os_version");
						String platform = item.getString("platform");
						String category = item.getString("category");
						String payout_type = item.getString("payout_type");
					    String click_url=item.getString("track_link");
					    //String description = item.getString("description");
					    
						
						/**
						 * 排除处理
						 */
						
						// 如果id、pkg、price、country、Platforms、clickURL、remaining_cap为空，舍弃不入库
						if(OverseaStringUtil.isBlank(id)
								|| OverseaStringUtil.isBlank(price)
								|| OverseaStringUtil.isBlank(countries)
								|| OverseaStringUtil.isBlank(platform)
								|| OverseaStringUtil.isBlank(click_url)){
							
							continue;
						}
						
						// CPI类型，单价太低(0.06)，舍弃不入库
						if(Float.valueOf(price) < 0.06){
							
							continue;
						}
						
						/**
						 * 特殊字段处理
						 */
						//处理国家
						countries = countries.replace(",", ":");
						
						//处理平台(0:andriod:1:ios:2:pc)
						String os_str = "";
						
						String[] platformsArr = platform.split(",");
						
						Map<String, String> platformsMap = new HashMap<String, String>();
						
						if(platformsArr != null
								&& platformsArr.length > 0){
							
							for(String platformItem : platformsArr){
								
								if("ANDROID".equals(platformItem.toUpperCase())){
									
									platformsMap.put("0", "0");
								}
								else if("IOS".equals(platformItem.toUpperCase())){
									
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
						
						/**
						 * 封装网盟广告对象
						 */
						
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(name);
						
						
						//都设置为CPI类型广告	
						advertisersOffer.setCost_type(101);
						advertisersOffer.setOffer_type(101);
						advertisersOffer.setConversion_flow(101);
						
						advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
						advertisersOffer.setName(name);//网盟offer名称
						advertisersOffer.setPkg(package_name);
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(preview_url);//预览链接
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setCountry(countries);
						advertisersOffer.setDaily_cap(10000);
						advertisersOffer.setPayout(Float.valueOf(price));
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);//设置为mobile类型
						advertisersOffer.setOs_version(min_os_version);//系统版本要求
						advertisersOffer.setDescription(category);
						advertisersOffer.setSend_type(0);//系统入库生成广告
						//advertisersOffer.setIncent_type(incetive);// 是否激励
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
