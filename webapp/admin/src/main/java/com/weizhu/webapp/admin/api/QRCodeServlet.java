package com.weizhu.webapp.admin.api;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class QRCodeServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		String content = ParamUtil.getString(httpRequest, "content", "");
		if (content.isEmpty()) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		int size = ParamUtil.getInt(httpRequest, "size", 120);
		if (size < 60) {
			size = 60;
		} else if (size > 600) {
			size = 600;
		}
		
		int onColor = ParamUtil.getInt(httpRequest, "on_color", MatrixToImageConfig.BLACK);
		int offColor = ParamUtil.getInt(httpRequest, "off_color", MatrixToImageConfig.WHITE);
		
		Map<EncodeHintType, Object> hintsMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
		hintsMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hintsMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		
		BitMatrix bitMatrix;
		try {
			bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hintsMap);
		} catch (WriterException e) {
			throw new ServletException(e);
		}
		
		httpResponse.setContentType("image/jpeg");
		MatrixToImageWriter.writeToStream(bitMatrix, "jpeg", httpResponse.getOutputStream(), new MatrixToImageConfig(onColor, offColor));
	}
	
}
