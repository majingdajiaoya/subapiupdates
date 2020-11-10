package com.network.task.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class OverseaDateUtil
{
    // public static java.sql.Date JavaDateToSqlDate(java.util.Date date)
    // {
    // return new java.sql.Date(date.getTime());
    // }

    public static java.util.Date SqlDateToJavaDate(java.sql.Date date)
    {
        return new java.util.Date(date.getTime());
    }

    public static Timestamp JavaDateToTimestamp(java.util.Date date)
    {
        return new Timestamp(date.getTime());
    }

    static DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
    static DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:MM:SS");

    public static Date StringToDate(String str, String hour)
    {
        Date date = null;
        try
        {
            Calendar c = Calendar.getInstance();
            date = format1.parse(str);
            int h = Integer.parseInt(hour);
            c.setTime(date);
            c.add(Calendar.HOUR_OF_DAY, h);
            date = c.getTime();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return date;
    }

    public static Date StringToDate(String str)
    {
        Date date = null;
        try
        {
            date = format1.parse(str);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return date;
    }
    /**
     * 把字符串时间转换为date类型
     * @param formate "yyyy-MM-dd HH:mm:ss"
     * @param time  "2015-08-12 12:12:12"
     * @return date 
     */
    public static Date getDateFromString(String formate,String time) {
	    Date date = null;
		DateFormat format1 = new SimpleDateFormat(formate);
        try
        {
            date = format1.parse(time);
            System.out.println(date);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return date;
	}

    public static String TimeToString(Long time)
    {
        return format1.format(new Date(time));
    }

    public static Date addTime(Date date, int day, int hour, int minute) // 今天的零点钟
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, day);
        calendar.add(Calendar.HOUR_OF_DAY, hour);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    public static long startTime() // 今天的零点钟
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTime().getTime();
        return start;
    }

    public static long endTime() // 明天的零点钟
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long time = calendar.getTime().getTime();
        return time;
    }

    public static String NgsteamToString(Date date)
    {
        return NgsteamToString(date, "yyyy-MM-dd");
    }

    public static String NgsteamToString(Date date, String format)
    {
        SimpleDateFormat formater = new SimpleDateFormat(format);
        return formater.format(date);
    }
}
