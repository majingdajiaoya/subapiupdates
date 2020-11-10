package com.network.task.util;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

/* 
 * 利用HttpClient进行post请求的工具类 
 */
public class HttpClientUtil {
	public static  String doPost(String url, Map<String, String> map, String charset) {
		HttpClient httpClient = null;
		HttpPost httpPost = null;
		String result = null;
		try {
			httpClient = new SSLClient();
			httpPost = new HttpPost(url);
			// 设置参数
			// {
			// "clientid": "Cappu42345dhy",
			// "apikey": "EB4975DADDDB40FC977A8E57EFA55C09"
			//
			// }
			httpPost.setEntity(new StringEntity("{\"clientid\": \"Cappu42345dhy\",\"apikey\": \"EB4975DADDDB40FC977A8E57EFA55C09\"}"));
			HttpResponse response = httpClient.execute(httpPost);
			result = response.toString();
			if (response != null) {
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					result = EntityUtils.toString(resEntity, charset);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
}