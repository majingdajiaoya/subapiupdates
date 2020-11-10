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

public class PullNewInplayablesOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullNewInplayablesOfferService.class);
	
	private Advertisers advertisers;
	
	public PullNewInplayablesOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Clickky广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Inplayable Offer begin := " + new Date());
			
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
				
//				Map<String, String> header = new HashMap<String ,String>();
//				
//				header.put("token", apikey);
//				
//				String str = HttpUtil.sendGet(apiurl, header);
			
				JSONObject jsonPullObject = JSON.parseObject(str);
				
				String status = jsonPullObject.getString("status");
				//Integer recordsTotal = jsonPullObject.getInteger("offer_total");
//				Integer recordsFiltered = jsonPullObject.getInteger("recordsFiltered");
				//Integer pagecount = jsonPullObject.getInteger("offer_count");
				
//				Integer count = jsonPullObject.getInteger("total_count");
				
				if(!"success".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
//				
//				logger.info("recordsTotal:= " + recordsTotal+"     recordsFiltered:"+pagecount);
//				if(pagecount == null
//						|| pagecount == 0){
//					
//					logger.info("available or  count is empty. " + " recordsFiltered:=" + pagecount);
//					
//					return;
//				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = JSONArray.parseArray(jsonPullObject.getString("datas"));
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer roffer_id = item.getInteger("id");// 104573764
							//String type = item.getString("type");//	"App"
							
							Integer device_type = null;
							device_type=0;

//							if("App".equals(type)){
//								
//								device_type = 0;// 手机mobile
//							}
							
							//String lead_type = item.getString("lead_type");// "install"
						    String offer_model = "CPI";// "CPI"
							//String offer_model = "cpi";// "CPI"
							
							String traffic_type =item.getString("isincent");
							
							Integer incetive = 0;// 是否激励(0:非激励 1:激励)
							if("0".equals(traffic_type)){
								incetive = 0;
							}
							else if("1".equals(traffic_type)){
								incetive = 1;
							}
							String kpitype=item.getString("kpitype");
							
//							SONObject targeting =  JSON.parseObject(item.getString("targeting"));
//							String countriesArray=item.getString("countries");
							
//							String paltfrom=targeting.getString("platform");
							String os_str =item.getString("platform"); 
							//String os_str = "0";
							if ("ANDROID".equals(os_str.toUpperCase())) {
								os_str = "0";
							} else if ("IOS".equals(os_str.toUpperCase())) {
								os_str = "1";
							}
//							else if ("Web".equals(paltfrom)) {
//								os_str = "2";
//							}
							String min_os_version =item.getString("min_os_version");

//							JSONArray include_min_os_version=item.getJSONArray("include_min_os_version");
//							if (include_min_os_version!=null&&include_min_os_version.size()>0) {
//								min_os_version = include_min_os_version.getString(0);
//							}
							

//							JSONObject product_info = null;
//							try{
//								product_info =JSON.parseObject(item.getString("product_info"));
//								
//							} catch (Exception e) {
//								e.printStackTrace();
//								logger.info(item.getString("product_info"));
//								continue;
//							}
							
							String name = item.getString("app_name");// "Legacy of Discord-FuriousWings"

							//String free = item.getString("free");// "true"
							String app_id = item.getString("app_pkg");// "com.gtarcade.lod"
							//String target_url = product_info.getString("preview_link");// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"
							String target_url = item.getString("preview_url");;// "https://play.google.com/store/apps/details?id=com.gtarcade.lod"

							String description = item.getString("des");
							//String kpi = item.getString("kpi");
							description=kpitype+" "+description;
//							String category = item.getString("appcategory");
							String icon = item.getString("app_icon");// "http://cdn.clickky.biz/s…Xzzyg=w3840_400x400.png"

							String creative_link = item.getString("material");
							String images_crative=null;

							if (creative_link!= null&&!creative_link.equals("{}")&&!creative_link.equals("[]")) {
								try {
									JSONArray images_JSONArray=new JSONArray();

									ArrayList<Map> arrayList = new ArrayList<Map>();
									JSONObject images_temp = JSON.parseObject(creative_link);
									
									 for (Map.Entry<String, Object> entry : images_temp.entrySet()) {
								            System.out.println(entry.getKey() + ":" + entry.getValue());
								            Map jsonObject = new HashMap();
								            jsonObject.put("url", entry.getValue());
								            jsonObject.put("size", entry.getKey());
								            arrayList.add(jsonObject);
								        }
									 images_JSONArray = JSONArray.parseArray(JSON.toJSONString(arrayList));
									 images_crative = images_JSONArray.toString();
									 
								} catch(Exception e) {
									logger.info("images is:"+creative_link);
									e.printStackTrace();
								}

							}
							
							//String description_lang = item.getString("description_lang");// ""
							//String instructions = item.getString("instructions");// ""
							//String expiration_date = item.getString("expiration_date");// ""
							
							String caps_daily_remaining_str = item.getString("daily_cap");// 9992
							Integer caps_daily_remaining = 10000;
							if(!OverseaStringUtil.isBlank(caps_daily_remaining_str)){
								if("".equals(caps_daily_remaining_str)){
									
									caps_daily_remaining = 10000;
								}
								else
								{
									
									caps_daily_remaining = Integer.parseInt(caps_daily_remaining_str);
								}
							}
							
							//String caps_daily = item.getString("caps_daily");// 9999
							//String caps_total = item.getString("caps_total");// "unlimited"
							//String caps_total_remaining	= item.getString("caps_total_remaining");// "unlimited"
							//Float avg_cr = item.getFloat("avg_cr");// 0.44
							//Integer rpc	= item.getInteger("rpc");// 0
							//boolean r = item.getBoolean("r");// false
							String link = item.getString("click_url");// "http://cpactions.com/api/v1.0/clk/track/proxy?ad_id=104573764&site_id=31466&response_id=59b8a7bd6cc66"
							//替换link中的 字段 &cid=[cid]&data1=[data1]&data2=[data2]&data3=[data3]&data4=[data4]&affsub1=[affsub1]&device_id=[device_id]&idfa=[idfa]&gaid=[gaid]
//					        $click = str_replace("[gaid]","{gaid}",$v['clickurl']);
////					        $click = str_replace("[affsub1]","{aff_sub}",$click);
//					        $click = str_replace("[cid]","{aff_sub}",$click);
//					        $click = str_replace("[idfa]","{idfa}",$click);
							
//							link=link.replace("{tid}", "{click_id}");
//							link=link.replace("{param}", "{aff_sub}");
							link=link.replace("{android}", "{andriod_id}");
//							link=link.replace("{region}", "{country}");
							link=link.replace("{channel}", "{sub_affid}");
							
//							if (os_str.equals("0")) { //android
//								link=link+"&device_id={gaid}";
//							} else if (os_str.equals("1")) { //ios
//								link=link+"&device_id={idfa}";
//							}
							//String carriers=item.getString("carriers");
							
							Float payout = item.getFloat("price");// 0.31
							
							JSONArray countriesArray = item.getJSONArray("countries");
							
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
							
							//String app_name = item.getString("app_name");// "Legacy of Discord-FuriousWings"
							//String app_description = item.getString("app_description");// "Google Play Summer Sale …Lodsupport@gtarcade.com"
							
							
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if(roffer_id == null
									|| payout == null
									|| countriesArray == null
									|| OverseaStringUtil.isBlank(link)
									|| OverseaStringUtil.isBlank(offer_model)
									){
								
								continue;
							}
							
							offer_model = offer_model.toUpperCase();
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if("CPI".equals(offer_model)
									&& payout < 0.06){
								
								continue;
							}
							// CPI类型，pkg为空，舍弃不入库
							if("CPI".equals(offer_model)
									&& OverseaStringUtil.isBlank(app_id)){
								
								continue;
							}
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(roffer_id));
							advertisersOffer.setName(name);
							
							if("CPI".equals(offer_model)||"CPA".equals(offer_model)){
								
								advertisersOffer.setCost_type(101);
								
								advertisersOffer.setOffer_type(101);
							}
							else if("CPL".equals(offer_model)){
								
								advertisersOffer.setCost_type(102);
								
								advertisersOffer.setOffer_type(102);//订阅类型
							}
							else{
								
								advertisersOffer.setCost_type(104);
								
								advertisersOffer.setOffer_type(104);//设置为其它类型
							}
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(app_id);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(icon);
							advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(link);
							
							advertisersOffer.setCountry(countries_str);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(caps_daily_remaining);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payout);
							//advertisersOffer.setExpiration(category);
							advertisersOffer.setCreatives(images_crative);
							if("CPI".equals(offer_model)||"CPA".equals(offer_model)){
								
								advertisersOffer.setConversion_flow(101);
							} else{
								
								advertisersOffer.setConversion_flow(104);//设置为其它类型
							}
							advertisersOffer.setSupported_carriers(null);
							advertisersOffer.setDescription(filterEmoji(description));
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(device_type);
							advertisersOffer.setOs_version(min_os_version);//系统版本要求
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
	
	 public static String filterEmoji(String source) { 
		  if (source != null && source.length() > 0) { 
		   return source.replaceAll("[\ud800\udc00-\udbff\udfff\ud800-\udfff]", ""); 
		  } else { 
		   return source; 
		  } 
		 }
	public static void main(String[] args)  {
		Advertisers tmp=new Advertisers();
		//http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=124&secret=0edb866cabafd269a4c977d7f33f90b4
		tmp.setApiurl("http://apipull.inplayable.com/index.php?m=server&p=getoffer&sid=124&secret={apikey}");
		tmp.setApikey("0edb866cabafd269a4c977d7f33f90b4");
		tmp.setId(21L);
		
		PullNewInplayablesOfferService mmm=new PullNewInplayablesOfferService(tmp);
		mmm.run();
	}
	
}
