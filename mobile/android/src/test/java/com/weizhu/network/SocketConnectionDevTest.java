package com.weizhu.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.weizhu.proto.OfficialProtos.OfficialMessagePush;
import com.weizhu.proto.ServiceProxy;
import com.weizhu.proto.UserService;
import com.weizhu.proto.WebRTCProtos.WebRTCAnswerCallMessagePush;
import com.weizhu.proto.WebRTCProtos.WebRTCHangUpCallMessagePush;
import com.weizhu.proto.WebRTCProtos.WebRTCIceCandidateMessagePush;
import com.weizhu.proto.WebRTCProtos.WebRTCIncomingCallMessagePush;
import com.weizhu.proto.WeizhuProtos;
import com.weizhu.proto.CommunityProtos.CommunityCommentMessagePush;
import com.weizhu.proto.CommunityProtos.CommunityPostMessagePush;
import com.weizhu.proto.IMProtos.IMGroupStatePush;
import com.weizhu.proto.IMProtos.IMP2PMessagePush;
import com.weizhu.proto.IMProtos.IMP2PStatePush;
import com.weizhu.proto.SystemProtos.SystemConfigStatePush;
import com.weizhu.proto.SystemProtos.SystemNewVersionStatePush;
import com.weizhu.proto.UserProtos.GetUserByIdRequest;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.WeizhuProtos.TestPush;
import com.weizhu.proto.WeizhuProtos.SocketEstablishResponse.Result;

@org.junit.Ignore
public class SocketConnectionDevTest {

	private static SocketConnection socketConn;
	
	private static UserService userService;
	
	// 10000124196:265b69b0c448ddd51c3d483624a239a8de3b27ce3bbddd6b91ce545e1fd64c7c
	// 10000124207:265b69b0c448ddd57a9169dba3d7754cdde4f2826839acf23c7952cfceb20eb5
	private static final ByteString sessionKey = ByteString.copyFrom(HexUtil.hex2bin("265b69b0c448ddd51c3d483624a239a8de3b27ce3bbddd6b91ce545e1fd64c7c"));
	
	@BeforeClass
	public static void init() {
		
		WeizhuProtos.Weizhu weizhuVersion = WeizhuProtos.Weizhu.newBuilder()
				.setPlatform(WeizhuProtos.Weizhu.Platform.ANDROID)
				.setVersionName("1.0.0")
				.setVersionCode(0)
				.setStage(WeizhuProtos.Weizhu.Stage.ALPHA)
				.setBuildTime((int)(System.currentTimeMillis()/1000L))
				.build();
		
		WeizhuProtos.Android android = WeizhuProtos.Android.newBuilder()
				.setDevice("device")
				.setManufacturer("LGE")
				.setBrand("google")
				.setModel("Nexus 5")
				.setSerial("02c288c1f0a697d9")
				.setRelease("4.4.4")
				.setSdkInt(19)
				.setCodename("REL")
				.build();
		
		socketConn = new SocketConnection(new TestListener(), 3, weizhuVersion, android);
		
		socketConn.connect(new InetSocketAddress("218.241.220.36", 8098), sessionKey, 0);
		
		userService = ServiceProxy.create(UserService.class, socketConn);
	}
	
	@Test
	public void test() throws Throwable {
		TimeUnit.SECONDS.sleep(60);
		
		long begin = System.currentTimeMillis();
		GetUserResponse response = userService.getUserById(GetUserByIdRequest.newBuilder().addUserId(10000124196L).build(), 0).get();
		long time = System.currentTimeMillis() - begin;
		System.out.println(time + "(ms) user : " + response.getUser(0).getBase().getUserName());
		
		TimeUnit.SECONDS.sleep(1800);
	}
	
	@AfterClass
	public static void destory() {
		
	}
	
	private static class TestListener implements PushListener {
		
		@Override
		public void log(InetSocketAddress remoteAddress, String msg, Throwable th) {
			printMsg("[DEBUG]\t" + remoteAddress + "\t" + msg, th);
		}
		
		@Override
		public void onEstablishing(InetSocketAddress remoteAddress) {
			printMsg("[INFO]\t" + remoteAddress + "\tonEstablishing", null);
		}
		
		@Override
		public void onWorking(InetSocketAddress remoteAddress) {
			printMsg("[INFO]\t" + remoteAddress + "\tonWorking", null);
		}

		@Override
		public void onTerminate(InetSocketAddress remoteAddress) {
			printMsg("[INFO]\t" + remoteAddress + "\tonTerminate", null);
		}
		
		@Override
		public void onVerifyFail(InetSocketAddress remoteAddress, Result failResult, String failText) {
			printMsg("[INFO]\t" + remoteAddress + "\tonVerifyFail\t" + failResult + "," + failText, null);
		}
		
		@Override
		public void onException(InetSocketAddress remoteAddress, Throwable e) {
			printMsg("[INFO]\t" + remoteAddress + "\tonException", e);
		}

		@Override
		public void onIOException(InetSocketAddress remoteAddress, IOException e) {
			printMsg("[INFO]\t" + remoteAddress + "\tonIOException", e);
		}

		@Override
		public void onResetPushSeq(long pushSeq) {
			printMsg("[INFO]\tonResetPushSeq\t" + pushSeq, null);
		}

		@Override
		public void onTestPush(long pushSeq, TestPush pushMsg, boolean hasMore) {
			printMsg("[INFO]\tonTestPush\t" + pushSeq + "," + pushMsg.getMessage() + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onIMP2PMessagePush(long pushSeq, IMP2PMessagePush p2pMsg, boolean hasMore) {
			printMsg("[INFO]\tonIMP2PMessagePush\t" + pushSeq + "," + p2pMsg.getUserId() + "," + p2pMsg.getMsg().getMsgSeq() + "," + (hasMore ? "(hasMore)" : ""), null);	
		}

		@Override
		public void onIMP2PStatePush(long pushSeq, IMP2PStatePush p2pState, boolean hasMore) {
			printMsg("[INFO]\tonIMP2PStatePush\t" + pushSeq + "," + p2pState.getUserId() + "," + (hasMore ? "(hasMore)" : ""), null);	
		}

		@Override
		public void onIMGroupStatePush(long pushSeq, IMGroupStatePush groupState, boolean hasMore) {
			printMsg("[INFO]\tonIMGroupStatePush\t" + pushSeq + "," + groupState.getGroupId() + "," + (hasMore ? "(hasMore)" : ""), null);	
		}
		
		@Override
		public void onSystemConfigStatePush(long pushSeq, SystemConfigStatePush configState, boolean hasMore) {
			printMsg("[INFO]\tonSystemConfigStatePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);	
		}

		@Override
		public void onSystemNewVersionStatePush(long pushSeq, SystemNewVersionStatePush newVersionState, boolean hasMore) {
			printMsg("[INFO]\tonSystemNewVersionStatePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);	
		}
		
		@Override
		public void onOfficialMessagePush(long pushSeq, OfficialMessagePush messagePush, boolean hasMore) {
			printMsg("[INFO]\tonOfficialMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);	
		}
		
		@Override
		public void onCommunityPostMessagePush(long pushSeq, CommunityPostMessagePush postPush, boolean hasMore) {
			printMsg("[INFO]\tonCommunityPostMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onCommunityCommentMessagePush(long pushSeq, CommunityCommentMessagePush commentPush, boolean hasMore) {
			printMsg("[INFO]\tonCommunityCommentMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onWebRTCIceCandidateMessagePush(long pushSeq,
				WebRTCIceCandidateMessagePush iceCandidateMessagePush,
				boolean hasMore) {
			printMsg("[INFO]\tonWebRTCIceCandidateMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onWebRTCIncomingCallMessagePush(long pushSeq,
				WebRTCIncomingCallMessagePush incomingCallMessagePush,
				boolean hasMore) {
			printMsg("[INFO]\tonWebRTCIncomingCallMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onWebRTCAnswerCallMessagePush(long pushSeq,
				WebRTCAnswerCallMessagePush answerCallMessagePush,
				boolean hasMore) {
			printMsg("[INFO]\tonWebRTCAnswerCallMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}

		@Override
		public void onWebRTCHangUpCallMessagePush(long pushSeq,
				WebRTCHangUpCallMessagePush hangUpCallMessagePush,
				boolean hasMore) {
			printMsg("[INFO]\tonWebRTCHangUpCallMessagePush\t" + pushSeq + "," + (hasMore ? "(hasMore)" : ""), null);
		}
		
		private void printMsg(String msg, Throwable th) {
			String dateStr = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
			System.out.println(dateStr + "\t" + msg);
			if (th != null) {
				th.printStackTrace(System.err);
			}
		}
		
	}
	
}
