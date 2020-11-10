package com.network.task.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.util.Map;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class HttpUtil {

	public static String sendPost(String url, String content)
			throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String result = null;
		try {
			HttpPost httpost = new HttpPost(url.trim());
			httpost.setEntity(new StringEntity(content, Consts.UTF_8));
			HttpResponse hresponse;
			hresponse = httpclient.execute(httpost);
			HttpEntity entity = hresponse.getEntity();
			result = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		return result;
	}

	public static String sendGet(String url) throws ClientProtocolException,
			IOException {

		if (url == null || url.trim().length() == 0) {

			return null;
		}

		if (url.startsWith("http:")) {

			return sendGet(url, null);
		} else if (url.startsWith("https:")) {

			return sendHttpsGet(url);
		}

		return null;

	}

	public static byte[] sendGetByte(String url, Map<String, String> header)
			throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		byte[] result = null;
		ByteArrayOutputStream bo = null;
		try {
			HttpGet httpget = new HttpGet(url.trim());
			httpclient.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 100000);
			httpclient.getParams().setParameter(
					CoreConnectionPNames.SO_TIMEOUT, 100000);
			if (header != null) {
				for (String key : header.keySet()) {
					String value = header.get(key);
					httpget.setHeader(key, value);
				}
			}

			HttpResponse hresponse;
			hresponse = httpclient.execute(httpget);
			HttpEntity entity = hresponse.getEntity();
			if (entity.getContentLength() <= 0)
				return null;
			InputStream is = entity.getContent();
			bo = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int r = 0;
			while ((r = is.read(buff)) > 0) {
				bo.write(buff, 0, r);
			}
			result = bo.toByteArray();
		} finally {
			httpclient.getConnectionManager().shutdown();
			if (bo != null)
				bo.close();
		}
		return result;
	}

	public static String sendGet(String url, Map<String, String> header)
			throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		String result = null;
		try {
			HttpGet httpget = new HttpGet(url.trim());
			httpclient.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 200000);
			httpclient.getParams().setParameter(
					CoreConnectionPNames.SO_TIMEOUT, 200000);

			if (header != null) {
				for (String key : header.keySet()) {
					String value = header.get(key);
					httpget.setHeader(key, value);
				}
			}

			HttpResponse hresponse;
			hresponse = httpclient.execute(httpget);
			HttpEntity entity = hresponse.getEntity();
			result = EntityUtils.toString(entity, "UTF-8");
			EntityUtils.consume(entity);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		return result;
	}

	/**
	 * https Get 方式
	 * 
	 * @param url
	 //* @param header
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String sendHttpsGet(String url)
			throws ClientProtocolException, IOException {

		try {
			Security.addProvider(new BouncyCastleProvider());

			HttpClient httpclient = new SSLClient();

			httpclient.getParams().setParameter(
					CoreConnectionPNames.CONNECTION_TIMEOUT, 100000);
			httpclient.getParams().setParameter(
					CoreConnectionPNames.SO_TIMEOUT, 100000);
			HttpGet httpget = new HttpGet(url);
			HttpParams params = httpclient.getParams();
			params.setParameter("param1", "paramValue1");

			httpget.setParams(params);

			ResponseHandler responseHandler = new BasicResponseHandler();

			String responseBody = (String) httpclient.execute(httpget, responseHandler);
			return responseBody;
		} catch (Exception ex) {

			ex.printStackTrace();

			return null;
		}
	}

	public static String getParam(Map<String, String> param) {
		StringBuilder str = new StringBuilder();
		int size = 0;
		for (Map.Entry<String, String> m : param.entrySet()) {
			str.append(m.getKey());
			str.append("=");
			str.append(m.getValue());
			if (size < param.size() - 1) {
				str.append("&");
			}
			size++;
		}
		System.out.println(str.toString());
		return str.toString();
	}

	public static String Post(String url, String key) {
		try {

			HttpPost httppost;// 用于提交登陆数据
			DefaultHttpClient httpclient;

			httpclient = new SSLClient();
			// mis登陆界面网址
			httppost = new HttpPost(url);

			httppost.setHeader("Content-Type",
					"application/x-www-form-urlencoded");
			String post = key;
			httppost.setEntity(new StringEntity(post, "utf-8"));

			try {
				// 提交登录数据
				HttpResponse re = httpclient.execute(httppost);
				Header[] h = re.getAllHeaders();
				for (Header header : h) {
					if (header.toString().contains("Set-Cookie")) {
						String temp = header.toString();
						String[] split = temp.split(";");
						String cookies = split[0];
						cookies = cookies.replace("Set-Cookie: ", "");
						return cookies;

					}
				}
				// 获得跳转的网址
				// Header locationHeader = re.getFirstHeader("Location");

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception e) {

		}
		return null;
	}

	public static String CuttGet(String url) {

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();

			httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

				@Override
				public void process(HttpRequest arg0, HttpContext arg1)
						throws HttpException, IOException {
					// TODO Auto-generated method stub
					if (!arg0.containsHeader("Accept-Encoding")) {
						arg0.addHeader("Accept-Encoding", "gzip");
					}
				}

			});

			httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

				public void process(final HttpResponse response,
						final HttpContext context) throws HttpException,
						IOException {
					HttpEntity entity = response.getEntity();
					Header ceheader = entity.getContentEncoding();
					if (ceheader != null) {
						HeaderElement[] codecs = ceheader.getElements();
						for (int i = 0; i < codecs.length; i++) {
							if (codecs[i].getName().equalsIgnoreCase("gzip")) {
								response.setEntity(new GzipDecompressingEntity(
										response.getEntity()));
								return;
							}
						}
					}
				}

			});
			HttpGet httpget = new HttpGet(url);

			// Execute HTTP request
			// System.out.println("executing request " + httpget.getURI());
			HttpResponse response = httpclient.execute(httpget);

			// System.out.println("----------------------------------------");
			// System.out.println(response.getStatusLine());
			// System.out.println(response.getLastHeader("Content-Encoding"));
			// System.out.println(response.getLastHeader("Content-Length"));
			// System.out.println("----------------------------------------");

			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String content = EntityUtils.toString(entity);
				// System.out.println(content);
				// System.out.println("----------------------------------------");
				// System.out.println("Uncompressed size: "+content.length());
				httpclient.getConnectionManager().shutdown();

				return content;
			}

			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources

		} catch (Exception e) {
			e.printStackTrace();

		}

		return null;
	}

	public static String PostTest(String url, String key) {
		String result = null;
		try {

			HttpPost httppost;// 用于提交登陆数据
			DefaultHttpClient httpclient;

			httpclient = new SSLClient();
			httppost = new HttpPost(url);

			httppost.setHeader("Content-type", "application/json");
			httppost.setHeader(
					"Authorization",
					"Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJfRVAxWUJLc1JZeWF5WHBheDlrSVZRIiwiaWF0IjoxNTUyODk0NjkyLCJqdGkiOiJLOEp1UnNtZWJEenV4ZGhPTU9Ja1hRIn0.r5YlruNu_v70MeC8Wmxq59KJgP7zKHhywa0dKsd-E-k");
			httppost.setEntity(new StringEntity(key, "utf-8"));
			try {
				// 提交登录数据
				HttpResponse response = httpclient.execute(httppost);
				if (response != null) {
					HttpEntity resEntity = response.getEntity();
					if (resEntity != null) {
						result = EntityUtils.toString(resEntity, "utf-8");
					}
				}

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception e) {

		}
		System.out.println(result);
		return result;
	}

	public static String execCurl(String apiurl,Map<String, String> header) throws ClientProtocolException, IOException {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(apiurl);
		httppost.setHeader("Content-Type", "application/json");
		if (header != null) {
			for (String key : header.keySet()) {
				String value = header.get(key);
				httppost.setHeader(key, value);
			}
		}
		HttpResponse response;
		response = httpclient.execute(httppost);
		String result=EntityUtils.toString(response.getEntity(), "UTF-8");
		return result;
	}
}
