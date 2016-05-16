package com.weizhu.service.system.test;

import com.google.protobuf.ExtensionRegistry;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.SystemProtos;

public class PrintNewVersion {

	public static void main(String[] args) throws ParseException {
		
		SystemProtos.NewVersion newVersion = SystemProtos.NewVersion.newBuilder()
				.setVersionName("微助android 1.1.5")
				.setVersionCode(56)
				.setFeatureText("1，新增发现下载，离线内容想看就看。\n2，新风格界面，更清新。\n3，社区新增热门评论。\n4，更多细节优化。")
				.setDownloadUrl("http://dn-weizhu-app.qbox.me/android/tag/1.1.5_56/feihe_1.1.5_56.apk")
				.setCheckMd5("8469f66a7866f924735b0e801701b8dd")
				.build();
		
		String jsonStr = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(newVersion);
		
		System.out.println(DBUtil.SQL_STRING_ESCAPER.escape(jsonStr));
		
		newVersion = SystemProtos.NewVersion.newBuilder()
				.setVersionName("微助iphone 1.1.5")
				.setVersionCode(58)
				.setFeatureText("1，新增发现下载，离线内容想看就看。\n2，新风格界面，更清新。\n3，社区新增热门评论。\n4，更多细节优化。")
				.setDownloadUrl("http://wehelpu.cn/download/feihe")
				.build();
		
		jsonStr = JsonUtil.PROTOBUF_JSON_FORMAT.printToString(newVersion);
		
		System.out.println(DBUtil.SQL_STRING_ESCAPER.escape(jsonStr));
		
		SystemProtos.NewVersion.Builder builder = SystemProtos.NewVersion.newBuilder();
		
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(jsonStr, ExtensionRegistry.getEmptyRegistry(), builder);
		
		System.out.println(newVersion.equals(builder.build()));
		
	}

}
