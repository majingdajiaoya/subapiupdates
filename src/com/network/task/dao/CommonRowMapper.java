package com.network.task.dao;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;

public class CommonRowMapper<T> implements RowMapper<T> {

	private static final Set<String> EXCLUDE_METHODS = new HashSet<String>();

	private Class<T> clazz;
	
	static {
		EXCLUDE_METHODS.add("getHibernateLazyInitializer");
		EXCLUDE_METHODS.add("getCallbacks");
		EXCLUDE_METHODS.add("getClass");
	}
	
	public CommonRowMapper(){
	}
	
	public CommonRowMapper(Class<T> clazz){
		this.clazz = clazz;
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet rs, int rowNum) {
		T t = null;
		try {
			ResultSetMetaData rm = rs.getMetaData();
			String key;
			String className = clazz.getName();
			t = (T) Class.forName(className).newInstance();
			Method[] methods = t.getClass().getMethods();
			Method method;
			String methodName;
			Object valueObj;
			for (int i = 0; i < rm.getColumnCount(); i++) {
				key = rm.getColumnLabel(i + 1);
				for (int j = 0; j < methods.length; j++) {
					method = methods[j];
					methodName = method.getName();
					// 只处理set方法
					if (methodName.startsWith("set") && methodName.length() > 3
							&& method.getParameterTypes().length == 1
							&& !EXCLUDE_METHODS.contains(methodName)
							&& key.equalsIgnoreCase(methodName.substring(3))) {
						key = methodName.substring(3).toLowerCase();
						valueObj = rs.getObject(key);
						if (valueObj != null) {
							if (!method.getParameterTypes()[0]
									.isInstance(valueObj)) {
								valueObj = method.getParameterTypes()[0]
										.getConstructor(String.class)
										.newInstance(
												new Object[] { valueObj
														.toString() });
							}
							method.invoke(t, new Object[] { valueObj });
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return t;
	}
}