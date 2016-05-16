package com.weizhu.service.component.test;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.weizhu.common.module.TestModule;
import com.weizhu.common.utils.TestUtil;
import com.weizhu.proto.ComponentProtos.GetScoreByIdRequest;
import com.weizhu.proto.ComponentProtos.GetScoreByIdResponse;
import com.weizhu.proto.ComponentProtos.GetScoreUserListRequest;
import com.weizhu.proto.ComponentProtos.GetScoreUserListResponse;
import com.weizhu.proto.ComponentProtos.GetUserScoreListRequest;
import com.weizhu.proto.ComponentProtos.GetUserScoreListResponse;
import com.weizhu.proto.ComponentProtos.ScoreRequest;
import com.weizhu.proto.ComponentProtos.ScoreResponse;
import com.weizhu.proto.ComponentProtos;
import com.weizhu.proto.ComponentService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.service.component.ComponentServiceModule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ComponentServiceTest {

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
	
	private final RequestHead requestHead;
	private final ComponentService componentService;
	
	public ComponentServiceTest(){
		this.requestHead = INJECTOR.getInstance(RequestHead.class);
		this.componentService = INJECTOR.getInstance(ComponentService.class);
	}
	
	@Test
	public void testGetScoreById() throws Exception{
		GetScoreByIdResponse response = componentService.getScoreById(requestHead, GetScoreByIdRequest.newBuilder()
				.addScoreId(1)
				.addScoreId(2)
				.build()
				).get();
		assertTrue(response.getScoreCount()>0);
		assertTrue(response.getRefScoreCountCount()>0);
		assertTrue(response.getRefScoreUserCount()>=0);//userId怎么获取呢？
		
		GetScoreByIdResponse response2 = componentService.getScoreById(requestHead, GetScoreByIdRequest.newBuilder()
				.addAllScoreId(Collections.emptyList()).build()
				).get();
		assertTrue(response2.getScoreList().isEmpty());
		assertTrue(response2.getRefScoreCountList().isEmpty());
		assertTrue(response2.getRefScoreUserList().isEmpty());
	}
	
	@Test
	public void testGetScoreUserList() throws Exception{
		GetScoreUserListResponse response = componentService.getScoreUserList(requestHead, GetScoreUserListRequest.newBuilder()
				.setScoreId(1)
				.setSize(2)
				.build()
				).get();
		assertTrue(response.getScoreUserCount()>0);
		assertTrue(response.getHasMore());
		
		GetScoreUserListResponse response2 = componentService.getScoreUserList(requestHead, GetScoreUserListRequest.newBuilder()
				.setScoreId(1)
				.setSize(2)
				.setOffsetIndex(ComponentProtos.ScoreUser.newBuilder().setScoreId(1).setScoreValue(4).setScoreTime(1461552578).setUserId(Long.valueOf("10000124199")).build().toByteString())
				.build()
				).get();
		assertTrue(response2.getScoreUserCount()>0);
		assertTrue(!response2.getHasMore());
	}
	
	@Test
	public void testGetUserScoreList() throws Exception{
		GetUserScoreListResponse response = componentService.getUserScoreList(requestHead, GetUserScoreListRequest.newBuilder()
				.setSize(2)
				.setUserId(Long.parseLong("10000124197"))
				.build()
				).get();
		assertTrue(response.getScoreUserCount()>0);
		assertTrue(response.getRefScoreCount()>0);
		
		GetUserScoreListResponse response2 = componentService.getUserScoreList(requestHead, GetUserScoreListRequest.newBuilder()
				.setSize(2)
				.setUserId(Long.parseLong("10000124197"))
				.setOffsetIndex(ComponentProtos.ScoreUser.newBuilder().setScoreId(2).setScoreValue(4).setScoreTime(1461552378).setUserId(Long.valueOf("10000124197")).build().toByteString())
				.build()
				).get();
		assertTrue(response2.getScoreUserCount()>0);
		assertTrue(response2.getRefScoreCount()>0);
		assertTrue(!response2.getHasMore());
	}
	
	@Test
	public void testScore() throws Exception{
		ScoreResponse respponse = componentService.score(requestHead, ScoreRequest.newBuilder()
				.setScoreId(6)
				.setScoreValue(4)
				.build()
				).get();
		assertTrue(ScoreResponse.Result.SUCC.equals(respponse.getResult()));
		
		ScoreResponse respponse2 = componentService.score(requestHead, ScoreRequest.newBuilder()
				.setScoreId(7)
				.setScoreValue(4)
				.build()
				).get();
		assertTrue(ScoreResponse.Result.FAIL_SCORE_NOT_EXSIT.equals(respponse2.getResult()));
		
		
		
	}
}
