package com.weizhu.service.apns.test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.weizhu.common.utils.HexUtil;

public class CreateCertSQL {

	public static void main(String[] args) throws Exception {

		File dir = new File(Resources.getResource("com/weizhu/service/apns/test/cert/").getFile());
		
		Map<String, byte[]> certDataMap = new HashMap<String, byte[]>();
		Map<String, String> certPassMap = new HashMap<String, String>();
		for (File f : dir.listFiles()) {
			final String fileName = f.getName();
			if (fileName.endsWith(".p12")) {
				certDataMap.put(fileName.substring(0, fileName.length() - 4), Files.toByteArray(f));
			} else if (fileName.endsWith(".pass")) {
				certPassMap.put(fileName.substring(0, fileName.length() - 5), Files.toString(f, Charsets.UTF_8));
			}
		}
		
		int expireTime = (int) (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2016-12-23 00:00:00").getTime() / 1000L);
		
		for (String name : certDataMap.keySet()) {
			String appId = name.split("_")[0];
			boolean isProduction = name.endsWith("_production");
			byte[] data = certDataMap.get(name);
			String pass = certPassMap.get(name);
			
			if (data != null && pass != null) {
				StringBuilder sql = new StringBuilder();
				sql.append("REPLACE INTO weizhu_apns_cert (app_id, is_production, cert_p12, cert_pass, expired_time) VALUES ('");
				sql.append(appId).append("', ");
				sql.append(isProduction ? "1" : "0").append(", UNHEX('");
				sql.append(HexUtil.bin2Hex(data)).append("'), '");
				sql.append(pass).append("', ");
				sql.append(expireTime).append("); ");
				
				System.out.println(sql.toString());
			}
			
		}
	}
	
}
