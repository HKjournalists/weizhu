package com.weizhu.service.allow.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.TextFormat;
import com.weizhu.common.module.FakeExternalServiceModule;
import com.weizhu.common.module.FakeProfileServiceModule;
import com.weizhu.common.module.FakePushServiceModule;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowProtos.CreateModelRequest;
import com.weizhu.proto.AllowProtos.CreateModelResponse;
import com.weizhu.proto.AllowProtos.DeleteModelRequest;
import com.weizhu.proto.AllowProtos.DeleteModelResponse;
import com.weizhu.proto.AllowProtos.DeleteRuleRequest;
import com.weizhu.proto.AllowProtos.DeleteRuleResponse;
import com.weizhu.proto.AllowProtos.GetModelByIdRequest;
import com.weizhu.proto.AllowProtos.GetModelByIdResponse;
import com.weizhu.proto.AllowProtos.GetModelListRequest;
import com.weizhu.proto.AllowProtos.GetModelListResponse;
import com.weizhu.proto.AllowProtos.GetModelRuleListRequest;
import com.weizhu.proto.AllowProtos.GetModelRuleListResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRequest;
import com.weizhu.proto.AllowProtos.UpdateModelResponse;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderRequest;
import com.weizhu.proto.AllowProtos.UpdateModelRuleOrderResponse;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateTeamRuleResponse;
import com.weizhu.proto.AllowProtos.UpdateUserRuleRequest;
import com.weizhu.proto.AllowProtos.UpdateUserRuleResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.service.allow.AllowServiceModule;
import com.weizhu.service.user.UserServiceModule;
import com.weizhu.service.user.test.UserServiceTestModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AllowServiceTest {
	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/allow/test/logback.xml");
	}

	private final AdminHead adminHead;
	private final AllowService allowService;
	
	@Inject
	public AllowServiceTest() {
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.allowService = INJECTOR.getInstance(AllowService.class);
	}
	
	private static final Injector INJECTOR = Guice.createInjector(
			new TestModule(), 
			new AllowServiceModule(), new AllowServiceTestModule(),
			new UserServiceTestModule(), new UserServiceModule(),
			new FakePushServiceModule(), new FakeProfileServiceModule(), new FakeExternalServiceModule());
	
	@BeforeClass
	public static void init() throws Exception {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	@Test
	public void bCheckAllowTest() {
		CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
				.addAllModelId(Collections.singleton(2))
				.addAllUserId(Arrays.asList(10000124188L,10000124189L,10000124194L,10000124203L,110L,001L,10000124212L, 10000124196L))
				.build();
		CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(adminHead, checkAllowRequest));
		
		List<AllowProtos.CheckAllowResponse.CheckResult> checkResultList = checkAllowResponse.getCheckResultList();
		System.out.println(TextFormat.printToUnicodeString(checkResultList.get(0)));
		assertTrue(!Collections.disjoint(checkResultList.get(0).getAllowUserIdList(), Arrays.asList(10000124188L, 10000124189L, 10000124194L, 10000124203L, 10000124196L)));
	}

	@Test
	public void cGetModelListTest() {
		GetModelListRequest getModelListRequest = GetModelListRequest.newBuilder()
				.setKeyword("测试")
				.setStart(0)
				.setLength(10)
				.build();
		GetModelListResponse getModelListResponse = Futures.getUnchecked(allowService.getModelList(adminHead, getModelListRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelListResponse));
		assertEquals(getModelListResponse.getModelList().get(0).getModelName(), "测试模型1");
		
		GetModelListRequest getModelListRequest1 = GetModelListRequest.newBuilder()
				.setStart(0)
				.setLength(10)
				.build();
		GetModelListResponse getModelListResponse1 = Futures.getUnchecked(allowService.getModelList(adminHead, getModelListRequest1));
		// System.out.println(TextFormat.printToUnicodeString(getModelListResponse1));
		assertTrue(getModelListResponse1.getModelCount() > 1);
	}

	@Test
	public void dGetModelByIdTest() {
		GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
				.addAllModelId(Arrays.asList(1))
				.build();
		GetModelByIdResponse getModeByIdResponse = Futures.getUnchecked(allowService.getModelById(adminHead, getModelByIdRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModeByIdResponse));
		assertEquals(getModeByIdResponse.getModelList().get(0).getModelName(), "权限");
	}
	
	@Test
	public void dGetModelRuleListTest() {
		GetModelRuleListRequest getModelRuleListRequest = GetModelRuleListRequest.newBuilder()
				.setModelId(2)
				.build();
		GetModelRuleListResponse getModeRuleListResponse = Futures.getUnchecked(allowService.getModelRuleList(adminHead, getModelRuleListRequest));
		// System.out.println("GetModelRuleListResponse info :" + TextFormat.printToUnicodeString(getModeRuleListResponse));
		assertTrue(getModeRuleListResponse.getRuleList().size() > 0);
	}

	@Test
	public void aCreateModelTest() {
		/**
		 * 此模型允许通过的人员是：10000124188L,10000124189L,10000124194L,10000124203L,10000124196L
		 */
		AllowProtos.UserRule userRule = AllowProtos.UserRule.newBuilder()
				.addAllUserId(Arrays.asList(10000124188L, 10000124189L))
				.build();
		AllowProtos.Rule rule1 = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setRuleName("测试规则1")
				.setAction(AllowProtos.Action.ALLOW)
				.setUserRule(userRule)
				.build();
		
		AllowProtos.TeamRule teamRule = AllowProtos.TeamRule.newBuilder()
				.addAllTeamId(Collections.singleton(3))
				.build();
		AllowProtos.Rule rule2 = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setRuleName("测试规则2")
				.setAction(AllowProtos.Action.ALLOW)
				.setTeamRule(teamRule)
				.build();
		
		AllowProtos.PositionRule positionRule = AllowProtos.PositionRule.newBuilder()
				.addAllPositionId(Collections.singleton(8))
				.build();
		AllowProtos.Rule rule3 = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setRuleName("测试规则3")
				.setAction(AllowProtos.Action.ALLOW)
				.setPositionRule(positionRule)
				.build();
		
		AllowProtos.LevelRule levelRule = AllowProtos.LevelRule.newBuilder()
				.addAllLevelId(Collections.singleton(2))
				.build();
		AllowProtos.Rule rule4 = AllowProtos.Rule.newBuilder()
				.setRuleId(0)
				.setRuleName("测试规则4")
				.setAction(AllowProtos.Action.ALLOW)
				.setLevelRule(levelRule)
				.build();
		
		List<AllowProtos.Rule> ruleList = Arrays.asList(rule1, rule2, rule3, rule4);
		
		// 表中有byte，程序插入
		CreateModelRequest createModelRequest = CreateModelRequest.newBuilder()
				.setDefaultAction(AllowProtos.Action.DENY)
				.setModelName("测试模型1")
				.addAllRule(ruleList)
				.build();
		CreateModelResponse createModelResponse = Futures.getUnchecked(allowService.createModel(adminHead, createModelRequest));
		assertEquals(createModelResponse.getResult(), CreateModelResponse.Result.SUCC);
	}

	@Test
	public void eDeleteModelTest() {
		DeleteModelRequest deleteModelRequest = DeleteModelRequest.newBuilder()
				.addAllModelId(Collections.singleton(1))
				.build();
		@SuppressWarnings("unused")
		DeleteModelResponse deleteModelResponse = Futures.getUnchecked(allowService.deleteModel(adminHead, deleteModelRequest));
		// System.out.println(TextFormat.printToUnicodeString(deleteModelResponse));
		
		GetModelListRequest getModelListRequest = GetModelListRequest.newBuilder()
				.setStart(0)
				.setLength(10)
				.build();
		GetModelListResponse getModelListResponse = Futures.getUnchecked(allowService.getModelList(adminHead, getModelListRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelListResponse));
		assertTrue(getModelListResponse.getModelCount() == 1);
	}

	@Test
	public void fDeleteRuleTest() {
		DeleteRuleRequest deleteRuleRequest = DeleteRuleRequest.newBuilder()
				.setModelId(2)
				.addAllRuleId(Collections.singleton(1))
				.build();
		@SuppressWarnings("unused")
		DeleteRuleResponse deleteRuleResponse = Futures.getUnchecked(allowService.deleteRule(adminHead, deleteRuleRequest));
//		System.out.println("规则删除接口：" + TextFormat.printToUnicodeString(deleteRuleResponse));
		
		GetModelRuleListRequest getModelRuleListRequest = GetModelRuleListRequest.newBuilder()
				.setModelId(2).build();
		
		GetModelRuleListResponse getModelRuleListResponse = Futures.getUnchecked(allowService.getModelRuleList(adminHead, getModelRuleListRequest));
//		System.out.println(TextFormat.printToUnicodeString(getModelRuleListResponse) + getModelRuleListResponse.getRuleCount());
		assertTrue(getModelRuleListResponse.getRuleCount() == 3);
	}

	@Test
	public void gUpdateModelTest() {
		UpdateModelRequest updateModelRequest = UpdateModelRequest.newBuilder()
				.setModelId(2)
				.setModelName("权限模型改名")
				.setDefaultAction(AllowProtos.Action.DENY)
				.build();
		
		@SuppressWarnings("unused")
		UpdateModelResponse updateModelResponse = Futures.getUnchecked(allowService.updateModel(adminHead, updateModelRequest));
		// System.out.println("更新权限模型" + TextFormat.printToUnicodeString(updateModelResponse));
		
		GetModelByIdRequest getModelByIdRequest = GetModelByIdRequest.newBuilder()
				.addAllModelId(Collections.singleton(2))
				.build();
		GetModelByIdResponse getModelByIdResponse = Futures.getUnchecked(allowService.getModelById(adminHead, getModelByIdRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelByIdResponse));
		assertEquals(getModelByIdResponse.getModelList().get(0).getModelName(), "权限模型改名");
	}

	@Test
	public void hUpdateModelRuleOrderTest() {
		UpdateModelRuleOrderRequest updateModelRuleOrderRequest = UpdateModelRuleOrderRequest.newBuilder()
				.setModelId(2)
				.addAllRuleId(Arrays.asList(3,2))
				.build();
		
		@SuppressWarnings("unused")
		UpdateModelRuleOrderResponse updateModelRuleOrderResponse = Futures.getUnchecked(allowService.updateModelRuleOrder(adminHead, updateModelRuleOrderRequest));
		// System.out.println(TextFormat.printToUnicodeString(updateModelRuleOrderResponse));
		
		GetModelRuleListRequest getModelRuleListRequest = GetModelRuleListRequest.newBuilder()
				.setModelId(2).build();
		
		GetModelRuleListResponse getModelRuleListResponse = Futures.getUnchecked(allowService.getModelRuleList(adminHead, getModelRuleListRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelRuleListResponse));
		assertTrue(getModelRuleListResponse.getRuleList().get(0).getRuleId() == 3);
	}

	@Test
	public void jUpdateUserRuleTest() {
		UpdateUserRuleRequest updateUserRuleRequest = UpdateUserRuleRequest.newBuilder()
				.setModelId(2)
				.setRuleId(1)
				.setRuleName("用户组测试")
				.setRuleAction(AllowProtos.Action.DENY)
				.addAllUserId(Collections.singleton(1L))
				.build();
		
		UpdateUserRuleResponse updateUserRuleResponse = Futures.getUnchecked(allowService.updateUserRule(adminHead, updateUserRuleRequest));
		// System.out.println(TextFormat.printToUnicodeString(updateUserRuleResponse));
		assertEquals(updateUserRuleResponse.getResult(), UpdateUserRuleResponse.Result.FAIL_RULE_NOT_EXIST);
		GetModelRuleListRequest getModelRuleListRequest = GetModelRuleListRequest.newBuilder()
				.setModelId(2).build();
		
		@SuppressWarnings("unused")
		GetModelRuleListResponse getModelRuleListResponse = Futures.getUnchecked(allowService.getModelRuleList(adminHead, getModelRuleListRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelRuleListResponse));
	}

	@Test
	public void iUpdateTeamRuleTest() {
		UpdateTeamRuleRequest updateTeamRuleRequest = UpdateTeamRuleRequest.newBuilder()
				.setModelId(2)
				.setRuleId(2)
				.setRuleName("组测试")
				.setRuleAction(AllowProtos.Action.DENY)
				.addAllTeamId(Collections.singleton(4))
				.build();
		
		@SuppressWarnings("unused")
		UpdateTeamRuleResponse updateTeamRuleResponse = Futures.getUnchecked(allowService.updateTeamRule(adminHead, updateTeamRuleRequest));
		// System.out.println(TextFormat.printToUnicodeString(updateTeamRuleResponse));
		
		GetModelRuleListRequest getModelRuleListRequest = GetModelRuleListRequest.newBuilder()
				.setModelId(2).build();
		
		GetModelRuleListResponse getModelRuleListResponse = Futures.getUnchecked(allowService.getModelRuleList(adminHead, getModelRuleListRequest));
		// System.out.println(TextFormat.printToUnicodeString(getModelRuleListResponse));
		assertEquals(getModelRuleListResponse.getRuleList().get(1).getRuleName(), "组测试");
	}

	@Test
	public void updatePositionRuleTest() {
		
		return ;
	}
}
