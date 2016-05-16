package com.weizhu.common.jedis;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

@Singleton
public class JedisTaskLoader {

	private static final Logger logger = LoggerFactory.getLogger(JedisTaskLoader.class);
	
	private static final char TYPE_KEY_SPLIT_CHAR = '@';
	private static final Splitter TYPE_KEY_SPLITER = Splitter.on(TYPE_KEY_SPLIT_CHAR).limit(2).trimResults();
	
	private final JedisPool jedisPool;
	private final String channel;
	private final ConcurrentMap<String, FactoryHolder> factoryHolderMap = new ConcurrentHashMap<String, FactoryHolder>();
	private final ConcurrentMap<String, TaskWrapper> taskWrapperMap = new ConcurrentHashMap<String, TaskWrapper>();
	
	@Inject
	public JedisTaskLoader(JedisPool jedisPool) {
		this(jedisPool, "__default_task_loader__");
	}
	
	public JedisTaskLoader(JedisPool jedisPool, String channel) {
		this.jedisPool = jedisPool;
		this.channel = channel;
		
		ListenerThread listenerThread = new ListenerThread();
		listenerThread.setName("JedisTaskLoader.ListenerThread(channel:" + channel + ")");
		listenerThread.setDaemon(true);
		listenerThread.start();
	}
	
	public void register(String type, Executor executor, TaskFactory taskFactory) {
		if (type.indexOf(TYPE_KEY_SPLIT_CHAR) >= 0) {
			throw new IllegalArgumentException("type contains invalid charactor");
		}
		if (this.factoryHolderMap.putIfAbsent(type, new FactoryHolder(executor, taskFactory)) != null) {
			throw new IllegalStateException("this type already exist : " + type);
		}
	}
	
	public void notifyLoad(String type, String key) {
		if (type.indexOf(TYPE_KEY_SPLIT_CHAR) >= 0) {
			throw new IllegalArgumentException("type contains invalid charactor");
		}
		if (key.indexOf(TYPE_KEY_SPLIT_CHAR) >= 0) {
			throw new IllegalArgumentException("key contains invalid charactor");
		}
		Jedis jedis = this.jedisPool.getResource();
		try {
			jedis.publish(this.channel, type + TYPE_KEY_SPLIT_CHAR + key);
		} finally {
			jedis.close();
		}
	}
	
	public void notifyLoadLocal(String type, String key) {
		if (type.indexOf(TYPE_KEY_SPLIT_CHAR) >= 0) {
			throw new IllegalArgumentException("type contains invalid charactor");
		}
		if (key.indexOf(TYPE_KEY_SPLIT_CHAR) >= 0) {
			throw new IllegalArgumentException("key contains invalid charactor");
		}
		
		this.doLoadTask(type, key);
	}
	
	private void doLoadTask(String type, String key) {
		final FactoryHolder holder = this.factoryHolderMap.get(type);
		if (holder == null) {
			return;
		}
		
		final String taskWrapperKey = type + TYPE_KEY_SPLIT_CHAR + key;
		try {
			TaskWrapper newWrapper = null;
			while (true) {
				TaskWrapper wrapper = this.taskWrapperMap.get(taskWrapperKey);
				if (wrapper == null) {
					if (newWrapper == null) {
						newWrapper = new TaskWrapper(taskWrapperKey, holder.factory.createTask(key));
					}
					if (this.taskWrapperMap.putIfAbsent(taskWrapperKey, newWrapper) == null) {
						holder.executor.execute(newWrapper);
						break;
					}
				} else {
					if (wrapper.notifyLoad()) {
						break;
					} else {
						if (newWrapper == null) {
							newWrapper = new TaskWrapper(taskWrapperKey, holder.factory.createTask(key));
						}
						if (this.taskWrapperMap.replace(taskWrapperKey, wrapper, newWrapper)) {
							holder.executor.execute(newWrapper);
							break;
						}
					}
				}
			}
		} catch (RejectedExecutionException e) {
			this.taskWrapperMap.remove(taskWrapperKey);
			logger.error("load task fail : " + taskWrapperKey, e);
		} catch (Throwable th) {
			logger.error("load task fail : " + taskWrapperKey, th);
		}
	}
	
	private final class TaskWrapper implements Runnable {

		private final String taskWrapperKey;
		private final Runnable task;
		
		TaskWrapper(String taskWrapperKey, Runnable task) {
			this.taskWrapperKey = taskWrapperKey;
			this.task = task;
		}
		
		private static final int STATE_UPDATE  = 0;
		private static final int STATE_RUNNING = 1;
		private static final int STATE_FINISH  = 2;
		
		private final AtomicInteger state = new AtomicInteger(STATE_UPDATE);

		boolean notifyLoad() {
			while (true) {
				int s = this.state.get();
				if (s == STATE_UPDATE) {
					return true;
				} else if (s == STATE_FINISH) {
					return false;
				} else if (s == STATE_RUNNING) {
					if (this.state.compareAndSet(STATE_RUNNING, STATE_UPDATE)) {
						return true;
					}
				} else {
					throw new IllegalStateException("invalid state : " + s);
				}
			}
		}

		@Override
		public void run() {
			try {
				do {
					this.state.set(STATE_RUNNING);
					this.task.run();
				} while (!this.state.compareAndSet(STATE_RUNNING, STATE_FINISH));
			} catch (Throwable th) {
				logger.error("run task fail : " + this.taskWrapperKey, th);
				this.state.set(STATE_FINISH);
			} finally {
				JedisTaskLoader.this.taskWrapperMap.remove(this.taskWrapperKey, this);
			}
		}
		
	}
	
	public static interface TaskFactory {
		Runnable createTask(String key);
	}
	
	private static final class FactoryHolder {
		
		final Executor executor;
		final TaskFactory factory;
		
		FactoryHolder(Executor executor, TaskFactory factory) {
			this.executor = executor;
			this.factory = factory;
		}
	}
	
	private final class ListenerThread extends Thread {
		
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Jedis jedis = JedisTaskLoader.this.jedisPool.getResource();
					try {
						jedis.subscribe(new JedisPubSub() {

							@Override
							public void onMessage(String channel, String message) {
								if (!JedisTaskLoader.this.channel.equals(channel)) {
									return;
								}
								
								List<String> list = TYPE_KEY_SPLITER.splitToList(message);
								if (list.size() < 2) {
									return;
								}
								
								final String type = list.get(0);
								final String key = list.get(1);
								
								JedisTaskLoader.this.doLoadTask(type, key);
							}
							
						}, JedisTaskLoader.this.channel);
					} finally {
						jedis.close();
					}
				} catch (Throwable th) {
					logger.error("load listener fail", th);
					try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						break;
					}
				}
			}
		}
	}
	
}
