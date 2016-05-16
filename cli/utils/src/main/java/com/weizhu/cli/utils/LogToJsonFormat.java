package com.weizhu.cli.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import com.google.gson.stream.JsonWriter;
import com.weizhu.common.utils.JsonUtil;

public class LogToJsonFormat implements Callable<Integer> {
	
	private final InputStream in;
	private final PrintStream out;
	private final PrintStream err;
	private final String loggerName;
	private final boolean isJsonMessage;
	private final boolean isCheckJsonMessage;
	private final boolean isForce;
	
	public LogToJsonFormat(
			InputStream in, PrintStream out, PrintStream err, 
			String loggerName, boolean isJsonMessage, boolean isCheckJsonMessage, boolean isForce
			) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.loggerName = loggerName;
		this.isJsonMessage = isJsonMessage;
		this.isCheckJsonMessage = isCheckJsonMessage;
		this.isForce = isForce;
	}
	
	private static final Pattern LOG_PATTERN = 
			Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (TRACE|DEBUG|INFO |WARN |ERROR) ");
	
	@Override
	public Integer call() throws Exception {
		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(this.in));
		
		String line = null;
		while ((line = reader.readLine()) != null) {
			try {
				Matcher m = LOG_PATTERN.matcher(line);
				if (m.find()) {
					Date date = df.parse(m.group(1));
					String level = m.group(2).trim();
					String message = line.substring(m.end());
					
					if (this.isJsonMessage && this.isCheckJsonMessage) {
						// check json format
						JsonUtil.JSON_PARSER.parse(message);
					}
					
					StringBuilder sb = new StringBuilder();
					JsonWriter jsonWriter = new JsonWriter(CharStreams.asWriter(sb));
					try {
						jsonWriter.beginObject();
						jsonWriter.name("timestamp");
						jsonWriter.value(date.getTime());
						jsonWriter.name("level");
						jsonWriter.value(level);
						jsonWriter.name("logger_name");
						jsonWriter.value(this.loggerName);
						jsonWriter.name("message");
						if (this.isJsonMessage) {
							jsonWriter.jsonValue(message);
						} else {
							jsonWriter.value(message);
						}
						jsonWriter.endObject();
						jsonWriter.flush();
					} finally {
						try {
							jsonWriter.close();
						} catch (IOException e) {
							// ignore
						}
					}
					
					out.println(sb.toString());
				} else if (!this.isForce) {
					this.err.println("invalid log pattern : " + line);
					return 1;
				}
			} catch (Throwable th) {
				this.err.println("hand log error: " + line);
				th.printStackTrace(this.err);
				if (!this.isForce) {
					return 1;
				}
			}
		}
		return 0;
	}
	
}
