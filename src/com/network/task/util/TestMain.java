package com.network.task.util;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

//对接口进行测试  
public class TestMain {
	private String url = "https://lemmonetapi.azurewebsites.net/myoffers/";
	private String charset = "utf-8";
	private HttpClientUtil httpClientUtil = null;

	public TestMain() {
		httpClientUtil = new HttpClientUtil();
	}

	public void test() {
		String httpOrgCreateTest = url;
		Map<String, String> createMap = new HashMap<String, String>();
		createMap.put("clientid", "1Cappu42345dhy");
		createMap.put("apikey", "EB4975DADDDB40FC977A8E57EFA55C09");
		String httpOrgCreateTestRtn = httpClientUtil.doPost(httpOrgCreateTest, createMap, charset);
		JSONObject json = JSON.parseObject(httpOrgCreateTestRtn);
		System.out.println(json);
	}

	public static void main(String[] args) {
		TestMain main = new TestMain();
		main.test();
	}
}