package com.weizhu.webapp.admin.api.exam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AdminExamProtos;
import com.weizhu.proto.AdminExamProtos.ImportQuestionRequest;
import com.weizhu.proto.AdminExamProtos.ImportQuestionResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class ImportQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory
			.getLogger(ImportQuestionServlet.class);

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final File uploadTmpDir;
	private final File importFailLogDir;

	@Inject
	public ImportQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService,
			@Named("admin_upload_tmp_dir") File uploadTmpDir,
			@Named("admin_question_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.uploadTmpDir = uploadTmpDir;
		this.importFailLogDir = importFailLogDir;
	}

	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final AdminHead head = adminHeadProvider.get();
		
		Writer responseWriter = new OutputStreamWriter(httpResponse.getOutputStream(), Charsets.UTF_8);
		try {
			process(head, httpRequest, httpResponse, responseWriter);
		} catch (Throwable th) {
			logger.error("process", th);
		} finally {
			httpResponse.setCharacterEncoding("UTF-8");
			if (httpResponse.getContentType() == null || httpResponse.getContentType().isEmpty()) {
				httpResponse.setContentType("application/json;charset=UTF-8");
			}
			httpResponse.addHeader("Access-Control-Allow-Origin", "*");
			responseWriter.flush();
		}
	}

	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}

	private void process(AdminHead head, HttpServletRequest httpRequest, HttpServletResponse httpResponse, Writer responseWriter) {
		final Integer questionCategoryId = ParamUtil.getInt(httpRequest, "question_category_id", null);
		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(httpRequest);
		if (!isMultipart) {
			return;
		}

		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(10 * 1024 * 1024);
		factory.setRepository(uploadTmpDir);

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Set overall request size constraint
		upload.setSizeMax(20 * 1024 * 1024);

		// Parse the request
		try {
			List<FileItem> fileItemList = upload.parseRequest(httpRequest);
			for (FileItem item : fileItemList) {
				if (item.getFieldName().equals("import_question_file")
						&& !item.isFormField()
						&& (item.getName().endsWith(".xls") || item.getName()
								.endsWith(".xlsx"))) {
					// parse file
					Workbook wb = null;
					ParseResult result = null;
					try {
						InputStream in = item.getInputStream();
						if (!in.markSupported()) {
							in = new PushbackInputStream(in, 8);
						}
						if (POIFSFileSystem.hasPOIFSHeader(in)) {
							wb = new HSSFWorkbook(in);
						} else if (POIXMLDocument.hasOOXMLHeader(in)) {
							wb = new XSSFWorkbook(OPCPackage.open(in));
						} else {
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_QUESTION_INVALID");
							ret.addProperty("fail_text", "导入文件格式不正确");
							new Gson().toJson(ret, responseWriter);
							return;
						}

						result = parseSheet(wb.getSheetAt(0));
					} finally {
						if (wb != null) {
							wb.close();
						}
					}

					// validate data
					if (!result.invalidQuestionList.isEmpty()) {
						String importFailLogName = "import_question_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";
						File location = new File(importFailLogDir.getPath());
						if (!location.exists()) {
							location.mkdirs();
						}
						
						File importFailLogFile = new File(importFailLogDir, importFailLogName);
						if (!importFailLogFile.exists()) {
							importFailLogFile.createNewFile();
						}
						
						PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(importFailLogFile, false))));
						try {
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							p.println("导入时间: " + df.format(new Date()));

							for (String invalidInfo : result.invalidQuestionList) {
								p.println(invalidInfo);
							}
						} finally {
							p.close();
						}
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_QUESTION_INVALID");
						ret.addProperty("fail_text", "导入考题不正确，详情见错误日志");
						new Gson().toJson(ret, responseWriter);
						return ;
					}

					// invoke service
					ImportQuestionRequest request = ImportQuestionRequest.newBuilder()
							.addAllQuestion(result.questionList)
							.setQuestionCategoryId(questionCategoryId)
							.build();

					final AdminHead adminHead = adminHeadProvider.get();
					ImportQuestionResponse response = Futures.getUnchecked(adminExamService.importQuestion(adminHead, request));
					switch (response.getResult()) {
					case SUCC:{
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "SUCC");
						new Gson().toJson(ret, responseWriter);
						return;
					}
					case FAIL_QUESTION_INVALID: {
						String importFailLogName = "import_question_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";
						
						File location = new File(importFailLogDir.getPath());
						if (!location.exists()) {
							location.mkdirs();
						}
						
						File importFailLogFile = new File(importFailLogDir, importFailLogName);
						if (!importFailLogFile.exists()) {
							importFailLogFile.createNewFile();
						}
						
						PrintWriter p = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(
										importFailLogFile, false))));
						try {
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							p.println("导入时间: " + df.format(new Date()));
							for (int i = 0; i < response
									.getInvalidQuestionCount(); ++i) {
								AdminExamProtos.ImportQuestionResponse.InvalidQuestion invalidUser = response
										.getInvalidQuestion(i);

								p.print(" 考题名称： ");
								p.print(invalidUser.getQuestionName());
								p.print(" 失败原因： ");
								p.println(invalidUser.getFailText());
							}
						} finally {
							p.close();
						}

						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_QUESTION_INVALID");
						ret.addProperty("fail_text", "导入考题不正确，详情见错误日志");
						new Gson().toJson(ret, responseWriter);
						return;
					}
					case FAIL_QUESTION_CATEGORY_INVALID: {
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_QUESTION_TYPE_INVALID");
						ret.addProperty("fail_text", "导入数据格式不正确");
						new Gson().toJson(ret, responseWriter);
						return;
					}
					default:
						return;
					}
				}
			}
		} catch (Throwable th) {
			logger.error("import fail", th);
			th.printStackTrace();
		}
	}

	private static class ParseResult {
		final List<ExamProtos.Question> questionList;
		final List<String> invalidQuestionList;

		public ParseResult(List<ExamProtos.Question> questionList, List<String> invalidQuestionList) {
			this.questionList = questionList;
			this.invalidQuestionList = invalidQuestionList;
		}
	}

	private ParseResult parseSheet(Sheet sheet) {
		List<String> invalidInfo = new ArrayList<String>();
		LinkedList<ExamProtos.Question> questionList = new LinkedList<ExamProtos.Question>();
		Iterator<Row> rowIt = sheet.rowIterator();
		// 读取表头
		
		int questionTypeIdx = -1;
		int questionNameIdx = -1;
		int rightAnswerIdx = -1;
		int answerStartIdx = -1;
		int answerEndIdx = -1;
		
		{
			Row row = null;
			while (rowIt.hasNext()) {
				row = rowIt.next();
				if (row.getCell(0).getStringCellValue().startsWith("题型*")) {
					break;
				}
				row = null;
			}
	
			if (row != null) {
				Iterator<Cell> cellIt = row.cellIterator();
				
				Cell cell = cellIt.hasNext() ? cellIt.next() : null;
				while(cell != null) {
					if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell.getStringCellValue().startsWith("题型*")) {
							questionTypeIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("题目*")) {
							questionNameIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("正确答案*")) {
							rightAnswerIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("答案项")) {
							answerStartIdx = cell.getColumnIndex();
							answerEndIdx = cell.getColumnIndex();
							cell = cellIt.hasNext() ? cellIt.next() : null;
							while (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
								answerEndIdx = cell.getColumnIndex();
								cell = cellIt.hasNext() ? cellIt.next() : null;
							}
							continue;
						} 
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}

		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			Iterator<Cell> cellIt = row.cellIterator();
			
			List<ExamProtos.Option> optionList = new ArrayList<ExamProtos.Option>();
			ExamProtos.Question.Builder questionBuilder = ExamProtos.Question.newBuilder();
			ExamProtos.Option.Builder optionBuilder = ExamProtos.Option.newBuilder();
			String rightAnswerIdStr = null;
			List<String> answerIdList = null;
			
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() >= answerStartIdx && cell.getColumnIndex() <= answerEndIdx) {
						optionBuilder.setOptionName(value);
						if (rightAnswerIdStr == null) {
							answerIdList = Collections.emptyList();
						} else {
							answerIdList = DBUtil.COMMA_SPLITTER.splitToList(rightAnswerIdStr);
						}
						
						if (answerIdList.contains((cell.getColumnIndex() - 2) + "")) {
							optionBuilder.setIsRight(true);
						} else {
							optionBuilder.setIsRight(false);
						}
						optionList.add(optionBuilder.setOptionId(0).build());
					} else if (cell.getColumnIndex() == questionTypeIdx) {
						if (value.contains("单选")) {
							questionBuilder.setType(ExamProtos.Question.Type.OPTION_SINGLE);
						} else if (value.contains("多选")) {
							questionBuilder.setType(ExamProtos.Question.Type.OPTION_MULTI);
						} else if (value.contains("判断")) {
							questionBuilder.setType(ExamProtos.Question.Type.OPTION_TF);
						} else {
							invalidInfo.add("第" + row.getRowNum() + "行， 缺少考题类型！");
							break;
						}
					} else if (cell.getColumnIndex() == questionNameIdx) {
						questionBuilder.setQuestionName(value);
					} else if (cell.getColumnIndex() == rightAnswerIdx) {
						rightAnswerIdStr = value.replace("，", ",");
					}
				}
			}
			if (questionBuilder.hasQuestionName() && questionBuilder.hasType()) {
				questionList.addFirst(questionBuilder.setQuestionId(0).addAllOption(optionList).build());
			}
		}
		return new ParseResult(questionList, invalidInfo);
	}
	
	private static String getValue(Cell cell) {
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
				DecimalFormat df = new DecimalFormat();
				df.setDecimalSeparatorAlwaysShown(false);
				return df.format(cell.getNumericCellValue());
			case Cell.CELL_TYPE_STRING:
				String str = cell.getStringCellValue();
				return str == null ? "" : str.trim();
			default:
				return "";
		}
	}
}
