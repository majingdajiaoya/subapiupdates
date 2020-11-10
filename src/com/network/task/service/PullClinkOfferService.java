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

public class PullClinkOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullClinkOfferService.class);
	
	private Advertisers advertisers;
	
	public PullClinkOfferService(Advertisers advertisers){
		
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

			logger.info("doPull Clink Offer begin := " + new Date());
				
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
					
			String str = HttpUtil.sendGet(apiurl);
			
			JSONObject jsonPullObject = JSON.parseObject(str);
			
			String errno = jsonPullObject.getString("result");// "errno":0
			
			
			if(errno == null
					|| !errno.equals("success")){
				
				logger.info("errno is not ok return. errno := " + errno);
				
				return 0;
			}
			
			total_page = jsonPullObject.getInteger("page_total");
			
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
						
						String id = item.getString("offer_id");// 1
						String pkg = item.getString("package_name");// 1
						Float  payout = item.getFloat("payout");// 1
						String preview_url = item.getString("preview_url");// 1
						String title = item.getString("name");// 1
						String country = item.getString("countries");// 1
						String platform = item.getString("platform");// 1
						String description = item.getString("desc");// 1
						String click_url = item.getString("tracking_link");// 1
						String icon = item.getString("icon_url");// 1
						
						String payout_type=item.getString("payout_type");
						
						String currency = item.getString("currency");
						if (currency == null || !currency.equals("USD")) {
							continue;
						}
						
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
								|| OverseaStringUtil.isBlank(country)
								|| OverseaStringUtil.isBlank(platform)
								|| OverseaStringUtil.isBlank(click_url)){
							
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
						country = country.replace("|", ":");
						
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
						os_str = os_str.substring(0, os_str.length() - 1);
						
						
						/**
						 * 封装网盟广告对象
						 */
						
						AdvertisersOffer advertisersOffer = new AdvertisersOffer();
						
						advertisersOffer.setAdv_offer_id(id);
						advertisersOffer.setName(title);
						
						//都设置为CPI类型广告	
						if("CPI".equals(payout_type)){
							
							advertisersOffer.setCost_type(101);
							
							advertisersOffer.setOffer_type(101);
						}
						else if("CPA".equals(payout_type)){
							
							advertisersOffer.setCost_type(102);
							
							advertisersOffer.setOffer_type(102);//订阅类型
						}
						else{
							
							advertisersOffer.setCost_type(104);
							
							advertisersOffer.setOffer_type(104);//设置为其它类型
						}
						
						advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
						advertisersOffer.setPkg(pkg);
						//advertisersOffer.setpkg_size();
						advertisersOffer.setMain_icon(icon);
						advertisersOffer.setPreview_url(preview_url);
						advertisersOffer.setClick_url(click_url);
						advertisersOffer.setCountry(country);
						advertisersOffer.setDaily_cap(9999);//不限制cap
						advertisersOffer.setIncent_type(0);
						advertisersOffer.setPayout(payout);
						advertisersOffer.setDescription(description);
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
	
	public static void main(String[] args) throws Exception {
		Advertisers tmp=new Advertisers();
		//http://appwalls.mobi/api/v1?api_key={apikey}&pub_id=3330&c=4752&incent=3&os=Android&country=ALL
		//http://api.metalex.io/index.php?request_uri=/Offer/Getoffer/getOffer&sid=118&secret={apikey}&pagesize=1
		tmp.setApiurl("http://api.clinkad.com/offer_api_v3?key={apikey}&pubid=9501&page={page}&pagesize=100");//https://api.trackkor.com/api-ext/v1/campaigns/。辛苦/
		tmp.setApikey("zpxh856e7muradfw");
		tmp.setId(25L);
		
		////&s1={aff_sub}&s2={sub_affid}&s5={idfa}&s4={gaid}
		PullClinkOfferService pp=new PullClinkOfferService(tmp);
		pp.run();
	}
	
}
