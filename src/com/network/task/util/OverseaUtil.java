package com.network.task.util;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class OverseaUtil
{
    private static final List<String>    EXCLUDE_METHODS = new ArrayList<String>();

    static
    {
        EXCLUDE_METHODS.add("getHibernateLazyInitializer");
        EXCLUDE_METHODS.add("getCallbacks");
        EXCLUDE_METHODS.add("getClass");
    }

    public static final SimpleDateFormat sdf1            = new SimpleDateFormat(
                                                                 "yyyyMMdd");
    public static final SimpleDateFormat sdf2            = new SimpleDateFormat(
                                                                 "yyyyMMddHHmmss");
    public static final SimpleDateFormat sdf3            = new SimpleDateFormat(
                                                                 "yyyy年MM月dd日");
    public static final SimpleDateFormat sdf4            = new SimpleDateFormat(
                                                                 "yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat sdf5            = new SimpleDateFormat(
                                                                 "yyyy");

    public static final SimpleDateFormat sdf6            = new SimpleDateFormat(
                                                                 "MM/dd");
    public static final SimpleDateFormat sdf7            = new SimpleDateFormat(
                                                                 "yyyy.MM.dd");

    private static SimpleDateFormat getSdf(int type)
    {
        switch (type)
        {
            case 1:
                return sdf1;
            case 2:
                return sdf2;
            case 3:
                return sdf3;
            case 4:
                return sdf4;
            case 5:
                return sdf5;
            case 6:
                return sdf6;
            case 7:
                return sdf7;
            default:
                return sdf1;
        }
    }

    public static int random(int low, int high)
    {
        return (int) (Math.random() * (high - low)) + low;
    }

    public static int parseVersion(String version)
    {
        int ret = 0;
        try
        {
            String[] strs = version.split("\\.");
            ret = Integer.parseInt(strs[0]) * 1000;
            if (strs.length > 1)
            {
                ret += Integer.parseInt(strs[1]) * 100;
            }
            if (strs.length > 2)
            {
                ret += +Integer.parseInt(strs[2]);
            }
        }
        catch (Exception e)
        {

        }
        return ret;
    }

    public static long getUrlFileSize(String address)
    {
        try
        {
            URL url = new URL(address);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            long len = urlConnection.getContentLength();
            if (len <= 0)
            {
                return 0;
            }
            else
            {
                return len;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return 0;
        }
    }

    // 字节换算
    private static final int MB = 1024 * 1024; // 定义MB的计算常量

    public static double ByteConversionMB(long KSize)
    {
        return Math.round(KSize / (float) MB); // 将其转换成MB

    }

    public static long genPushCmdId()
    {
        String id = new Date().getTime() + "" + random(0, 100);
        long cmdId = Long.parseLong(id);
        return cmdId;
    }

    /**
     * 检查IP地址格式
     * 
     * @param ip
     * @return
     */
    public static boolean IsIP(String ip)
    {
        Pattern pattern = Pattern
                .compile("^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$");
        Matcher matcher = pattern.matcher(ip);
        boolean b = matcher.matches();
        return b;
        // return System.Text.RegularExpressions.Regex.IsMatch(ip,
        // "^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$");
    }

    /**
     * 获得当前星期一 数字形式
     * 
     * @param ip
     * @return
     */
    public static String weekstr()
    {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        int day_of_week = c.get(Calendar.DAY_OF_WEEK);
        return String.valueOf(day_of_week);
    }

    public static Map<String, Object> modelToMap(final Object modelObj)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Method[] methods = modelObj.getClass().getMethods();
        Method method;
        for (int i = 0; i < methods.length; i++)
        {
            method = methods[i];
            fillFieldToMap(method, modelObj, map);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static void fillFieldToMap(final Method m, final Object obj,
            Map<String, Object> result)
    {
        String methodName = m.getName();
        try
        {
            if (methodName.startsWith("get") && methodName.length() > 3
                    && m.getParameterTypes().length == 0
                    && !EXCLUDE_METHODS.contains(methodName))
            {
                String key = String.valueOf(Character.toLowerCase(methodName
                        .charAt(3)));
                if (methodName.length() > 4)
                {
                    key += methodName.substring(4);
                }
                boolean isset = true;
                if (isset)
                {
                    Object valueObj = m.invoke(obj, new Object[] {});
                    if (valueObj != null)
                    {
                        String value = null;
                        if (valueObj instanceof Date)
                        {
                            value = sdf4.format((Date) valueObj);
                            result.put(key, value);
                        }
                        else if (valueObj instanceof List)
                        {
                            List<Map<String, Object>> retlist = new ArrayList<Map<String, Object>>();
                            List list = (List) valueObj;
                            for (int i = 0, size = list.size(); i < size; i++)
                            {
                                retlist.add(modelToMap(list.get(i)));
                            }
                            result.put(key, retlist);
                        }
                        else if (valueObj instanceof Boolean)
                        {
                            // boolean返回给前台是统一为1 true,0 false
                            if ((Boolean) valueObj)
                            {
                                value = "1";
                            }
                            else
                            {
                                value = "0";
                            }
                            result.put(key, value);
                        }
                        else
                        {
                            value = valueObj.toString();
                            result.put(key, value);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定格式的日期
     * 
     * @type 日期格式 1 yyyyMMdd 2yyyyMMddHHmmss 3yyyy年MM月dd日 4yyyy-MM-dd HH:mm:ss
     */
    public static String getDateFormatString(Date date, int type)
    {
        if (date == null)
        {
            return "";
        }
        SimpleDateFormat sdf = getSdf(type);
        try
        {
            return sdf.format(date);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static int[] NgsteamIntegerToInt(List<Integer> list)
    {
        if (list != null)
        {
            int[] ret = new int[list.size()];
            int index = 0;
            for (Integer i : list)
            {
                ret[index] = i;
                index++;
            }
            return ret;
        }
        return null;
    }

    public static String getIpAddr(HttpServletRequest request)
    {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
