package com.weizhu.webapp.admin.api.qa;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.proto.AdminProtos.AdminHead;

@Singleton
@SuppressWarnings("serial")
public class GetImportFailLogServlet extends HttpServlet {

	private final Provider<AdminHead> adminHeadProvider;

	private final File importFailLogDir;

	@Inject
	public GetImportFailLogServlet(Provider<AdminHead> adminHeadProvider, @Named("admin_qa_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.importFailLogDir = importFailLogDir;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = adminHeadProvider.get();

		String importFailLogName = "import_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";

		File importFailLogFile = new File(importFailLogDir, importFailLogName);

		if (!importFailLogFile.exists()) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "no import fail log");
			return;
		}

		httpResponse.setContentType("text/plain; charset=utf-8");
		httpResponse.setHeader("Content-Disposition", "attachment;filename=import_fail_log.txt");

		FileReader fileReader = new FileReader(importFailLogFile);
		try {
			char[] buf = new char[1024];
			int cnt = -1;
			while ((cnt = fileReader.read(buf)) != -1) {
				httpResponse.getWriter().write(buf, 0, cnt);
			}
		} finally {
			fileReader.close();
		}
	}
}
