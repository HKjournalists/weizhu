package com.weizhu.webapp.admin.api.qa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminQAProtos;
import com.weizhu.proto.AdminQAProtos.ImportQuestionRequest.QuestionAnswer;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ImportQuestionServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(ImportQuestionServlet.class);
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;
	private final File uploadTmpDir;
	private final File importFailLogDir;

	@Inject
	public ImportQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService, @Named("admin_upload_tmp_dir") File uploadTmpDir,
			@Named("admin_qa_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminQAService = adminQAService;
		this.uploadTmpDir = uploadTmpDir;
		this.importFailLogDir = importFailLogDir;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		int categoryId = ParamUtil.getInt(httpRequest, "category_id", -1);
		final AdminHead head = this.adminHeadProvider.get();
		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(httpRequest);
		if (!isMultipart) {
			return;
		}

		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(3 * 1024 * 1024);
		factory.setRepository(uploadTmpDir);

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Set overall request size constraint
		upload.setSizeMax(20 * 1024 * 1024);

		// Parse the request
		ParseResult result = null;
		try {
			List<FileItem> fileItemList = upload.parseRequest(httpRequest);
			for (FileItem item : fileItemList) {
				if (item.getFieldName().equals("import_qa_question_file") && !item.isFormField() && (item.getName().endsWith(".xls") || item.getName().endsWith(".xlsx"))) {

					// parse file
					InputStream in = null;
					Workbook wb = null;

					try {
						in = item.getInputStream();
						if (!in.markSupported()) {
							in = new PushbackInputStream(in, 8);
						}
						if (POIFSFileSystem.hasPOIFSHeader(in)) {
							wb = new HSSFWorkbook(in);
						} else if (POIXMLDocument.hasOOXMLHeader(in)) {
							wb = new XSSFWorkbook(OPCPackage.open(in));
						} else {
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_USER_INVALID");
							ret.addProperty("fail_text", "导入文件格式不正确");

							httpResponse.setContentType("application/json;charset=UTF-8");
							JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
							return;
						}

						result = parseSheet(wb.getSheetAt(0));
						
						// validate data
						if (!result.invalidContentList.isEmpty()) {
							writeImportFailLog(head, result.invalidContentList);
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_QUESTION_INVALID");
							ret.addProperty("fail_text", "导入问题不正确，详情见错误日志");

							httpResponse.setContentType("application/json;charset=UTF-8");
							JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
							return;
						}
					} finally {
						if (wb != null) {
							wb.close();
						}
						if (in != null) {
							in.close();
						}
					}

				}
			}
		} catch (Throwable th) {
			logger.error("import fail", th);
			th.printStackTrace();
		}
		if (!result.invalidContentList.isEmpty()) {
			JsonObject ret = new JsonObject();
			ret.addProperty("result", "FAIL_QUESTION_INVALID");
			ret.addProperty("fail_text", "导入问题不正确，详情见错误日志");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
		}else if (result.queAnsContentList.isEmpty()) {
			JsonObject reponseResult = new JsonObject();
			reponseResult.addProperty("result", "FAIL_QUESTION_IS_NULL");
			reponseResult.addProperty("fail_text", "上传的数据不能为空");
			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(reponseResult, httpResponse.getWriter());
		} else {
			// 4. 调用Service		
			AdminQAProtos.ImportQuestionRequest request = AdminQAProtos.ImportQuestionRequest.newBuilder()
					.addAllQuestionAnswer(result.queAnsContentList)
					.setCategoryId(categoryId)
					.build();

			AdminQAProtos.ImportQuestionResponse response = Futures.getUnchecked(this.adminQAService.importQuestion(head, request));

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
		}
	}

	private ParseResult parseSheet(Sheet sheet) {
		Iterator<Row> rowIt = sheet.rowIterator();

		// 读取表头
		int queContentColumnIdx = -1;
		int ansContentColumnIds = -1;

		{
			Row row = null;
			while (rowIt.hasNext()) {
				row = rowIt.next();

				String value = getValue(row.getCell(0));
				if (value.startsWith("问题内容")) {
					break;
				}
				row = null;
			}

			if (row != null) {
				Iterator<Cell> cellIt = row.cellIterator();

				Cell cell = cellIt.hasNext() ? cellIt.next() : null;
				while (cell != null) {
					if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell.getStringCellValue().startsWith("问题内容")) {
							queContentColumnIdx = cell.getColumnIndex();
						}
						if (cell.getStringCellValue().startsWith("回答内容")) {
							ansContentColumnIds = cell.getColumnIndex();
						}
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}

		List<AdminQAProtos.ImportQuestionRequest.QuestionAnswer> queAnsContentList = new ArrayList<AdminQAProtos.ImportQuestionRequest.QuestionAnswer>();
		List<String> invalidContentList = new ArrayList<String>();
		QuestionAnswer.Builder queAnsBuilder = QuestionAnswer.newBuilder();
		while (rowIt.hasNext()) {
			queAnsBuilder.clear();
			Row row = rowIt.next();
			if (getValue(row.getCell(0)).isEmpty()) {
				continue;
			}
			Iterator<Cell> cellIt = row.cellIterator();
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() == queContentColumnIdx) {
						if (value.length() > 100) {
							invalidContentList.add(value);
						} else {
							queAnsBuilder.setQuestionContent(value);
						}
					}
					if (cell.getColumnIndex() == ansContentColumnIds) {
						if (value.length() > 1000) {
							invalidContentList.add(value);
						} else {
							queAnsBuilder.setAnswerContent(value);
						}
					}
				}
			}
			if(queAnsBuilder.hasQuestionContent()){
				queAnsContentList.add(queAnsBuilder.build());
			}
			
		}
		return new ParseResult(queAnsContentList, invalidContentList);
	}

	private static String getValue(Cell cell) {
		if (cell == null) {
			return "";
		}
		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_BLANK:
				return "";
			case Cell.CELL_TYPE_BOOLEAN:
				return Boolean.toString(cell.getBooleanCellValue());
			case Cell.CELL_TYPE_ERROR:
				return "";
			case Cell.CELL_TYPE_FORMULA:
				return "";
			case Cell.CELL_TYPE_NUMERIC:
				DecimalFormat df = new DecimalFormat("0.#");
				df.setDecimalSeparatorAlwaysShown(false);
				return df.format(cell.getNumericCellValue());
			case Cell.CELL_TYPE_STRING:
				String str = cell.getStringCellValue();
				return str == null ? "" : str.trim();
			default:
				return "";
		}
	}

	private static class ParseResult {
		final List<AdminQAProtos.ImportQuestionRequest.QuestionAnswer> queAnsContentList;
		final List<String> invalidContentList;

		ParseResult(List<AdminQAProtos.ImportQuestionRequest.QuestionAnswer> queAnsContentList, List<String> invalidContentList) {
			this.queAnsContentList = queAnsContentList;
			this.invalidContentList = invalidContentList;
		}
	}

	private void writeImportFailLog(AdminHead head, List<String> invalidInfoList) throws IOException {
		String importFailLogName = "import_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";
		File importFailLogFile = new File(importFailLogDir, importFailLogName);
		Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(importFailLogFile, false)));
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			w.write("导入时间: " + df.format(new Date()) + "\r\n");
			for (String invalidInfo : invalidInfoList) {
				w.write(invalidInfo + "\r\n");
			}
		} finally {
			w.close();
		}
	}
}
