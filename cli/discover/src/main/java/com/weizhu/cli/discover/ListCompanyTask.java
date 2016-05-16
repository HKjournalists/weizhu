package com.weizhu.cli.discover;

import java.util.concurrent.Callable;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.weizhu.common.utils.JsonUtil;

public class ListCompanyTask implements Callable<Integer> {
	
	private final String serverUrlPrefix;
	private final CloseableHttpClient serverHttpClient;
	
	public ListCompanyTask(String serverUrlPrefix, CloseableHttpClient serverHttpClient) {
		this.serverUrlPrefix = serverUrlPrefix;
		this.serverHttpClient = serverHttpClient;
	}
	
	@Override
	public Integer call() throws Exception {
		HttpGet httpGet = new HttpGet(this.serverUrlPrefix + "/api/company/get_company_list.json");
		CloseableHttpResponse response = serverHttpClient.execute(httpGet);
		try {
			String content = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
			JsonArray array = JsonUtil.tryGetArray(JsonUtil.JSON_PARSER.parse(content), "company");
			if (array != null) {
				for (JsonElement e : array) {
					Long companyId = JsonUtil.tryGetLong(e, "company_id");
					String companyName = JsonUtil.tryGetString(e, "company_name");
					if (companyId != null && companyName != null) {
						System.out.println(companyId + "\t" + companyName);
					}
				}
			}
		} finally {
			response.close();
		}
		return 0;
	}

}
