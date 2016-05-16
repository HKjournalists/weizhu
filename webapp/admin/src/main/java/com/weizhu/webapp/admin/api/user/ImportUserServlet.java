package com.weizhu.webapp.admin.api.user;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserService;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.ImportUserRequest;
import com.weizhu.proto.AdminUserProtos.ImportUserResponse;
import com.weizhu.proto.AdminUserProtos.RawUserExtends;


@Singleton
@SuppressWarnings("serial")
public class ImportUserServlet extends HttpServlet {
	
	private static final Logger logger = LoggerFactory.getLogger(ImportUserServlet.class);
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminUserService adminUserService;
	
	private final File uploadTmpDir;
	private final File importFailLogDir;
	
	@Inject
	public ImportUserServlet(Provider<AdminHead> adminHeadProvider, AdminUserService adminUserService,
			@Named("admin_upload_tmp_dir") File uploadTmpDir,
			@Named("admin_user_import_fail_log_dir") File importFailLogDir) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminUserService = adminUserService;
		this.uploadTmpDir = uploadTmpDir;
		this.importFailLogDir = importFailLogDir;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		
		final AdminHead head = adminHeadProvider.get();

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
		try {
			List<FileItem> fileItemList = upload.parseRequest(httpRequest);
			
			for (FileItem item : fileItemList) {
				if (item.getFieldName().equals("import_user_file") && !item.isFormField() && 
						(item.getName().endsWith(".xls") || item.getName().endsWith(".xlsx"))) {
					// parse file
					InputStream in = null;
					Workbook wb = null; 
					ParseResult result = null;
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
						if (!result.invalidInfoList.isEmpty()) {
							writeImportFailLog(head, result.invalidInfoList);
							
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_USER_INVALID");
							ret.addProperty("fail_text", "导入用户不正确，详情见错误日志");
							
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
					
					// invoke service
					
					ImportUserRequest request = ImportUserRequest.newBuilder()
							.addAllRawUser(result.rawUserList)
							.build();
					
					ImportUserResponse response = Futures.getUnchecked(adminUserService.importUser(head, request));
					
					switch(response.getResult()) {
						case SUCC:
						case FAIL_PERMISSION_DENIED:
							httpResponse.setContentType("application/json;charset=UTF-8");
							JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
							return;
						case FAIL_USER_INVALID: {
							
							List<String> invalidInfoList = new ArrayList<String>(response.getInvalidUserCount());
							for (AdminUserProtos.ImportUserResponse.InvalidUser invalidUser : response.getInvalidUserList()) {
								AdminUserProtos.RawUser rawUser = result.rawUserList.get(invalidUser.getInvalidIndex());
								List<Integer> rowIdxList = rawUser == null ? null : result.rawIdToRowIdxMap.get(rawUser.getRawId());
								
								StringBuilder sb = new StringBuilder();
								sb.append("[ROW ");
								if (rowIdxList == null || rowIdxList.isEmpty()) {
									sb.append("未知");
								} else {
									boolean isFirst = true;
									for (Integer rowId : rowIdxList) {
										if (isFirst) {
											isFirst = false;
										} else {
											sb.append(",");
										}
										sb.append(rowId + 1);
									}
								}
								
								sb.append(" 人员 ");
								if (rawUser == null) {
									sb.append("未知");
								} else {
									sb.append(rawUser.getRawId());
								}
								
								sb.append("] ");
								sb.append(invalidUser.getInvalidText());
								
								invalidInfoList.add(sb.toString());
							}
							
							writeImportFailLog(head, invalidInfoList);
							
							JsonObject ret = new JsonObject();
							ret.addProperty("result", "FAIL_USER_INVALID");
							ret.addProperty("fail_text", "导入用户不正确，详情见错误日志");
							
							httpResponse.setContentType("application/json;charset=UTF-8");
							JsonUtil.GSON.toJson(ret, httpResponse.getWriter());
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
	
	private ParseResult parseSheet(Sheet sheet) {
		Iterator<Row> rowIt = sheet.rowIterator();
		
		// 读取表头
		int teamColumnStartIdx = -1;
		int teamColumnEndIdx = -1;
		int rawIdColumnIdx = -1;
		int nameColumnIdx = -1;
		int genderColumnIdx = -1;
		int positionColumnIdx = -1;
		int levelColumnIdx = -1;
		int mobileNoIdx = -1;
		int phoneNoIdx = -1;
		int emailIdx = -1;
		int expertIdx = -1;
		int abilityTagIdx = -1;
		Map<Integer, String> extsIdxMap = new TreeMap<Integer, String>();

		{
			Row row = null;
			while (rowIt.hasNext()) {
				row = rowIt.next();
				
				String value = getValue(row.getCell(0));
				if (value.startsWith("工号")) {
					break;
				}
				row = null;
			}
	
			if (row != null) {
				Iterator<Cell> cellIt = row.cellIterator();
				
				Cell cell = cellIt.hasNext() ? cellIt.next() : null;
				while(cell != null) {
					if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell.getStringCellValue().startsWith("部门")) {
							teamColumnStartIdx = cell.getColumnIndex();
							teamColumnEndIdx = cell.getColumnIndex();
							cell = cellIt.hasNext() ? cellIt.next() : null;
							while (cell != null && (cell.getCellType() == Cell.CELL_TYPE_BLANK ||
									(cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getStringCellValue().trim().isEmpty())
									)) {
								teamColumnEndIdx = cell.getColumnIndex();
								cell = cellIt.hasNext() ? cellIt.next() : null;
							}
							continue;
						} else if (cell.getStringCellValue().startsWith("工号")) {
							rawIdColumnIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("姓名")) {
							nameColumnIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("性别")) {
							genderColumnIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("职务")) {
							positionColumnIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("职级")) {
							levelColumnIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("手机号码")) {
							mobileNoIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("座机")) {
							phoneNoIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("企业E-mail")) {
							emailIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("专家")) {
							expertIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("能力标签")) {
							abilityTagIdx = cell.getColumnIndex();
						} else if (cell.getStringCellValue().startsWith("扩展字段")) {
							Row nxtRow = sheet.getRow(row.getRowNum() + 1);
							if (nxtRow != null) {
								
								String extsName = getValue(nxtRow.getCell(cell.getColumnIndex()));
								if (!extsName.isEmpty()) {
									extsIdxMap.put(cell.getColumnIndex(), extsName);
								}
								
								cell = cellIt.hasNext() ? cellIt.next() : null;
								while (cell != null && (cell.getCellType() == Cell.CELL_TYPE_BLANK ||
										(cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getStringCellValue().trim().isEmpty())
										)) {
									extsName = getValue(nxtRow.getCell(cell.getColumnIndex()));
									if (!extsName.isEmpty()) {
										extsIdxMap.put(cell.getColumnIndex(), extsName);
									}
									cell = cellIt.hasNext() ? cellIt.next() : null;
								}
								continue;
							}
						}
					}
					cell = cellIt.hasNext() ? cellIt.next() : null;
				}
			}
		}
		
		LinkedHashMap<String, AdminUserProtos.RawUser.Builder> rawUserBuilderMap = new LinkedHashMap<String, AdminUserProtos.RawUser.Builder>();
		Map<String, List<Integer>> rawIdToRowIdMap = new HashMap<String, List<Integer>>();
		List<String> invalidInfoList = new ArrayList<String>();
		
		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			if (getValue(row.getCell(0)).isEmpty() || 
					getValue(row.getCell(0)).startsWith("例：")) {
				continue;
			}
			
			List<String> teamList = new ArrayList<String>();
			AdminUserProtos.RawUser.Builder rawUserBuilder = AdminUserProtos.RawUser.newBuilder();
			
			String position = null;
			Iterator<Cell> cellIt = row.cellIterator();
			while (cellIt.hasNext()) {
				Cell cell = cellIt.next();
				String value = getValue(cell);
				if (value != null && !value.isEmpty()) {
					if (cell.getColumnIndex() >= teamColumnStartIdx && cell.getColumnIndex() <= teamColumnEndIdx) {
						teamList.add(value);
					} else if (cell.getColumnIndex() == rawIdColumnIdx) {
						rawUserBuilder.setRawId(value);
					} else if (cell.getColumnIndex() == nameColumnIdx) {
						rawUserBuilder.setUserName(value);
					} else if (cell.getColumnIndex() == genderColumnIdx) {
						rawUserBuilder.setGender(value);
					} else if (cell.getColumnIndex() == positionColumnIdx) {
						position = value;
					} else if (cell.getColumnIndex() == levelColumnIdx) {
						rawUserBuilder.setLevel(value);
					} else if (cell.getColumnIndex() == mobileNoIdx) {
						String[] strs = value.replaceAll("\\s", "").split("\\D");
						for (String str : strs) {
							if (str != null && !str.isEmpty()) {
								rawUserBuilder.addMobileNo(str);
							}
						}
					} else if (cell.getColumnIndex() == phoneNoIdx) {
						String[] strs = value.replaceAll("\\s", "").split("[,;]");
						for (String str : strs) {
							if (str != null && !str.isEmpty()) {
								rawUserBuilder.addPhoneNo(str);
							}
						}
					} else if (cell.getColumnIndex() == emailIdx) {
						rawUserBuilder.setEmail(value);
					} else if (cell.getColumnIndex() == expertIdx) {
						if (value.equals("是")) {
							rawUserBuilder.setIsExpert(true);
						}
					} else if (cell.getColumnIndex() == abilityTagIdx) {
						String[] strs = value.replaceAll("\\s", "").split("[,;，；]");
						for (String str : strs) {
							if (str != null && !str.trim().isEmpty()) {
								rawUserBuilder.addAbilityTag(str.trim());
							}
						}
					} else if (extsIdxMap.containsKey(cell.getColumnIndex())){
						String name = extsIdxMap.get(cell.getColumnIndex());
						rawUserBuilder.addUserExts(RawUserExtends.newBuilder()
								.setName(name)
								.setValue(value)
								.build());
					}
				}
			}
			
			if (!teamList.isEmpty()) {
				AdminUserProtos.RawUserTeam.Builder rawUserTeamBuilder = AdminUserProtos.RawUserTeam.newBuilder();
				
				// String prevName = null;
				for (String teamName : teamList) {
					if (teamName != null && !teamName.isEmpty()) {
						// if (prevName == null || !teamName.equals(prevName)) {
						// 	rawUserTeamBuilder.addTeam(teamName);
						//	prevName = teamName;
						//}
						rawUserTeamBuilder.addTeam(teamName);
					}
				}
				
				if (position != null) {
					rawUserTeamBuilder.setPosition(position);
				}
				
				if (rawUserTeamBuilder.getTeamCount() > 0) {
					rawUserBuilder.addUserTeam(rawUserTeamBuilder.build());
				}
			}
			
			AdminUserProtos.RawUser.Builder builder = rawUserBuilderMap.get(rawUserBuilder.getRawId());
			if (builder == null) {
				if (!rawUserBuilder.hasRawId()) {
					invalidInfoList.add("[ROW " + (row.getRowNum() + 1) + " 人员 未知 ] 人员ID为空");
				} else if (!rawUserBuilder.hasUserName()) {
					invalidInfoList.add("[ROW " + (row.getRowNum() + 1) + " 人员 " + rawUserBuilder.getRawId() + "] 人员姓名为空");
				} else {
					rawUserBuilderMap.put(rawUserBuilder.getRawId(), rawUserBuilder);
				}
			} else {
				builder.addAllUserTeam(rawUserBuilder.getUserTeamList());
			}
			
			List<Integer> list = rawIdToRowIdMap.get(rawUserBuilder.getRawId());
			if (list == null) {
				list = new ArrayList<Integer>();
				rawIdToRowIdMap.put(rawUserBuilder.getRawId(), list);
			}
			list.add(row.getRowNum());
		}
		
		List<AdminUserProtos.RawUser> list = new ArrayList<AdminUserProtos.RawUser>(rawUserBuilderMap.size());
		for (AdminUserProtos.RawUser.Builder builder : rawUserBuilderMap.values()) {
			list.add(builder.build());
		}
		
		return new ParseResult(list, rawIdToRowIdMap, invalidInfoList);
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
	
	private static class ParseResult {
		final List<AdminUserProtos.RawUser> rawUserList;
		final Map<String, List<Integer>> rawIdToRowIdxMap;
		final List<String> invalidInfoList;
		
		ParseResult(List<AdminUserProtos.RawUser> rawUserList, Map<String, List<Integer>> rawIdToRowIdxMap, List<String> invalidInfoList) {
			this.rawUserList = rawUserList;
			this.rawIdToRowIdxMap = rawIdToRowIdxMap;
			this.invalidInfoList = invalidInfoList;
		}
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
}
