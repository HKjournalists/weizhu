package com.weizhu.cli.discover;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;
import com.google.gson.JsonElement;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminDiscoverProtos;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.AdminDiscoverProtos.CreateItemResponse;

public class ImportItemTask implements Callable<Integer> {
	
	private final String serverUrlPrefix;
	private final CloseableHttpClient serverHttpClient;
	private final String uploadUrlPrefix;
	private final String sessionKey;
	private final String dataDir;
	private final long companyId;
	
	public ImportItemTask(
			String serverUrlPrefix, CloseableHttpClient serverHttpClient,
			String uploadUrlPrefix, String sessionKey, String dataDir, long companyId
			) {
		this.serverUrlPrefix = serverUrlPrefix;
		this.serverHttpClient = serverHttpClient;
		this.uploadUrlPrefix = uploadUrlPrefix;
		this.sessionKey = sessionKey;
		this.dataDir = dataDir;
		this.companyId = companyId;
	}
	
	@Override
	public Integer call() throws Exception {
		final File excelFile = new File(this.dataDir + "/discover.xlsx");
		final File imageDir = new File(this.dataDir + "/image");
		final File documentDir = new File(this.dataDir + "/document");
		final File videoDir = new File(this.dataDir + "/video");
		final File audioDir = new File(this.dataDir + "/audio");
		
		if (!excelFile.exists()) {
			System.err.println(this.dataDir + "/discover.xlsx 不存在，无法导入");
			return 1;
		}
		
		CookieStore cookieStore = new BasicCookieStore();
		BasicClientCookie cookie = new BasicClientCookie("x-boss-session-key", sessionKey);
		URL url = new URL(uploadUrlPrefix);
		cookie.setDomain(url.getHost());
		cookie.setPath(url.getPath());
		cookieStore.addCookie(cookie);
		final CloseableHttpClient uploadHttpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).build();
		
		final XSSFWorkbook wb = new XSSFWorkbook(excelFile);
		try {
			Set<Integer> idxSet = new TreeSet<Integer>();
			final Sheet sheet = wb.getSheetAt(0);
			for (Row row : sheet) {
				if (row.getRowNum() < 2) {
					continue;
				}
				
				final Integer idx = Ints.tryParse(getValue(row.getCell(0)));
				if (idx == null || idx <= 0) {
					continue;
				}
				if (!idxSet.add(idx)) {
					System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 条目序号重复！" + idx);
					return 1;
				}
				
				AdminDiscoverProtos.CreateItemRequest.Builder builder = AdminDiscoverProtos.CreateItemRequest.newBuilder();
				builder.setItemName(getValue(row.getCell(1)));
				builder.setItemDesc(getValue(row.getCell(2)));
				builder.setEnableComment("是".equals(getValue(row.getCell(3))));
				builder.setEnableScore("是".equals(getValue(row.getCell(4))));
				builder.setEnableRemind("是".equals(getValue(row.getCell(5))));
				builder.setEnableLike("是".equals(getValue(row.getCell(6))));
				builder.setEnableShare("是".equals(getValue(row.getCell(7))));
				builder.setEnableExternalShare("是".equals(getValue(row.getCell(8))));
				
				String imageName = this.uploadImage(uploadHttpClient, imageDir, idx);
				builder.setImageName(imageName == null ? "" : imageName);
				
				final boolean enableDownload = "是".equals(getValue(row.getCell(9)));
				final String itemType = getValue(row.getCell(10));
				if ("链接".equals(itemType)) {
					String webUrl = getValue(row.getCell(11));
					if (webUrl.isEmpty()) {
						System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 链接类型条目链接为空");
						continue;
					}
					
					builder.setWebUrl(DiscoverV2Protos.WebUrl.newBuilder()
							.setWebUrl(webUrl)
							.setIsWeizhu("是".equals(getValue(row.getCell(12))))
							.build());
				} else if ("文档".equals(itemType)) {
					DiscoverV2Protos.Document doc = this.uploadDocument(uploadHttpClient, documentDir, idx, enableDownload);
					if (doc == null) {
						System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 文档类型条目上传失败");
						continue;
					}
					
					builder.setDocument(doc);
				} else if ("视频".equals(itemType)) {
					DiscoverV2Protos.Video video = this.uploadVideo(uploadHttpClient, videoDir, idx, enableDownload);
					if (video == null) {
						System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 视频类型条目上传失败");
						continue;
					}
					
					builder.setVideo(video);
				} else if ("音频".equals(itemType)) {
					DiscoverV2Protos.Audio audio = this.uploadAudio(uploadHttpClient, audioDir, idx, enableDownload);
					if (audio == null) {
						System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 音频类型条目上传失败");
						continue;
					}
					
					builder.setAudio(audio);
				} else {
					System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 未知条目类型" + itemType);
					return 1;
				}
				
				HttpPost httpPost = new HttpPost(this.serverUrlPrefix + "/api/discover/create_item.json?company_id=" + this.companyId);
				httpPost.setEntity(EntityBuilder.create().setText(JsonUtil.PROTOBUF_JSON_FORMAT.printToString(builder.build())).setContentType(ContentType.APPLICATION_JSON).build());
				
				CloseableHttpResponse httpResponse = null;
				InputStream input = null;
				try {
					httpResponse = serverHttpClient.execute(httpPost);
					input = httpResponse.getEntity().getContent();
					CreateItemResponse.Builder responseBuilder = CreateItemResponse.newBuilder();
					JsonUtil.PROTOBUF_JSON_FORMAT.merge(input, responseBuilder);
					
					if (responseBuilder.getResult() != CreateItemResponse.Result.SUCC) {
						System.err.println("row " + (row.getRowNum() + 1) + " 发生错误: 上传失败, " + responseBuilder.getResult() + ":" + responseBuilder.getFailText());
						return 1;
					}
					
					System.out.println("row " + (row.getRowNum() + 1) + "上传成功: " + responseBuilder.getItemId());
				} finally {
					if (input != null) {
						input.close();
					}
					if (httpResponse != null) {
						httpResponse.close();
					}
				}
			}
		} finally {
			wb.close();
			uploadHttpClient.close();
		}
		return 0;
	}
	
	private static String getValue(@Nullable Cell cell) {
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
	
	private String uploadImage(CloseableHttpClient uploadHttpClient, File uploadDir, int idx) throws Exception {
		File jpgImageFile = new File(uploadDir, "item_" + idx + ".jpg");
		File pngImageFile = new File(uploadDir, "item_" + idx + ".png");
		
		final File imageFile;
		if (jpgImageFile.exists()) {
			imageFile = jpgImageFile;
		} else if (pngImageFile.exists()) {
			imageFile = pngImageFile;
		} else {
			return null;
		}
		
		int retry = 0;
		while (true) {
			retry++;
			
			HttpPost httpPost = new HttpPost(this.uploadUrlPrefix + "/api/boss/upload_image.json");
			
			httpPost.setEntity(MultipartEntityBuilder.create()
					.addPart("company_id", new StringBody(Long.toString(this.companyId), ContentType.TEXT_PLAIN))
					.addPart("upload_file", new FileBody(imageFile))
					.build());
			
			CloseableHttpResponse httpResponse = null;
			try {
				httpResponse = uploadHttpClient.execute(httpPost);
				String content = EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
				
				JsonElement json = JsonUtil.JSON_PARSER.parse(content);
				
				String result = JsonUtil.tryGetString(json, "result");
				String imageName = JsonUtil.tryGetString(json, "image_name");
				if ("SUCC".equals(result) && imageName != null) {
					return imageName;
				} else {
					System.err.println("upload image " + imageFile.getName() + " fail : " + content);
					return null;
				}
			} catch (Exception e) {
				if (retry >= 3) {
					throw e;
				}
				e.printStackTrace();
				TimeUnit.SECONDS.sleep(3);
			} finally {
				if (httpResponse != null) {
					httpResponse.close();
				}
			}
		}
	}
	
	private DiscoverV2Protos.Document uploadDocument(CloseableHttpClient uploadHttpClient, File uploadDir, int idx, boolean enableDownload) throws Exception {
		final File documentFile = new File(uploadDir, "item_" + idx + ".pdf");
		if (!documentFile.exists()) {
			return null;
		}
		
		int retry = 0;
		while (true) {
			retry ++;
			
			HttpPost httpPost = new HttpPost(this.uploadUrlPrefix + "/api/boss/upload_discover_document.json");
			
			httpPost.setEntity(MultipartEntityBuilder.create()
					.addPart("company_id", new StringBody(Long.toString(this.companyId), ContentType.TEXT_PLAIN))
					.addPart("upload_file", new FileBody(documentFile))
					.build());
			
			CloseableHttpResponse httpResponse = null;
			try {
				httpResponse = uploadHttpClient.execute(httpPost);
				String content = EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
				
				JsonElement json = JsonUtil.JSON_PARSER.parse(content);
				
				String result = JsonUtil.tryGetString(json, "result");
				String fileName = JsonUtil.tryGetString(json, "file_name");
				String fileUrl = JsonUtil.tryGetString(json, "file_url");
				Integer fileSize = JsonUtil.tryGetInt(json, "file_size");
				Boolean isAuth = JsonUtil.tryGetBoolean(json, "is_auth");
				String checkMd5 = JsonUtil.tryGetString(json, "check_md5");
				
				if ("SUCC".equals(result) && fileName != null && fileUrl != null && fileSize != null && checkMd5 != null) {
					return DiscoverV2Protos.Document.newBuilder()
							.setDocumentUrl(fileUrl)
							.setDocumentType(fileName.substring(fileName.lastIndexOf('.') + 1))
							.setDocumentSize(fileSize)
							.setCheckMd5(checkMd5)
							.setIsDownload(enableDownload)
							.setIsAuthUrl(isAuth == null ? false : isAuth)
							.build();
				} else {
					System.err.println("upload document " + documentFile.getName() + " fail : " + content);
					return null;
				}
			} catch (Exception e) {
				if (retry >= 3) {
					throw e;
				}
				e.printStackTrace();
				TimeUnit.SECONDS.sleep(3);
			} finally {
				if (httpResponse != null) {
					httpResponse.close();
				}
			}
		}
	}
	
	private DiscoverV2Protos.Video uploadVideo(CloseableHttpClient uploadHttpClient, File uploadDir, int idx, boolean enableDownload) throws Exception {
		final File videoFile = new File(uploadDir, "item_" + idx + ".mp4");
		if (!videoFile.exists()) {
			return null;
		}
		
		int retry = 0;
		while (true) {
			retry ++;
			
			HttpPost httpPost = new HttpPost(this.uploadUrlPrefix + "/api/boss/upload_discover_video.json");
			
			httpPost.setEntity(MultipartEntityBuilder.create()
					.addPart("company_id", new StringBody(Long.toString(this.companyId), ContentType.TEXT_PLAIN))
					.addPart("upload_file", new FileBody(videoFile))
					.build());
			
			CloseableHttpResponse httpResponse = null;
			try {
				httpResponse = uploadHttpClient.execute(httpPost);
				String content = EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
				
				JsonElement json = JsonUtil.JSON_PARSER.parse(content);
				
				String result = JsonUtil.tryGetString(json, "result");
				String fileName = JsonUtil.tryGetString(json, "file_name");
				String fileUrl = JsonUtil.tryGetString(json, "file_url");
				Integer fileSize = JsonUtil.tryGetInt(json, "file_size");
				Integer time = JsonUtil.tryGetInt(json, "time");
				Boolean isAuth = JsonUtil.tryGetBoolean(json, "is_auth");
				String checkMd5 = JsonUtil.tryGetString(json, "check_md5");
				
				if ("SUCC".equals(result) && fileName != null && fileUrl != null && fileSize != null && time != null && checkMd5 != null) {
					return DiscoverV2Protos.Video.newBuilder()
							.setVideoUrl(fileUrl)
							.setVideoType(fileName.substring(fileName.lastIndexOf('.') + 1))
							.setVideoSize(fileSize)
							.setVideoTime(time)
							.setCheckMd5(checkMd5)
							.setIsDownload(enableDownload)
							.setIsAuthUrl(isAuth == null ? false : isAuth)
							.build();
				} else {
					System.err.println("upload video " + videoFile.getName() + " fail : " + content);
					return null;
				}
			} catch (Exception e) {
				if (retry >= 3) {
					throw e;
				}
				e.printStackTrace();
				TimeUnit.SECONDS.sleep(3);
			} finally {
				if (httpResponse != null) {
					httpResponse.close();
				}
			}
		}
	}
	
	private DiscoverV2Protos.Audio uploadAudio(CloseableHttpClient uploadHttpClient, File uploadDir, int idx, boolean enableDownload) throws Exception {
		final File audioFile = new File(uploadDir, "item_" + idx + ".mp3");
		if (!audioFile.exists()) {
			return null;
		}
		
		int retry = 0;
		while (true) {
			retry ++;
			
			HttpPost httpPost = new HttpPost(this.uploadUrlPrefix + "/api/boss/upload_discover_audio.json");
			
			httpPost.setEntity(MultipartEntityBuilder.create()
					.addPart("company_id", new StringBody(Long.toString(this.companyId), ContentType.TEXT_PLAIN))
					.addPart("upload_file", new FileBody(audioFile))
					.build());
			
			CloseableHttpResponse httpResponse = null;
			try {
				httpResponse = uploadHttpClient.execute(httpPost);
				String content = EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
				
				JsonElement json = JsonUtil.JSON_PARSER.parse(content);
				
				String result = JsonUtil.tryGetString(json, "result");
				String fileName = JsonUtil.tryGetString(json, "file_name");
				String fileUrl = JsonUtil.tryGetString(json, "file_url");
				Integer fileSize = JsonUtil.tryGetInt(json, "file_size");
				Integer time = JsonUtil.tryGetInt(json, "time");
				Boolean isAuth = JsonUtil.tryGetBoolean(json, "is_auth");
				String checkMd5 = JsonUtil.tryGetString(json, "check_md5");
				
				if ("SUCC".equals(result) && fileName != null && fileUrl != null && fileSize != null && time != null && checkMd5 != null) {
					return DiscoverV2Protos.Audio.newBuilder()
							.setAudioUrl(fileUrl)
							.setAudioType(fileName.substring(fileName.lastIndexOf('.') + 1))
							.setAudioSize(fileSize)
							.setAudioTime(time)
							.setCheckMd5(checkMd5)
							.setIsDownload(enableDownload)
							.setIsAuthUrl(isAuth == null ? false : isAuth)
							.build();
				} else {
					System.err.println("upload audio " + audioFile.getName() + " fail : " + content);
					return null;
				}
			} catch (Exception e) {
				if (retry >= 3) {
					throw e;
				}
				e.printStackTrace();
				TimeUnit.SECONDS.sleep(3);
			} finally {
				if (httpResponse != null) {
					httpResponse.close();
				}
			}
		}
	}

}
