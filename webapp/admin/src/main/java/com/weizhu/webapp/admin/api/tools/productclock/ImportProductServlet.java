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
import com.weizhu.proto.ProductclockProtos.ImportProductRequest;
import com.weizhu.proto.ProductclockProtos.ImportProductResponse;
import com.weizhu.proto.ToolsProductclockService;

@Singleton
public class ImportProductServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ImportCustomerSerlvet.class);
	
	private final Provider<AdminHead> adminHeadProvider;
	private final ToolsProductclockService toolsProductclockService;
	private final File uploadTmpDir;
	private final File importFailLogDir;
	
	@Inject
	public ImportProductServlet(Provider<AdminHead> adminHeadProvider, ToolsProductclockService toolsProductclockService,
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
				if (item.getFieldName().equals("import_product_file")
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
							ret.addProperty("result", "FAIL_PRODUCT_INVALID");
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
					if (!result.invalidProductList.isEmpty()) {
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

							for (String invalidInfo : result.invalidProductList) {
								p.println(invalidInfo);
							}
						} finally {
							p.close();
						}
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_CUSTOMER_INVALID");
						ret.addProperty("fail_text", "导入产品不正确，详情见错误日志");
						new Gson().toJson(ret, responseWriter);
						return ;
					}

					// invoke service
					ImportProductRequest request = ImportProductRequest.newBuilder()
							.addAllProduct(result.productList)
							.build();

					final AdminHead adminHead = adminHeadProvider.get();
					ImportProductResponse response = Futures.getUnchecked(toolsProductclockService.importProduct(adminHead, request));
					switch (response.getResult()) {
						case SUCC:{
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "SUCC");
							new Gson().toJson(ret, responseWriter);
							return;
						}
						case FAIL_PRODUCT_INVALID: {
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
								for (int i = 0; i < response.getInvalidProductCount(); ++i) {
									ProductclockProtos.ImportProductResponse.InvalidProduct invalidProduct = response.getInvalidProduct(i);
	
									p.println(" 产品名称： ");
									p.print(invalidProduct.getProductName());
									p.println(" 失败原因： ");
									p.print(invalidProduct.getFailText());
								}
							} finally {
								p.close();
							}
	
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_CUSTOMER_INVALID");
							ret.addProperty("fail_text", "导入产品不正确，详情见错误日志");
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
		final List<ProductclockProtos.Product> productList;
		final List<String> invalidProductList;

		public ParseResult(List<ProductclockProtos.Product> productList, List<String> invalidProductList) {
			this.productList = productList;
			this.invalidProductList = invalidProductList;
		}
	}

	private ParseResult parseSheet(Sheet sheet) {
		Iterator<Row> rowIt = sheet.rowIterator();
		// 读取表头
		
		int productNameIdx     = -1;
		int productDescIdx     = -1;
		int defaulRemindDayIdx = -1;
		
		{
			Row row = null;
			while (rowIt.hasNext()) {
				row = rowIt.next();
				if (row.getCell(0).getStringCellValue().startsWith("产品名称*")) {
					break;
				}
				row = null;
			}

			if (row != null) {
				Iterator<Cell> cellIt = row.cellIterator();
				
				Cell cell = cellIt.hasNext() ? cellIt.next() : null;
				while(cell != null) {
					if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell.getStringCellValue().startsWith("产品名称")) {
							productNameIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("产品描述")) {
							productDescIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("默认提醒周期")) {
							defaulRemindDayIdx = cell.getColumnIndex();
						}
						
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}

		LinkedList<ProductclockProtos.Product> productList = new LinkedList<ProductclockProtos.Product>();
		List<String> invalidInfo = new ArrayList<String>();
		
		ProductclockProtos.Product.Builder productBuilder = ProductclockProtos.Product.newBuilder();
		while (rowIt.hasNext()) {
			productBuilder.clear();
			
			Row row = rowIt.next();
			Iterator<Cell> cellIt = row.cellIterator();
			
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() == productNameIdx) { 
						productBuilder.setProductName(value);
					} else if (cell.getColumnIndex() == productDescIdx) {
						if (!value.isEmpty()) {
							productBuilder.setProductDesc(value);
						}
					} else if (cell.getColumnIndex() == defaulRemindDayIdx) {
						productBuilder.setDefaultRemindDay(Integer.parseInt(value));
					}
				}
			}
			
			if (!productBuilder.hasProductName() && productBuilder.getProductName().isEmpty()) {
				invalidInfo.add("第" + row.getRowNum() + "行， 缺少产品名称！");
				break;
			}
			if (!productBuilder.hasDefaultRemindDay()) {
				invalidInfo.add("第" + row.getRowNum() + "行， 缺少默认提醒周期！");
				break;
			}
			
			productList.add(productBuilder.setProductId(0).build());
		}
		
		return new ParseResult(productList, invalidInfo);
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
