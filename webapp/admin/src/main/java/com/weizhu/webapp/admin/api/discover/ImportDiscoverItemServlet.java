package com.weizhu.webapp.admin.api.discover;

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
import com.weizhu.proto.DiscoverV2Protos.WebUrl;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemRequest;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminProtos.AdminHead;

@Singleton
@SuppressWarnings("serial")
public class ImportDiscoverItemServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportDiscoverItemServlet.class);
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminDiscoverService adminDiscoverService;
	private final File uploadTmpDir;
	private final File importFailLogDir;
	
	@Inject
	public ImportDiscoverItemServlet(Provider<AdminHead> adminHeadProvider, 
			AdminDiscoverService adminDiscoverService,
			@Named("admin_upload_tmp_dir") File uploadTmpDir, 
			@Named("admin_discover_import_fail_log_dir") File importFailLogDir
			) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminDiscoverService = adminDiscoverService;
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
				if (item.getFieldName().equals("import_discover_item_file") && !item.isFormField()
						&& (item.getName().endsWith(".xls") || item.getName().endsWith(".xlsx"))) {

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
						if (result.hasError) {
							writeImportFailLog(head, result);
						}

					} finally {
						if (wb != null) {
							wb.close();
						}
						if (in != null) {
							in.close();
						}
					}
					
					// validate data
					if (result.hasError) {
						JsonObject ret = new JsonObject();
						ret.addProperty("result", "FAIL_DISCOVER_ITEM_INVALID");
						ret.addProperty("fail_text", "导入发现课件不正确，详情见错误日志");

						httpResponse.setContentType("application/json;charset=UTF-8");
						JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
						return;
					} else {
						// 4. 调用Service		
						AdminDiscoverProtos.ImportItemRequest request = AdminDiscoverProtos.ImportItemRequest.newBuilder()
								.addAllCreateItemRequest(result.itemRequestList)
								.build();

						AdminDiscoverProtos.ImportItemResponse response = Futures.getUnchecked(this.adminDiscoverService.importItem(head, request));

						httpResponse.setContentType("application/json;charset=UTF-8");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
					}
				}
			}
		} catch (Throwable th) {
			logger.error("import fail", th);
			th.printStackTrace();
		}

	}

	private ParseResult parseSheet(Sheet sheet) {
		Iterator<Row> rowIt = sheet.rowIterator();

		// 读取表头
		int itemNameColumnId = -1;
		int itemDescColumnId = -1;
		int webUrlColumnId = -1;
		int isWeizhuUrlColumnId = -1;
		int enableScoreColumnId = -1;
		int enableCommentColumnId = -1;
		int enableLikeColumnId = -1;
		int enableRemindColumnId = -1;
		int enableShareColumnId = -1;

		Row row = null;
		while (rowIt.hasNext()) {
			row = rowIt.next();

			String value = getValue(row.getCell(0));
			if (value.startsWith("课件标题")) {
				break;
			}
			row = null;
		}

		if (row != null) {
			Iterator<Cell> cellIt = row.cellIterator();

			Cell cell = cellIt.hasNext() ? cellIt.next() : null;
			while (cell != null) {
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					if (cell.getStringCellValue().startsWith("课件标题")) {
						itemNameColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("课件描述")) {
						itemDescColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("链接地址")) {
						webUrlColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("是否为内部连接")) {
						isWeizhuUrlColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("允许评分")) {
						enableScoreColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("允许评论")) {
						enableCommentColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("允许点赞")) {
						enableLikeColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("允许提醒用户评分评论")) {
						enableRemindColumnId = cell.getColumnIndex();
					}
					if (cell.getStringCellValue().startsWith("允许分享")) {
						enableShareColumnId = cell.getColumnIndex();
					}
				}
				cell = cellIt.hasNext() ? cellIt.next() : null;
			}
		}

		List<AdminDiscoverProtos.CreateItemRequest> itemRequestList = new ArrayList<AdminDiscoverProtos.CreateItemRequest>();
		List<String> invalidItemNameList = new ArrayList<String>();
		List<String> invalidItemDescList = new ArrayList<String>();
		List<String> invalidWebUrlList = new ArrayList<String>();
		List<String> invalidIsWeizhuUrlList = new ArrayList<String>();
		List<String> invalidEnableScoreList = new ArrayList<String>();
		List<String> invalidEnableCommentList = new ArrayList<String>();
		List<String> invalidEnableLikeList = new ArrayList<String>();
		List<String> invalidEnableRemindList = new ArrayList<String>();
		List<String> invalidEnableShareList = new ArrayList<String>();

		CreateItemRequest.Builder itemRequestBuilder = CreateItemRequest.newBuilder();
		while (rowIt.hasNext()) {
			itemRequestBuilder.clear();
			row = rowIt.next();
			if (getValue(row.getCell(0)).isEmpty()) {
				continue;
			}
			String webUrl = null;
			Boolean isWeizhuUrl = null;
			Iterator<Cell> cellIt = row.cellIterator();
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() == itemNameColumnId) {
						if (value.length() > 100) {
							invalidItemNameList.add(value);
						}
						itemRequestBuilder.setItemName(value);
					}
					if (cell.getColumnIndex() == itemDescColumnId) {
						if (value.length() > 1000) {
							invalidItemDescList.add(value);
						}
						itemRequestBuilder.setItemDesc(value);
					}

					if (cell.getColumnIndex() == webUrlColumnId) {
						if (value.length() > 1000) {
							invalidWebUrlList.add(value);
						}
						webUrl = value;
					}
					if (cell.getColumnIndex() == isWeizhuUrlColumnId) {
						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidIsWeizhuUrlList.add(value);
						}
						isWeizhuUrl = Boolean.parseBoolean(value);
					}

					if (cell.getColumnIndex() == enableScoreColumnId) {

						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidEnableScoreList.add(value);
						}
						itemRequestBuilder.setEnableScore(Boolean.parseBoolean(value));
					}
					if (cell.getColumnIndex() == enableCommentColumnId) {

						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidEnableCommentList.add(value);
						}
						itemRequestBuilder.setEnableComment(Boolean.parseBoolean(value));
					}
					if (cell.getColumnIndex() == enableLikeColumnId) {

						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidEnableLikeList.add(value);
						}
						itemRequestBuilder.setEnableLike(Boolean.parseBoolean(value));
					}
					if (cell.getColumnIndex() == enableRemindColumnId) {

						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidEnableRemindList.add(value);
						}
						itemRequestBuilder.setEnableRemind(Boolean.parseBoolean(value));
					}
					
					if (cell.getColumnIndex() == enableShareColumnId) {

						if (!"TRUE".equalsIgnoreCase(value) && !"FALSE".equalsIgnoreCase(value)) {
							invalidEnableShareList.add(value);
						}
						itemRequestBuilder.setEnableShare(Boolean.parseBoolean(value));
					}
				}
			}

			if (webUrl != null && isWeizhuUrl != null) {
				itemRequestBuilder.setWebUrl(WebUrl.newBuilder().setWebUrl(webUrl).setIsWeizhu(isWeizhuUrl).build());
			}
			// 设置图片默认值
			itemRequestBuilder.setImageName("");

			if (itemRequestBuilder.hasItemName()) {
				itemRequestList.add(itemRequestBuilder.build());
			}

		}
		return new ParseResult(itemRequestList,
				invalidItemNameList,
				invalidItemDescList,
				invalidWebUrlList,
				invalidIsWeizhuUrlList,
				invalidEnableScoreList,
				invalidEnableCommentList,
				invalidEnableLikeList,
				invalidEnableRemindList,
				invalidEnableShareList);
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
		final List<AdminDiscoverProtos.CreateItemRequest> itemRequestList;
		final List<String> invalidItemNameList;
		final List<String> invalidItemDescList;
		final List<String> invalidWebUrlList;
		final List<String> invalidIsWeizhuUrlList;
		final List<String> invalidEnableScoreList;
		final List<String> invalidEnableCommentList;
		final List<String> invalidEnableLikeList;
		final List<String> invalidEnableRemindList;
		final List<String> invalidEnableShareList;

		final boolean hasError;

		ParseResult(List<AdminDiscoverProtos.CreateItemRequest> itemRequestList, List<String> invalidItemNameList,
				List<String> invalidItemDescList, List<String> invalidWebUrlList, List<String> invalidIsWeizhuUrlList,
				List<String> invalidEnableScoreList, List<String> invalidEnableCommentList, List<String> invalidEnableLikeList,
				List<String> invalidEnableRemindList, List<String> invalidEnableShareList) {
			this.itemRequestList = itemRequestList;
			this.invalidItemNameList = invalidItemNameList;
			this.invalidItemDescList = invalidItemDescList;
			this.invalidWebUrlList = invalidWebUrlList;
			this.invalidIsWeizhuUrlList = invalidIsWeizhuUrlList;
			this.invalidEnableScoreList = invalidEnableScoreList;
			this.invalidEnableCommentList = invalidEnableCommentList;
			this.invalidEnableLikeList = invalidEnableLikeList;
			this.invalidEnableRemindList = invalidEnableRemindList;
			this.invalidEnableShareList = invalidEnableShareList;
			
			if (invalidItemNameList.isEmpty() && invalidItemDescList.isEmpty() && invalidWebUrlList.isEmpty() && invalidIsWeizhuUrlList.isEmpty()
					&& invalidEnableScoreList.isEmpty() && invalidEnableCommentList.isEmpty() && invalidEnableLikeList.isEmpty()
					&& invalidEnableRemindList.isEmpty() && invalidEnableShareList.isEmpty()) {
				this.hasError = false;
			} else {
				this.hasError = true;
			}
		}
	}

	private void writeImportFailLog(AdminHead head, ParseResult result) throws IOException {
		String importFailLogName = "import_fail_" + head.getCompanyId() + "_" + head.getSession().getAdminId() + "_"
				+ head.getSession().getSessionId() + ".txt";
		File importFailLogFile = new File(importFailLogDir, importFailLogName);
		Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(importFailLogFile, false)));
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (!result.invalidItemNameList.isEmpty()) {
				w.write("课件名称，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidItemNameList) {
					w.write(invalidInfo + "\r\n");
				}
			}
			if (!result.invalidItemDescList.isEmpty()) {
				w.write("课件描述，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidItemDescList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidWebUrlList.isEmpty()) {
				w.write("链接地址，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidWebUrlList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidIsWeizhuUrlList.isEmpty()) {
				w.write("是否为内部连接，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidIsWeizhuUrlList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidEnableScoreList.isEmpty()) {
				w.write("允许评分，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidEnableScoreList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidEnableCommentList.isEmpty()) {
				w.write("允许评论，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidEnableCommentList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidEnableLikeList.isEmpty()) {
				w.write("允许点赞，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidEnableLikeList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidEnableRemindList.isEmpty()) {
				w.write("允许提醒用户评分评论，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidEnableRemindList) {
					w.write(invalidInfo + "\r\n");
				}
			}

			if (!result.invalidEnableShareList.isEmpty()) {
				w.write("允许分享，导入时间: " + df.format(new Date()) + "\r\n");
				for (String invalidInfo : result.invalidEnableShareList) {
					w.write(invalidInfo + "\r\n");
				}
			}
		} finally {
			w.close();
		}
	}
}
