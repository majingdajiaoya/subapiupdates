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

public class PullTolerOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullTolerOfferService.class);
	
	private Advertisers advertisers;
	
	public PullTolerOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Toler广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Toler Offer begin := " + new Date());
			
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
				
				String status = jsonPullObject.getString("msg");//"msg": "ok"
				
  				JSONObject jsonDataObject = jsonPullObject.getJSONObject("data");
  				
  				Integer total = jsonDataObject.getInteger("recordsTotal");//总记录数
  				
  				if(!"ok".equals(status)){
  					
  					logger.info("msg is not ok return. msg := " + status);
  					
  					return;
  				}
  				
  				if(total == null
  						|| total == 0){
  					
  					logger.info("total is empty." +"count:=" + total);
  					
  					return;
  				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonDataObject.getJSONArray("data");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer offer_id = item.getInteger("offer_id");// offerId
  							String name = item.getString("offer_name");//offer名称
  							String offer_type = item.getString("offer_type");//	//设备类型"offer_type": "Mobile",
  							String preViewUrl = item.getString("preview_url");//预览链接
  							String click_url = item.getString("offer_url"); //"offer_url": "https://click.link2ads.com/click/index?a=57&o=346691",
  							String offer_model = item.getString("payout_type");// "payout_type": "CPA",
  							Float price = item.getFloat("payout");//价格
  				
  							JSONArray osArray = item.getJSONArray("os");
				            
				            /**
				             * 排除处理
				             */
				            
				            //如果id、price、country、clickURL、mobile_platform、pkg为空，舍弃不入库
							if(offer_id == null
									|| price == null
									|| osArray == null
									|| osArray.size() == 0
									|| OverseaStringUtil.isBlank(click_url)){
								
								continue;
							}
							
							// 单价太低(0.06)，舍弃不入库
							if(price < 0.06){
								
								continue;
							}
							
							Integer device_type = null;
  							String pkg_name = null;
							
							if("Mobile".equals(offer_type)){
  								
  								device_type = 0;// 手机mobile
  							}
  							else{
  								
  								continue;
  							}
							
							/**
							 * 特殊处理字段
							 */
							
							//系统平台
  							String os_str = "";
  							
  							if(osArray != null
  									&& osArray.size() > 0){
  								
  								for(int k = 0; k < osArray.size(); k++){
  									
  									String osItem = osArray.getString(k);
  									
  									if(osItem != null){
  										
  										if("android".equals(osItem.toLowerCase())){
  											
  											os_str += "0:";
  											
  											//在只有id一个参数的时候且id为最后一个参数的时候生效
  											preViewUrl = preViewUrl.replace("\\\\%3D", "=");
  											
  											
  											if(preViewUrl.contains("\\%26hl")){
  												
  												preViewUrl = preViewUrl.substring(0, preViewUrl.indexOf("\\%26hl") - 1);//.replace("\\%26hl", "&");
  											}
  											
  											String newUrl[] = preViewUrl.split("id=");
  											
  			  	  					    	pkg_name = newUrl[newUrl.length-1];
  										}
  										if("ios".equals(osItem.toLowerCase())){
  											
  											os_str += "1:";
  											
  											//在有参数的时候
  											if(preViewUrl.contains("?")){
  												
  												pkg_name = (String) preViewUrl.subSequence(preViewUrl.indexOf("id"), preViewUrl.indexOf("?"));
  											}
  											//在没有参数的时候
  											else{
  												
  												String newUrl[]=preViewUrl.split("id");
  												
  	  			  	  					    	pkg_name="id"+newUrl[newUrl.length-1];
  											}
  										}
  									}
  								}
  							}
  							//去除最后一个:号
  							if(!OverseaStringUtil.isBlank(os_str)){
  								
  								os_str = os_str.substring(0, os_str.length() - 1);
  							}
  							
  							String countries_str = item.getJSONObject("country").getString("value");
  							
  							if(OverseaStringUtil.isBlank(countries_str)){
  								
  								continue;
  							}
  							
  							String countries ="";
  							String countryArray[]=countries_str.split(",");
  							
  							for(String country:countryArray){
  								
  								countries=countries+country.toUpperCase() + ":";
  							}
  									
  							//去除最后一个:号
  							if(!OverseaStringUtil.isBlank(countries)){
  								
  								countries = countries.substring(0, countries.length() - 1);
  							}
							
							
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							offer_model = offer_model.toUpperCase();
  					
  							if("CPA".equals(offer_model)){
  								
  								advertisersOffer.setCost_type(101);
  								advertisersOffer.setOffer_type(101);
  								advertisersOffer.setConversion_flow(101);
  							}
  							else{
  								
  								advertisersOffer.setCost_type(104);
  								advertisersOffer.setOffer_type(104);//设置为其它类型
  								advertisersOffer.setConversion_flow(104);
  							}
  							
  							advertisersOffer.setPayout(price);//设置价格
  							advertisersOffer.setAdv_offer_id(String.valueOf(offer_id));//设置offerId
  							advertisersOffer.setName(name);//设置名称
  							advertisersOffer.setPkg(pkg_name);
  							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
  							advertisersOffer.setPreview_url(preViewUrl);
  							advertisersOffer.setClick_url(click_url);
  							advertisersOffer.setDevice_type(device_type);//设备类型			
  							advertisersOffer.setDaily_cap(10000);
  							advertisersOffer.setOs(os_str);//设置操作系统类型
  							advertisersOffer.setCountry(countries);//设置国家
  							advertisersOffer.setDevice_type(device_type);//设置平台类型
  							advertisersOffer.setSend_type(0);//下发类型
  							advertisersOffer.setIncent_type(0);// 是否激励
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
	
}
