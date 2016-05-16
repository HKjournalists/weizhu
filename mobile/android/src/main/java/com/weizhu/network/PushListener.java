package com.weizhu.network;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.weizhu.proto.CommunityProtos;
import com.weizhu.proto.IMProtos;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.proto.SystemProtos;
import com.weizhu.proto.WebRTCProtos;
import com.weizhu.proto.WeizhuProtos;

public interface PushListener {
	
	void log(InetSocketAddress remoteAddress, String msg, Throwable th);
	
	void onEstablishing(InetSocketAddress remoteAddress);
	
	void onWorking(InetSocketAddress remoteAddress);
	
	void onTerminate(InetSocketAddress remoteAddress);
	
	void onVerifyFail(InetSocketAddress remoteAddress, WeizhuProtos.SocketEstablishResponse.Result failResult, String failText);
	
	void onException(InetSocketAddress remoteAddress, Throwable e);
	
	void onIOException(InetSocketAddress remoteAddress, IOException e);
	
	// 业务push
	
	void onResetPushSeq(long pushSeq);
	
	void onTestPush(long pushSeq, WeizhuProtos.TestPush pushMsg, boolean hasMore);
	
	void onIMP2PMessagePush(long pushSeq, IMProtos.IMP2PMessagePush p2pMsg, boolean hasMore);
	
	void onIMP2PStatePush(long pushSeq, IMProtos.IMP2PStatePush p2pState, boolean hasMore);
	
	void onIMGroupStatePush(long pushSeq, IMProtos.IMGroupStatePush groupState, boolean hasMore);
	
	void onSystemConfigStatePush(long pushSeq, SystemProtos.SystemConfigStatePush configState, boolean hasMore);
	
	void onSystemNewVersionStatePush(long pushSeq, SystemProtos.SystemNewVersionStatePush newVersionState, boolean hasMore);
	
	void onOfficialMessagePush(long pushSeq, OfficialProtos.OfficialMessagePush messagePush, boolean hasMore);
	
	void onCommunityPostMessagePush(long pushSeq, CommunityProtos.CommunityPostMessagePush postPush, boolean hasMore);
	
	void onCommunityCommentMessagePush(long pushSeq, CommunityProtos.CommunityCommentMessagePush commentPush, boolean hasMore);
	
	void onWebRTCIceCandidateMessagePush(long pushSeq, WebRTCProtos.WebRTCIceCandidateMessagePush iceCandidateMessagePush, boolean hasMore);
	
	void onWebRTCIncomingCallMessagePush(long pushSeq, WebRTCProtos.WebRTCIncomingCallMessagePush incomingCallMessagePush, boolean hasMore);
	
	void onWebRTCAnswerCallMessagePush(long pushSeq, WebRTCProtos.WebRTCAnswerCallMessagePush answerCallMessagePush, boolean hasMore);
	
	void onWebRTCHangUpCallMessagePush(long pushSeq, WebRTCProtos.WebRTCHangUpCallMessagePush hangUpCallMessagePush, boolean hasMore);
	
}
