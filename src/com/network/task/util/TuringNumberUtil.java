package com.network.task.util;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TuringNumberUtil {
	
	public boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	public static boolean getRom(Integer ration) {
		int max = 100;
		int min = 1;
		Random random = new Random();

		if ((random.nextInt(max) % (max - min + 1) + min) > ration) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 浮点型取位数
	 * 
	 * @param value 原值
	 * @param formatPattern "0.000"格式
	 * @return
	 */
	public static Float floatDecimalFormat(Float value, String formatPattern) {
		
		if(formatPattern == null
				|| formatPattern.trim().length() == 0){
			
			return null;
		}
		
		if(value == null){
			
			return (float) 0;
		}
		
		DecimalFormat df = new DecimalFormat(formatPattern);
		
		return Float.valueOf(df.format(value));
	}

}
