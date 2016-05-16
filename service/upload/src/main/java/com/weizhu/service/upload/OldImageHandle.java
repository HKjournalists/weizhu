package com.weizhu.service.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.process.ArrayListOutputConsumer;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.UploadProtos;

/*

java -cp weizhu-all-server-branch-francislin-201511302010-17f3af7.jar com.weizhu.service.upload.OldImageHandle /data/weizhu/www/avatar/ /data/weizhu/weizhu-upload/image/ /data/out.sql

*/
public class OldImageHandle {

	public static void main(String[] args) throws Exception {
		File srcImageDir = new File(args[0]);
		File dstImageDir = new File(args[1]);
		File sqlOutputFile = new File(args[2]);
		
		PrintWriter pw = new PrintWriter(sqlOutputFile, "UTF8");
		
		Pattern fileNamePattern = Pattern.compile("^([0-9a-f]{32})\\.(jpg|png|gif)$");
		
		for (File imageFile : srcImageDir.listFiles()) {
			try {
				if (imageFile.isDirectory()) {
					continue;
				}
				
				Matcher m = fileNamePattern.matcher(imageFile.getName().toLowerCase());
				if (!m.find()) {
					System.out.println("invalid file : " + imageFile.getName());
					continue;
				}
				
				String md5 = m.group(1);
				String type = m.group(2);
				
				if (!md5.equals(doGetMd5(imageFile))) {
					System.out.println("file " + imageFile.getName() + " md5 check fail");
				}
				
				UploadProtos.Image image = getImageInfo(imageFile, md5, (int)imageFile.length());
				if (image == null) {
					System.out.println("file " + imageFile.getName() + " invalid image format");
					continue;
				}
				if (!type.equals(image.getType())) {
					System.out.println("file " + imageFile.getName() + " invalid image suffix : " + image.getType());
				}
				
				resizeAndSaveImage(imageFile, new int[]{60, 120, 240, 480}, image, dstImageDir);
				
				pw.print("INSERT IGNORE INTO weizhu_upload_image (name, `type`, size, md5, width, hight) VALUES ('");
				pw.print(DBUtil.SQL_STRING_ESCAPER.escape(image.getName()));
				pw.print("', '");
				pw.print(DBUtil.SQL_STRING_ESCAPER.escape(image.getType()));
				pw.print("', ");
				pw.print(image.getSize());
				pw.print(", '");
				pw.print(DBUtil.SQL_STRING_ESCAPER.escape(image.getMd5()));
				pw.print("', ");
				pw.print(image.getWidth());
				pw.print(", ");
				pw.print(image.getHight());
				pw.println(");");
			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
		
		pw.close();
	}
	
	private static String doGetMd5(File imageFile) throws Exception {
		Hasher hasher = Hashing.md5().newHasher();
		
		FileInputStream in = new FileInputStream(imageFile);
		ByteStreams.copy(in, Funnels.asOutputStream(hasher));
		in.close();
		
		return hasher.hash().toString();
	}
	
	private static UploadProtos.Image getImageInfo(File imageFile, String md5, int size) {
		IMOperation op = new IMOperation();
		op.ping();
		op.format("%m\n%w\n%h\n");
		op.addImage(imageFile.getAbsolutePath());
		
		IdentifyCmd identifyCmd = new IdentifyCmd();
		identifyCmd.setSearchPath("/usr/bin");
		
		ArrayListOutputConsumer output=new ArrayListOutputConsumer();
		identifyCmd.setOutputConsumer(output);
		try {
			identifyCmd.run(op);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IM4JavaException e) {
			throw new RuntimeException(e);
		}
		ArrayList<String> cmdOutput = output.getOutput();
		if (cmdOutput.size() < 3) {
			return null;
		}
		
		UploadProtos.Image.Builder builder = UploadProtos.Image.newBuilder();
		
		String format = cmdOutput.get(0).trim();
		if ("JPEG".equals(format)) {
			builder.setType("jpg");
		} else if ("PNG".equals(format)){
			builder.setType("png");
		} else if ("GIF".equals(format)){
			builder.setType("gif");
		} else {
			return null;
		}
		
		try {
			builder.setWidth(Integer.parseInt(cmdOutput.get(1)));
			builder.setHight(Integer.parseInt(cmdOutput.get(2)));
		} catch (NumberFormatException e) {
			return null;
		}
		builder.setName(md5 + "." + builder.getType());
		builder.setSize(size);
		builder.setMd5(md5);
		return builder.build();
	}
	
	private static void resizeAndSaveImage(File tmpImageFile, int[] thumbSizes, UploadProtos.Image image, File dstImageDir) throws Exception {
		// 为了防止单个文件夹下的文件过多，加一个二级目录
		final String localSaveFolder = image.getName().substring(0, 2);
		
		File originalFile = new File(dstImageDir.getAbsolutePath() + "/original/" + localSaveFolder + "/"+ image.getName());
		Files.createParentDirs(originalFile);
		Files.copy(tmpImageFile, originalFile);
		
		for (int thumbSize : thumbSizes) {
			File thumbFile = new File(dstImageDir.getAbsolutePath() + "/thumb" + thumbSize + "/" + localSaveFolder + "/"+ image.getName());
			Files.createParentDirs(thumbFile);
			
			IMOperation op = new IMOperation();  
	        op.addImage(tmpImageFile.getAbsolutePath());  
	        op.resize(thumbSize, thumbSize, '>');
	        op.addImage(thumbFile.getAbsolutePath());
	        
	        ConvertCmd convertCmd = new ConvertCmd();  
	        convertCmd.setSearchPath("/usr/bin");  
	        convertCmd.run(op);
		}
	}
}
