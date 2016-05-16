package com.weizhu.service.component.test;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.AdminComponentProtos.CreateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.CreateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.AdminComponentProtos.GetScoreListRequest;
import com.weizhu.proto.AdminComponentProtos.GetScoreListResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreResponse;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateRequest;
import com.weizhu.proto.AdminComponentProtos.UpdateScoreStateResponse;
import com.weizhu.proto.AdminComponentService;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.service.component.ComponentServiceModule;

public class AdminComponentServiceTest {

	static {
		System.setProperty("logback.configurationFile", "com/weizhu/service/component/test/logback.xml");
	}
	
	private static final Injector INJECTOR = Guice.createInjector(new TestModule(),
			new ComponentServiceTestModule(),
			new ComponentServiceModule());
	
	@BeforeClass
	public static void init() {
		TestUtil.clearCache(INJECTOR);
		TestUtil.loadTestDataDB(INJECTOR);
	}
	
	private final AdminHead adminHead;
	private final AdminComponentService adminComponentService;
	
	public AdminComponentServiceTest(){
		this.adminHead = INJECTOR.getInstance(AdminHead.class);
		this.adminComponentService = INJECTOR.getInstance(AdminComponentService.class);
	}
	
	@Test
	public void testGetScoreById() throws Exception {
		GetScoreByIdResponse response = adminComponentService.getScoreById(adminHead,GetScoreByIdRequest.newBuilder()
				.addScoreId(1)
				.addScoreId(2)
				.build()
				).get();
		assertTrue(response.getScoreCount()>0);
		assertTrue(response.getRefScoreCountCount()>0);
		
		GetScoreByIdResponse response2 = adminComponentService.getScoreById(adminHead,GetScoreByIdRequest.newBuilder()
				.addAllScoreId(Collections.emptyList())
				.build()
				).get();
		assertTrue(response2.getScoreList().isEmpty());
		assertTrue(response2.getRefScoreCountList().isEmpty());
	}
	
	@Test
	public void testGetScoreList() throws Exception {
		GetScoreListResponse response = adminComponentService.getScoreList(adminHead, GetScoreListRequest.newBuilder()
				.setStart(0)
				.setLength(2)
				.setState(ComponentProtos.State.NORMAL).build()
				).get();
		assertTrue(response.getScoreCount()>0);
		assertTrue(response.getRefScoreCountCount()>0);
		
		GetScoreListResponse response2 = adminComponentService.getScoreList(adminHead, GetScoreListRequest.newBuilder()
				.setStart(-1)
				.setLength(-1)
				.setState(ComponentProtos.State.NORMAL).build()
				).get();
		assertTrue(response2.getScoreList().isEmpty());
		assertTrue(response2.getRefScoreCountList().isEmpty());
	}
	
	@Test
	public void testCreateScore() throws Exception {
		CreateScoreResponse response = adminComponentService.createScore(adminHead, CreateScoreRequest.newBuilder()
				.setScoreName("打分活动7")
				.setType(ComponentProtos.Score.Type.FIVE_STAR)
				.setResultView(ComponentProtos.Score.ResultView.AFTER_SCORE)
				.build()
				).get();
		assertTrue(CreateScoreResponse.Result.SUCC.equals(response.getResult()));
		assertTrue(response.getScoreId()==7);
	}
	
	@Test
	public void testUpdateScore() throws Exception {
		UpdateScoreResponse response = adminComponentService.updateScore(adminHead, UpdateScoreRequest.newBuilder()
				.setScoreId(6)
				.setScoreName("打分活动第6个")
				.setResultView(ComponentProtos.Score.ResultView.ALWAYS_SHOW)
				.build()
				).get();
		assertTrue(UpdateScoreResponse.Result.SUCC.equals(response.getResult()));
	}
	
	@Test
	public void testUpdateScoreState() throws Exception {
		UpdateScoreStateResponse response = adminComponentService.updateScoreState(adminHead, UpdateScoreStateRequest.newBuilder()
				.addScoreId(5)
				.setState(ComponentProtos.State.DISABLE).build()
				).get();
		assertTrue(UpdateScoreStateResponse.Result.SUCC.equals(response.getResult()));
	}
	
}
