package com.weizhu.web;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import com.weizhu.common.server.ServerConst;
import com.weizhu.common.utils.JsonUtil;

public class LogUtil {
	
	private static final Logger WEIZHU_WEBAPP_ACCESS_LOGGER = LoggerFactory.getLogger("weizhu_webapp_access");
	
	public static void logWebappAccess(
			HttpServletRequest httpRequest, 
			HttpServletResponse httpResponse, 
			Message head, long duration, @Nullable Throwable throwable
			) throws IOException {
		
		if ((throwable == null && WEIZHU_WEBAPP_ACCESS_LOGGER.isInfoEnabled()) 
			|| (throwable != null && WEIZHU_WEBAPP_ACCESS_LOGGER.isErrorEnabled())
			) {
			
			StringBuilder json = new StringBuilder();
			
			json.append("{\"duration\":").append(duration);
			json.append(",\"server\":\"").append(ServerConst.SERVER_NAME);
			json.append("\",\"head\":{\"type\":\"").append(head.getClass().getSimpleName()).append("\",\"data\":");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(head, json);
			json.append("},\"request\":");
			
			JsonObject requestObj = new JsonObject();
			requestObj.addProperty("content_length", httpRequest.getContentLengthLong());
			requestObj.addProperty("method", httpRequest.getMethod());
			requestObj.addProperty("request_uri", httpRequest.getRequestURI());
			requestObj.addProperty("query_str", httpRequest.getQueryString());
			
			JsonUtil.GSON.toJson(requestObj, json);
			
			json.append(",\"response\":{\"status\":").append(httpResponse.getStatus()).append("}");
			
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
				WEIZHU_WEBAPP_ACCESS_LOGGER.info(json.toString());
			} else {
				WEIZHU_WEBAPP_ACCESS_LOGGER.error(json.toString());
			}
		}
	}

}
