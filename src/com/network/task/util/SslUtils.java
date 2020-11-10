package com.network.task.util;

import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

public class SslUtils {
	private static void trustAllHttpsCertificates() throws Exception {
		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager tm = new miTM();
		trustAllCerts[0] = tm;
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	static class miTM implements TrustManager, X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			return;
		}
	}

	/**
	 * 忽略HTTPS请求的SSL证书，必须在openConnection之前调用
	 * 
	 * @throws Exception
	 */
	public static void ignoreSsl() throws Exception {
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				System.out.println("Warning: URL Host: " + urlHostName + " vs. "
						+ session.getPeerHost());
				return true;
			}
		};
		trustAllHttpsCertificates();
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

	public  String getRequest(String url, int timeOut, String header) throws Exception {
		URL u = new URL(url);
		if ("https".equalsIgnoreCase(u.getProtocol())) {
			SslUtils.ignoreSsl();
		}
		URLConnection conn = u.openConnection();
		conn.setConnectTimeout(timeOut);
		conn.setReadTimeout(timeOut);
		conn.setRequestProperty("Authorization", header);
		return IOUtils.toString(conn.getInputStream());
	}
	
	
	public String getAppierRequest(String url, int timeOut) throws Exception {
		URL u = new URL(url);
		if ("https".equalsIgnoreCase(u.getProtocol())) {
			SslUtils.ignoreSsl();
		}
		URLConnection conn = u.openConnection();
		conn.setConnectTimeout(timeOut);
		conn.setReadTimeout(timeOut);
		conn.setRequestProperty("Authorization", "1h7lm1Jog3KVQjV2Ffg90VE84E-mhOO32YLWBfSlgfbPD1AliiYyawmQgNDQvdiJO_mRXYHlysd9sW2-h6WehkKfGUKgcTeSgfu1wAegn7_btCzxvI3KKagDX7iSW7Qo1LQb8nJZYNv2y9a8Tt9qsjSZRVPq5gNHoNtXh1CvDD0");
		return IOUtils.toString(conn.getInputStream());
	}

	public static String postRequest(String urlAddress, String args, int timeOut) throws Exception {
		URL url = new URL(urlAddress);
		if ("https".equalsIgnoreCase(url.getProtocol())) {
			SslUtils.ignoreSsl();
		}
		URLConnection u = url.openConnection();
		u.setDoInput(true);
		u.setDoOutput(true);
		u.setConnectTimeout(timeOut);
		u.setReadTimeout(timeOut);
		OutputStreamWriter osw = new OutputStreamWriter(u.getOutputStream(), "UTF-8");
		osw.write(args);
		osw.flush();
		osw.close();
		u.getOutputStream();
		return IOUtils.toString(u.getInputStream());
	}

	public static void main(String[] args) throws Exception {
		String posturl = "https://lemmonetapi.azurewebsites.net/myoffers/";
		String clientid = "Cappu42345dhy";
		String apikey = "EB4975DADDDB40FC977A8E57EFA55C09";
		String creatives = "true";
		String offerID = "";
		String postData = "clientid=" + clientid + "&apikey=" + apikey + "&creatives=" + creatives
				+ "&offerID=" + offerID;
		System.out.println(postData);
		System.out.println(postRequest(posturl, postData, 30000));
	}
}
