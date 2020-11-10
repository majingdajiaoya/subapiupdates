package com.network.task.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;



public class Base64Utils {

	
	/**
	 * 仅仅BASE64
	 * 
	 * @param str
	 * @return
	 */
	public static String StringToBase64(String str){
		
		try {
			
			if(str != null
					&& str.length() > 0){
			
		        byte[] enbytes = Base64.encodeBase64Chunked(str.getBytes("UTF-8"));
		        
		        String encodeStr = new String(enbytes);
		        
		        return encodeStr;
			}
			
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
		}
        
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String str = "var objs = document.getElementsByTagName(\"img\");";
		String base64Str = StringToBase64(str);
	}

}
