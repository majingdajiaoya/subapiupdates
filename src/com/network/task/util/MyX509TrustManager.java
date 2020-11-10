package com.network.task.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MyX509TrustManager {
	private static final class DefaultTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	private static HttpsURLConnection getHttpsURLConnection(String uri,
			String method) throws IOException {
		SSLContext ctx = null;
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(new KeyManager[0],
					new TrustManager[] { new DefaultTrustManager() },
					new SecureRandom());
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		SSLSocketFactory ssf = ctx.getSocketFactory();

		URL url = new URL(uri);
		HttpsURLConnection httpsConn = (HttpsURLConnection) url
				.openConnection();
		httpsConn.setSSLSocketFactory(ssf);
		httpsConn.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded;charset=UTF-8");
		httpsConn.setRequestProperty("Authorization", "username");
		httpsConn
				.setRequestProperty(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		/*
		 * 在握手期间，如果 URL 的主机名和服务器的标识主机名不匹配， 则验证机制可以回调此接口的实现程序来确定是否应该允许此连接。
		 * 策略可以是基于证书的或依赖于其他验证方案。 当验证 URL 主机名使用的默认规则失败时使用这些回调。
		 */
		httpsConn.setHostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		});
		httpsConn.setRequestMethod(method);
		httpsConn.setDoInput(true);
		httpsConn.setDoOutput(true);
		return httpsConn;
	}

	private static byte[] getBytesFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] kb = new byte[1024];
		int len;
		while ((len = is.read(kb)) != -1) {
			baos.write(kb, 0, len);
		}
		byte[] bytes = baos.toByteArray();
		baos.close();
		is.close();
		return bytes;
	}

	private static void setBytesToStream(OutputStream os, byte[] bytes)
			throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		byte[] kb = new byte[1024];
		int len;
		while ((len = bais.read(kb)) != -1) {
			os.write(kb, 0, len);
		}
		os.flush();
		os.close();
		bais.close();
	}

	public static byte[] doGet(String uri) throws IOException {
		HttpsURLConnection httpsConn = getHttpsURLConnection(uri, "GET");
		return getBytesFromStream(httpsConn.getInputStream());
	}

	public static byte[] doPost(String uri, String data) throws IOException {
		HttpsURLConnection httpsConn = getHttpsURLConnection(uri, "POST");
		setBytesToStream(httpsConn.getOutputStream(), data.getBytes());
		return getBytesFromStream(httpsConn.getInputStream());
	}

	public static void main(String[] args) throws IOException {
		String uri = "https://item-api.aff-adeals.com/api/auth/v2?_site=361&_l=cappumedia&_p=917eee5d41f181ba018166e726ee636b4aee75e228bec403039dd007c036e003";
		byte[] bytes = MyX509TrustManager.doPost(uri,"");
		System.out.println(new String(bytes, "utf-8"));
	}
}
