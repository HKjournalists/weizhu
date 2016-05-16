package com.weizhu.common.utils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.protobuf.format.JsonFormat;

public class JsonUtil {

	public static final Gson GSON = new Gson();
	
	public static final JsonParser JSON_PARSER = new JsonParser();
	
	public static final JsonFormat PROTOBUF_JSON_FORMAT = new JsonFormat();
	
	static {
		PROTOBUF_JSON_FORMAT.setDefaultCharset(Charsets.UTF_8);
	}
	
	public static JsonObject tryGetObject(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetObject(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
	}
	
	public static JsonArray tryGetArray(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetArray(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
		}
	}
	
	public static String tryGetString(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetString(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
		}
	}
	
	public static Long tryGetLong(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetLong(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			try {
				return e != null && e.isJsonPrimitive() ? e.getAsLong() : null;
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}
	
	public static Integer tryGetInt(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetInt(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			try {
				return e != null && e.isJsonPrimitive() ? e.getAsInt() : null;
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}
	
	public static Double tryGetDouble(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetDouble(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			try {
				return e != null && e.isJsonPrimitive() ? e.getAsDouble() : null;
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}
	
	public static Float tryGetFloat(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetFloat(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			try {
				return e != null && e.isJsonPrimitive() ? e.getAsFloat() : null;
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}
	
	public static Boolean tryGetBoolean(JsonElement element, String field) {
		if (!element.isJsonObject()) {
			return null;
		}
		
		final JsonObject obj = element.getAsJsonObject();
		final int idx = field.indexOf('.');
		if (idx >= 0) {
			JsonElement e = obj.get(field.substring(0, idx));
			return e != null ? tryGetBoolean(e, field.substring(idx + 1)) : null;
		} else {
			JsonElement e = obj.get(field);
			return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : null;
		}
	}
	
}
