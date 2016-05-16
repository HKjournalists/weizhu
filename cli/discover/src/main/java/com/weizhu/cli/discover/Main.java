package com.weizhu.cli.discover;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.apache.http.client.CookieStore;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import com.weizhu.common.cli.CmdLineParser;

public class Main {

	/**
	 * weizhu-discover-cli.jar
	 * weizhu-discover-cli.bat
	 * 
	 * data
	 *   |--discover.xlsx
	 *   |--image
	 *   |   `--item_icon_1.png
	 *   |--video
	 *   |   `--item_1.mp4
	 *   |--document
	 *   |   `--item_1.pdf
	 *   `--audio
	 *       `--item_1.mp3
	 * 
	 * import
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/cli/discover/logback.xml");
		}
		
		final CmdLineParser parser = new CmdLineParser();
		
		final CmdLineParser.Option<String> serverUrlPrefixOption = parser.addStringOption('s', "server_url_prefix");
		final CmdLineParser.Option<String> serverTrustStoreTypeOption = parser.addStringOption("server_trust_store_type");
		final CmdLineParser.Option<String> serverTrustStoreOption = parser.addStringOption("server_trust_store");
		final CmdLineParser.Option<String> serverTrustStorePasswordOption = parser.addStringOption("server_trust_store_password");
		final CmdLineParser.Option<String> serverKeyStoreTypeOption = parser.addStringOption("server_key_store_type");
		final CmdLineParser.Option<String> serverKeyStoreOption = parser.addStringOption("server_key_store");
		final CmdLineParser.Option<String> serverKeyStorePasswordOption = parser.addStringOption("server_key_store_password");
		
		final CmdLineParser.Option<String> sessionKeyOption = parser.addStringOption("session_key");
		final CmdLineParser.Option<Boolean> isListCompanyOption = parser.addBooleanOption('l', "list_company");
		final CmdLineParser.Option<Boolean> isExportOption = parser.addBooleanOption('e', "export");
		final CmdLineParser.Option<Boolean> isImportOption = parser.addBooleanOption('i', "import");
		final CmdLineParser.Option<String> dataDirOption = parser.addStringOption('d', "data_dir");
		final CmdLineParser.Option<Long> companyIdOption = parser.addLongOption('c', "company_id");
		final CmdLineParser.Option<String> uploadUrlPrefixOption = parser.addStringOption("upload_url_prefix");
		
		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
			return;
		}
		
		String serverUrlPrefix = parser.getOptionValue(serverUrlPrefixOption);
		if (serverUrlPrefix == null || serverUrlPrefix.trim().isEmpty()) {
			System.err.println("server_url_prefix 参数不能为空！");
			printUsage();
			System.exit(2);
			return;
		}
		
		String serverTrustStoreType = parser.getOptionValue(serverTrustStoreTypeOption);
		String serverTrustStore = parser.getOptionValue(serverTrustStoreOption);
		String serverTrustStorePassword = parser.getOptionValue(serverTrustStorePasswordOption);
		String serverKeyStoreType = parser.getOptionValue(serverKeyStoreTypeOption);
		String serverKeyStore = parser.getOptionValue(serverKeyStoreOption);
		String serverKeyStorePassword = parser.getOptionValue(serverKeyStorePasswordOption);
		
		String sessionKey = parser.getOptionValue(sessionKeyOption);
		if (sessionKey == null || sessionKey.trim().isEmpty()) {
			System.err.println("session_key 参数不能为空！");
			printUsage();
			System.exit(2);
			return;
		}
		
		final CloseableHttpClient serverHttpClient;
		try {
			serverHttpClient = buildServerHttpClient(
					serverUrlPrefix,
					serverTrustStoreType, 
					serverTrustStore, 
					serverTrustStorePassword, 
					serverKeyStoreType, 
					serverKeyStore, 
					serverKeyStorePassword, 
					sessionKey);
		} catch (Exception e) {
			System.err.println("serverHttpClient 构建失败！");
			e.printStackTrace(System.err);
			printUsage();
			System.exit(2);
			return;
		}
		try {
			boolean isListCompany = parser.getOptionValue(isListCompanyOption, Boolean.FALSE);
			boolean isExport = parser.getOptionValue(isExportOption, Boolean.FALSE);
			boolean isImport = parser.getOptionValue(isImportOption, Boolean.FALSE);
			String dataDir = parser.getOptionValue(dataDirOption, null);
			Long companyId = parser.getOptionValue(companyIdOption, null);
			String uploadUrlPrefix = parser.getOptionValue(uploadUrlPrefixOption, null);
			
			if (isListCompany && !isExport && !isImport) {
				
				System.exit(new ListCompanyTask(serverUrlPrefix, serverHttpClient).call());
				
			} else if (!isListCompany && isExport && !isImport) {
				if (dataDir == null || companyId == null) {
					System.err.println("dataDir 和 company_id 参数不能为空！");
					printUsage();
					System.exit(2);
					return;
				}
				
				System.exit(new ExportItemTask(serverUrlPrefix, serverHttpClient, sessionKey, dataDir, companyId).call());

			} else if (!isListCompany && !isExport && isImport) {
				if (dataDir == null || companyId == null || uploadUrlPrefix == null) {
					System.err.println("dataDir, company_id 和 upload_url_prefix 参数不能为空！");
					printUsage();
					System.exit(2);
					return;
				}
				
				System.exit(new ImportItemTask(serverUrlPrefix, serverHttpClient, uploadUrlPrefix, sessionKey, dataDir, companyId).call());
				
			} else {
				System.err.println("list_company, export, import 三个参数必须填一个，并且不能同时出现");
				printUsage();
				System.exit(2);
				return;
			}
		} catch (Exception e) {
			System.err.println("执行异常");
			e.printStackTrace(System.err);
			printUsage();
			System.exit(2);
		} finally {
			serverHttpClient.close();
		}
	}
	
	private static void printUsage() {
		System.err.println("[-s, --server_url_prefix]: 服务端http url地址，例如: https://boss.wehelpu.cn:8443/boss");
		System.err.println("[--server_trust_store_type, --server_trust_store, --server_trust_store_password]: server trust cert");
		System.err.println("[--server_key_store_type, --server_key_store, --server_key_store_password]: server key cert");
		System.err.println("[--session_key]: 会话key");
		System.err.println("[-l, --list_company]: 列出所有公司信息");
		System.err.println("[-e, --export]: 导出发现数据");
		System.err.println("[-i, --import]: 导入发现数据");
		System.err.println("[-d, --data_dir]: 发现数据存放目录");
		System.err.println("[-c, --company_id]: 导入／导出的公司id");
	}
	
	private static CloseableHttpClient buildServerHttpClient(
			String serverUrlPrefix,
			@Nullable String serverTrustStoreType,
			@Nullable String serverTrustStore, 
			@Nullable String serverTrustStorePassword, 
			@Nullable String serverKeyStoreType, 
			@Nullable String serverKeyStore, 
			@Nullable String serverKeyStorePassword,
			String sessionKey
			) throws Exception {
		
		final KeyStore trustStore;
		if (serverTrustStore == null ) {
			trustStore = null;
		} else {
			trustStore = KeyStore.getInstance(serverTrustStoreType == null ? KeyStore.getDefaultType() : serverTrustStoreType);
			trustStore.load(new FileInputStream(serverTrustStore), serverTrustStorePassword == null ? new char[0] : serverTrustStorePassword.toCharArray());			
		}
		
		final KeyStore keyStore;
		if (serverKeyStore == null ) {
			keyStore = null;
		} else {
			keyStore = KeyStore.getInstance(serverKeyStoreType == null ? KeyStore.getDefaultType() : serverKeyStoreType);
			keyStore.load(new FileInputStream(serverKeyStore), serverKeyStorePassword == null ? new char[0] : serverKeyStorePassword.toCharArray());			
		}
		
		final SSLContext sslContext;
		if (trustStore == null && keyStore == null) {
			sslContext = null;
		} else {
			SSLContextBuilder builder = SSLContexts.custom();
			if (trustStore != null) {
				builder.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy());
			}
			if (keyStore != null) {
				builder.loadKeyMaterial(keyStore, serverKeyStorePassword == null ? new char[0] : serverKeyStorePassword.toCharArray());
			}
			sslContext = builder.build();
		}
		
		final HttpClientBuilder httpClientBuilder = HttpClients.custom();
		
		if (sslContext != null) {
			httpClientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext));
		}
		
		CookieStore cookieStore = new BasicCookieStore();
		BasicClientCookie cookie = new BasicClientCookie("x-boss-session-key", sessionKey);
		URL url = new URL(serverUrlPrefix);
		cookie.setDomain(url.getHost());
		cookie.setPath(url.getPath());
		cookieStore.addCookie(cookie);
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		
		return httpClientBuilder.build();
	}

}
