package com.network.task.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
	// 默认显示日期的格式
	public static final String DATAFORMAT_STR = "yyyy-MM-dd";
	//日期短格式
	public static final String DATAFORMAT_SHORT_STR = "yyyyMMdd";

	// 默认显示日期的格式
	public static final String YYYY_MM_DATAFORMAT_STR = "yyyy-MM";

	// 默认显示日期时间的格式
	public static final String DATATIMEF_STR = "yyyy-MM-dd HH:mm:ss";
	// 默认显示日期时间的短格式
	public static final String DATATIMEF_SHORT_STR = "yyyyMMddHHmmss";
	
	public static final String DATATIMEF2_STR = "yyyy-MM-dd HH:mm";
	// 默认显示简体中文日期的格式
	public static final String ZHCN_DATAFORMAT_STR = "yyyy年MM月dd日";

	// 默认显示简体中文日期时间的格式
	public static final String ZHCN_DATATIMEF_STR = "yyyy年MM月dd日HH时mm分ss秒";

	// 默认显示简体中文日期时间的格式
	public static final String ZHCN_DATATIMEF_STR_4yMMddHHmm = "yyyy年MM月dd日HH时mm分";

	private static DateFormat dateFormat = null;

	private static DateFormat dateTimeFormat = null;

	private static DateFormat zhcnDateFormat = null;

	private static DateFormat dateTimeNOsFormat = null;
	
	private static DateFormat zhcnDateTimeFormat = null;
	
	private static DateFormat dateFormatShort = null;
	
	private static DateFormat dateTimeFormatShort = null;
	
	static DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
	
	static {
		dateFormat = new SimpleDateFormat(DATAFORMAT_STR);
		dateTimeFormat = new SimpleDateFormat(DATATIMEF_STR);
		dateTimeNOsFormat = new SimpleDateFormat(DATATIMEF2_STR);
		zhcnDateFormat = new SimpleDateFormat(ZHCN_DATAFORMAT_STR);
		zhcnDateTimeFormat = new SimpleDateFormat(ZHCN_DATATIMEF_STR);
		dateFormatShort = new SimpleDateFormat(DATAFORMAT_SHORT_STR);
		dateTimeFormatShort = new SimpleDateFormat(DATATIMEF_SHORT_STR);
	}

	/**
	 * 当前date转换成指定格式的字符串
	 * @param dateFormatStr
	 * @return
	 */
	public static String getNowDataStr(String dateFormatStr){
		SimpleDateFormat sf = new SimpleDateFormat(dateFormatStr);
		return sf.format(new Date());
	}
	
	/**
	 * 当前date转换成指定格式date
	 * @param dateFormatStr
	 * @return
	 */
	public static Date getNowData(String dateFormatStr){
		Date now = null;
		try{
			SimpleDateFormat sf = new SimpleDateFormat(dateFormatStr);
			String nowStr = sf.format(new Date());
			now = sf.parse(nowStr);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return now;
	}
	/**
	 * 获取Date中的分钟
	 * 
	 * @param d
	 * @return
	 */
	public static int getMin(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.MINUTE);
	}

	/**
	 * 获取Date中的小时(24小时)
	 * 
	 * @param d
	 * @return
	 */
	public static int getHour(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 获取Date中的秒
	 * 
	 * @param d
	 * @return
	 */
	public static int getSecond(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.SECOND);
	}

	/**
	 * 获取星期×的日
	 * 
	 * @param d
	 * @return
	 */
	public static int getWeekDay(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 获取xxxx-xx-xx的日
	 * 
	 * @param d
	 * @return
	 */
	public static int getDay(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 获取月份，1-12月
	 * 
	 * @param d
	 * @return
	 */
	public static int getMonth(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.MONTH) + 1;
	}

	/**
	 * 获取19xx,20xx形式的年
	 * 
	 * @param d
	 * @return
	 */
	public static int getYear(Date d) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		return now.get(Calendar.YEAR);
	}

	/**
	 * 获取日期d的days天后的一个Date
	 * 
	 * @param d
	 * @param days
	 * @return
	 */
	public static Date getInternalDateByDay(Date d, int days) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		now.add(Calendar.DATE, days);
		return now.getTime();
	}

	/**
	 * 获取n分钟后的一个Date
	 * 
	 * @param d
	 * @param minutes
	 * @return
	 */
	public static Date getInternalDateByMintues(Date d, int minutes) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		now.add(Calendar.MINUTE, minutes);
		return now.getTime();
	}

	/**
	 * 获取s秒后的一个Date
	 * 
	 * @param d
	 * @param days
	 * @return
	 */
	public static Date getInternalDateBySecond(Date d, int seconds) {
		Calendar now = Calendar.getInstance(TimeZone.getDefault());
		now.setTime(d);
		now.add(Calendar.SECOND, seconds);
		return now.getTime();
	}

	/**
	 * 判断是否是同一天
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean isSameDay(Date d1, Date d2) {
		if (d1 == null || d2 == null) {
			return false;
		}
		if (getYear(d1) == getYear(d2) && getMonth(d1) == getMonth(d2)
				&& getDay(d1) == getDay(d2)) {
			return true;
		}
		return false;
	}

	/**
	 * 是否是昨天
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isYesterday(Date a) {

		Calendar c = Calendar.getInstance();
		c.set(Calendar.DATE, c.get(Calendar.DATE) - 1);
		Date today = c.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		return format.format(today).equals(format.format(a));
	}

	/**
	 * 向后推一天
	 * 
	 * @param d
	 * @param day
	 * @return
	 */
	public static Date getDateAfter(Date d, int day) {
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
		return now.getTime();
	}

	/**
	 * 是否是今天
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isToday(Date a) {

		Calendar c = Calendar.getInstance();
		Date today = c.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		return format.format(today).equals(format.format(a));

	}
	
	public static Date StringToDate(String str) {
		
		if(str == null
				|| str.length() == 0){
			
			return null;
		}
		
		Date date = null;
		
		try {
			date = format1.parse(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * 获取当天YYYY-mm-DD的时间
	 * 
	 * @return
	 */
	public static Date getNowDay() {
		String nowDay = dateFormat.format(new Date());
		try {
			return dateFormat.parse(nowDay);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 获取格式化后的时间（yyyy-MM-dd hh:MM:ss）
	 * 
	 * @param args
	 */
	public static String getDateString(Date d) {
		String str = dateTimeFormat.format(d);
		return str;
	}
	/**
	 * 获取格式化后的时间（yyyy-MM-dd）
	 * @param d
	 * @return
	 */
	public static String getDateStringNoTime(Date d) {
		String str = dateFormat.format(d);
		return str;
	}
	/**
	 * 获取格式化后的时间（yyyy-MM-dd hh:MM）
	 * @param d
	 * @return
	 */
	public static String getDateStringNoS(Date d) {
		String str = dateTimeNOsFormat.format(d);
		return str;
	}
	
	/**
	 * 字符串转化成Date类型
	 * 
	 * @param date
	 * @return
	 */
	public static Date getDateByString(String date) {
		Date bfd = null;
		try {
			bfd = dateTimeFormat.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return bfd;
	}

	/**
	 * 字符串转化成日期Date（yyyy-MM-dd）
	 * 
	 * @return
	 */
	public static Date getDateFormatByString(String date) {
		Date bfd = null;
		try {
			bfd = dateFormat.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return bfd;
	}

	/**
	 * 获取格式化后的当前时间（yyyy-MM-dd hh:MM:ss）
	 * 
	 * @param args
	 */
	public static Date getDayTime(String str) {

		String backOrForwardDay = dateFormat.format(new Date());
		backOrForwardDay += str;
		Date bfd = null;
		try {
			bfd = dateTimeFormat.parse(backOrForwardDay);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return bfd;
	}

	/**
	 * 计算两个日期(yyyy-MM-dd)之间差几天
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static int getDiffDay(Date start, Date end) {
		Date startDate = getDateFormatByString(dateFormat.format(start));
		Date endDate = getDateFormatByString(dateFormat.format(end));
		return (int) ((endDate.getTime() - startDate.getTime()) / (60 * 60 * 24 * 1000));
	}
	/**
	 * 比较第一个日期是否比第二个日期早
	 * @param start  第一个日期字符串
	 * @param end    第二个日期字符串
	 * @return   当第一个日期 比第二个日期早时 返回true
	 */
	public static boolean compareDate(String start,String end){
		Date startDate = getDateFormatByString(start);
		Date endDate  = getDateFormatByString(end);
		return endDate.getTime() - startDate.getTime()>0;
	}
	/**
	 * 获取格式化后的date时间(yyyy-MM-dd )加时间
	 * 
	 * @param date
	 * @param str
	 * @return
	 */
	public static Date getDayTime(Date date, String str) {
		String backOrForwardDay = dateFormat.format(date);
		backOrForwardDay += " " + str;
		Date bfd = null;
		try {
			bfd = dateTimeFormat.parse(backOrForwardDay);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return bfd;
	}
	
	public static void main(String[] args){
		System.out.println(getNowDataStr("yyyyMMdd"));
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

}
