package com.weizhu.webapp.admin.api.tools.productclock;

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
import com.weizhu.proto.ProductclockProtos;
import com.weizhu.proto.ProductclockProtos.Gender;
import com.weizhu.proto.ProductclockProtos.ImportCustomerRequest;
import com.weizhu.proto.ProductclockProtos.ImportCustomerResponse;
import com.weizhu.proto.ToolsProductclockService;
import com.weizhu.webapp.admin.api.tools.ToolsUtil;

@Singleton
public class ImportCustomerSerlvet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImportCustomerSerlvet.class);
	
	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final File uploadTmpDir;
	private final File importFailLogDir;
	
	@Inject
	public ImportCustomerSerlvet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
			@Named("admin_upload_tmp_dir") File uploadTmpDir,
			@Named("admin_tools_productclock_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.toolsProductclockService = toolsProductclockService;
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
				if (item.getFieldName().equals("import_customer_file")
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
							ret.addProperty("result", "FAIL_CUSTOMER_INVALID");
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
					if (!result.invalidCustomerList.isEmpty()) {
						String importFailLogName = "import_productclock_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";
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

							for (String invalidInfo : result.invalidCustomerList) {
								p.println(invalidInfo);
							}
						} finally {
							p.close();
						}
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_CUSTOMER_INVALID");
						ret.addProperty("fail_text", "导入客户不正确，详情见错误日志");
						new Gson().toJson(ret, responseWriter);
						return ;
					}

					// invoke service
					ImportCustomerRequest request = ImportCustomerRequest.newBuilder()
							.addAllCustomer(result.customerList)
							.build();

					final AdminHead adminHead = adminHeadProvider.get();
					ImportCustomerResponse response = Futures.getUnchecked(toolsProductclockService.importCustomer(adminHead, request));
					switch (response.getResult()) {
						case SUCC:{
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "SUCC");
							new Gson().toJson(ret, responseWriter);
							return;
						}
						case FAIL_CUSTOMER_INVALID: {
							String importFailLogName = "import_productclock_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_" + head.getSession().getSessionId() + ".txt";
							
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
								for (int i = 0; i < response.getInvalidCustomerCount(); ++i) {
									ProductclockProtos.ImportCustomerResponse.InvalidCustomer invalidCustomer = response.getInvalidCustomer(i);
	
									p.print(" 客户名称： ");
									p.print(invalidCustomer.getCustomerName());
									p.print(" 失败原因： ");
									p.print(invalidCustomer.getFailText());
								}
							} finally {
								p.close();
							}
	
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_CUSTOMER_INVALID");
							ret.addProperty("fail_text", "导入客户不正确，详情见错误日志");
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
		final List<ProductclockProtos.Customer> customerList;
		final List<String> invalidCustomerList;

		public ParseResult(List<ProductclockProtos.Customer> customerList, List<String> invalidCustomerList) {
			this.customerList = customerList;
			this.invalidCustomerList = invalidCustomerList;
		}
	}

	private ParseResult parseSheet(Sheet sheet) {
		Iterator<Row> rowIt = sheet.rowIterator();
		// 读取表头
		
		int customerNameIdx       = -1;
		int mobileNoIdx           = -1;
		int genderIdx             = -1;
		int birthdaySolarIdx      = -1;
		int birthdayLunarIdx      = -1;
		int weddingSolarIdx       = -1;
		int weddingLunarIdx       = -1;
		int addressIdx            = -1;
		int remarkIdx             = -1;
		int bolongUserMobileNoIdx = -1;
		int isRemindIdx           = -1;
		int daysAgoRemindIdx      = -1;
		
		{
			Row row = null;
			while (rowIt.hasNext()) {
				row = rowIt.next();
				if (row.getCell(0).getStringCellValue().startsWith("客户姓名*")) {
					break;
				}
				row = null;
			}
	
			if (row != null) {
				Iterator<Cell> cellIt = row.cellIterator();
				
				Cell cell = cellIt.hasNext() ? cellIt.next() : null;
				while(cell != null) {
					if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell.getStringCellValue().startsWith("客户姓名")) {
							customerNameIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("手机号")) {
							mobileNoIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("性别")) {
							genderIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("生日（阳历）")) {
							birthdaySolarIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("生日（阴历）")) {
							birthdayLunarIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("结婚纪念日（阳历）")) {
							weddingSolarIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("结婚纪念日（阴历）")) {
							weddingLunarIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("住址")) {
							addressIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("备注")) {
							remarkIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("所属客户（手机号）")) {
							bolongUserMobileNoIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("是否提醒")) {
							isRemindIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("提前几天提醒")) {
							daysAgoRemindIdx = cell.getColumnIndex();
						}
						
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}

		LinkedList<ProductclockProtos.Customer> customerList = new LinkedList<ProductclockProtos.Customer>();
		List<String> invalidInfo = new ArrayList<String>();
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		ProductclockProtos.Customer.Builder customerBuilder = ProductclockProtos.Customer.newBuilder();
		while (rowIt.hasNext()) {
			customerBuilder.clear();
			
			Row row = rowIt.next();
			Iterator<Cell> cellIt = row.cellIterator();
			
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() == customerNameIdx) { 
						customerBuilder.setCustomerName(value);
					} else if (cell.getColumnIndex() == mobileNoIdx) {
						if (!value.isEmpty()) {
							customerBuilder.setMobileNo(value);
						}
					} else if (cell.getColumnIndex() == genderIdx) {
						customerBuilder.setGender(value.equals("女") ? Gender.FEMALE : Gender.MALE);
					} else if (cell.getColumnIndex() == birthdaySolarIdx) {
						if (!value.isEmpty()) {
							try {
								customerBuilder.setBirthdaySolar((int) (df.parse(value).getTime()/1000L));
							} catch (Exception ex) {

							}
						}
					} else if (cell.getColumnIndex() == birthdayLunarIdx) {
						if (!value.isEmpty()) {
							customerBuilder.setBirthdayLunar(ToolsUtil.lunarTosolarTimeStamp(value));
						}
					} else if (cell.getColumnIndex() == weddingSolarIdx) {
						if (!value.isEmpty()) {
							try {
								customerBuilder.setWeddingSolar((int) (df.parse(value).getTime()/1000L));
							} catch (Exception ex) {
								
							}
						}
					} else if (cell.getColumnIndex() == weddingLunarIdx) {
						if (!value.isEmpty()) {
							customerBuilder.setWeddingLunar(ToolsUtil.lunarTosolarTimeStamp(value));
						}
					} else if (cell.getColumnIndex() == addressIdx) {
						if (!value.isEmpty()) {
							customerBuilder.setAddress(value);
						}
					} else if (cell.getColumnIndex() == remarkIdx) {
						if (!value.isEmpty()) {
							customerBuilder.setRemark(value);
						}
					} else if (cell.getColumnIndex() == bolongUserMobileNoIdx) {
						
					} else if (cell.getColumnIndex() == isRemindIdx) {
						customerBuilder.setIsRemind(value.equals("是") ? true : false);
					} else if (cell.getColumnIndex() == daysAgoRemindIdx) {
						customerBuilder.setDaysAgoRemind(Integer.parseInt(value));
					}
				}
			}
			
			if (!customerBuilder.hasCustomerName() && customerBuilder.getCustomerName().isEmpty()) {
				invalidInfo.add("第" + row.getRowNum() + "行， 缺少客户姓名！");
				break;
			}
			if (!customerBuilder.hasIsRemind()) {
				invalidInfo.add("第" + row.getRowNum() + "行， 缺少是否提醒字段！");
				break;
			}
			
			customerList.add(customerBuilder.setCustomerId(0).build());
		}
		
		return new ParseResult(customerList, invalidInfo);
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
