package com.network.task.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

public class OverseaMd5
{
    public static Logger log          = Logger.getLogger(OverseaMd5.class);
    private static char  mHexDigits[] = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String md5Encryp(String source)
    {
        StringBuffer sb = new StringBuffer(32);
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(source.getBytes("utf-8"));

            for (int i = 0; i < array.length; i++)
            {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                        .toUpperCase().substring(1, 3));
            }
        }
        catch (Exception e)
        {
            log.error("Can not encode the string '" + source + "' to MD5!", e);
            return null;
        }

        return sb.toString();
    }

    public static String md5(String source)
    {
        String result = null;
        try
        {
            result = DigestUtils.md5Hex(source.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            // logger.error("", e);
        }
        return result;
    }

    public static String overseaMd5(String source)
    {
        byte[] hash;

        try
        {
            hash = MessageDigest.getInstance("MD5").digest(
                    source.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte b : hash)
        {
            int i = (b & 0xFF);
            if (i < 0x10)
                hex.append('0');
            hex.append(Integer.toHexString(i));
        }

        return hex.toString();
    }
    
    public static String overseaSHA256(String source)
    {
    	MessageDigest md = null; 
        try { 
            md = MessageDigest.getInstance("SHA-256"); 
        } catch (NoSuchAlgorithmException e) { 
            e.printStackTrace(); 
        } 
        if (null != md) { 
            byte[] origBytes = source.getBytes(); 
            md.update(origBytes); 
            byte[] digestRes = md.digest(); 
            String digestStr = getDigestStr(digestRes); 
            return digestStr; 
        }
        return null; 
    }
    
    private static String getDigestStr(byte[] origBytes) { 
        String tempStr = null; 
        StringBuilder stb = new StringBuilder(); 
        for (int i = 0; i < origBytes.length; i++) { 
            // System.out.println("and by bit: " + (origBytes[i] & 0xff)); 
            // System.out.println("no and: " + origBytes[i]); 
            // System.out.println("---------------------------------------------"); 
            // 这里按位与是为了把字节转整时候取其正确的整数，java中一个int是4个字节 
            // 如果origBytes[i]最高位为1，则转为int时，int的前三个字节都被1填充了 
            tempStr = Integer.toHexString(origBytes[i] & 0xff); 
            if (tempStr.length() == 1) { 
                stb.append("0"); 
            } 
            stb.append(tempStr);

        } 
        return stb.toString(); 
    }
    
    
    public static String getFileMD5String(String filePath)
    {
        try
        {
            InputStream fis;
            fis = new FileInputStream(filePath);
            byte[] buf = new byte[1024];
            MessageDigest mMessageDigest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while ((numRead = fis.read(buf)) != -1)
            {
                mMessageDigest.update(buf, 0, numRead);
            }
            fis.close();
            return bufferToHex(mMessageDigest.digest()).toLowerCase();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    private static String bufferToHex(byte bytes[])
    {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte bytes[], int m, int n)
    {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++)
        {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringBuffer)
    {
        char c0 = mHexDigits[(bt & 0xf0) >> 4]; // 取字节中�?4 位的数字转换
        char c1 = mHexDigits[bt & 0xf]; // 取字节中�?4 位的数字转换
        stringBuffer.append(c0);
        stringBuffer.append(c1);
    }
    public static void main(String[] args) {
    String s="12JirgLt709G7Y97/97KoBEBrYaNt24813";
    String r=md5Encryp(s);
    String r1=md5(s);
    String r2=overseaMd5(s);
    System.out.println(r);
    System.out.println(r1);
    System.out.println(r);
	}
}
