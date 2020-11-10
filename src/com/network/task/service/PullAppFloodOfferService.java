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

public class PullAppFloodOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullAppFloodOfferService.class);
	
	private Advertisers advertisers;
	
	public PullAppFloodOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Leadbolt广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull AppFlood Offer begin := " + new Date());
			
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
				String status=jsonPullObject.getString("status");
				Integer offers_num_this_page = jsonPullObject.getInteger("offers_num_this_page");
				if (status==null||!"OK".equals(status)) {
					return;
				}
				
				if (offers_num_this_page.intValue()<=0) {
					return;
				}
				
				JSONArray offersArray = jsonPullObject.getJSONArray("offers");
				
				if(offersArray == null
						|| offersArray.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer size := " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer campaign_id = item.getInteger("offerid");// 430633
							String platform = item.getString("platform");// "android"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
							Float payout_amt = item.getFloat("payout");// 2.55
							String countriesArray = item.getString("geo");// ["AU","US"]
							String pkg = item.getString("package");
							String preview_url = item.getString("preview_url");
							String app_name = item.getString("app_name");
							String min_os_version = item.getString("min_version");// "4.4"
							String payout_type = item.getString("pricetype");// "CPA"
							String linkout_url = item.getString("offer_url");
							
							String mainicon=item.getString("icon_url");
							
							/**
							 * 特殊处理字段
							 */
							
							// 系统平台
							if(!OverseaStringUtil.isBlank(platform)){
								
								if("android".equals(platform.toLowerCase())){
									
									platform = "0";
									linkout_url=linkout_url+"&aff_sub3={gaid}";
								}
								if("ios".equals(platform.toLowerCase())){
									
									platform = "1";
									linkout_url=linkout_url+"&aff_sub3={idfa}";
								}
							}
							
							// 国家
							String countries_str = countriesArray.replace("|", ":");
							
//							if(countriesArray != null
//									&& countriesArray.size() > 0){
//								
//								for(int k = 0; k < countriesArray.size(); k++){
//									
//									String countriesItem = countriesArray.getString(k);
//									
//									if(countriesItem != null){
//										
//										countries_str += countriesItem.toUpperCase() + ":";
//									}
//								}
//							}
//							//去除最后一个:号
//							if(!OverseaStringUtil.isBlank(countries_str)){
//								
//								countries_str = countries_str.substring(0, countries_str.length() - 1);
//							}
							
							// 如果id、price、country、clickURL、mobile_platform、pkg、payout_type为空，舍弃不入库
							if(campaign_id == null
									|| payout_amt == null
									|| OverseaStringUtil.isBlank(countries_str)
									|| OverseaStringUtil.isBlank(linkout_url)
									|| OverseaStringUtil.isBlank(platform)
									|| OverseaStringUtil.isBlank(pkg)
									|| OverseaStringUtil.isBlank(payout_type)){
								
								continue;
							}
							
							// 单价太低(0.06)，舍弃不入库
							if(payout_amt < 0.06){
								
								continue;
							}
							Integer daily_cap = item.getInteger("daily_cap");
							if (daily_cap<=0) {
								continue;
							}
							//处理 icon，description,还有 creatives等
							String carrier = item.getString("carrier");
							String Description=item.getString("offer_description");
							String pkgsize=item.getString("app_size");
							
							JSONObject creatives = item.getJSONObject("creative_link");// ["AU","US"]
							String images_crative=null;
							ArrayList<Map> arrayList = new ArrayList<Map>();
							if (creatives!=null) {
								 for (Map.Entry<String, Object> entry : creatives.entrySet()) {
							            System.out.println(entry.getKey() + ":" + entry.getValue());
							            if (arrayList.size()<3) {
								            Map jsonObject = new HashMap();
								            //JSONArray urlss=JSONArray.parseArray((String)entry.getValue());
//								            String urlss=(String)entry.getValue();
//								            urlss=urlss.replace("[", "");
//								            urlss=urlss.replace("]", "");
//								            String[] urlss2=urlss.split(",");
								            JSONArray urlss=(JSONArray)entry.getValue();
								            
								            jsonObject.put("url", urlss.get(0));
								            jsonObject.put("size", entry.getKey());
								            arrayList.add(jsonObject);
							            }
							        }
									JSONArray images_JSONArray=new JSONArray();

									if (arrayList.size()>0) {
										 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
										 images_crative = images_JSONArray.toString();
									}
							}
							
							/**
							 * 生成广告池对象
							 */
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(campaign_id));
							advertisersOffer.setName(app_name);
							
							//
							if("CPI".equals(payout_type.toUpperCase())){
								
								advertisersOffer.setCost_type(101);	
								advertisersOffer.setOffer_type(101);
								advertisersOffer.setConversion_flow(101);
							} else if ("CPA".equals(payout_type.toUpperCase())) {
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
							//advertisersOffer.setp();
							advertisersOffer.setMain_icon(mainicon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(linkout_url);
							advertisersOffer.setCountry(countries_str);
							//advertisersOffer.setEcountry(ecountry);
							advertisersOffer.setDaily_cap(daily_cap);//无cap限制，默认3000
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout_amt);
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(images_crative);
							advertisersOffer.setSupported_carriers(carrier);
							advertisersOffer.setDescription(Description);
							advertisersOffer.setOs(platform);
							advertisersOffer.setDevice_type(0);// 0代表手机mobile
							advertisersOffer.setOs_version(min_os_version);//系统版本要求
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers(); //{apikey}
		//http://api.appflood.com/s2s_get_p_ads?token=f2932e40b36434b3&page=1&pagesize=500&incent=0&pricetype=cpi&conversionflow=101&payout=0.06
		tmp.setApiurl("http://api.appflood.com/s2s_get_p_ads?token={apikey}&page=1&pagesize=500&incent=0&conversionflow=101&payout=0.06");
		tmp.setApikey("f2932e40b36434b3");
		tmp.setId(26L);
		
		PullAppFloodOfferService mmm=new PullAppFloodOfferService(tmp);
		mmm.run();
	}
	
}
