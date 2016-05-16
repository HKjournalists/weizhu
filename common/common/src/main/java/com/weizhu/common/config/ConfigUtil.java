package com.weizhu.common.config;

import java.io.File;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

public class ConfigUtil {

	public static String getNotEmpty(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (value == null) {
			throw new InvalidConfigException(key, value, "value is null");
		}
		String trimValue = value.trim();
		if (trimValue.isEmpty()) {
			throw new InvalidConfigException(key, value, "value is empty");
		}
		return trimValue;
	}
	
	public static String getNullable(Properties confProperties, String key) {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		return value == null ? null : value.trim();
	}
	
	public static String getNullToEmpty(Properties confProperties, String key) {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		return value == null ? "" : value.trim();
	}
	
	public static int getInt(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (value == null) {
			throw new InvalidConfigException(key, value, "value is null");
		}
		Integer intValue = Ints.tryParse(value.trim());
		if (intValue == null) {
			throw new InvalidConfigException(key, value, "value is not integer");
		}
		return intValue;
	}
	
	public static Integer getNullOrInt(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		Integer intValue = Ints.tryParse(value.trim());
		if (intValue == null) {
			throw new InvalidConfigException(key, value, "value is not integer");
		}
		return intValue;
	}
	
	public static boolean getBoolean(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (value == null) {
			throw new InvalidConfigException(key, value, "value is null");
		}
		
		value = value.trim();
		if ("TRUE".equalsIgnoreCase(value)) {
			return true;
		} else if ("FALSE".equalsIgnoreCase(value)) {
			return false;
		} else {
			throw new InvalidConfigException(key, value, "value is not boolean");
		}
	}
	
	public static Boolean getNullOrBoolean(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		
		value = value.trim();
		if ("TRUE".equalsIgnoreCase(value)) {
			return true;
		} else if ("FALSE".equalsIgnoreCase(value)) {
			return false;
		} else {
			throw new InvalidConfigException(key, value, "value is not boolean");
		}
	}
	
	public static File getDir(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (Strings.isNullOrEmpty(value)) {
			throw new InvalidConfigException(key, value, "value is empty");
		}
		File dir = new File(value);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				return dir;
			} else {
				throw new InvalidConfigException(key, value, "is not dir");
			}
		} else {
			throw new InvalidConfigException(key, value, "dir not exist");
		}
	}
	
	public static File getDirIfNonExistCreate(Properties confProperties, String key) throws InvalidConfigException {
		Preconditions.checkNotNull(key, "key");
		String value = confProperties.getProperty(key);
		if (Strings.isNullOrEmpty(value)) {
			throw new InvalidConfigException(key, value, "value is empty");
		}
		File dir = new File(value);
		if (dir.exists()) {
			if (dir.isDirectory()) {
				return dir;
			} else {
				throw new InvalidConfigException(key, value, "is not dir");
			}
		} else {
			if (dir.mkdir()) {
				return dir;
			} else {
				throw new InvalidConfigException(key, value, "create dir fail");
			}
		}
	}
}
