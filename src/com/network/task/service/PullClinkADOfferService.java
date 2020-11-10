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

public class PullClinkADOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullClinkADOfferService.class);
	
	private Advertisers advertisers;
	
	public PullClinkADOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取ClinkAD广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull ClinkAD Offer begin := " + new Date());
			
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
				
				String result = jsonPullObject.getString("result");
				
				if(!"success".equals(result)){
					
					logger.info("result is not success return. result := " + result);
					
					return;
				}
				
				JSONArray offerArray = jsonPullObject.getJSONArray("offers");
				
				if(offerArray == null
						|| offerArray.size()==0){
					
					logger.info("offersArray is empty");
					
					return;
				}
					
				logger.info("begin pull offer " + offerArray.size());
				
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				//遍利jsonArray对象
				for (int i = 0; i < offerArray.size(); i++){
					  
				    JSONObject item = offerArray.getJSONObject(i);
				    
				    if (item != null){
				    	
				      Integer offer_id = item.getInteger("oid");
				      String title = item.getString("name");
				      String icon_url = item.getString("appIconUrl");//icon图标
				      String pkg_name = item.getString("PackageName");
				      Float payout = item.getFloat("payout");
				      String click_url = item.getString("tracking_link");//广告点击url
				      String preview_url=item.getString("PreviewUrl");//previewUrl
				      String category = item.getString("category");
				      String countries = item.getString("countries");
				      String platform = item.getString("platform");
				      String min_os_version = item.getString("minOSVersion");
				      
				      // 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
				      if (offer_id == null
				    		  	|| OverseaStringUtil.isBlank(pkg_name)
				    		  	|| payout == null
				    		  	|| OverseaStringUtil.isBlank(countries) 
				    		  	|| OverseaStringUtil.isBlank(platform)
				    		  	|| OverseaStringUtil.isBlank(click_url)) {
				    	  
				    	  logger.info("offer_id := " + offer_id
				    			  + " or pkg_name := " + pkg_name
				    			  + " or maxpayout := " + payout
				    			  + " or countries := " + countries
				    			  + " or platform := " + platform
				    			  + " or click_url := " + click_url
				    			  + " is empty, continue");
				    	  
				    	  continue;
				      }
				      
				      if (payout <= 0.06){
				    	  
				    	  logger.info("payout is lower := " + payout + " return");
				    	  
				    	  continue;
				      }
				      //获取包名
				      
				      /**
				       * 特殊字段处理
				       */
				       
				      countries = countries.replace("|", ":");
				          
				      //处理平台(0:andriod:1:ios:2:pc)
				      String os_str = "";
								
		    		  if("ANDROID".equals(platform.toUpperCase())){
							
		    			  os_str = "0";
		    		  }
		    		  else if("IOS".equals(platform.toUpperCase())){
							
		    			  os_str = "1";
		    		  }
				    	  
			
		    		  // 非激励量
		    		  //Integer incetive = Integer.valueOf(0);
						  
		    		  AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						
		    		  advertisersOffer.setAdvertisers_id(advertisersId);
						
		    		  advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));
		    		  advertisersOffer.setName(title);
						
		    		  advertisersOffer.setCost_type(Integer.valueOf(101));
		    		  advertisersOffer.setOffer_type(Integer.valueOf(101));
		    		  advertisersOffer.setConversion_flow(Integer.valueOf(101));
						
		    		  advertisersOffer.setPkg(pkg_name);
		    		  advertisersOffer.setMain_icon(icon_url);
		    		  advertisersOffer.setPreview_url(preview_url);
		    		  advertisersOffer.setClick_url(click_url);
		    		  advertisersOffer.setCountry(countries);
		    		  advertisersOffer.setDaily_cap(Integer.valueOf(10000));/*每日cap限制*/
		    		  advertisersOffer.setPayout(payout);
		    		  advertisersOffer.setDescription(category);
		    		  advertisersOffer.setOs(os_str);
		    		  advertisersOffer.setDevice_type(Integer.valueOf(0));
		    		  advertisersOffer.setOs_version(min_os_version);
		    		  advertisersOffer.setSend_type(Integer.valueOf(0));//下发类型(0:自动下发，1:手动下发)
		    		  advertisersOffer.setIncent_type(0);//不是激励类型
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
	
}
