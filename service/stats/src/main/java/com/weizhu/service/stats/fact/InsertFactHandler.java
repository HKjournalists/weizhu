package com.weizhu.service.stats.fact;

import java.util.List;

import com.google.gson.JsonObject;

public interface InsertFactHandler {

	String kafkaGroupId();
	List<String> kafkaTopicList();
	String handleInsertSQL(List<JsonObject> jsonList);
}
