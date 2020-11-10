package com.network.task.util;

import java.util.Comparator;
import java.util.Map;

public class ComparatorObject implements Comparator {

	public int compare(Object arg0, Object arg1) {
		Map<String, Object> user0 = (Map<String, Object>) arg0;
		Map<String, Object> user1 = (Map<String, Object>) arg1;

		int flag = (String.valueOf(user1.get("oninstall"))).compareTo(String
				.valueOf(user0.get("oninstall")));
		return flag;
	}

}
