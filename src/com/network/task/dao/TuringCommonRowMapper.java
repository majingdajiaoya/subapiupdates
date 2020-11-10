package com.network.task.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

import com.network.task.log.NetworkLog;

public class TuringCommonRowMapper implements RowMapper<Object> {
	private static final List<String> EXCLUDE_METHODS = new ArrayList<String>();

	static {
		EXCLUDE_METHODS.add("getHibernateLazyInitializer");
		EXCLUDE_METHODS.add("getCallbacks");
		EXCLUDE_METHODS.add("getClass");
	}
	private Object obj;

	@SuppressWarnings("unchecked")
	public TuringCommonRowMapper(Class obj) {
		try {
			this.obj = obj.newInstance();
		} catch (InstantiationException e) {
			NetworkLog.log(e);
		} catch (IllegalAccessException e) {
			NetworkLog.log(e);
		}
	}

	public Object mapRow(ResultSet rs, int num) throws SQLException {
		ResultSetMetaData rm = rs.getMetaData();
		String key;
		try {
			this.obj = obj.getClass().newInstance();
		} catch (InstantiationException e) {
			NetworkLog.log(e);
			return null;
		} catch (IllegalAccessException e) {
			NetworkLog.log(e);
			return null;
		}
		Method[] methods = obj.getClass().getMethods();
		Method method;
		String methodName;
		Object valueObj;
		for (int i = 0; i < rm.getColumnCount(); i++) {
			// key = rm.getColumnName(i + 1);
			key = rm.getColumnLabel(i + 1);
			for (int j = 0; j < methods.length; j++) {
				method = methods[j];
				methodName = method.getName();
				// 只处理set方法
				if (methodName.startsWith("set") && methodName.length() > 3 && method.getParameterTypes().length == 1 && !EXCLUDE_METHODS.contains(methodName)
						&& key.equalsIgnoreCase(methodName.substring(3))) {
					key = methodName.substring(3).toLowerCase();
					valueObj = rs.getObject(key);
					if (valueObj != null) {
						// if (valueObj.getClass().equals(cls)) {
						try {
							if (!method.getParameterTypes()[0].isInstance(valueObj)) {
								try {
									valueObj = method.getParameterTypes()[0].getConstructor(String.class).newInstance(new Object[] { valueObj.toString() });
								} catch (SecurityException e) {
									NetworkLog.log(e);
									return null;
								} catch (InstantiationException e) {
									NetworkLog.log(e);
									return null;
								} catch (NoSuchMethodException e) {
									NetworkLog.log(e);
									return null;
								}
							}
							method.invoke(obj, new Object[] { valueObj });
						} catch (IllegalArgumentException e) {
							NetworkLog.log(e);
							return null;
						} catch (IllegalAccessException e) {
							NetworkLog.log(e);
							return null;
						} catch (InvocationTargetException e) {
							NetworkLog.log(e);
							return null;
						}
					}
					break;
				}
			}
		}
		return obj;
	}
}
