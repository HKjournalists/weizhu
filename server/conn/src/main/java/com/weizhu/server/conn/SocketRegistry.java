package com.weizhu.server.conn;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.weizhu.proto.WeizhuProtos;

@Singleton
public final class SocketRegistry {

	private final ConcurrentMap<ChannelKey, ImmutableList<ChannelHolder>> registerMap = new ConcurrentHashMap<ChannelKey, ImmutableList<ChannelHolder>>();

	@Inject
	public SocketRegistry() {
	}
	
	public void register(WeizhuProtos.Session session, ImmutableSet<String> pushNameSet, Channel channel) {
		final ChannelKey key = new ChannelKey(session.getCompanyId(), session.getUserId());
		final ChannelHolder holder = new ChannelHolder(session, pushNameSet, channel);

		ImmutableList<ChannelHolder> oldList = null;
		ImmutableList<ChannelHolder> newList = null;
		do {
			oldList = registerMap.get(key);
			if (oldList == null) {
				oldList = registerMap.putIfAbsent(key, ImmutableList.of(holder));
				if (oldList == null) {
					break;
				}
			}
			newList = ImmutableList.<ChannelHolder>builder().add(holder).addAll(oldList).build();
		} while (!registerMap.replace(key, oldList, newList));
	}

	public void unregister(WeizhuProtos.Session session, Channel channel) {
		final ChannelKey key = new ChannelKey(session.getCompanyId(), session.getUserId());
		
		ImmutableList<ChannelHolder> oldList = null;
		ImmutableList<ChannelHolder> newList = null;
		do {
			oldList = registerMap.get(key);
			if (oldList == null || oldList.isEmpty()) {
				break;
			}
			ImmutableList.Builder<ChannelHolder> newListBuilder = ImmutableList
					.builder();

			boolean isFind = false;
			for (int i = 0; i < oldList.size(); ++i) {
				if (oldList.get(i).channel() == channel) {
					isFind = true;
				} else {
					newListBuilder.add(oldList.get(i));
				}
			}
			if (!isFind) {
				break;
			}

			newList = newListBuilder.build();
		} while (!registerMap.replace(key, oldList, newList));
	}

	public ImmutableList<ChannelHolder> get(long companyId, long userId) {
		final ChannelKey key = new ChannelKey(companyId, userId);
		
		ImmutableList<ChannelHolder> list = registerMap.get(key);
		if (list == null) {
			return ImmutableList.of();
		} else {
			return list;
		}
	}
	
	private static class ChannelKey {
		private final long companyId;
		private final long userId;
		
		public ChannelKey(long companyId, long userId) {
			this.companyId = companyId;
			this.userId = userId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (companyId ^ (companyId >>> 32));
			result = prime * result + (int) (userId ^ (userId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ChannelKey other = (ChannelKey) obj;
			if (companyId != other.companyId)
				return false;
			if (userId != other.userId)
				return false;
			return true;
		}
	}

	public static class ChannelHolder {
		private final WeizhuProtos.Session session;
		private final ImmutableSet<String> pushNameSet;
		private final Channel channel;

		public ChannelHolder(WeizhuProtos.Session session, ImmutableSet<String> pushNameSet, Channel channel) {
			this.session = session;
			this.pushNameSet = pushNameSet;
			this.channel = channel;
		}

		public WeizhuProtos.Session session() {
			return session;
		}
		
		public ImmutableSet<String> pushNameSet() {
			return pushNameSet;
		}

		public Channel channel() {
			return channel;
		}
	}
}
