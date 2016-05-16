package com.weizhu.cli.discover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.DiscoverV2Protos;
import com.weizhu.proto.SystemProtos.GetAuthUrlResponse;
import com.weizhu.proto.SystemProtos.GetBossConfigResponse;
import com.weizhu.proto.AdminDiscoverProtos.GetItemListResponse;

public class ExportItemTask implements Callable<Integer> {
	
	private final String serverUrlPrefix;
	private final CloseableHttpClient serverHttpClient;
	@SuppressWarnings("unused")
	private final String sessionKey;
	private final String dataDir;
	private final long companyId;
	
	public ExportItemTask(
			String serverUrlPrefix, CloseableHttpClient serverHttpClient,
			String sessionKey, String dataDir, long companyId
			) {
		this.serverUrlPrefix = serverUrlPrefix;
		this.serverHttpClient = serverHttpClient;
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
		
		if (excelFile.exists()) {
			System.err.println(this.dataDir + "/discover.xlsx 已经存在，无法导出");
			return 1;
		}
		
		if (imageDir.exists()) {
			if (!imageDir.isDirectory()) {
				System.err.println(this.dataDir + "/image 不是目录，无法导出");
				return 1;
			}
		} else {
			imageDir.mkdir();
		}
		
		if (documentDir.exists()) {
			if (!documentDir.isDirectory()) {
				System.err.println(this.dataDir + "/document 不是目录，无法导出");
				return 1;
			}
		} else {
			documentDir.mkdir();
		}
		
		if (videoDir.exists()) {
			if (!videoDir.isDirectory()) {
				System.err.println(this.dataDir + "/video 不是目录，无法导出");
				return 1;
			}
		} else {
			videoDir.mkdir();
		}
		
		if (audioDir.exists()) {
			if (!audioDir.isDirectory()) {
				System.err.println(this.dataDir + "/audio 不是目录，无法导出");
				return 1;
			}
		} else {
			audioDir.mkdir();
		}
		
		final String imageUrlPrefix = this.getImageUrlPrefix();
		
		final CloseableHttpClient downloadHttpClient = HttpClients.createDefault();
		final XSSFWorkbook wb = new XSSFWorkbook(Resources.getResource("com/weizhu/cli/discover/discover.xlsx").openStream());
		try {
			XSSFCellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			XSSFFont font = wb.createFont();
			font.setFontHeight(12);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);
			
			int start = 0;
			final int length = 20;
			int idx = 1;
			final Sheet sheet = wb.getSheetAt(0);
			while (true) {
				final GetItemListResponse response;
				
				HttpGet httpGet = new HttpGet(this.serverUrlPrefix + "/api/discover/get_item_list.json?company_id=" + this.companyId + "&start=" + start + "&length=" + length + "&order_create_time_asc=true");
				CloseableHttpResponse httpResponse = serverHttpClient.execute(httpGet);
				try {
					InputStream input = httpResponse.getEntity().getContent();
					try {
						GetItemListResponse.Builder builder = GetItemListResponse.newBuilder();
						JsonUtil.PROTOBUF_JSON_FORMAT.merge(input, builder);
						response = builder.build();
					} finally {
						input.close();
					}
				} finally {
					httpResponse.close();
				}
				
				for (DiscoverV2Protos.Item item : response.getItemList()) {
					if (item.getBase().getContentCase() == DiscoverV2Protos.Item.Base.ContentCase.WEB_URL
							|| item.getBase().getContentCase() == DiscoverV2Protos.Item.Base.ContentCase.DOCUMENT
							|| item.getBase().getContentCase() == DiscoverV2Protos.Item.Base.ContentCase.VIDEO
							|| item.getBase().getContentCase() == DiscoverV2Protos.Item.Base.ContentCase.AUDIO
							) {
						Row row = sheet.createRow(idx + 1);
						
						row.createCell(0).setCellValue(idx);
						row.createCell(1).setCellValue(item.getBase().getItemName());
						row.createCell(2).setCellValue(item.getBase().getItemDesc());
						row.createCell(3).setCellValue(item.getBase().getEnableComment() ? "是": "否");
						row.createCell(4).setCellValue(item.getBase().getEnableScore() ? "是": "否");
						row.createCell(5).setCellValue(item.getBase().getEnableRemind() ? "是": "否");
						row.createCell(6).setCellValue(item.getBase().getEnableLike() ? "是": "否");
						row.createCell(7).setCellValue(item.getBase().getEnableShare() ? "是": "否");
						row.createCell(8).setCellValue(item.getBase().getEnableExternalShare() ? "是": "否");
						
						switch (item.getBase().getContentCase()) {
							case WEB_URL:
								row.createCell(10).setCellValue("链接");
								row.createCell(11).setCellValue(item.getBase().getWebUrl().getWebUrl());
								row.createCell(12).setCellValue(item.getBase().getWebUrl().getIsWeizhu() ? "是": "否");
								break;
							case DOCUMENT:
								row.createCell(9).setCellValue(item.getBase().getDocument().getIsDownload() ? "是": "否");
								row.createCell(10).setCellValue("文档");
								break;
							case VIDEO:
								row.createCell(9).setCellValue(item.getBase().getVideo().getIsDownload() ? "是": "否");
								row.createCell(10).setCellValue("视频");
								break;
							case AUDIO:
								row.createCell(9).setCellValue(item.getBase().getAudio().getIsDownload() ? "是": "否");
								row.createCell(10).setCellValue("音频");
								break;
							default:
								break;
						}
						
						for(int i=0; i<=12; ++i) {
							Cell cell = row.getCell(i);
							if (cell == null) {
								cell = row.createCell(i);
								cell.setCellValue("");
							}
							cell.setCellStyle(cellStyle);
						}
						
						// download image
						this.downloadImage(imageUrlPrefix, item.getBase().getImageName(), downloadHttpClient, "item_" + idx);
						
						switch (item.getBase().getContentCase()) {
							case DOCUMENT:
								this.downloadFile(
										item.getBase().getDocument().getDocumentUrl(), 
										item.getBase().getDocument().getDocumentType(), 
										item.getBase().getDocument().getIsAuthUrl(), 
										downloadHttpClient, documentDir, "item_" + idx);
								break;
							case VIDEO:
								this.downloadFile(
										item.getBase().getVideo().getVideoUrl(), 
										item.getBase().getVideo().getVideoType(), 
										item.getBase().getVideo().getIsAuthUrl(), 
										downloadHttpClient, videoDir, "item_" + idx);
								break;
							case AUDIO:
								this.downloadFile(
										item.getBase().getAudio().getAudioUrl(), 
										item.getBase().getAudio().getAudioType(), 
										item.getBase().getAudio().getIsAuthUrl(), 
										downloadHttpClient, audioDir, "item_" + idx);
								break;
							default:
								break;
						}
						
						idx++;
					}
				}
				
				start += length;
				if (start >= response.getFilteredSize()) {
					break;
				}
			}
			
			OutputStream output = new FileOutputStream(excelFile);
			try {
				wb.write(output);
			} finally {
				output.close();
			}
		} finally {
			wb.close();
			downloadHttpClient.close();
		}
		return 0;
	}
	
	private String getImageUrlPrefix() throws Exception {
		CloseableHttpResponse response = null;
		InputStream input = null;
		try {
			response = serverHttpClient.execute(new HttpGet(this.serverUrlPrefix + "/api/get_boss_config.json"));
			input = response.getEntity().getContent();
			GetBossConfigResponse.Builder builder = GetBossConfigResponse.newBuilder();
			JsonUtil.PROTOBUF_JSON_FORMAT.merge(input, builder);
			return builder.getImage().getImageUrlPrefix();
		} finally {
			Closeables.close(input, false);
			Closeables.close(response, false);
		}
	}
	
	private void downloadImage(String imageUrlPrefix, String imageName, CloseableHttpClient downloadHttpClient, String downloadFileNamePrefix) throws Exception {
		int dotIdx = imageName.lastIndexOf('.');
		if (dotIdx < 0) {
			return;
		}
		
		final String downloadFileName = downloadFileNamePrefix + imageName.substring(dotIdx);
		final String imageUrl = imageUrlPrefix + imageName;
		
		System.out.println("下载图片" + downloadFileName + ": " + imageUrl);
		
		CloseableHttpResponse response = null;
		InputStream input = null;
		OutputStream output = null;
		try {
			response = downloadHttpClient.execute(new HttpGet(imageUrl));
			input = response.getEntity().getContent();
			output = new FileOutputStream(this.dataDir + "/image/" + downloadFileName);
			ByteStreams.copy(input, output);
		} finally {
			Closeables.close(output, false);
			Closeables.close(input, false);
			Closeables.close(response, false);
		}
	}
	
	private void downloadFile(String fileUrl, String fileType, boolean isAuthUrl, CloseableHttpClient downloadHttpClient, File downloadDir, String downloadFileNamePrefix) throws Exception {
		if (fileUrl.isEmpty() || fileType.isEmpty()) {
			return;
		}
		
		if (isAuthUrl) {
			CloseableHttpResponse response = null;
			InputStream input = null;
			try {
				response = serverHttpClient.execute(new HttpGet(this.serverUrlPrefix + "/api/discover/get_auth_url.json?company_id=" + this.companyId + "&url=" + URLEncoder.encode(fileUrl, "UTF-8")));
				input = response.getEntity().getContent();
				GetAuthUrlResponse.Builder builder = GetAuthUrlResponse.newBuilder();
				JsonUtil.PROTOBUF_JSON_FORMAT.merge(input, builder);
				fileUrl = builder.getAuthUrl();
			} finally {
				Closeables.close(input, false);
				Closeables.close(response, false);
			}
		}
		
		final String downloadFileName = downloadFileNamePrefix + "." + fileType;
		
		System.out.println("下载文件" + downloadFileName + ": " + fileUrl);
		
		CloseableHttpResponse response = null;
		InputStream input = null;
		OutputStream output = null;
		try {
			response = downloadHttpClient.execute(new HttpGet(fileUrl));
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return;
			}
			
			input = response.getEntity().getContent();
			output = new FileOutputStream(new File(downloadDir, downloadFileName));
			ByteStreams.copy(input, output);
		} finally {
			Closeables.close(output, false);
			Closeables.close(input, false);
			Closeables.close(response, false);
		}
	}
	
}
