package com.weizhu.common.utils;

import java.util.List;

public class DataPage<T> {
	private final List<T> dataList;
	private final int totalSize;
	private final int filteredSize;
	
	public DataPage(List<T> dataList, int totalSize, int filteredSize) {
		this.dataList = dataList;
		this.totalSize = totalSize;
		this.filteredSize = filteredSize;
	}
	
	public List<T> dataList() {
		return dataList;
	}
	
	public int totalSize() {
		return totalSize;
	}
	
	public int filteredSize() {
		return filteredSize;
	}
}
