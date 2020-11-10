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

public class PullJiubangOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullJiubangOfferService.class);
	
	private Advertisers advertisers;
	
	public PullJiubangOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Jiubang广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Jiubang Offer begin := " + new Date());
			
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
				
				String advosId = jsonPullObject.getString("advposId");// 2449
				Integer size = jsonPullObject.getInteger("size");// "204"
				
				if(size == null
						|| size == 0){
					
					logger.info("size is empty, return");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
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
							
							Integer campaign_id = item.getInteger("campaign_id");//"campaign_id": 80644139,
							//String packet_name = item.getString("pkgname");//"pkgname": "com.bitmango.rolltheballunrollme",
							/*String currency = item.getString("payout_currency");//"payout_currency": "USD",*/						
							Float  price = item.getFloat("payout_amount");//"payout_amount": 0.33,
							String icon = item.getString("icon");//"icon": "https://lh3.googleusercontent.com/arhybrDSDtJf1qdUQdcnLhNXnJjL7nBinyyfrE_ir34JJeep49yf-fP3vxzYnu-PMenI=w300",
							String payout_type = item.getString("payout_type");//"payout_type": "CPA"
							String click_url = item.getString("click_url");//"click_url": "http://advclick.wecloudbak.com/advclick?advposid=2449&mapid=80644139&aid={aid}&adid={adid}",
							String mobile_platform = item.getString("mobile_platform");//"mobile_platform": "Android",
							String title = item.getString("title");// "title": "Roll the Ball® - slide puzzle",
							String pkg = item.getString("mobile_app_id");//"mobile_app_id": "com.bitmango.rolltheballunrollme",
				            String preview_url= item.getString("banner");
				            JSONArray countryArray=item.getJSONArray("countries");
				            String mobile_min_version = item.getString("mobile_min_version");
				            
				            /**
				             * 排除处理
				             */
				            
				            //如果id、price、country、clickURL、mobile_platform、pkg为空，舍弃不入库
							if(campaign_id == null
									|| price == null
									|| countryArray == null
									|| countryArray.size() == 0
									|| OverseaStringUtil.isBlank(click_url)
									|| OverseaStringUtil.isBlank(mobile_platform)
									|| OverseaStringUtil.isBlank(pkg)){
								
								continue;
							}
							
							// 单价太低(0.06)，舍弃不入库
							if(price < 0.06){
								
								continue;
							}
							
							/**
							 * 特殊处理字段
							 */
							String os_str = "";
							
							if("ANDROID".equals(mobile_platform.toUpperCase())){
								
								os_str = "0";
							}
							else if("IOS".equals(mobile_platform.toUpperCase())){
								
								os_str = "1";
							}
							
							// 国家
							String countries = "";
							
							for(int j= 0; j < countryArray.size(); j++){
								
								countries += countryArray.getString(j) + ":";
							}
							
							countries = countries.substring(0, countries.length() - 1);
							
							
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
							advertisersOffer.setName(title);
							
							//CPI类型
							if("CPA".equals(payout_type)){
								
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
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(click_url);
							advertisersOffer.setCountry(countries);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(10000);//无cap限制，默认10000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(price);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							//advertisersOffer.setSupported_carriers("");
							//advertisersOffer.setDescription(category);
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(mobile_min_version);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							//advertisersOffer.setIncent_type(incentivized);// 是否激励
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
