package com.weizhu.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Splitter;

public final class ParamUtil {
	
	public static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
	
	private ParamUtil() {
	}
	
	public static String getString(HttpServletRequest httpRequest, String param, String defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		String value = httpRequest.getParameter(param);
		return value != null ? value.trim() : defaultValue;
	}
	
	public static Integer getInt(HttpServletRequest httpRequest, String param, Integer defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String value = httpRequest.getParameter(param);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	public static Long getLong(HttpServletRequest httpRequest, String param, Long defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String value = httpRequest.getParameter(param);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	public static Boolean getBoolean(HttpServletRequest httpRequest, String param, Boolean defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String value = httpRequest.getParameter(param);
		if (value == null) {
			return defaultValue;
		} else if ("true".equalsIgnoreCase(value.trim())) {
			return true;
		} else if ("false".equalsIgnoreCase(value.trim())) {
			return false;
		} else {
			return defaultValue;
		}
	}
	
	public static List<Integer> getIntList(HttpServletRequest httpRequest, String param, List<Integer> defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String valueStr = httpRequest.getParameter(param);
		if (valueStr == null) {
			return defaultValue;
		}
		
		List<String> valueList = COMMA_SPLITTER.splitToList(valueStr);
		if (valueList == null) {
			return defaultValue;
		} else if (valueList.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Integer> list = new ArrayList<Integer>(valueList.size());
		for (String value : valueList) {
			try {
				list.add(Integer.parseInt(value.trim()));
			} catch (NumberFormatException e) {
			}
		}
		return list;
	}
	
	public static List<Long> getLongList(HttpServletRequest httpRequest, String param, List<Long> defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String valueStr = httpRequest.getParameter(param);
		if (valueStr == null) {
			return defaultValue;
		}
		
		List<String> valueList = COMMA_SPLITTER.splitToList(valueStr);
		if (valueList == null) {
			return defaultValue;
		} else if (valueList.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<Long> list = new ArrayList<Long>(valueList.size());
		for (String value : valueList) {
			try {
				list.add(Long.parseLong(value.trim()));
			} catch (NumberFormatException e) {
			}
		}
		return list;
	}

	public static List<String> getStringList(HttpServletRequest httpRequest, String param, List<String> defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String valueStr = httpRequest.getParameter(param);
		if (valueStr == null) {
			return defaultValue;
		}
		
		List<String> valueList = COMMA_SPLITTER.splitToList(valueStr);
		if (valueList == null) {
			return defaultValue;
		} else if (valueList.isEmpty()) {
			return Collections.emptyList();
		} else {
			return valueList;
		}
	}
	
	public static <T extends Enum<?>> T getEnum(HttpServletRequest httpRequest, Class<T> enumClass, String param, T defaultValue) {
		if (httpRequest == null || param == null) {
			return defaultValue;
		}
		
		String valueStr = httpRequest.getParameter(param);
		if (valueStr == null) {
			return defaultValue;
		}
		
		for (T e : enumClass.getEnumConstants()) {
			if (valueStr.equals(e.name())) {
				return e;
			}
		}
		return defaultValue;
	}
}
