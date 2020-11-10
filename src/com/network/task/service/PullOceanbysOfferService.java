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

public class PullOceanbysOfferService implements PullOfferServiceTask{

	protected static Logger logger = Logger.getLogger(PullOceanbysOfferService.class);
	
	private Advertisers advertisers;
	
	public PullOceanbysOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取oceanbys广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			logger.info("doPull Oceanbys Offer begin := " + new Date());
					
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
				apiurl = apiurl.replace("{page}", "1");
						
				String str = HttpUtil.sendGet(apiurl);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				Integer code = jsonPullObject.getInteger("code");
				String page = jsonPullObject.getString("page");
				Integer total_page = jsonPullObject.getInteger("total_page");
				Integer totalCount = jsonPullObject.getInteger("totalCount");
				
				
				if(code == 200
						&& total_page > 0
						&& totalCount > 0){
					
					List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
					
					JSONArray dataArray = jsonPullObject.getJSONArray("data");
					
					if(dataArray != null
							&& dataArray.size() > 0){
						
						logger.info("begin pull offer " + dataArray.size());
						
						for(int i = 0; i < dataArray.size(); i++){
							
							JSONObject item = dataArray.getJSONObject(i);
							
							if(item != null){
								
								String apk	= item.getString("apk");
								String banner = item.getString("banner");
								String country = item.getString("country");
								String description = item.getString("description");
								String ecountry = item.getString("ecountry");
								String id = item.getString("id");
								Integer incent = item.getInteger("incent");
								Integer is_exchange = item.getInteger("is_exchange");
								Integer is_onlypush = item.getInteger("is_onlypush");
								String mainicon = item.getString("mainicon");
								String preview_link = item.getString("preview_link");
								String category = item.getString("category");
								String name = item.getString("name");
								Integer object_cap = item.getInteger("object_cap");
								String operator = item.getString("operator");
								String pkg = item.getString("pkg");
								Float price = item.getFloat("price");
								Integer sinstall = item.getInteger("sinstall");
								Integer size = item.getInteger("size");
								Integer status = item.getInteger("status");
								Integer type = item.getInteger("type");//系统类型 1 安卓; 2 IOS
								String offer_type = item.getString("offer_type");
								Integer remaining_cap = item.getInteger("remaining_cap");
								String platform = item.getString("platform");
								String min_version = item.getString("min_version");
								boolean gaid_must = item.getBoolean("gaid_must");
								String click = item.getString("click");
								
								// 如果id、price、offer_type、type、country、click、remaining_cap为空，舍弃不入库
								if(OverseaStringUtil.isBlank(id)
										|| price == null
										|| OverseaStringUtil.isBlank(offer_type)
										|| type == null
										|| OverseaStringUtil.isBlank(country)
										|| OverseaStringUtil.isBlank(click)
										|| remaining_cap == null){
									
									continue;
								}
								
								offer_type = offer_type.toUpperCase();
								
								// CPI类型，单价太低(0.06)，舍弃不入库
								if("CPI".equals(offer_type)
										&& price < 0.06){
									
									continue;
								}
								
								AdvertisersOffer advertisersOffer = new AdvertisersOffer();
								
								advertisersOffer.setAdv_offer_id(id);
								advertisersOffer.setName(name);
								
								if("CPI".equals(offer_type)){
									
									advertisersOffer.setCost_type(101);
									
									advertisersOffer.setOffer_type(101);
								}
								else if("CPA".equals(offer_type)){
									
									advertisersOffer.setCost_type(102);
									
									advertisersOffer.setOffer_type(102);//订阅类型
								}
								else{
									
									advertisersOffer.setCost_type(104);
									
									advertisersOffer.setOffer_type(104);//设置为其它类型
								}
								
								advertisersOffer.setAdvertisers_id(advertisersId);//10代表oceanbys
								advertisersOffer.setPkg(pkg);
								//advertisersOffer.setpkg_size();
								advertisersOffer.setMain_icon(mainicon);
								advertisersOffer.setPreview_url(preview_link);
								advertisersOffer.setClick_url(click);
								advertisersOffer.setCountry(country);
								advertisersOffer.setEcountry(ecountry);
								advertisersOffer.setDaily_cap(remaining_cap);
								//advertisersOffer.setsend_cap();
								advertisersOffer.setPayout(price);
								//advertisersOffer.setexpiration();
								//advertisersOffer.setcreatives();
								if("CPI".equals(offer_type)){
									
									advertisersOffer.setConversion_flow(101);
								}
								else{
									
									advertisersOffer.setConversion_flow(104);//设置为其它类型
								}
								advertisersOffer.setSupported_carriers(operator);
								advertisersOffer.setDescription(description);
								
								if(1 == type){
									
									advertisersOffer.setOs("0");
									advertisersOffer.setDevice_type(0);
								}
								else if(2 == type){
									
									advertisersOffer.setOs("1");
									advertisersOffer.setDevice_type(0);
								}
								advertisersOffer.setOs_version(min_version);
								advertisersOffer.setSend_type(0);//系统入库生成广告
								advertisersOffer.setIncent_type(incent);
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
			}
			catch (Exception e) {
				
				e.printStackTrace();
				
				NetworkLog.exceptionLog(e);
			}
		}
	}
	
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=124&secret=0edb866cabafd269a4c977d7f33f90b4
		tmp.setApiurl("http://i.ttechhk.com/v1/affiliate/myoffer?API-Key=36436ebb290aa2c8251d62d7b43f4220");
		tmp.setApikey("0edb866cabafd269a4c977d7f33f90b4");
		tmp.setId(21L);
		
		PullOceanbysOfferService mmm=new PullOceanbysOfferService(tmp);
		mmm.run();
	}
	
}
