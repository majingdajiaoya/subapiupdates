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

public class PullBatmobiOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullBatmobiOfferService.class);
	
	private Advertisers advertisers;
	
	public PullBatmobiOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Baidu广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){

			logger.info("doPull batmobi Offer begin := " + new Date());
				
			Long advertisersId = advertisers.getId();
			
			String apiurl = advertisers.getApiurl();
			String apikey = advertisers.getApikey();
			
			if(advertisersId == null
					|| OverseaStringUtil.isBlank(apiurl)
					|| OverseaStringUtil.isBlank(apikey)){
				
				logger.info("advertisersId or apiurl or apikey is null return");
				
				return;
			}
			
			List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
			
			int beginPage = 1;
			
			int totalPage = doPagePullOffer(advertisersOfferList, advertisersId, apiurl, apikey, beginPage);
			
			for(int i = 2; i <= totalPage; i++){
				
				doPagePullOffer(advertisersOfferList, advertisersId, apiurl, apikey, i);
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
	
	/**
	 * 翻页处理
	 * 
	 * @param advertisersId
	 * @param apiurl
	 * @param apikey
	 * @param page
	 */
	private int doPagePullOffer(List<AdvertisersOffer> advertisersOfferList, Long advertisersId, String apiurl, String apikey, int page){
		
		Integer total_page = 0;
		
		try {
			
			apiurl = apiurl.replace("{apikey}", apikey);
			apiurl = apiurl.replace("{page}", String.valueOf(page));
					
			String str = HttpUtil.CuttGet(apiurl);
		
			JSONObject jsonPullObject = JSON.parseObject(str);
			
//			Integer errno = jsonPullObject.getInteger("errno");// "errno":0
//			
//			
//			if(errno == null
//					|| errno != 0){
//				
//				logger.info("errno is not ok return. errno := " + errno);
//				
//				return 0;
//			}
			
			//JSONObject data = jsonPullObject.getJSONObject("data");
			
			//获取分页等参数值
			//JSONObject params = data.getJSONObject("params");
			
			total_page = jsonPullObject.getInteger("pages");
			
			JSONArray adList = jsonPullObject.getJSONArray("offers");
			
			if(adList == null
					|| adList.size() == 0){
				
				logger.info("adList is empty");
				
				return 0;
			}
			
			if(adList != null
					&& adList.size() > 0){
				
				logger.info("begin pull offer " + adList.size());
				
				for(int i = 0; i < adList.size(); i++){
	
					JSONObject item = adList.getJSONObject(i);
					
					if(item != null){
						
						Integer intent=item.getInteger("intent");
						
						String payout_currency = item.getString("payout_currency");
						if (!"USD".equals(payout_currency)) {
							continue;
						}
						String acquisition_flow =  item.getString("acquisition_flow");
						
						
						String id = item.getString("camp_id");// 8763
						String pkg = item.getString("mobile_app_id");// "com.uc.iflow",
						Float  payout = item.getFloat("payout_amount");// 0.88,
						Integer conversions_cap = item.getInteger("daily_cap");// 0,
						String preview_url = item.getString("preview_url");// "https:\/\/play.google.com\/store\/apps\/details?id=com.uc.iflow",
						String title = item.getString("name");// "UC News - Latest News, Live Cricket Score, Videos",
						JSONArray countriesArray = item.getJSONArray("countries");// "IN",
						
						
						String platform = item.getString("mobile_platform");// "Android",
						String description = item.getString("name");// "com.uc.iflow",
						
						String click_url = item.getString("click_url");// "http:\/\/duclick.baidu.com\/click\/affClick?aff_id=512&offer_id=8763",
						//JSONArray iconArr = item.getJSONArray("icon");// icon url 列表
						
						if ((description == null||description.equals("")) &&title!=null) {
							description = title;
						}
						/**
						 * 排除处理
						 */
						
						// 如果id、pkg、price、country、Platforms、clickURL、cap为空，舍弃不入库
						if(id == null
								|| OverseaStringUtil.isBlank(pkg)
								|| payout == null
								|| countriesArray==null
								|| countriesArray.size()==0
								|| OverseaStringUtil.isBlank(platform)
								|| OverseaStringUtil.isBlank(click_url)
								|| conversions_cap == null
								|| conversions_cap < 0){
							
							continue;
						}
						
						// CPI类型，单价太低(0.06)，舍弃不入库
						if(payout < 0.06){
							
							continue;
						}
						
						/**
						 * 特殊字段处理
						 */
						//处理国家
						//country = country.replace(",", ":");
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
						
						//处理平台(0:andriod:1:ios:2:pc)
						
						String os_str =item.getString("mobile_platform"); 
						if ("android".equals(os_str.toLowerCase())) {
							os_str = "0";
							click_url=click_url+"&adv_id={gaid}";
						} else if ("ios".equals(os_str.toLowerCase())) {
							os_str = "1";
							click_url=click_url+"&adv_id={idfa}";
						}
						
						// icon设置
						String icon = null;
						
						
						/**
						 * 封装网盟广告对象
						 */
						
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(filterEmoji(title));
						
						//都设置为CPI类型广告	
						if ("CPI".equals(acquisition_flow)) {
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
						} else if ("CPA".equals(acquisition_flow)) {
							advertisersOffer.setCost_type(102);
							advertisersOffer.setOffer_type(102);
							advertisersOffer.setConversion_flow(102);
							
						} else  {
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
						advertisersOffer.setCountry(countries_str);
						if(conversions_cap == 0){
							
							advertisersOffer.setDaily_cap(99999);//不限制cap
						}
						else{
							
							advertisersOffer.setDaily_cap(conversions_cap);
						}
						advertisersOffer.setIncent_type(intent);
						advertisersOffer.setPayout(payout);
						advertisersOffer.setDescription(filterEmoji(description));
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);//设置为mobile类型
						//advertisersOffer.setOs_version(minOS);//系统版本要求
						advertisersOffer.setSend_type(0);//系统入库生成广告
						//advertisersOffer.setIncent_type(incetive);// 是否激励
						advertisersOffer.setSmartlink_type(1);// 非Smartlink类型
						advertisersOffer.setStatus(0);//设置为激活状态
						
						advertisersOfferList.add(advertisersOffer);
					}
				}
			}
		}
		catch (Exception e) {
			
			e.printStackTrace();
			
			NetworkLog.exceptionLog(e);
		}
		
		return total_page;
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
		
		//http://api.3point14.affise.com/3.0/partner/offers?api-key=fe2ddd357fda511d53f8f0cfa7c215a348784632&limit=500
		
		//http://api.3point14.affise.com/3.0/partner/offers?api-key={apikey}&limit=10000
		
		//http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		
		//http://bulk3.batmobi.net/api/network?app_key=HARNO1DGGHDP4H1H3M0EPBUO&limit=500&page=1
		
		
		tmp.setApiurl("http://bulk3.batmobi.net/api/network?app_key={apikey}&limit=500&page={page}");
		tmp.setApikey("HARNO1DGGHDP4H1H3M0EPBUO");
		tmp.setId(25L);
		
		//&aff_sub={aff_sub}&aff_site_id={sub_affid}
		PullBatmobiOfferService mmm=new PullBatmobiOfferService(tmp);
		mmm.run();
	}
}
