package com.weizhu.service.official;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.utils.ProfileManager;
import com.weizhu.proto.AdminOfficialProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.OfficialProtos.OfficialMessage;
import com.weizhu.proto.WeizhuProtos.RequestHead;

@Singleton
public class OfficialRobot {
	
	private final Executor serviceExecutor;
	
	@Inject
	public OfficialRobot(@Named("service_executor") Executor serviceExecutor) {
		this.serviceExecutor = serviceExecutor;
	}
	
	public ListenableFuture<List<OfficialProtos.OfficialMessage>> sendMessage(
			RequestHead head, long officialId, OfficialProtos.OfficialMessage msg, ProfileManager.Profile profile) {
		if (officialId == (long) (AdminOfficialProtos.ReservedOfficialId.WEIZHU_SECRETARY_VALUE)) {
			return this.sendWeizhuSecretaryMessage(head, msg, profile);
		} else {
			return null;
		}
	}
	
	private static final ProfileManager.ProfileKey<Boolean> WEIZHU_SECRETARY_ROBOT_V5KF_ENABLE = 
			ProfileManager.createKey("official:weizhu_secretary_robot_v5kf_enable", false);
	private static final ProfileManager.ProfileKey<String> WEIZHU_SECRETARY_ROBOT_V5KF_URL = 
			ProfileManager.createKey("official:weizhu_secretary_robot_v5kf_url", (String) null);
	private static final ProfileManager.ProfileKey<String> WEIZHU_SECRETARY_ROBOT_V5KF_PROXY = 
			ProfileManager.createKey("official:weizhu_secretary_robot_v5kf_proxy", (String) null);

	private static final Pattern RESERVED_PATTERN = Pattern.compile("\\[(USER|DISCOVER):(\\d+)\\]");
	
	private ListenableFuture<List<OfficialProtos.OfficialMessage>> sendWeizhuSecretaryMessage(RequestHead head, OfficialProtos.OfficialMessage msg, ProfileManager.Profile profile) {
		if (!profile.get(WEIZHU_SECRETARY_ROBOT_V5KF_ENABLE)) {
			return null;
		}
		
		if (!msg.hasText() || msg.getText().getContent().isEmpty()) {
			return Futures.immediateFuture(
					Collections.singletonList(OfficialMessage.newBuilder()
							.setMsgSeq(-1L)
							.setMsgTime(0)
							.setIsFromUser(false)
							.setText(OfficialMessage.Text.newBuilder()
									.setContent("我只认识文本信息哦!")
									.build())
							.build()));
		}
		
		final String v5kfUrl = profile.get(WEIZHU_SECRETARY_ROBOT_V5KF_URL);
		final Proxy v5kfProxy;
		
		String v5kfProxyAddr = profile.get(WEIZHU_SECRETARY_ROBOT_V5KF_PROXY);
		if (Strings.isNullOrEmpty(v5kfProxyAddr)) {
			v5kfProxy = null;
		} else {
			HostAndPort hostAndPort = HostAndPort.fromString(v5kfProxyAddr);
			v5kfProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort()));
		}
		
		final String content = msg.getText().getContent();
		
		final ListenableFutureTask<List<OfficialMessage>> task = ListenableFutureTask.create(new Callable<List<OfficialMessage>>() {

			@Override
			public List<OfficialMessage> call() throws Exception {

				String sendUrl = v5kfUrl
						.replace("${user_id}", head.getSession().getCompanyId() + "-" + head.getSession().getUserId())
						.replace("${msg}", URLEncoder.encode(content, "UTF8"));
				
				InputStream input = null;
				try {
					URLConnection conn = v5kfProxy == null ? new URL(sendUrl).openConnection() : new URL(sendUrl).openConnection(v5kfProxy);
					conn.setConnectTimeout(5000); // 5s
					conn.setReadTimeout(10000); // 10s
					
					conn.connect();
					
					input = conn.getInputStream();
					
					String responseStr = CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
					
					if (responseStr == null || responseStr.trim().isEmpty()) {
						return Collections.emptyList();
					}
					
					LinkedList<OfficialProtos.OfficialMessage> msgList = new LinkedList<OfficialProtos.OfficialMessage>();
					
					StringBuilder sb = new StringBuilder();
					Matcher matcher = RESERVED_PATTERN.matcher(responseStr.trim());
					int start = 0;
					while (matcher.find()) {
						sb.append(responseStr.substring(start, matcher.start()));
						
						String typeStr = matcher.group(1);
						String idStr = matcher.group(2);
						
						if ("USER".equals(typeStr)) {
							msgList.add(OfficialMessage.newBuilder()
								.setMsgSeq(-1L)
								.setMsgTime(0)
								.setIsFromUser(false)
								.setUser(OfficialMessage.User.newBuilder()
										.setUserId(Long.parseLong(idStr))
										.build())
								.build());
						} else if ("DISCOVER".equals(typeStr)) {
							msgList.add(OfficialMessage.newBuilder()
									.setMsgSeq(-1L)
									.setMsgTime(0)
									.setIsFromUser(false)
									.setDiscoverItem(OfficialMessage.DiscoverItem.newBuilder()
											.setItemId(Long.parseLong(idStr))
											.build())
									.build());
						}
						
						start = matcher.end();
					}
					sb.append(responseStr.substring(start));

					String msgContent = sb.toString();
					if (!msgContent.trim().isEmpty()) {
						msgList.addFirst(OfficialMessage.newBuilder()
								.setMsgSeq(-1L)
								.setMsgTime(0)
								.setIsFromUser(false)
								.setText(OfficialMessage.Text.newBuilder()
										.setContent(msgContent.trim())
										.build())
								.build());
					}
					
					return msgList;
					
				} finally {
					if (input != null) {
						try {
							input.close();
						} catch (Exception e) {
							//ignore
						}
					}
				}
			}
			
		});
		this.serviceExecutor.execute(task);
		return task;
	}
	
}
