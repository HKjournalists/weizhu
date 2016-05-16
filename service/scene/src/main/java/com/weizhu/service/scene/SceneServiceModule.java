package com.weizhu.service.scene;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.proto.AdminSceneService;
import com.weizhu.proto.SceneService;
import com.weizhu.service.scene.tools.recommender.AdminRecommenderManager;
import com.weizhu.service.scene.tools.recommender.RecommenderManager;

public class SceneServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind(RecommenderManager.class);
		bind(AdminRecommenderManager.class);

		bind(SceneServiceImpl.class).in(Singleton.class);
		bind(AdminSceneServiceImpl.class).in(Singleton.class);

		Multibinder<String> createTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_create_table.sql"));
		createTableSQLBinder.addBinding().toInstance("com/weizhu/service/scene/db_create_table.sql");

		Multibinder<String> dropTableSQLBinder = Multibinder.newSetBinder(binder(), String.class, Names.named("db_drop_table.sql"));
		dropTableSQLBinder.addBinding().toInstance("com/weizhu/service/scene/db_drop_table.sql");
	}

	@Provides
	@Singleton
	public SceneService provideSceneService(SceneServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(SceneService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("SceneService")
	public ServiceInvoker provideSceneServiceInvoker(SceneServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(SceneService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}

	@Provides
	@Singleton
	public AdminSceneService provideAdminSceneService(AdminSceneServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor) {
		return ServiceStub.createServiceApi(AdminSceneService.class, serviceImpl, serviceExecutor);
	}

	@Provides
	@Singleton
	@Named("AdminSceneService")
	public ServiceInvoker provideAdminSceneServiceInvoker(AdminSceneServiceImpl serviceImpl, @Named("service_executor") Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return ServiceStub.createServiceInvoker(AdminSceneService.class, serviceImpl, serviceExecutor, influxDBReporter);
	}
}
