package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Provider;
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ResponseType;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@SuppressWarnings("serial")
public class JsonProxyServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;
	private final ServiceInvoker serviceInvoker;
	private final String functionName;
	private final Message requestDefaultInstance;
	private final Message responseDefaultInstance;
	
	public JsonProxyServlet(Provider<AdminHead> adminHeadProvider, ServiceInvoker serviceInvoker, Class<?> serviceApi, String functionName) {
		this.adminHeadProvider = adminHeadProvider;
		this.serviceInvoker = serviceInvoker;
		this.functionName = functionName;
		
		Method funcMethod = null;
		for (Method method : serviceApi.getMethods()) {
			if (method.getName().equals(functionName)
					&& method.getParameterTypes().length == 2 
					&& method.getParameterTypes()[0] == AdminHead.class
					&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {
				funcMethod = method;
				break;
			}
		}
		
		if (funcMethod == null) {
			throw new RuntimeException("cannot find service function : " + serviceApi.getName() + "." + functionName);
		}
		
		try {
			this.requestDefaultInstance = (Message) funcMethod.getParameterTypes()[1].getMethod("getDefaultInstance").invoke(null);
			this.responseDefaultInstance = (Message) funcMethod.getAnnotation(ResponseType.class).value().getMethod("getDefaultInstance").invoke(null);
		} catch (Exception e) {
			throw new RuntimeException("cannot get request / response defaultInstance", e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		String requestStr = ParamUtil.getString(httpRequest, "request", "{}");

		final AdminHead head = this.adminHeadProvider.get();
		final Message request;
		
		try {
			Message.Builder requestBuilder = this.requestDefaultInstance.newBuilderForType();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(requestStr, ExtensionRegistry.getEmptyRegistry(), requestBuilder); // ParseException
			request = requestBuilder.build();
		} catch (ParseException e) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_REQUEST_JSON_INVALID");
			resultObj.addProperty("fail_text", e.getMessage());
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return;
		} catch (UninitializedMessageException e) {
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("result", "FAIL_REQUEST_MSG_UNINIT");
			resultObj.addProperty("fail_text", e.getMessage());
			
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(resultObj, httpResponse.getWriter());
			return;
		}
		
		final ByteString responseByteString = Futures.getUnchecked(this.serviceInvoker.invoke(this.functionName, head, request.toByteString()));
		
		Message response = this.responseDefaultInstance.getParserForType().parseFrom(responseByteString);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
