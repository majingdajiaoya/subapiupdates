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

public class PullAdxmiOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullAdxmiOfferService.class);
	
	private Advertisers advertisers;
	
	public PullAdxmiOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Adxmi广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){

			logger.info("doPull Adxmi Offer begin := " + new Date());
				
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
			
//			//最多拉取2页1000条广告
//			if(totalPage > 2){
//				
//				totalPage = 2;
//			}
			
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
			
			Integer c = jsonPullObject.getInteger("c");// "c":0
			Integer total = jsonPullObject.getInteger("total");
			Integer page_size = jsonPullObject.getInteger("page_size");// 每页数量
			Integer n = jsonPullObject.getInteger("n");// "n":47 每页具体数量
			
			if(c == null
					|| c != 0){
				
				logger.info("c is not ok return. c := " + c);
				
				return 0;
			}
			
			if(total == null
					|| total == 0){
				
				logger.info("total is empty, ruturn");
				
				return 0;
			}
			
			if(page_size == null
					|| page_size == 0){
				
				logger.info("page_size is empty, ruturn");
				
				return 0;
			}
			
			//计算总页数 总条数/每页数量
			if(total % page_size == 0){
				
				total_page = total / page_size;
			}
			else{
				
				total_page = total / page_size + 1;
			}
			
			// 获取广告
			JSONArray offers = jsonPullObject.getJSONArray("offers");
			
			if(offers == null
					|| offers.size() == 0){
				
				logger.info("offers is empty");
				
				return 0;
			}
			
			if(offers != null
					&& offers.size() > 0){
				
				logger.info("begin pull offer " + offers.size());
				
				for(int i = 0; i < offers.size(); i++){
	
					JSONObject item = offers.getJSONObject(i);
					
					if(item != null){
						
						String id = item.getString("id");// "992757774783880852"
						String name = item.getString("name");
						String pkg = item.getString("package");// "com.uc.iflow",
						Float  payout = item.getFloat("payout");// 0.88,
						Integer cap = item.getInteger("cap");// 0,0代表不限制
						String preview_url = item.getString("preview_url");// "https:\/\/play.google.com\/store\/apps\/details?id=com.uc.iflow",
						JSONArray countryArr = item.getJSONArray("country");// "IN" "AT",为空则代表不限制国家
						JSONArray osArr = item.getJSONArray("os");// "android ios",
						String traffic = item.getString("traffic");// incentive or non-incentive
						String os_version = item.getString("os_version");
						String trackinglink = item.getString("trackinglink");// "http://t.api.yyapi.net/v1/tracking?ad=992757774783880852&app_id=0b5cf3ad58e05dbb&pid=3",
						String icon_url = item.getString("icon_url");// icon url 列表
						String payout_type = item.getString("payout_type");// "CPA" "CPI"
						String desc= item.getString("adtxt");
						
						/**
						 * 排除处理
						 */
						
						// 如果id、pkg、price、country、platforms、clickURL、cap、payout_type为空，舍弃不入库
						if(id == null
								|| OverseaStringUtil.isBlank(pkg)
								|| payout == null
								|| countryArr == null
								|| osArr == null
								|| OverseaStringUtil.isBlank(trackinglink)
								|| cap == null
								|| OverseaStringUtil.isBlank(payout_type)){
							
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
						String countries = "";
						if(countryArr != null
								&& countryArr.size() > 0){
							
							for(int j = 0; j < countryArr.size(); j++){
								
								String countryItem = countryArr.getString(j);
								
								countries += countryItem + ":";
							}
						}
						//去除最后一个:号
						if(!OverseaStringUtil.isBlank(countries)){
							
							countries = countries.substring(0, countries.length() - 1);
						}
						
						//处理平台(0:andriod:1:ios:2:pc)
						String os_str = "";
						if(osArr != null
								&& osArr.size() > 0){
							
							for(int j = 0; j < osArr.size(); j++){
								
								String osItem = osArr.getString(j);
								
								if("ANDROID".equals(osItem.toUpperCase())){
									
									os_str += "0" + ":";
								}
								else if("IOS".equals(osItem.toUpperCase())){
									
									os_str += "1" + ":";
								}
							}
						}
						//去除最后一个:号
						if(!OverseaStringUtil.isBlank(os_str)){
							
							os_str = os_str.substring(0, os_str.length() - 1);
						}
						
						// incetive(0:非激励 1:激励)
						Integer incetive = null;
						if(!OverseaStringUtil.isBlank(traffic)){
							
							if("incentive".equals(traffic)){
								
								incetive = 1;
							}
							else if("non-incentive".equals(traffic)){
								
								incetive = 0;
							}
						}
						//
						//                "1200x627":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_2_prod_708_ea93b7381f854a0bcb78ce0b762fd52d.jpg",
		               // "x":"http://sm-campaign.s3-us-west-2.amazonaws.com/resource/icons/source_1_prod_670_source_55_prod_708_59b23dc121a71.jpg"
						String images_crative=null;
						JSONArray creativeJSONArray = item.getJSONArray("creative");
						if (creativeJSONArray!=null &&creativeJSONArray.size()>0) {
							JSONArray images_JSONArray=new JSONArray();

							ArrayList<Map> arrayList = new ArrayList<Map>();
							for (int j=0;j<creativeJSONArray.size();j++) {
								JSONObject creativetemp = creativeJSONArray.getJSONObject(j);
								if (arrayList.size()>3) {
									break;
								}
					            Map jsonObject = new HashMap();
					            jsonObject.put("url", creativetemp.get("url"));
					            jsonObject.put("size", creativetemp.get("width")+"*"+creativetemp.get("height"));
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
						if("CPI".equals(payout_type.toUpperCase())){
							
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
						}
						else if("CPA".equals(payout_type.toUpperCase())){
							
							advertisersOffer.setCost_type(102);
							advertisersOffer.setOffer_type(102);
							advertisersOffer.setConversion_flow(102);
						}
						else{
							
							advertisersOffer.setCost_type(104);
							advertisersOffer.setOffer_type(104);
							advertisersOffer.setConversion_flow(104);
						}
						
						System.out.println(trackinglink);
						advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
						advertisersOffer.setPkg(pkg);
						//advertisersOffer.setpkg_size();
						advertisersOffer.setMain_icon(icon_url);
						advertisersOffer.setPreview_url(preview_url);
						advertisersOffer.setClick_url(trackinglink);
						advertisersOffer.setCountry(countries);
						if(cap == 0){
							
							advertisersOffer.setDaily_cap(10000);//不限制cap
						}
						else{
							
							advertisersOffer.setDaily_cap(cap);
						}
						
						advertisersOffer.setPayout(payout);
						advertisersOffer.setDescription(desc);
						advertisersOffer.setCreatives(images_crative);   
						
						advertisersOffer.setOs(os_str);
						advertisersOffer.setDevice_type(0);//设置为mobile类型
						advertisersOffer.setOs_version(os_version);//系统版本要求
						advertisersOffer.setSend_type(0);//系统入库生成广告
						advertisersOffer.setIncent_type(incetive);// 是否激励
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
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://ad.api.yyapi.net/v2/offline?app_id=baf49f8ede94d07e&page=1&page_size=100&payout_type=CPI
		tmp.setApiurl("http://ad.api.yyapi.net/v2/offline?app_id={apikey}&page={page}&page_size=500&payout_type=CPI");
		tmp.setApikey("6b35cc71b1e8219603049656be1b3045");
		tmp.setId(25L);
		//&user_id={aff_sub}&chn={sub_affid}&andid={android_id}&advid={gaid}&idfa={idfa}
		
		PullAdxmiOfferService mmm=new PullAdxmiOfferService(tmp);
		mmm.run();
	}
	
}
