package com.weizhu.common.server;

import java.io.IOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import com.weizhu.common.utils.JsonUtil;

public class LogUtil {

	private static final Logger WEIZHU_API_ACCESS_LOGGER = LoggerFactory.getLogger("weizhu_api_access");
	
	public static void logApiAccess(
			Message head, int requestBodyLength, 
			String responseResult, @Nullable String responseFailText, int responseBodyLength,
			long duration, @Nullable Throwable throwable) throws IOException {
		
		if ((throwable == null && WEIZHU_API_ACCESS_LOGGER.isInfoEnabled()) 
				|| (throwable != null && WEIZHU_API_ACCESS_LOGGER.isErrorEnabled())
				) {
			
			StringBuilder json = new StringBuilder();
			
			json.append("{\"duration\":").append(duration);
			json.append(",\"server\":\"").append(ServerConst.SERVER_NAME);
			json.append("\",\"head\":{\"type\":\"").append(head.getClass().getSimpleName()).append("\",\"data\":");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(head, json);
			json.append("},\"request\":");
			
			JsonObject requestObj = new JsonObject();
			requestObj.addProperty("body_length", requestBodyLength);
			
			JsonUtil.GSON.toJson(requestObj, json);
			
			json.append(",\"response\":");
			
			JsonObject responseObj = new JsonObject();
			responseObj.addProperty("result", responseResult);
			if (responseFailText != null) {
				responseObj.addProperty("fail_text", responseFailText);
			}
			responseObj.addProperty("body_length", responseBodyLength);
			
			JsonUtil.GSON.toJson(requestObj, json);
			
			if (throwable != null) {
				json.append(",\"exception\":");
				
				JsonObject exceptionObj = new JsonObject();
				exceptionObj.addProperty("type", throwable.getClass().getSimpleName());
				exceptionObj.addProperty("message", throwable.getMessage());
				exceptionObj.addProperty("stack_strace", Throwables.getStackTraceAsString(throwable));
				
				JsonUtil.GSON.toJson(exceptionObj, json);
			}
			
			json.append("}");
			
			if (throwable == null) {
				WEIZHU_API_ACCESS_LOGGER.info(json.toString());
			} else {
				WEIZHU_API_ACCESS_LOGGER.error(json.toString());
			}
		}
	}
	
	
}
