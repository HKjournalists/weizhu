package com.weizhu.cli.utils;

import java.io.File;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.weizhu.common.cli.CmdLineParser;

public class Main {
	
	public static void main(String[] args) throws Exception {
		if (args.length <= 0) {
			printUsage();
			System.exit(2);
			return;
		}
		
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/cli/utils/logback.xml");
		}
		
		final String cmd = args[0];
		if ("LogToJsonFormat".equals(cmd)) {
			System.exit(callLogToJsonFormat(args));
		} else if ("InvokeService".equals(cmd)) {
			System.exit(callInvokeService(args));
		}
		// TODO extends
		else {
			System.err.println("unknown cmd: " + cmd);
			printUsage();
			System.exit(2);
		}
	}
	
	private static void printUsage() {
		System.err.println("Usage: [LogToJsonFormat]");
		printUsageLogToJsonFormat("\t");
		System.err.println("Usage: [InvokeService]");
		printUsageInvokeService("\t");
		// TODO extends
	}
	
	private static void printUsageLogToJsonFormat(String indent) {
		System.err.println(indent + "LogToJsonFormat");
		System.err.println(indent + "\t[-n,--logger_name]: logger_name");
		System.err.println(indent + "\t[-j,--json_messsage]: log message is json");
		System.err.println(indent + "\t[-c,--check_json_messsage]: if log message is json, check format");
		System.err.println(indent + "\t[-f,--force]: if log parse error, force to continue");
	}
	
	private static Integer callLogToJsonFormat(String[] args) throws Exception {
		final CmdLineParser parser = new CmdLineParser();
		
		final CmdLineParser.Option<String> loggerNameOption = parser.addStringOption('n', "logger_name");
		final CmdLineParser.Option<Boolean> isJsonMessageOption = parser.addBooleanOption('j', "json_messsage");
		final CmdLineParser.Option<Boolean> isCheckJsonMessageOption = parser.addBooleanOption('c', "check_json_messsage");
		final CmdLineParser.Option<Boolean> isForceOption = parser.addBooleanOption('f', "force");
		
		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsageLogToJsonFormat("");
			return 2;
		}
		
		String loggerName = parser.getOptionValue(loggerNameOption, "");
		Boolean isJsonMessage = parser.getOptionValue(isJsonMessageOption, Boolean.FALSE);
		Boolean isCheckJsonMessage = parser.getOptionValue(isCheckJsonMessageOption, Boolean.FALSE);
		Boolean isForce = parser.getOptionValue(isForceOption, Boolean.FALSE);
		
		return new LogToJsonFormat(System.in, System.out, System.err, 
				loggerName, isJsonMessage, isCheckJsonMessage, isForce).call();
	}
	
	private static void printUsageInvokeService(String indent) {
		System.err.println(indent + "InvokeService");
		System.err.println(indent + "\t[--server_addr]: server address. host:port");
		System.err.println(indent + "\t[--service_name]: service name. UserService");
		System.err.println(indent + "\t[--function_name]: function name. getUserById");
		System.err.println(indent + "\t[--head_type]: head type. ");
		System.err.println(indent + "\t[--head_data]: head data. json format");
		System.err.println(indent + "\t[--head_file]: head file. file content is json format");
	}
	
	private static Integer callInvokeService(String[] args) throws Exception {
		final CmdLineParser parser = new CmdLineParser();
		
		final CmdLineParser.Option<String> serverAddrOption = parser.addStringOption("server_addr");
		final CmdLineParser.Option<String> serviceNameOption = parser.addStringOption("service_name");
		final CmdLineParser.Option<String> functionNameOption = parser.addStringOption("function_name");
		final CmdLineParser.Option<String> headTypeOption = parser.addStringOption("head_type");
		final CmdLineParser.Option<String> headDataOption = parser.addStringOption("head_data");
		final CmdLineParser.Option<String> headFileOption = parser.addStringOption("head_file");
		
		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsageInvokeService("");
			return 2;
		}
		
		String serverAddr = parser.getOptionValue(serverAddrOption, null);
		String serviceName = parser.getOptionValue(serviceNameOption, null);
		String functionName = parser.getOptionValue(functionNameOption, null);
		String headType = parser.getOptionValue(headTypeOption, null);
		
		if (Strings.isNullOrEmpty(serverAddr)) {
			System.err.println("cannot find server_addr");
			printUsageInvokeService("");
			return 2;
		}
		if (Strings.isNullOrEmpty(serviceName)) {
			System.err.println("cannot find service_name");
			printUsageInvokeService("");
			return 2;
		}
		if (Strings.isNullOrEmpty(functionName)) {
			System.err.println("cannot find function_name");
			printUsageInvokeService("");
			return 2;
		}
		if (Strings.isNullOrEmpty(headType)) {
			System.err.println("cannot find head_type");
			printUsageInvokeService("");
			return 2;
		}
		
		String headData = parser.getOptionValue(headDataOption, null);
		String headFile = parser.getOptionValue(headFileOption, null);
		
		String headDataJson;
		if (headData != null) {
			headDataJson = headData;
		} else if (headFile != null) {
			headDataJson = Files.toString(new File(headFile), Charsets.UTF_8);
		} else {
			System.err.println("cannot find head_data or head_file");
			printUsageInvokeService("");
			return 2;
		}
		return new InvokeService(System.in, System.out, System.err, 
				serverAddr, serviceName, functionName, headType, headDataJson).call();
	}

}
