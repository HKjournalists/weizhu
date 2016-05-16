package com.weizhu.webapp.boss.api.discover;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListRequest;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetItemListServlet extends HttpServlet {

	private final Provider<BossHead> bossHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	
	@Inject
	public GetItemListServlet(Provider<BossHead> bossHeadProvider, AdminDiscoverService adminDiscoverService) {
		this.bossHeadProvider = bossHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		Long companyId = ParamUtil.getLong(httpRequest, "company_id", null);
		int start = ParamUtil.getInt(httpRequest, "start", 0);
		int length = ParamUtil.getInt(httpRequest, "length", 0);
		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		String itemName = ParamUtil.getString(httpRequest, "item_name", null);
		Boolean orderCreateTimeAsc = ParamUtil.getBoolean(httpRequest, "order_create_time_asc", null);
		
		final BossHead head = companyId == null ? this.bossHeadProvider.get() : this.bossHeadProvider.get().toBuilder().setCompanyId(companyId).build();
		
		GetItemListRequest.Builder requestBuilder = GetItemListRequest.newBuilder()
				.setStart(start)
				.setLength(length);
		if (categoryId != null) {
			requestBuilder.setCategoryId(categoryId);
		}
		if (itemName != null && !itemName.isEmpty()) {
			requestBuilder.setItemName(itemName);
		}
		if (orderCreateTimeAsc != null) {
			requestBuilder.setOrderCreateTimeAsc(orderCreateTimeAsc);
		}
		
		GetItemListResponse response = Futures.getUnchecked(this.adminDiscoverService.getItemList(head, requestBuilder.build()));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
