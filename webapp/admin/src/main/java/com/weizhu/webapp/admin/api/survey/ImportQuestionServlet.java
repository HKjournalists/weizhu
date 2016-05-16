package com.weizhu.webapp.admin.api.survey;

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
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.SurveyProtos;
import com.weizhu.proto.SurveyProtos.ImportQuestionRequest;
import com.weizhu.proto.SurveyProtos.ImportQuestionResponse;
import com.weizhu.proto.SurveyService;
import com.weizhu.web.ParamUtil;

@Singleton
public class ImportQuestionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(ImportQuestionServlet.class);

	private final Provider<AdminHead> adminHeadProvider;
	private final SurveyService surveyService;
	private final File uploadTmpDir;
	private final File importFailLogDir;
	
	@Inject
	public ImportQuestionServlet(Provider<AdminHead> adminHeadProvider, SurveyService surveyService,
			@Named("admin_upload_tmp_dir") File uploadTmpDir,
			@Named("admin_survey_question_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.surveyService = surveyService;
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
		final int surveyId = ParamUtil.getInt(httpRequest, "survey_id", 0);
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
							.setSurveyId(surveyId)
							.build();

					final AdminHead adminHead = adminHeadProvider.get();
					ImportQuestionResponse response = Futures.getUnchecked(surveyService.importQuestion(adminHead, request));
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
								for (int i = 0; i < response.getInvalidQuestionCount(); ++i) {
									SurveyProtos.ImportQuestionResponse.InvalidQuestion invalidUser = response.getInvalidQuestion(i);
	
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
						case FAIL_SURVEY_INVALID: {
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_SURVEY_INVALID");
							ret.addProperty("fail_text", "指定的调研不存在");
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
		final List<SurveyProtos.Question> questionList;
		final List<String> invalidQuestionList;

		public ParseResult(List<SurveyProtos.Question> questionList, List<String> invalidQuestionList) {
			this.questionList = questionList;
			this.invalidQuestionList = invalidQuestionList;
		}
	}

	private ParseResult parseSheet(Sheet sheet) {
		List<String> invalidInfo = new ArrayList<String>();
		LinkedList<SurveyProtos.Question> questionList = new LinkedList<SurveyProtos.Question>();
		Iterator<Row> rowIt = sheet.rowIterator();
		// 读取表头
		
		int questionTypeIdx = -1;
		int questionNameIdx = -1;
		int optionalIdx     = -1;
		int optionStartIdx  = -1;
		int optionEndIdx    = -1;
		
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
						} else if (cell.getStringCellValue().startsWith("是否必填")) {
							optionalIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("选项")) {
							optionStartIdx = cell.getColumnIndex();
							optionEndIdx = cell.getColumnIndex();
							cell = cellIt.hasNext() ? cellIt.next() : null;
							while (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
								optionEndIdx = cell.getColumnIndex();
								cell = cellIt.hasNext() ? cellIt.next() : null;
							}
							continue;
						} 
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}
		
		SurveyProtos.Question.Builder questionBuilder = SurveyProtos.Question.newBuilder();

		SurveyProtos.Vote.Builder voteBuilder = SurveyProtos.Vote.newBuilder();
		SurveyProtos.InputSelect.Builder inputSelectBuilder = SurveyProtos.InputSelect.newBuilder();
		SurveyProtos.InputText.Builder inputTextBuilder = SurveyProtos.InputText.newBuilder();
		
		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			Iterator<Cell> cellIt = row.cellIterator();
			
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() == questionTypeIdx) {
						if (value.contains("单选")) {
							voteBuilder.clear();
							questionBuilder.clear();
							
							SurveyProtos.Vote.Question.Builder voteQuestionBuilder = SurveyProtos.Vote.Question.newBuilder();
							SurveyProtos.Vote.Option.Builder optionBuilder = SurveyProtos.Vote.Option.newBuilder();
							while (cellIt.hasNext()) {
								
								cell = cellIt.next();
								value = getValue(cell);
								
								if (value.isEmpty()) {
									continue;
								}
								
								if (cell.getColumnIndex() == questionNameIdx) {
									questionBuilder.setQuestionName(value);
									continue;
								} else if (cell.getColumnIndex() == optionalIdx) {
									if (value.equals("是")) {
										questionBuilder.setIsOptional(true);
									} else {
										questionBuilder.setIsOptional(false);
									}
									continue;
								} else if (cell.getColumnIndex() >= optionStartIdx && cell.getColumnIndex() <= optionEndIdx) {
									optionBuilder.clear();
									
									optionBuilder.setOptionName(value).setOptionId(0);
								}
								voteQuestionBuilder.addOption(optionBuilder.build());
							}
							voteQuestionBuilder.setCheckNum(1);
							questionBuilder.setVote(voteQuestionBuilder.build()).setQuestionId(0);
							
							questionList.add(questionBuilder.build());
							
						} else if (value.contains("多选")) {
							voteBuilder.clear();
							questionBuilder.clear();
							
							SurveyProtos.Vote.Question.Builder voteQuestionBuilder = SurveyProtos.Vote.Question.newBuilder();
							SurveyProtos.Vote.Option.Builder optionBuilder = SurveyProtos.Vote.Option.newBuilder();
							while (cellIt.hasNext()) {
								
								cell = cellIt.next();
								value = getValue(cell);
								
								if (value.isEmpty()) {
									continue;
								}
								
								if (cell.getColumnIndex() == questionNameIdx) {
									questionBuilder.setQuestionName(value);
									continue;
								} else if (cell.getColumnIndex() == optionalIdx) {
									if (value.equals("是")) {
										questionBuilder.setIsOptional(true);
									} else {
										questionBuilder.setIsOptional(false);
									}
									continue;
								} else if (cell.getColumnIndex() >= optionStartIdx && cell.getColumnIndex() <= optionEndIdx) {
									optionBuilder.clear();
									
									optionBuilder.setOptionName(value).setOptionId(0);
								}
								voteQuestionBuilder.addOption(optionBuilder.build());
							}
							voteQuestionBuilder.setCheckNum(voteQuestionBuilder.getOptionCount());
							questionBuilder.setVote(voteQuestionBuilder.build()).setQuestionId(0);
							
							questionList.add(questionBuilder.build());
							
						} else if (value.contains("下拉框")) {
							inputSelectBuilder.clear();
							questionBuilder.clear();
							
							SurveyProtos.InputSelect.Question.Builder inputSelectQuestionBuilder = SurveyProtos.InputSelect.Question.newBuilder();
							SurveyProtos.InputSelect.Option.Builder optionBuilder = SurveyProtos.InputSelect.Option.newBuilder();
							while (cellIt.hasNext()) {
								
								cell = cellIt.next();
								value = getValue(cell);
								
								if (value.isEmpty()) {
									continue;
								}
								
								if (cell.getColumnIndex() == questionNameIdx) {
									questionBuilder.setQuestionName(value);
									continue;
								} else if (cell.getColumnIndex() == optionalIdx) {
									if (value.equals("是")) {
										questionBuilder.setIsOptional(true);
									} else {
										questionBuilder.setIsOptional(false);
									}
									continue;
								} else if (cell.getColumnIndex() >= optionStartIdx && cell.getColumnIndex() <= optionEndIdx) {
									optionBuilder.clear();
									
									optionBuilder.setOptionName(value).setOptionId(0);
								}
								inputSelectQuestionBuilder.addOption(optionBuilder.build());
							}
							questionBuilder.setInputSelect(inputSelectQuestionBuilder.build()).setQuestionId(0);
							
							questionList.add(questionBuilder.build());
							
						} else if (value.contains("填空")) {
							inputTextBuilder.clear();
							questionBuilder.clear();
							
							SurveyProtos.InputText.Question.Builder inputTextQuestionBuilder = SurveyProtos.InputText.Question.newBuilder();
							while (cellIt.hasNext()) {
								
								cell = cellIt.next();
								value = getValue(cell);
								
								if (value.isEmpty()) {
									continue;
								}
								
								if (cell.getColumnIndex() == questionNameIdx) {
									questionBuilder.setQuestionName(value);
									continue;
								} else if (cell.getColumnIndex() == optionalIdx) {
									if (value.equals("是")) {
										questionBuilder.setIsOptional(true);
									} else {
										questionBuilder.setIsOptional(false);
									}
								}
								inputTextQuestionBuilder.setInputPrompt("");
							}
							questionBuilder.setInputText(inputTextQuestionBuilder.build()).setQuestionId(0);
							
							questionList.add(questionBuilder.build());
							
						} else {
							invalidInfo.add("第" + row.getRowNum() + "行， 缺少考题类型！");
							break;
						}
					}
				}
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
