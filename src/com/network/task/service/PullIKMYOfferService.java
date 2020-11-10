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

public class PullIKMYOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullIKMYOfferService.class);
	
	private Advertisers advertisers;
	
	public PullIKMYOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取IKMY广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull IKMY Offer begin := " + new Date());
			
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
				
				String qk = jsonPullObject.getString("qk");
				
				if(OverseaStringUtil.isBlank(qk)){
					
					logger.info("qk is null, return");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("ads");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer ad_id = item.getInteger("ad_id");// 7933014
							Float price = item.getFloat("price");// 2.55
							String c2code = item.getString("c2code");// "JP"
							String pkg = item.getString("pkg");
							Integer os = item.getInteger("os");// 0:ios  1:android
							String min_os = item.getString("min_os");// "4.1"
							Integer adtype = item.getInteger("adtype");// 1:cpi  2:cps订阅
							String title = item.getString("title");
							String click_url = item.getString("click_url");
							String icon = item.getString("icon");
							
							/**
							 * 特殊处理字段
							 */
							String mobile_platform = "";
							
							if(os != null){
								if(os == 0){
									
									mobile_platform = "1";
								}
								if(os == 1){
									
									mobile_platform = "0";
								}
							}
							
							// 国家
							c2code = c2code.replace(",", ":");

							
							// 如果id、price、country、clickURL、mobile_platform、pkg为空，舍弃不入库
							if(ad_id == null
									|| price == null
									|| OverseaStringUtil.isBlank(c2code)
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
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(ad_id));
							advertisersOffer.setName(title);
							
							//CPI类型
							if(adtype == 1){
								
								advertisersOffer.setCost_type(101);	
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							}
							else if(adtype == 2){
								
								advertisersOffer.setCost_type(102);	
								advertisersOffer.setOffer_type(102);
								advertisersOffer.setConversion_flow(102);
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
							//advertisersOffer.setPreview_url(preload_click_url);
							advertisersOffer.setClick_url(click_url);
							advertisersOffer.setCountry(c2code);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(10000);//无cap限制，默认10000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(price);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							//advertisersOffer.setSupported_carriers("");
							//advertisersOffer.setDescription(category);
							advertisersOffer.setOs(mobile_platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(min_os);//系统版本要求
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
