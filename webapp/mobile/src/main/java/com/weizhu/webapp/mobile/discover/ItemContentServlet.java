package com.weizhu.webapp.mobile.discover;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.DiscoverProtos;
import com.weizhu.proto.DiscoverService;
import com.weizhu.proto.DiscoverProtos.GetItemContentRequest;
import com.weizhu.proto.DiscoverProtos.GetItemContentResponse;
import com.weizhu.proto.DiscoverProtos.GetItemPVRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ItemContentServlet extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger("weizhu_discover_item");
	
	private final Provider<RequestHead> requestHeadProvider;
	private final DiscoverService discoverService;
	
	@Inject
	public ItemContentServlet(Provider<RequestHead> requestHeadProvider, DiscoverService discoverService) {
		this.requestHeadProvider = requestHeadProvider;
		this.discoverService = discoverService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		Long itemId = ParamUtil.getLong(httpRequest, "item_id", null);
		if (itemId == null) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "item_id is empty");
			return;
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		logger.info("|" + head.getSession().getCompanyId() + "|" + head.getSession().getUserId() + "|" + itemId + "|");
		
		GetItemContentRequest request = GetItemContentRequest.newBuilder()
				.setItemId(itemId)
				.build();

		GetItemContentResponse response = Futures.getUnchecked(this.discoverService.getItemContent(head, request));
		
		if (!response.hasItemContent()) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "item not found");
			return;
		}
		
		DiscoverProtos.ItemContent itemContent = response.getItemContent();
		
		discoverService.getItemPV(head, 
				GetItemPVRequest.newBuilder()
				.setItemId(itemId)
				.setIsIncrePv(true)
				.build());
		
		switch (itemContent.getContentTypeCase()) {
			case REDIRECT_URL:
				String url = itemContent.getRedirectUrl();
				if (url.contains("${company_id}")) {
					url = url.replace("${company_id}", String.valueOf(head.getSession().getCompanyId()));
				}
				if (url.contains("${user_id}")) {
					url = url.replace("${user_id}", String.valueOf(head.getSession().getUserId()));
				}
				if (url.contains("${short_user_id}")) {
					String v = String.valueOf(head.getSession().getUserId());
					if (v.length() > 9) {
						v = v.substring(v.length() - 9);
					}
					url = url.replace("${short_user_id}", v);
				}
				httpResponse.sendRedirect(url);
				return;
			case EXAM_ID:
				httpRequest.setAttribute("exam_id", itemContent.getExamId());
				httpRequest.getRequestDispatcher("/exam/exam_info").forward(httpRequest, httpResponse);
				return;
			case CONTENTTYPE_NOT_SET:
			default:
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "item content not found");
				return;
		}
	}
	
}
