package com.weizhu.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.proto.IMProtos.CreateGroupChatRequest;
import com.weizhu.proto.IMProtos.CreateGroupChatResponse;
import com.weizhu.proto.IMProtos.GetGroupChatByIdRequest;
import com.weizhu.proto.IMProtos.GetGroupChatByIdResponse;
import com.weizhu.proto.IMProtos.GetGroupChatListRequest;
import com.weizhu.proto.IMProtos.GetGroupChatListResponse;
import com.weizhu.proto.IMProtos.GetGroupMessageRequest;
import com.weizhu.proto.IMProtos.GetMessageResponse;
import com.weizhu.proto.IMProtos.GetP2PChatListRequest;
import com.weizhu.proto.IMProtos.GetP2PChatListResponse;
import com.weizhu.proto.IMProtos.GetP2PMessageRequest;
import com.weizhu.proto.IMProtos.JoinGroupChatRequest;
import com.weizhu.proto.IMProtos.JoinGroupChatResponse;
import com.weizhu.proto.IMProtos.LeaveGroupChatRequest;
import com.weizhu.proto.IMProtos.LeaveGroupChatResponse;
import com.weizhu.proto.IMProtos.SendGroupMessageRequest;
import com.weizhu.proto.IMProtos.SendGroupMessageResponse;
import com.weizhu.proto.IMProtos.SendP2PMessageRequest;
import com.weizhu.proto.IMProtos.SendP2PMessageResponse;
import com.weizhu.proto.IMProtos.SetGroupNameRequest;
import com.weizhu.proto.IMProtos.SetGroupNameResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;

public interface IMService {

	/* p2p chat */
	
	@ResponseType(GetMessageResponse.class)
	ListenableFuture<GetMessageResponse> getP2PMessage(RequestHead head, GetP2PMessageRequest request);
	
	@WriteMethod
	@ResponseType(SendP2PMessageResponse.class)
	ListenableFuture<SendP2PMessageResponse> sendP2PMessage(RequestHead head, SendP2PMessageRequest request);
	
	@ResponseType(GetP2PChatListResponse.class)
	ListenableFuture<GetP2PChatListResponse> getP2PChatList(RequestHead head, GetP2PChatListRequest request);
	
	/* group chat */
	
	@ResponseType(GetGroupChatByIdResponse.class)
	ListenableFuture<GetGroupChatByIdResponse> getGroupChatById(RequestHead head, GetGroupChatByIdRequest request);
	
	@ResponseType(GetMessageResponse.class)
	ListenableFuture<GetMessageResponse> getGroupMessage(RequestHead head, GetGroupMessageRequest request);
	
	@WriteMethod
	@ResponseType(CreateGroupChatResponse.class)
	ListenableFuture<CreateGroupChatResponse> createGroupChat(RequestHead head, CreateGroupChatRequest request);
	
	@WriteMethod
	@ResponseType(SetGroupNameResponse.class)
	ListenableFuture<SetGroupNameResponse> setGroupName(RequestHead head, SetGroupNameRequest request);
	
	@WriteMethod
	@ResponseType(JoinGroupChatResponse.class)
	ListenableFuture<JoinGroupChatResponse> joinGroupChat(RequestHead head, JoinGroupChatRequest request);
	
	@WriteMethod
	@ResponseType(LeaveGroupChatResponse.class)
	ListenableFuture<LeaveGroupChatResponse> leaveGroupChat(RequestHead head, LeaveGroupChatRequest request);
	
	@WriteMethod
	@ResponseType(SendGroupMessageResponse.class)
	ListenableFuture<SendGroupMessageResponse> sendGroupMessage(RequestHead head, SendGroupMessageRequest request);
	
	@ResponseType(GetGroupChatListResponse.class)
	ListenableFuture<GetGroupChatListResponse> getGroupChatList(RequestHead head, GetGroupChatListRequest request);
	
}
