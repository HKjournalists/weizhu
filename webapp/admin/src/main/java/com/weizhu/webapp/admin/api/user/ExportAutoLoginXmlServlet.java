package com.weizhu.webapp.admin.api.user;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;

@Singleton
@SuppressWarnings("serial")
public class ExportAutoLoginXmlServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	@Inject
	public ExportAutoLoginXmlServlet(
			Provider<AdminHead> adminHeadProvider, 
			AdminUserService adminUserService
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		final AdminHead head = adminHeadProvider.get();
		final GetUserListRequest request = GetUserListRequest.newBuilder().buildPartial();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();
			
			Element appElement = document.createElement("app");
			Element userListElement = document.createElement("userlist");
			
			int start = 0;
			final int length = 500;
			while (true) {
				
				GetUserListResponse response = Futures.getUnchecked(
						adminUserService.getUserList(head, 
								request.toBuilder()
									.setStart(start)
									.setLength(length)
									.build()));
				
				for (UserProtos.User user : response.getUserList()) {
					if (user.getBase().getMobileNoCount() <= 0) {
						continue;
					}
					
					Element userElement = document.createElement("user");
					userElement.setAttribute("Name", user.getBase().getUserName());
					userElement.setAttribute("Id", String.valueOf(user.getBase().getUserId()));
					
					Element companyIdElement = document.createElement("company_id");
					companyIdElement.setTextContent(String.valueOf(head.getCompanyId()));
					
					Element userIdElement = document.createElement("user_id");
					userIdElement.setTextContent(String.valueOf(user.getBase().getUserId()));
					
					Element mobileNoElement = document.createElement("mobile_no");
					mobileNoElement.setTextContent(user.getBase().getMobileNo(0));
					
					userElement.appendChild(companyIdElement);
					userElement.appendChild(userIdElement);
					userElement.appendChild(mobileNoElement);
					userListElement.appendChild(userElement);
				}
				
				start += length;
				if (start >= response.getFilteredSize()) {
					break;
				}
			}
			
			appElement.appendChild(userListElement);
			document.appendChild(appElement);
			document.setXmlStandalone(true);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_auto_login.xml");
			
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(httpResponse.getOutputStream());
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.transform(source, result);
			
		} catch (ParserConfigurationException e) {
			throw new ServletException(e);
		} catch (TransformerException e) {
			throw new ServletException(e);
		}
	}

}
