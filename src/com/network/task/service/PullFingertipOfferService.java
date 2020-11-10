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

public class PullFingertipOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullFingertipOfferService.class);
	
	private Advertisers advertisers;
	
	public PullFingertipOfferService(Advertisers advertisers){
		
		this.advertisers = advertisers;
	}
	
	/**
	 * 拉取Fingertip Interactive(庆恒广告主)广告信息，并入临时表中
	 * 
	 * 1. 首先判断网盟是否停止，如果停止无效，则不拉取
	 * 2. 拉取广告，并进行广告是否下线比对，对下线的广告进行渠道广告停止
	 * 3. 更新单价的广告处理(单价更新广告，设置相应的更新到网盟广告表和渠道广告表)
	 */
	public void run(){
		
		//同步互斥
		synchronized(this){
			
			logger.info("doPull Fingertip Offer begin := " + new Date());
			
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
				
				Integer result = jsonPullObject.getInteger("result");
				
				if(result != 0){
					
					logger.info("result is not 0 return. result := " + result);
					
					return;
				}
					
				List<AdvertisersOffer> advertisersOfferList = new ArrayList<AdvertisersOffer>();
				
				JSONArray offersArray = jsonPullObject.getJSONArray("ads");
				
				if(offersArray != null
						&& offersArray.size() > 0){
					
					logger.info("begin pull offer " + offersArray.size());
					
					for(int i = 0; i < offersArray.size(); i++){
						
						JSONObject item = offersArray.getJSONObject(i);
						
						if(item != null){
							
							Integer offerid = item.getInteger("offerid");// 104573764
							String name = item.getString("name");//	"cleaner"
							String os = item.getString("os");//	"android"
							String payout = item.getString("payout");//	"0.01"
							String pkg = item.getString("pkg");// "com.fog.magic.mycleaner"
							Integer type = item.getInteger("type");// 1:android; 2:cps 3: IOS
							String mainicon = item.getString("mainicon");// ""
							String country = item.getString("country");// "ID:MY:TH:PH:BR"
							Integer incent = item.getInteger("incent");// 0 非激励 1 激励
							Integer cap = item.getInteger("cap");//10000
							String click = item.getString("click");// ""
							
							
							// 如果id、price、country、clickURL、offer_model、remaining_cap为空，舍弃不入库
							if(offerid == null
									|| OverseaStringUtil.isBlank(payout)
									|| OverseaStringUtil.isBlank(country)
									|| OverseaStringUtil.isBlank(pkg)
									|| OverseaStringUtil.isBlank(click)
									|| cap == null){
								
								continue;
							}
							
							/**
							 * 特殊处理字段
							 */
							
							// 价格
							Float payoutFloat = null;
							
							if(!OverseaStringUtil.isBlank(payout)){
								
								payoutFloat = Float.valueOf(payout);
							}
							
							// 系统平台
							String os_str = "";
							
							if(os != null){
								if("android".equals(os.toLowerCase())){
									
									os_str += "0:";
								}
								if("ios".equals(os.toLowerCase())){
									
									os_str += "1:";
								}
							}
							
							//去除最后一个:号
							if(!OverseaStringUtil.isBlank(os_str)){
								
								os_str = os_str.substring(0, os_str.length() - 1);
							}
							
							
							// CPI类型，单价太低(0.06)，舍弃不入库
							if(payoutFloat != null
									&& payoutFloat < 0.06){
								
								continue;
							}
							
							
							AdvertisersOffer advertisersOffer = new AdvertisersOffer();
							
							advertisersOffer.setAdv_offer_id(String.valueOf(offerid));
							advertisersOffer.setName(name);
							
							
							//CPI类型
							advertisersOffer.setCost_type(101);
							advertisersOffer.setOffer_type(101);
							advertisersOffer.setConversion_flow(101);
							
							advertisersOffer.setAdvertisers_id(advertisersId);//网盟ID
							advertisersOffer.setPkg(pkg);
							//advertisersOffer.setpkg_size();
							advertisersOffer.setMain_icon(mainicon);
							//advertisersOffer.setPreview_url(target_url);
							advertisersOffer.setClick_url(click);
							
							advertisersOffer.setCountry(country);
							
							//advertisersOffer.setEcountry(ecountry);
							
							advertisersOffer.setDaily_cap(cap);
							//advertisersOffer.setsend_cap();
							advertisersOffer.setPayout(payoutFloat);
							//advertisersOffer.setExpiration(expriation);
							//advertisersOffer.setcreatives();
							//advertisersOffer.setSupported_carriers("");
							//advertisersOffer.setDescription(categoryStr);
							advertisersOffer.setOs(os_str);
							
							advertisersOffer.setDevice_type(0);// mobile
							//advertisersOffer.setOs_version(min_version);//系统版本要求
							advertisersOffer.setSend_type(0);//系统入库生成广告
							advertisersOffer.setIncent_type(incent);// 是否激励
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
