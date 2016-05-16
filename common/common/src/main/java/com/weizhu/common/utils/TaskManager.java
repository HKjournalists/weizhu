package com.weizhu.common.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.weizhu.common.CommonProtos;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Singleton
public class TaskManager {
	
	public static interface TaskPrototype<T extends Message> {
		void execute(@Nullable T data);
	}
	
	public static interface TaskHandler<T extends Message> {
		void executeAll(@Nullable T data);
		void executeLocal(@Nullable T data);
		void schedule(String key, @Nullable T data, int executeTime);
		void schedulePeriod(String key, @Nullable T data, int startTime, int periodTime);
		void cancelSchedule(String key);
	}

	private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
	
	private final ScheduledExecutorService scheduledExecutor;
	private final JedisPool jedisPool;
	private final byte[] eventKey;
	private final String lockKeyDomain;
	
	private final ConcurrentMap<String, TaskHandlerImpl<?>> taskHandlerImplRegistry = 
			new ConcurrentHashMap<String, TaskHandlerImpl<?>>();
	
	@Inject
	public TaskManager(@Named("service_scheduled_executor") ScheduledExecutorService scheduledExecutor, JedisPool jedisPool) {
		this(scheduledExecutor, jedisPool, "common:task_manager:");
	}
	
	public TaskManager(
			ScheduledExecutorService scheduledExecutor,
			JedisPool jedisPool, 
			String keyDomain
			) {
		this.scheduledExecutor = scheduledExecutor;
		this.jedisPool = jedisPool;
		if (!keyDomain.endsWith(":")) {
			keyDomain += ":";
		}
		this.eventKey = (keyDomain + "event").getBytes(Charsets.UTF_8);
		this.lockKeyDomain = keyDomain + "lock:";
		
		ListenerThread listenerThread = new ListenerThread();
		listenerThread.setName("TaskManager.ListenerThread(" + keyDomain + ")");
		listenerThread.setDaemon(true);
		listenerThread.start();
	}
	
	public <T extends Message> TaskHandler<T> register(String type, Parser<T> parser, TaskPrototype<T> prototype) {
		final TaskHandlerImpl<T> impl = new TaskHandlerImpl<T>(type, parser, prototype);
		if (this.taskHandlerImplRegistry.putIfAbsent(type, impl) != null) {
			throw new RuntimeException("task type already exists : " + type);
		}
		return impl;
	}
	
	private final class TaskHandlerImpl<T extends Message> implements TaskHandler<T> {
		
		private final String type;
		private final Parser<T> parser;
		private final TaskPrototype<T> prototype;
		
		TaskHandlerImpl(String type, Parser<T> parser, TaskPrototype<T> prototype) {
			this.type = type;
			this.parser = parser;
			this.prototype = prototype;
		}
		
		ExecuteTask<T> createExecuteTask(@Nullable ByteString data) throws InvalidProtocolBufferException {
			return new ExecuteTask<T>(this.type, this.prototype, data == null ? null : this.parser.parseFrom(data));
		}
		
		ScheduleTask<T> createScheduleTask(String key, @Nullable ByteString data, int executeTime) throws InvalidProtocolBufferException {
			return new ScheduleTask<T>(this.type, this.prototype, key, data == null ? null : this.parser.parseFrom(data), executeTime);
		}
		
		SchedulePeriodTask<T> createSchedulePeriodTask(String key, @Nullable ByteString data, int startTime, int periodTime, int executeTime) throws InvalidProtocolBufferException {
			return new SchedulePeriodTask<T>(this.type, this.prototype, key, data == null ? null : this.parser.parseFrom(data), startTime, periodTime, executeTime);
		}

		@Override
		public void executeAll(@Nullable T data) {
			CommonProtos.UtilsTaskEvent.Execute.Builder builder = CommonProtos.UtilsTaskEvent.Execute.newBuilder();
			builder.setType(this.type);
			if (data != null) {
				builder.setData(data.toByteString());
			}
			
			final byte[] message = CommonProtos.UtilsTaskEvent.newBuilder()
					.setExecute(builder.build())
					.build().toByteArray();
			Jedis jedis = TaskManager.this.jedisPool.getResource();
			try {
				jedis.publish(TaskManager.this.eventKey, message);
			} finally {
				jedis.close();
			}
		}

		@Override
		public void executeLocal(@Nullable T data) {
			TaskManager.this.scheduledExecutor.execute(new ExecuteTask<T>(this.type, this.prototype, data));
		}

		@Override
		public void schedule(String key, @Nullable T data, int executeTime) {
			Preconditions.checkNotNull(key, "key");
			Preconditions.checkArgument(executeTime >= 0, "executeTime");
			
			CommonProtos.UtilsTaskEvent.Schedule.Builder builder = CommonProtos.UtilsTaskEvent.Schedule.newBuilder();
			builder.setType(this.type);
			builder.setKey(key);
			if (data != null) {
				builder.setData(data.toByteString());
			}
			builder.setExecuteTime(executeTime);
			
			final byte[] message = CommonProtos.UtilsTaskEvent.newBuilder()
					.setSchedule(builder.build())
					.build().toByteArray();
			Jedis jedis = TaskManager.this.jedisPool.getResource();
			try {
				jedis.publish(TaskManager.this.eventKey, message);
			} finally {
				jedis.close();
			}
		}

		@Override
		public void schedulePeriod(String key, @Nullable T data, int startTime, int periodTime) {
			Preconditions.checkNotNull(key, "key");
			Preconditions.checkArgument(startTime >= 0, "startTime");
			Preconditions.checkArgument(periodTime > 0, "periodTime");
			
			CommonProtos.UtilsTaskEvent.SchedulePeriod.Builder builder = CommonProtos.UtilsTaskEvent.SchedulePeriod.newBuilder();
			builder.setType(this.type);
			builder.setKey(key);
			if (data != null) {
				builder.setData(data.toByteString());
			}
			builder.setStartTime(startTime);
			builder.setPeriodTime(periodTime);
			
			final byte[] message = CommonProtos.UtilsTaskEvent.newBuilder()
					.setSchedulePeriod(builder.build())
					.build().toByteArray();
			Jedis jedis = TaskManager.this.jedisPool.getResource();
			try {
				jedis.publish(TaskManager.this.eventKey, message);
			} finally {
				jedis.close();
			}
		}

		@Override
		public void cancelSchedule(String key) {
			Preconditions.checkNotNull(key, "key");
			
			final byte[] message = CommonProtos.UtilsTaskEvent.newBuilder()
					.setCancelSchedule(CommonProtos.UtilsTaskEvent.CancelSchedule.newBuilder()
							.setType(this.type)
							.setKey(key)
							.build())
					.build().toByteArray();
			Jedis jedis = TaskManager.this.jedisPool.getResource();
			try {
				jedis.publish(TaskManager.this.eventKey, message);
			} finally {
				jedis.close();
			}
		}
		
	}
	
	private final class ExecuteTask<T extends Message> implements Runnable {

		private final String type;
		private final TaskPrototype<T> prototype;
		private final T data;
		
		ExecuteTask(String type, TaskPrototype<T> prototype, @Nullable T data) {
			this.type = type;
			this.prototype = prototype;
			this.data = data;
		}
		
		@Override
		public void run() {
			final long begin = System.currentTimeMillis();
			Throwable throwable = null;
			try {
				this.prototype.execute(this.data);
			} catch (Throwable th) {
				throwable = th;
			} finally {
				long time = System.currentTimeMillis() - begin;
				if (throwable == null) {
					logger.info("execute task succ " + time + "(ms) : " + this.type + "/" + (this.data == null ? "null" : JsonUtil.PROTOBUF_JSON_FORMAT.printToString(this.data)));
				} else {
					logger.info("execute task fail " + time + "(ms) : " + this.type + "/" + (this.data == null ? "null" : JsonUtil.PROTOBUF_JSON_FORMAT.printToString(this.data)), throwable);
				}
			}
		}
	}
	
	private final class ScheduleTask<T extends Message> implements Runnable {

		private final String type;
		private final TaskPrototype<T> prototype;
		private final String key;
		private final T data;
		private final int executeTime;
		
		ScheduleTask(String type, TaskPrototype<T> prototype, String key, @Nullable T data, int executeTime) {
			this.type = type;
			this.prototype = prototype;
			this.key = key;
			this.data = data;
			this.executeTime = executeTime;
		}
		
		@Override
		public void run() {
			final long begin = System.currentTimeMillis();
			Throwable throwable = null;
			boolean isExecute = false;
			try {
				final Long ret;
				Jedis jedis = TaskManager.this.jedisPool.getResource();
				try {
					ret = jedis.setnx(TaskManager.this.lockKeyDomain + this.type + ":" + this.key + ":" + this.executeTime, "lock");
				} finally {
					jedis.close();
				}
				
				if (ret != null && ret > 0) {
					isExecute = true;
					this.prototype.execute(this.data);
				}
			} catch (Throwable th) {
				throwable = th;
			} finally {
				long time = System.currentTimeMillis() - begin;
				
				StringBuilder sb = new StringBuilder();
				sb.append("schedule task ").append(isExecute ? "execute" : "skip");
				sb.append(throwable == null ? " succ " : " fail ").append(time).append("(ms) : ");
				sb.append("time_diff=").append(begin - this.executeTime * 1000L).append("(ms) ");
				sb.append(this.type).append("/");
				sb.append(this.key).append("/");
				sb.append(this.data == null ? "null" : JsonUtil.PROTOBUF_JSON_FORMAT.printToString(this.data));
				if (throwable == null) {
					logger.info(sb.toString());
				} else {
					logger.error(sb.toString(), throwable);
				}
			}
		}
		
	}
	
	private final class SchedulePeriodTask<T extends Message> implements Runnable {

		private final String type;
		private final TaskPrototype<T> prototype;
		private final String key;
		private final T data;
		@SuppressWarnings("unused")
		private final int startTime;
		private final int periodTime;
		
		private int nextExecuteTime;
		
		SchedulePeriodTask(String type, TaskPrototype<T> prototype, String key, @Nullable T data, int startTime, int periodTime, int nextExecuteTime) {
			this.type = type;
			this.prototype = prototype;
			this.key = key;
			this.data = data;
			this.startTime = startTime;
			this.periodTime = periodTime;
			this.nextExecuteTime = nextExecuteTime;
		}
		
		@Override
		public void run() {
			final int executeTime;
			synchronized (this) {
				executeTime = this.nextExecuteTime;
				this.nextExecuteTime += periodTime;
			}
			
			final long begin = System.currentTimeMillis();
			Throwable throwable = null;
			boolean isExecute = false;
			try {
				final Long ret;
				Jedis jedis = TaskManager.this.jedisPool.getResource();
				try {
					ret = jedis.setnx(TaskManager.this.lockKeyDomain + this.type + ":" + this.key + ":" + executeTime, "lock");
				} finally {
					jedis.close();
				}
				
				if (ret != null && ret > 0) {
					isExecute = true;
					this.prototype.execute(this.data);
				}
			} catch (Throwable th) {
				throwable = th;
			} finally {
				long time = System.currentTimeMillis() - begin;
				
				StringBuilder sb = new StringBuilder();
				sb.append("schedule period task ").append(isExecute ? "execute" : "skip");
				sb.append(throwable == null ? " succ " : " fail ").append(time).append("(ms) : ");
				sb.append("time_diff=").append(begin - executeTime * 1000L).append("(ms) ");
				sb.append(this.type).append("/");
				sb.append(this.key).append("/");
				sb.append(this.data == null ? "null" : JsonUtil.PROTOBUF_JSON_FORMAT.printToString(this.data));
				if (throwable == null) {
					logger.info(sb.toString());
				} else {
					logger.error(sb.toString(), throwable);
				}
			}
		}
		
	}
	
	private final class ListenerThread extends Thread {
		
		@Override
		public void run() {
			final Map<String, FutureHolder> futureMap = new HashMap<String, FutureHolder>();
			while (!Thread.interrupted()) {
				try {
					Jedis jedis = TaskManager.this.jedisPool.getResource();
					try {
						jedis.subscribe(new BinaryJedisPubSub() {

							@Override
							public void onMessage(byte[] channel, byte[] message) {
								try {
									if (!Arrays.equals(TaskManager.this.eventKey, channel)) {
										return;
									}
									
									final CommonProtos.UtilsTaskEvent event = CommonProtos.UtilsTaskEvent.parseFrom(message);
									switch (event.getEventTypeCase()) {
										case EXECUTE: {
											final CommonProtos.UtilsTaskEvent.Execute execute = event.getExecute();
											final TaskHandlerImpl<?> taskHandlerImpl = TaskManager.this.taskHandlerImplRegistry.get(execute.getType());
											if (taskHandlerImpl == null) {
												logger.warn("execute task fail : cannot find type " + execute.getType());
											}
											ExecuteTask<?> task = taskHandlerImpl.createExecuteTask(execute.hasData() ? execute.getData() : null);
											TaskManager.this.scheduledExecutor.execute(task);
											break;
										}
										case SCHEDULE: {
											final CommonProtos.UtilsTaskEvent.Schedule schedule = event.getSchedule();
											final TaskHandlerImpl<?> taskHandlerImpl = TaskManager.this.taskHandlerImplRegistry.get(schedule.getType());
											if (taskHandlerImpl == null) {
												logger.warn("schedule task fail : cannot find type " + schedule.getType());
											}
											
											FutureHolder holder = futureMap.get(schedule.getType() + ":" + schedule.getKey());
											if (holder == null || !holder.event.equals(event)) {
												if (holder != null) {
													holder.future.cancel(false);
												}
												
												ScheduleTask<?> task = taskHandlerImpl.createScheduleTask(schedule.getKey(), schedule.hasData() ? schedule.getData() : null, schedule.getExecuteTime());
												long delay = schedule.getExecuteTime() * 1000L - System.currentTimeMillis();
												Future<?> future = TaskManager.this.scheduledExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
												futureMap.put(schedule.getType() + ":" + schedule.getKey(), new FutureHolder(event, future));
											}
											break;
										}
										case SCHEDULE_PERIOD: {
											final CommonProtos.UtilsTaskEvent.SchedulePeriod schedulePeriod = event.getSchedulePeriod();
											final TaskHandlerImpl<?> taskHandlerImpl = TaskManager.this.taskHandlerImplRegistry.get(schedulePeriod.getType());
											if (taskHandlerImpl == null) {
												logger.warn("schedule period task fail : cannot find type " + schedulePeriod.getType());
											}
											
											FutureHolder holder = futureMap.get(schedulePeriod.getType() + ":" + schedulePeriod.getKey());
											if (holder == null || !holder.event.equals(event)) {
												if (holder != null) {
													holder.future.cancel(false);
												}
												
												int now = (int) (System.currentTimeMillis() / 1000L);
												int div = (now - schedulePeriod.getStartTime() - 1) / schedulePeriod.getPeriodTime() + 1;
												if (div < 0) {  
													div = 0;
												}
												int nextExecuteTime = div * schedulePeriod.getPeriodTime() + schedulePeriod.getStartTime();
												
												SchedulePeriodTask<?> task = taskHandlerImpl.createSchedulePeriodTask(schedulePeriod.getKey(), schedulePeriod.hasData() ? schedulePeriod.getData() : null, schedulePeriod.getStartTime(), schedulePeriod.getPeriodTime(), nextExecuteTime);
												Future<?> future = TaskManager.this.scheduledExecutor.scheduleAtFixedRate(task, nextExecuteTime - now, schedulePeriod.getPeriodTime(), TimeUnit.SECONDS);
												futureMap.put(schedulePeriod.getType() + ":" + schedulePeriod.getKey(), new FutureHolder(event, future));
											}
											break;
										}
										case CANCEL_SCHEDULE: {
											final CommonProtos.UtilsTaskEvent.CancelSchedule cancelSchedule = event.getCancelSchedule();
											FutureHolder holder = futureMap.remove(cancelSchedule.getType() + ":" + cancelSchedule.getKey());
											if (holder != null) {
												holder.future.cancel(false);
											}
											break;
										}
										default:
											logger.warn("listener thread receive unknown : " + event.getEventTypeCase());
											break;
									}
									
									Iterator<Entry<String, FutureHolder>> it = futureMap.entrySet().iterator();
									while (it.hasNext()) {
										Entry<String, FutureHolder> entry = it.next();
										if (entry.getValue().future.isDone() || entry.getValue().future.isCancelled()) {
											it.remove();
										}
									}
								} catch (Throwable th) {
									logger.error("listener thread handle message error : " + new String(channel, Charsets.UTF_8), th);
								}
							}
							
						}, TaskManager.this.eventKey);
					} finally {
						jedis.close();
					}
				} catch (Throwable th) {
					logger.error("listener subscribe fail", th);
					try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						break;
					}
				}
			}
		}
	}
	
	private static class FutureHolder {
		
		private final CommonProtos.UtilsTaskEvent event;
		private final Future<?> future;
		
		FutureHolder(CommonProtos.UtilsTaskEvent event, Future<?> future) {
			this.event = event;
			this.future = future;
		}
	}
	
}