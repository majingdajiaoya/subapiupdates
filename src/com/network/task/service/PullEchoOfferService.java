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

public class PullEchoOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullEchoOfferService.class);
	
	private Advertisers advertisers;
	
	public PullEchoOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取ECHO广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull ECHO Offer begin := " + new Date());
			
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
				
				String appyurl = "http://leverage.echo226.com/2015-03-01/bulk/75717141/apply?auth=e80deb84260142c2a17b72d9fb43e789";
						
				String str = HttpUtil.sendGet(appyurl);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				JSONArray offersArray = jsonPullObject.getJSONArray("rows");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offer rows empty, return");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							
//							termsAndConditions	"no fraud\nno adult\nno incent"
//							payout	0.7
//							deviceTypes	"phone,tablet"
//							requireApproval	true
//							description	"Souq.com app allows you …s a free return policy."
//							kpiClassification	"soft"
//							payoutType	"CPI"
//							countries	"sa"
//							applicationStatus	"Not Applied"
//							applicationLink	"http://leverage.echo226.…75717141/75013515/apply"
//							creatives	[2]
//							currency	"USD"
//							featured	0
//							appDetails	Object
//							previewUrl	"https://itunes.apple.com…wq.kwm/id675000850?mt=8"
//							osVersions	"ios_8,ios_9,ios_other,ios_10,ios_11"
//							os	"ios"
//							id	75013515
//							categories	[1]
//							name	"souq - ios | SA"

							
							Integer id = item.getInteger("id");// 75013515
							String name = item.getString("name");//	"CPI"
							Float payout = item.getFloat("payout");// 0.31
							String payoutType = item.getString("payoutType");//	"CPI"
							String countries = item.getString("countries");// "sa,in,us"
							String os = item.getString("os");
							String osVersions = item.getString("osVersions");
							String previewUrl = item.getString("previewUrl");
							
							JSONObject appDetails = item.getJSONObject("appDetails");
							
							String appId = appDetails.getString("appId");
							String appIcon = appDetails.getString("appIcon");
							
							/**
							 * 特殊处理
							 */
							countries = countries.replace(",", ":");
							countries = countries.toUpperCase();
							
							String caps_daily_remaining_str = item.getString("caps_daily_remaining");// 9992
							Integer caps_daily_remaining = null;
							if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
								if("unlimited".equals(caps_daily_remaining_str)){
									
									caps_daily_remaining = 10000;
								}
								else{
									
									caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
								}
							}
							
							JSONObject targeting = 	item.getJSONObject("targeting");
							JSONObject allowed = targeting.getJSONObject("allowed");
							
							
							String os_str = "";
							
							if("android".equals(os.toLowerCase())){
								
								os_str = "0";
							}
							if("ios".equals(os.toLowerCase())){
								
								os_str = "1";
							}
								
							//去除最后一个:号
							if(!OverseaStringUtil.isBlank(os_str)){
								
								os_str = os_str.substring(0, os_str.length() - 1);
							}
							
							JSONArray countriesArray = allowed.getJSONArray("countries");
							
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
							
							// 如果id、price、country、clickURL、payoutType、remaining_cap为空，舍弃不入库
							if(id == null
									|| payout == null
									|| countriesArray == null
									|| countriesArray.size() == 0
									//|| OverseaStringUtil.isBlank(link)
									|| OverseaStringUtil.isBlank(payoutType)
									|| caps_daily_remaining == null){
								
								continue;
							}
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if(payout < 0.06){
								
								continue;
							}
							// CPI类型，pkg为空，舍弃不入库
							if(OverseaStringUtil.isBlank(appId)){
								
								continue;
							}
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(id));
							advertisersOffer.setName(name);
							
							if("CPI".equals(payoutType.toUpperCase())){
								
								advertisersOffer.setCost_type(101);
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							}
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(appId);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(appIcon);
							advertisersOffer.setPreview_url(previewUrl);
							advertisersOffer.setClick_url(null);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(caps_daily_remaining);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							//advertisersOffer.setSupported_carriers("");
							//advertisersOffer.setDescription(categoryStr);
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);
							advertisersOffer.setOs_version(osVersions);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(null);// 是否激励
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
