package com.network.task.service;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullAltamobOfferService implements PullOfferServiceTask {
	
	protected static Logger logger = Logger.getLogger(PullAltamobOfferService.class);
	  
	private Advertisers advertisers;
	  
	public PullAltamobOfferService(Advertisers advertisers){
		  
		  this.advertisers = advertisers;
	}
	  
	public void run(){
		  
		synchronized (this){
	    	
		logger.info("doPull altamob Offer begin := " + new Date());
	  
		try{

		    Long advertisersId = this.advertisers.getId();
		    
		    String apiurl = this.advertisers.getApiurl();
		    String apikey = this.advertisers.getApikey();
		    
		    if (advertisersId == null
		    		|| OverseaStringUtil.isBlank(apiurl)
		    		|| OverseaStringUtil.isBlank(apikey)){
		    	
		    	logger.info("advertisersId or apiurl or apikey is null return");
		  
		    	return;
		    }
		
			Map<String, String> header = new HashMap<String ,String>();
			
			header.put("token", apikey);
			
			String str = HttpUtil.sendGet(apiurl, header);
			
			JSONArray offersArr = JSONArray.parseArray(str);
			
			if(offersArr == null
					|| offersArr.size() == 0){
				
				logger.info("offersArray is empty");
				
				return;
			}
				
			logger.info("begin pull offer " + offersArr.size());
			
			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
			  
			for (int i = 0; i < offersArr.size(); i++){
				  
			    JSONObject item = offersArr.getJSONObject(i);
			    
			    if (item != null){
			    	
			      Integer offer_id = item.getInteger("offer_id");
			      String title = item.getString("title");
			      String icon_url = item.getString("icon_url");
			      String pkg_name = item.getString("pkg_name");
			      Float maxpayout = item.getFloat("maxpayout");
			      String click_url = item.getString("click_url");
			      String app_store_url = item.getString("app_store_url");
			      String countries = item.getString("geo");
			      String platform = item.getString("platform");
			      String category = item.getString("category");
			      String min_os_version = item.getString("min_os_version");
			      String description="";
			      description = item.getString("description");
			      if ("".equals(description)) {
			    	  description = title;
			      }
			      
			      
			      click_url=click_url.replace("{transaction_id}", "{aff_sub}");
			      click_url=click_url.replace("{p}", "");
			      click_url=click_url.replace("{aid}", "{andriod_id}");
			      click_url=click_url.replace("{geo}", "{country}");
			      click_url=click_url.replace("{client_version}", "");
			      
			      if (offer_id == null
			    		  	|| OverseaStringUtil.isBlank(pkg_name)
			    		  	|| maxpayout == null
			    		  	|| OverseaStringUtil.isBlank(countries) 
			    		  	|| OverseaStringUtil.isBlank(platform)
			    		  	|| OverseaStringUtil.isBlank(click_url)) {
			    	  
			    	  logger.info("offer_id := " + offer_id
			    			  + " or pkg_name := " + pkg_name
			    			  + " or maxpayout := " + maxpayout
			    			  + " or countries := " + countries
			    			  + " or platform := " + platform
			    			  + " or click_url := " + click_url
			    			  + " is empty, return");
			    	  
			    	  continue;
			      }
			      
			      if (maxpayout <= 0.06){
			    	  
			    	  logger.info("payout is lower := " + maxpayout + " return");
			    	  
			    	  continue;
			      }
			      
			      /**
			       * 特殊字段处理
			       */
			       
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
					
					// 非激励量
					Integer incetive = Integer.valueOf(0);
					  
					AdvertisersOffer advertisersOffer = new AdvertisersOffer();
					  
					advertisersOffer.setAdv_offer_id(String.valueOf(offer_id + "_" + countries));//以offer_id+geo组合做主键
					advertisersOffer.setName(title);
					  
					advertisersOffer.setCost_type(Integer.valueOf(101));
					advertisersOffer.setOffer_type(Integer.valueOf(101));
					advertisersOffer.setConversion_flow(Integer.valueOf(101));
					
					advertisersOffer.setAdvertisers_id(advertisersId);
					
					advertisersOffer.setPkg(pkg_name);
					advertisersOffer.setMain_icon(icon_url);
					advertisersOffer.setPreview_url(app_store_url);
					advertisersOffer.setClick_url(click_url);
					  
					advertisersOffer.setCountry(countries);
					  
					advertisersOffer.setDaily_cap(Integer.valueOf(90000));
					  
					advertisersOffer.setPayout(maxpayout);
					  
					advertisersOffer.setDescription(filterEmoji(description));
					advertisersOffer.setOs(os_str);
					  
					advertisersOffer.setDevice_type(Integer.valueOf(0));
					advertisersOffer.setOs_version(min_os_version);
					advertisersOffer.setSend_type(Integer.valueOf(0));
					advertisersOffer.setIncent_type(incetive);
					advertisersOffer.setSmartlink_type(Integer.valueOf(1));
					advertisersOffer.setStatus(Integer.valueOf(0));
					  
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
		catch (Exception e) {
					
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
	 
		public static void main(String[] args)  {
			Advertisers tmp=new Advertisers();
			//{apikey}
			//https://api.taptica.com/v2/bulk?token=7Bhisedf1PmH8bs7goa2jw%3d%3d&platforms=iPhone&version=2&format=json
			tmp.setApiurl("http://api.altamob.com/adfetch/v1/s2s/campaign/fetch?platform=android");
			tmp.setApikey("40f29a89-525d-fe1b-92cd-1997c5955141");
			tmp.setId(19L);
			
			PullAltamobOfferService mmm=new PullAltamobOfferService(tmp);
			mmm.run();
		}
    
}
