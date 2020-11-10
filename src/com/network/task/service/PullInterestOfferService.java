package com.network.task.service;

import java.util.Date;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.network.task.bean.Advertisers;
import com.network.task.log.NetworkLog;
import com.network.task.service.base.PullOfferServiceTask;
import com.network.task.util.HttpUtil;
import com.network.task.util.OverseaStringUtil;
import common.Logger;

public class PullInterestOfferService implements PullOfferServiceTask{
	
	protected static Logger logger = Logger.getLogger(PullInterestOfferService.class);
	
	private Advertisers advertisers;
	
	public PullInterestOfferService(Advertisers advertisers){
		
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
			
			logger.info("doPull Zoomys Offer begin := " + new Date());
			
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
				
				String status = jsonPullObject.getString("result");
				Integer recordsTotal = jsonPullObject.getInteger("offer_total");
//				Integer recordsFiltered = jsonPullObject.getInteger("recordsFiltered");
				Integer pagecount = jsonPullObject.getInteger("offer_count");
				
//				Integer count = jsonPullObject.getInteger("total_count");
				
				if(!"success".equals(status)){
					
					logger.info("status is not ok return. status := " + status);
					
					return;
				}
				
				logger.info("recordsTotal:= " + recordsTotal+"     recordsFiltered:"+pagecount);
				if(pagecount == null
						|| pagecount == 0){
					
					logger.info("available or  count is empty. " + " recordsFiltered:=" + pagecount);
					
					return;
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
		//http://api.clinkad.com/offer_api_v3?key=tizv1j8w9u7ykjck&pubid=6393&pagesize=200&type=CPI
		tmp.setApiurl("http://api.clinkad.com/offer_api_v3?{apikey}&pagesize=200&type=CPI");
		tmp.setApikey("key=tizv1j8w9u7ykjck&pubid=6393");
		tmp.setId(21L);
		
		PullInterestOfferService mmm=new PullInterestOfferService(tmp);
		mmm.run();
	}
	
}
