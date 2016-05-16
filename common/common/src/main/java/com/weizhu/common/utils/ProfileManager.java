package com.weizhu.common.utils;

import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Converter;
import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ProfileProtos;
import com.weizhu.proto.ProfileService;
import com.weizhu.proto.ProfileProtos.GetProfileRequest;
import com.weizhu.proto.ProfileProtos.GetProfileResponse;
import com.weizhu.proto.ProfileProtos.SetProfileRequest;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;

@Singleton
public class ProfileManager {

	private static final Logger logger = LoggerFactory.getLogger(ProfileManager.class);
	
	private final ProfileService profileService;
	
	@Inject
	public ProfileManager(ProfileService profileService) {
		this.profileService = profileService;
	}
	
	private GetProfileRequest toRequest(String namePrefix, String... namePrefixs) {
		GetProfileRequest.Builder requestBuilder = GetProfileRequest.newBuilder();
		requestBuilder.addNamePrefix(namePrefix);
		for (String str : namePrefixs) {
			requestBuilder.addNamePrefix(str);
		}
		return requestBuilder.build();
	}

	public Profile getProfile(AnonymousHead head, String namePrefix, String... namePrefixs) {
		return new Profile(Futures.getUnchecked(this.profileService.getProfile(head, toRequest(namePrefix, namePrefixs))));
	}
	
	public Profile getProfile(RequestHead head, String namePrefix, String... namePrefixs) {
		return new Profile(Futures.getUnchecked(this.profileService.getProfile(head, toRequest(namePrefix, namePrefixs))));
	}
	
	public Profile getProfile(AdminHead head, String namePrefix, String... namePrefixs) {
		return new Profile(Futures.getUnchecked(this.profileService.getProfile(head, toRequest(namePrefix, namePrefixs))));
	}
	
	public Profile getProfile(BossHead head, String namePrefix, String... namePrefixs) {
		return new Profile(Futures.getUnchecked(this.profileService.getProfile(head, toRequest(namePrefix, namePrefixs))));
	}
	
	public Profile getProfile(SystemHead head, String namePrefix, String... namePrefixs) {
		return new Profile(Futures.getUnchecked(this.profileService.getProfile(head, toRequest(namePrefix, namePrefixs))));
	}
	
	public void setProfile(AdminHead head, ProfileBuilder builder) {
		if (builder.requestBuilder.getProfileCount() > 0) {
			this.profileService.setProfile(head, builder.requestBuilder.build());
		}
	}
	
	public static class ProfileKey<T> {
		private final String name;
		private final T defaultValue;
		private final Converter<String, T> converter;
		
		ProfileKey(String name, @Nullable T defaultValue, Converter<String, T> converter) {
			this.name = name;
			this.defaultValue = defaultValue;
			this.converter = converter;
		}
	}
	
	public static class Profile {
		
		private final ImmutableMap<String, String> valueMap;
		
		private Profile() {
			valueMap = ImmutableMap.of();
		}
		
		public Profile(GetProfileResponse response) {
			Map<String, String> valueMap = Maps.newTreeMap();
			for (ProfileProtos.Profile p : response.getProfileList()) {
				valueMap.put(p.getName(), p.getValue());
			}
			this.valueMap = ImmutableMap.copyOf(valueMap);
		}
		
		public <T> T get(ProfileKey<T> key) {
			String str = this.valueMap.get(key.name);
			if (str == null) {
				return key.defaultValue;
			}
			try {
				return key.converter.convert(str);
			} catch (RuntimeException e) {
				logger.error("invalid profile " + key.name + " : " + str);
				return key.defaultValue;
			}
		}
		
		public ImmutableMap<String, String> valueMap() {
			return valueMap;
		}
		
		public static final Profile EMPTY = new Profile();
		
	}
	
	public static class ProfileBuilder {
		
		private final SetProfileRequest.Builder requestBuilder = SetProfileRequest.newBuilder();
		
		public <T> ProfileBuilder set(ProfileKey<T> key, T value) {
			requestBuilder.addProfile(ProfileProtos.Profile.newBuilder().setName(key.name).setValue(key.converter.reverse().convert(value)).build());
			return this;
		}
		
	}
	
	public static ProfileKey<String> createKey(String name, @Nullable String defaultValue) {
		return new ProfileKey<String>(name, defaultValue, Converter.<String>identity());
	}
	
	private static final Converter<String, Boolean> BOOLEAN_CONVERTER = new Converter<String, Boolean>() {

		@Override
		protected Boolean doForward(String a) {
			if (!"true".equals(a) && !"false".equals(a)) {
				throw new IllegalArgumentException("invalid boolean : " + a);
			}
			return Boolean.parseBoolean(a);
		}

		@Override
		protected String doBackward(Boolean b) {
			return Boolean.toString(b);
		}
		
	};
	
	public static ProfileKey<Boolean> createKey(String name, @Nullable Boolean defaultValue) {
		return new ProfileKey<Boolean>(name, defaultValue, BOOLEAN_CONVERTER);
	}
	
	public static ProfileKey<Integer> createKey(String name, @Nullable Integer defaultValue) {
		return new ProfileKey<Integer>(name, defaultValue, Ints.stringConverter());
	}
	
	public static ProfileKey<Long> createKey(String name, @Nullable Long defaultValue) {
		return new ProfileKey<Long>(name, defaultValue, Longs.stringConverter());
	}
	
	public static ProfileKey<Float> createKey(String name, @Nullable Float defaultValue) {
		return new ProfileKey<Float>(name, defaultValue, Floats.stringConverter());
	}
	
	public static ProfileKey<Double> createKey(String name, @Nullable Double defaultValue) {
		return new ProfileKey<Double>(name, defaultValue, Doubles.stringConverter());
	}
	
	public static <T extends Enum<T>> ProfileKey<T> createKey(String name, @Nullable T defaultValue, Class<T> enumClass) {
		return new ProfileKey<T>(name, defaultValue, Enums.<T>stringConverter(enumClass));
	}
	
	private static class MessageConverter<T extends Message> extends Converter<String, T> {

		private final Message defaultInstance;
		
		public MessageConverter(T defaultInstance) {
			this.defaultInstance = defaultInstance;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T doForward(String a) {
			try {
				Message.Builder builder = this.defaultInstance.newBuilderForType();
				JsonUtil.PROTOBUF_JSON_FORMAT.merge(a, ExtensionRegistry.getEmptyRegistry(), builder);
				return (T) builder.build();
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}

		@Override
		protected String doBackward(T b) {
			return JsonUtil.PROTOBUF_JSON_FORMAT.printToString(b);
		}

	}
	
	public static <T extends Message> ProfileKey<T> createKey(String name, @Nullable T defaultValue, T defaultInstance) {
		return new ProfileKey<T>(name, defaultValue, new MessageConverter<T>(defaultInstance));
	}
	
}
