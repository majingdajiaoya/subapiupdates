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

public class PullIronsourceOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullIronsourceOfferService.class);
	
	private Advertisers advertisers;
	
	public PullIronsourceOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Ironsource广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Ironsource Offer begin := " + new Date());
			
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
				
				JSONObject jsonObject = JSON.parseObject(str);
				
				Boolean error = jsonObject.getBoolean("error");// "error":false
				
				if(error){
					
					logger.info("error is " + error + " reutrn");
					
					return;
				}
				
				JSONArray offersArray = jsonObject.getJSONArray("ads");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							String offerid = item.getString("campaign_id");
							String platform = item.getString("platform");
							String pkg = item.getString("packageName");
							JSONArray country = item.getJSONArray("geoTargeting");
							String name = item.getString("title");
							String price = item.getString("bid").trim();
							String clickURL = item.getString("clickURL");
							String category = item.getString("category");
							String campaignType = item.getString("campaignType");//campaignType "CPI"
							String minOSVersion = item.getString("minOSVersion");//minOSVersion	"4"
							
							
							/**
							 * 排除处理
							 */
							// 如果id、price、country、clickURL、platform为空，舍弃不入库
							if(OverseaStringUtil.isBlank(offerid)
									|| OverseaStringUtil.isBlank(price)
									|| country == null
									|| country.size() == 0
									|| OverseaStringUtil.isBlank(clickURL)
									|| OverseaStringUtil.isBlank(platform)){
								
								continue;
							}
							
							Float payout = Float.parseFloat(price);
							
							if(payout < 0.06){
								
								continue;
							}
							
							//只入库CPI类型单子
							if(OverseaStringUtil.isBlank(campaignType)
									|| !"CPI".equals(campaignType.toUpperCase())){
								
								continue;
							}
							
							/**
							 * 特殊字段处理
							 */
							
							//平台
							String os_str = "";
							
							if("android".equals(platform.toLowerCase())){
								
								os_str += "0:";
							}
							if("ios".equals(platform.toLowerCase())){
								
								os_str += "1:";
							}
							
							//去除最后一个:号
							if(!OverseaStringUtil.isBlank(os_str)){
								
								os_str = os_str.substring(0, os_str.length() - 1);
							}
							
							//国家
							StringBuffer countries = new StringBuffer();
							
							for (Object obj_country : country) {
								
								countries.append(obj_country).append(":");
							}
							
							String country_str = countries.deleteCharAt(countries.length() - 1).toString();
								
							
							/**
							 * 封装网盟广告对象
							 */
								
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(offerid);
							advertisersOffer.setName(name);
							
							//CPI类型	
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(pkg);
							//advertisersOffer.setpkg_size();
							//advertisersOffer.setMain_icon(icon_url);
							//advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(clickURL);
							advertisersOffer.setCountry(country_str);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(10000);//不限制
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(category);
							advertisersOffer.setOs(os_str);
							advertisersOffer.setDevice_type(0);//mobile
							advertisersOffer.setOs_version(minOSVersion);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(0);// 0 : 否，是否激励
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
