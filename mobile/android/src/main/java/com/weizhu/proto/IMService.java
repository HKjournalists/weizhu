package com.weizhu.proto;

import com.weizhu.network.Future;
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


public interface IMService {

	/* p2p chat */
	
	@ResponseType(GetMessageResponse.class)
	Future<GetMessageResponse> getP2PMessage(GetP2PMessageRequest request, int priorityNum);
	
	@ResponseType(SendP2PMessageResponse.class)
	Future<SendP2PMessageResponse> sendP2PMessage(SendP2PMessageRequest request, int priorityNum);
	
	@ResponseType(GetP2PChatListResponse.class)
	Future<GetP2PChatListResponse> getP2PChatList(GetP2PChatListRequest request, int priorityNum);
	
	/* group chat */
	
	@ResponseType(GetGroupChatByIdResponse.class)
	Future<GetGroupChatByIdResponse> getGroupChatById(GetGroupChatByIdRequest request, int priorityNum);
	
	@ResponseType(GetMessageResponse.class)
	Future<GetMessageResponse> getGroupMessage(GetGroupMessageRequest request, int priorityNum);
	
	@ResponseType(CreateGroupChatResponse.class)
	Future<CreateGroupChatResponse> createGroupChat(CreateGroupChatRequest request, int priorityNum);
	
	@ResponseType(SetGroupNameResponse.class)
	Future<SetGroupNameResponse> setGroupName(SetGroupNameRequest request, int priorityNum);
	
	@ResponseType(JoinGroupChatResponse.class)
	Future<JoinGroupChatResponse> joinGroupChat(JoinGroupChatRequest request, int priorityNum);
	
	@ResponseType(LeaveGroupChatResponse.class)
	Future<LeaveGroupChatResponse> leaveGroupChat(LeaveGroupChatRequest request, int priorityNum);
	
	@ResponseType(SendGroupMessageResponse.class)
	Future<SendGroupMessageResponse> sendGroupMessage(SendGroupMessageRequest request, int priorityNum);
	
	@ResponseType(GetGroupChatListResponse.class)
	Future<GetGroupChatListResponse> getGroupChatList(GetGroupChatListRequest request, int priorityNum);
	
}
