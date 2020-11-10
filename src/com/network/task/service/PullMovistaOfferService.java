package com.network.task.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.bean.AdvertisersOffer;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferCommonService;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaMd5;
import com.network.task.util.OverseaStringUtil;
import com.network.task.util.TuringSpringHelper;
import common.Logger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullMovistaOfferService implements PullOfferServiceTask {
	
	protected static Logger logger = Logger.getLogger(PullMovistaOfferService.class);
	  
	private Advertisers advertisers;
	  
	public PullMovistaOfferService(Advertisers advertisers){
		  
		  this.advertisers = advertisers;
	}
	  
	public void run(){
		  
		synchronized (this){
	    	
		logger.info("doPull Movista  Offer begin := " + new Date());
		List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();

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
		    String offset= doPagePullOffer(advertisersOfferList,advertisersId,apiurl,apikey,null);
		    if (offset!=null) {
		    	for (int i=0;i<20;i++) {
		    		offset= doPagePullOffer(advertisersOfferList,advertisersId,apiurl,apikey,offset);
		    		if (offset ==null) {
		    			break;
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
		catch (Exception e) {
					
		   e.printStackTrace();
					
		   NetworkLog.exceptionLog(e);
		}
	  }
	}
	
	public static String doPagePullOffer(List<AdvertisersOffer> advertisersOfferList, Long advertisersId, String apiurl, String apikey, String offset){
		String offsetyuanshi = null;
		try{
		    
		    if (advertisersId == null
		    		|| OverseaStringUtil.isBlank(apiurl)
		    		|| OverseaStringUtil.isBlank(apikey)){
		    	logger.info("advertisersId or apiurl or apikey is null return");
		    	return null;
		    }
		   apiurl = apiurl.replace("{apikey}", apikey);
		    
		   long time=System.currentTimeMillis()/1000;
		   String token=OverseaMd5.md5("LOWUPD66YHVIP46YVGVP"+OverseaMd5.md5(String.valueOf(time)));
		   
		   
		   apiurl=apiurl+"&time="+time+"&token="+token;
		   if (offset!=null) {
			   apiurl=apiurl+"&time="+time+"&token="+token+"&offset="+offset;
		   }
			
			String str = HttpUtil.sendGet(apiurl);
			
			JSONObject jsonPullObject = JSON.parseObject(str);
			int balance_offers_num = jsonPullObject.getIntValue("balance_offers_num");
			if (balance_offers_num<=0) {
				return null;
			} else {
				offsetyuanshi= jsonPullObject.getString("offset");
			}
			
			JSONArray offersArr = jsonPullObject.getJSONArray("offers");
			
			if(offersArr == null
					|| offersArr.size() == 0){
				
				logger.info("offersArray is empty");
				
				return null;
			}
				
			logger.info("begin pull offer " + offersArr.size());
			  
			for (int i = 0; i < offersArr.size(); i++){
				  
			    JSONObject item = offersArr.getJSONObject(i);
			    
			    if (item != null){
			    	
			      Integer offer_id = item.getInteger("campid");
			      String title = item.getString("offer_name");
			      String icon_url = item.getString("icon_link");
			      String pkg_name = item.getString("package_name");
			      Float maxpayout = item.getFloat("price");
			      String click_url = item.getString("tracking_link");
			      String app_store_url = item.getString("preview_link");
			      //String countries = item.getString("geo");
			      String Incentbool = item.getString("traffic_source");
			      
			      String traffic_network =item.getString("traffic_network");
			      Integer isIncent =0;
			      if ("non-incent".equals(Incentbool)) {
			    	  isIncent = 0;
			      } else {
			    	  isIncent = 1;
			      }
			      
			      String cover_url = item.getString("cover_url");
			      JSONArray countryObject =item.getJSONArray("geo");
			      String platforms= item.getString("platform");
			      
			      String min_os_version = item.getString("min_version");
			      String description="";
			      description = item.getString("app_desc");
			      if ("".equals(description)) {
			    	  description = title;
			      }
			      
			      String countries="";
			      if (countryObject!=null &&countryObject.size()>0){
			    	  for (int k=0;k<countryObject.size();k++) {
			    		  //JSONObject countryitem =  countryObject.getJSONObject(k);
			    		  String countryitem=countryObject.getString(k);
			    		  
			    		  if (countryitem!=null) {
			    			  if ("".equals(countries)) {
			    				  countries = countryitem;
			    			  } else {
			    				  countries =countries+":"+countryitem;
			    			  }
			    		  }
			    		  
			    	  }
			    	  
			      }
			      //http://pixel.admobclick.com/v1/ad/click?subsite_id=30829&
			      //transaction_id={transaction_id}&id=1271&offer_id=142611501&geo={geo}&aid={aid}&client_version={client_version}&gaid={gaid}&tmark=1522295239141&p={p}&d=ZLABKDAFB
//			      
//			      click_url=click_url.replace("{transaction_id}", "{aff_sub}");
//			      click_url=click_url.replace("{p}", "");
//			      click_url=click_url.replace("{aid}", "{andriod_id}");
//			      click_url=click_url.replace("{geo}", "{country}");
//			      click_url=click_url.replace("{client_version}", "");
			      
			      if (offer_id == null
			    		  	|| OverseaStringUtil.isBlank(pkg_name)
			    		  	|| maxpayout == null
			    		  	|| OverseaStringUtil.isBlank(countries) 
			    		  	|| OverseaStringUtil.isBlank(click_url)) {
			    	  
			    	  logger.info("offer_id := " + offer_id
			    			  + " or pkg_name := " + pkg_name
			    			  + " or maxpayout := " + maxpayout
			    			  + " or countries := " + countries
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
			       
			      //countries = countries.replace(",", ":");
			          
			      //处理平台(0:andriod:1:ios:2:pc)
			      String os_str = "";
	    		  if("ANDROID".equals(platforms.toUpperCase())){
	    			  os_str="0";
	    			  click_url=click_url+"&idfa={gaid}";
	    		  }
	    		  else if("IOS".equals(platforms.toUpperCase())){
	    			  click_url=click_url+"&idfa={idfa}";
	    			  os_str="1";
	    		  }

					
					// 非激励量
					//Integer incetive = Integer.valueOf(0);
					String images_crative=null;
	    		  JSONArray creatives =item.getJSONArray("creatives");
	    		  if (creatives!=null &&creatives.size() > 0) {
						JSONArray images_JSONArray=new JSONArray();

						ArrayList<Map> arrayList = new ArrayList<Map>();
						
	    			  for (int k=0;k<creatives.size();k++) {
	    				  String imageurl=creatives.getString(k);
	    				  if (arrayList.size()>3) {
	    					  break;
	    				  }
				            Map jsonObject = new HashMap();
				            jsonObject.put("url", imageurl);
				            jsonObject.put("size", "*");
				            arrayList.add(jsonObject);
	    			  }
						 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
						 images_crative = images_JSONArray.toString();
	    			  
	    		  }
	    		  String daily_cap = item.getString("daily_cap");
	    		  int caps=99999;
	    		  if ("open cap".equals(daily_cap)) {
	    			  
	    		  } else {
	    			  caps = Integer.valueOf(daily_cap);
	    			  if (caps == 0) {
	    				  continue;
	    			  }
	    			  
	    		  }
	    		  
	    		  
					String price_model= item.getString("price_model");
					  
					AdvertisersOffer advertisersOffer = new AdvertisersOffer();
					  
					advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));//以offer_id+geo组合做主键
					advertisersOffer.setName(title);
					  
					if ("CPI".equals(price_model.toUpperCase())) {
						advertisersOffer.setCost_type(Integer.valueOf(101));
						advertisersOffer.setOffer_type(Integer.valueOf(101));
						advertisersOffer.setConversion_flow(Integer.valueOf(101));
					} else if ("CPA".equals(price_model.toUpperCase())) {
						advertisersOffer.setCost_type(Integer.valueOf(102));
						advertisersOffer.setOffer_type(Integer.valueOf(102));
						advertisersOffer.setConversion_flow(Integer.valueOf(102));
					} else {
						advertisersOffer.setCost_type(Integer.valueOf(104));
						advertisersOffer.setOffer_type(Integer.valueOf(104));
						advertisersOffer.setConversion_flow(Integer.valueOf(104));
					}

					
					advertisersOffer.setAdvertisers_id(advertisersId);
					
					advertisersOffer.setPkg(pkg_name);
					advertisersOffer.setMain_icon(icon_url);
					advertisersOffer.setPreview_url(app_store_url);
					advertisersOffer.setClick_url(click_url);
					  
					advertisersOffer.setCountry(countries);
					  
					advertisersOffer.setDaily_cap(caps);
					  
					advertisersOffer.setPayout(maxpayout);
					  
					advertisersOffer.setDescription(filterEmoji(description));
					advertisersOffer.setOs(os_str);
					advertisersOffer.setSupported_carriers(traffic_network);
					
					advertisersOffer.setCreatives(images_crative);
					advertisersOffer.setDevice_type(Integer.valueOf(0));
					advertisersOffer.setOs_version(min_os_version);
					advertisersOffer.setSend_type(Integer.valueOf(0));
					advertisersOffer.setIncent_type(isIncent);
					advertisersOffer.setSmartlink_type(Integer.valueOf(1));
					advertisersOffer.setStatus(Integer.valueOf(0));
					  
					advertisersOfferList.add(advertisersOffer);
			    }
			}
	
//			logger.info("after filter pull offer size := " + advertisersOfferList.size());
//		
//			// 入网盟广告
//			if(advertisersId != null
//				&& advertisersOfferList != null
//				&& advertisersOfferList.size() > 0){
//			
//					PullOfferCommonService pullOfferCommonService 
//						= (PullOfferCommonService)TuringSpringHelper.getBean("pullOfferCommonService");
//					
//					pullOfferCommonService.doPullOffer(advertisers, advertisersOfferList);
//			}
		}
		catch (Exception e) {
					
		   e.printStackTrace();
					
		   NetworkLog.exceptionLog(e);
		}
		
		return offsetyuanshi;
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
			//http://3s.mobvista.com/v4.php?m=index&cb=cb13103&time=1523342453&token=cbe47d2472488ef0bf3f456dbae9eccc
			//{apikey}
			//3s.mobvista.com/v4.php?m=index&cb=cb13103
			tmp.setApiurl("http://3s.mobvista.com/v4.php?m=index&cb={apikey}");
			tmp.setApikey("cb13103");
			tmp.setId(19L);
			
			PullMovistaOfferService mmm=new PullMovistaOfferService(tmp);
			mmm.run();
		}
    
}
