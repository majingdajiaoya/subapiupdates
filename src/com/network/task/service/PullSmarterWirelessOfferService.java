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

public class PullSmarterWirelessOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullSmarterWirelessOfferService.class);
	
	private Advertisers advertisers;
	
	public PullSmarterWirelessOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Smarter Wireless广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){

			logger.info("doPull tapgerine Offer begin := " + new Date());
					
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
				
				String status = jsonPullObject.getString("status");// "status":"OK"
				Integer page = jsonPullObject.getInteger("page");// "page":1,
				Integer limit = jsonPullObject.getInteger("limit");// "limit":5000,
				Integer total = jsonPullObject.getInteger("total");// "total":111,
				
				if(status == null
						|| !"success".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArr = jsonPullObject.getJSONArray("offers");
				
				if(offersArr == null
						|| offersArr.size() == 0){
					
					logger.info("offersArray is empty");
					
					return;
				}
				
				if(offersArr != null
						&& offersArr.size() > 0){
					
					logger.info("begin pull offer " + offersArr.size());
					
					for(int i = 0; i < offersArr.size(); i++){
	
						JSONObject item = offersArr.getJSONObject(i);
						
						if(item != null){
							
							String id = item.getString("id");// "1716071",
							String name = item.getString("name");// "\u72c2\u66b4\u4e4b\u7ffc",
							String icon = item.getString("icon");// "http:\/\/images.startappservice.com",
							String packet_name = item.getString("packet_name");// "com.icantw.wings",
							String network = item.getString("network");// "34",
							String packet_size = item.getString("packet_size");// "45.00",
							Float  price = item.getFloat("price");// 0.88,
							String currency = item.getString("currency");// "USD",
							String click_url = item.getString("click_url");// "http:\/\/aws.smarter-wireless.net\/index.php?m=network&p=click&app_id=76&offer_id=1716071&gaid=&aff_sub=&andid=&idfa=&appname=",
							String daily_cap = item.getString("daily_cap");// "750",
							String preview_url = item.getString("preview_url");// "https:\/\/play.google.com\/store\/apps\/details?id=com.icantw.wings",
							//String countries = item.getString("countries");// "HK",
							String incent = item.getString("incent");// "0",
							String platform = item.getString("platform");// "android",
							String category = item.getString("category");// "Others",
							String completionAction = item.getString("completionAction");// "Download Install & Open",
							String kpi = item.getString("kpi");// "Download Install & Open",
							String minOS = item.getString("minOS");// ""							
							String description = item.getString("description");// ""
							if (completionAction!=null) {
								description =description+" "+completionAction;
							}
							if (kpi!=null) {
								description =kpi +" " +description;
							}
							
							click_url=click_url.replace("{channel}", "{sub_affid}");
							click_url=click_url.replace("{android}", "{andriod_id}");
							//TODO
							//Restrictions;
							JSONArray countriesArray = item.getJSONArray("countries");// ["AU","US"]
							// 国家
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
							
							
							/**
							 * 排除处理
							 */
							
							// 如果id、pkg、price、country、Platforms、clickURL、remaining_cap为空，舍弃不入库
							if(OverseaStringUtil.isBlank(id)
									|| OverseaStringUtil.isBlank(packet_name)
									|| price == null
									|| OverseaStringUtil.isBlank(countries_str)
									|| OverseaStringUtil.isBlank(platform)
									|| OverseaStringUtil.isBlank(click_url)
									|| OverseaStringUtil.isBlank(daily_cap)){
								
								continue;
							}
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if(price < 0.06){
								
								continue;
							}
							
							/**
							 * 特殊字段处理
							 */
							//处理国家
							//countries = countries.replace(",", ":");

							
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
							if(OverseaStringUtil.isBlank(os_str)){
								
								continue;
							}
							
							os_str = os_str.substring(0, os_str.length() - 1);
							
							//激励类型(0:非激励 1:激励)
							Integer incetive = null;
							
							if(!OverseaStringUtil.isBlank(incent)){
								
								if("0".equals(incent)){
									
									incetive = 0;
								}
								else if("1".equals(incent)){
									
									incetive = 1;
								}
							}
							
							String images = item.getString("images");
							//                "1200x627":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_2_prod_708_ea93b7381f854a0bcb78ce0b762fd52d.jpg",
			               // "x":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_55_prod_708_59b23dc121a71.jpg"
							String images_crative=null;
							if (images!= null&&!images.equals("{}")&&!images.equals("[]")) {
								JSONArray images_JSONArray=new JSONArray();

								ArrayList<Map> arrayList = new ArrayList<Map>();
								JSONObject images_temp = JSON.parseObject(images);
								
								 for (Map.Entry<String, Object> entry : images_temp.entrySet()) {
							         if (arrayList.size()>3) {
							        	 break;
							         }   
									 System.out.println(entry.getKey() + ":" + entry.getValue());
							            
							            Map jsonObject = new HashMap();
							            jsonObject.put("url", entry.getValue());
							            jsonObject.put("size", entry.getKey());
							            arrayList.add(jsonObject);
							        }
								 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
								 images_crative = images_JSONArray.toString();
							}
							
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
							advertisersOffer.setPkg(packet_name);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(preview_url);
							advertisersOffer.setClick_url(click_url);
							
							advertisersOffer.setCountry(countries_str);
						
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(Integer.parseInt(daily_cap));
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(price);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives(images_crative);
							advertisersOffer.setCreatives(images_crative);
							
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(description);
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);//设置为mobile类型
							advertisersOffer.setOs_version(minOS);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(incetive);// 是否激励
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
	
	//
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		//http://api.mobismarter.com/index.php?m=advert&p=getoffer&{apikey}&page=1&limit=500
		tmp.setApiurl("http://api.mobismarter.com/index.php?m=advert&p=getoffer&{apikey}&page=1&limit=500");
		tmp.setApikey("app_id=210&app_key=8af01e5dfcf9271d2ce80ff2d61be5ed");
		tmp.setId(26L);
		
		PullSmarterWirelessOfferService mmm=new PullSmarterWirelessOfferService(tmp);
		mmm.run();
	}
}
