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
import com.network.task.util.TuringStringUtil;
import common.Logger;

public class PullStartappOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullStartappOfferService.class);
	
	private Advertisers advertisers;
	
	public PullStartappOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Startapp广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Startapp Offer begin := " + new Date());
			
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
						
				String str = HttpUtil.CuttGet(apiurl);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				Integer responseCode = jsonPullObject.getInteger("responseCode");
				
				
				if(responseCode != 200){
					
					logger.info("responseCode is not 200 return. responseCode := " + responseCode);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("campaigns");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							String campID = item.getString("campID");//	"DAP_94965b9097b2181670fa35bf071532f8"
							String title = item.getString("title");
							String clickUrl = item.getString("clickUrl");
							Boolean isApp = item.getBoolean("isApp");
							String pck = item.getString("pck");
							String payout = item.getString("payout");
							String bidType = item.getString("bidType");// "CPI"
							String os = item.getString("os");//ANDROID iOS
							//String deviceType = item.getString("deviceType");//
							String minOSVersion = item.getString("minOSVersion");
							JSONObject countries = item.getJSONObject("countries");
							JSONArray countriesArray = countries.getJSONArray("include");
							JSONArray assets = item.getJSONArray("assets");
							String desc = item.getString("desc");
							
							String icon=null;
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if(campID == null
									|| OverseaStringUtil.isBlank(payout)
									|| countriesArray == null
									|| countriesArray.size() == 0
									|| OverseaStringUtil.isBlank(clickUrl)
									|| OverseaStringUtil.isBlank(payout)){
								
								continue;
							}
							
							//处理emoji表情
							title = TuringStringUtil.filterEmoji(title);
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if(Float.valueOf(payout) < 0.06){
								
								continue;
							}
							// CPI类型，pkg为空，舍弃不入库
							if(!"CPI".equals(bidType)){
								
								continue;
							}
							
							if(isApp == null
									|| !isApp){
								
								continue;
							}
							
							/**
							 * 特殊处理
							 */

							//平台
							String os_str = "";
							if(!OverseaStringUtil.isBlank(os)){

								if("android".equals(os.toLowerCase())){
									
									os_str += "0:";
									clickUrl = clickUrl+"&advId={gaid}&segId=204610784";
								}
								if("ios".equals(os.toLowerCase())){
									
									os_str += "1:";
									clickUrl = clickUrl+"&advId={idfa}&segId=204610784";
								}
							}
							//去除最后一个:号
							if(!OverseaStringUtil.isBlank(os_str)){
								
								os_str = os_str.substring(0, os_str.length() - 1);
							}
							String privrewurl=TuringStringUtil.getPriviewUrl(os_str, pck);
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
							JSONArray images_JSONArray=new JSONArray();
							String images_crative=null;

							if (assets!= null &&assets.size()>0) {
								ArrayList<Map> arrayList = new ArrayList<Map>();
								
								for (int j=0;j<assets.size();j++) {
									JSONObject imtrm=assets.getJSONObject(j);
									String url=imtrm.getString("img");
									int w=imtrm.getIntValue("adw");
									int h=imtrm.getIntValue("adh");
									if (w==h) {
										if (w==75) {
											icon = url;
										}
									} else {
										if (arrayList.size()<3) {
								            Map jsonObject = new HashMap();
								            jsonObject.put("url", url);
								            jsonObject.put("size", w+"x"+h);
								            arrayList.add(jsonObject);
										}
									}
								}
								
								if (arrayList.size()>0) {
									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									 images_crative = images_JSONArray.toString();
								}
								
							}
							
							
							/**
							 * 封装
							 */
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(campID);
							advertisersOffer.setName(title);
							
							if("CPI".equals(bidType)){
								
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
							advertisersOffer.setPkg(pck);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(privrewurl);
							advertisersOffer.setClick_url(clickUrl);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(99999);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(Float.valueOf(payout));
							//advertisersOffer.setExpiration(expriation);
							advertisersOffer.setCreatives(images_crative);
							//advertisersOffer.setSupported_carriers("");
							advertisersOffer.setDescription(TuringStringUtil.filterEmoji(desc));
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);// mobile
							advertisersOffer.setOs_version(minOSVersion);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(0);// 是否激励(0:非激励 1:激励)
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
	
	 public static String filterEmoji(String source) { 
		  if (source != null && source.length() > 0) { 
		   return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", ""); 
		  } else { 
		   return source; 
		  } 
		 }
	
	//https://api.startappservice.com/1.1/management/bulk/campaigns?partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&payout=0.1
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//https://api.startappservice.com/1.1/management/bulk/campaigns?partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&payout=0.1
		//{apikey}
		//
		
		tmp.setApiurl("http://api.startappservice.com/1.1/management/bulk/campaigns?{apikey}&os=0&bidType=CPI&countries=US,KR,JP,IN,ID,BR,MX,MY,PH,TH,RU&minPayoutCPI=0.1");
		tmp.setApikey("partner=143752920&token=1a3e587d5c04ca93285f382731f5e0cb&segId=204610784");
		tmp.setId(25L);
		
		PullStartappOfferService mmm=new PullStartappOfferService(tmp);
		mmm.run();
	}
}
