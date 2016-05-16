// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: boss_dao.proto

package com.weizhu.service.boss;

public final class BossDAOProtos {
  private BossDAOProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface SessionKeyOrBuilder extends
      // @@protoc_insertion_point(interface_extends:weizhu.boss.dao.SessionKey)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string boss_id = 1;</code>
     */
    boolean hasBossId();
    /**
     * <code>required string boss_id = 1;</code>
     */
    java.lang.String getBossId();
    /**
     * <code>required string boss_id = 1;</code>
     */
    com.google.protobuf.ByteString
        getBossIdBytes();

    /**
     * <code>required int64 session_id = 2;</code>
     */
    boolean hasSessionId();
    /**
     * <code>required int64 session_id = 2;</code>
     */
    long getSessionId();

    /**
     * <code>required int32 login_time = 3;</code>
     */
    boolean hasLoginTime();
    /**
     * <code>required int32 login_time = 3;</code>
     */
    int getLoginTime();
  }
  /**
   * Protobuf type {@code weizhu.boss.dao.SessionKey}
   */
  public static final class SessionKey extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:weizhu.boss.dao.SessionKey)
      SessionKeyOrBuilder {
    // Use SessionKey.newBuilder() to construct.
    private SessionKey(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private SessionKey(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final SessionKey defaultInstance;
    public static SessionKey getDefaultInstance() {
      return defaultInstance;
    }

    public SessionKey getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private SessionKey(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000001;
              bossId_ = bs;
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              sessionId_ = input.readInt64();
              break;
            }
            case 24: {
              bitField0_ |= 0x00000004;
              loginTime_ = input.readInt32();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.weizhu.service.boss.BossDAOProtos.internal_static_weizhu_boss_dao_SessionKey_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.weizhu.service.boss.BossDAOProtos.internal_static_weizhu_boss_dao_SessionKey_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.weizhu.service.boss.BossDAOProtos.SessionKey.class, com.weizhu.service.boss.BossDAOProtos.SessionKey.Builder.class);
    }

    public static com.google.protobuf.Parser<SessionKey> PARSER =
        new com.google.protobuf.AbstractParser<SessionKey>() {
      public SessionKey parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new SessionKey(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<SessionKey> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    public static final int BOSS_ID_FIELD_NUMBER = 1;
    private java.lang.Object bossId_;
    /**
     * <code>required string boss_id = 1;</code>
     */
    public boolean hasBossId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required string boss_id = 1;</code>
     */
    public java.lang.String getBossId() {
      java.lang.Object ref = bossId_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          bossId_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string boss_id = 1;</code>
     */
    public com.google.protobuf.ByteString
        getBossIdBytes() {
      java.lang.Object ref = bossId_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        bossId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int SESSION_ID_FIELD_NUMBER = 2;
    private long sessionId_;
    /**
     * <code>required int64 session_id = 2;</code>
     */
    public boolean hasSessionId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required int64 session_id = 2;</code>
     */
    public long getSessionId() {
      return sessionId_;
    }

    public static final int LOGIN_TIME_FIELD_NUMBER = 3;
    private int loginTime_;
    /**
     * <code>required int32 login_time = 3;</code>
     */
    public boolean hasLoginTime() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>required int32 login_time = 3;</code>
     */
    public int getLoginTime() {
      return loginTime_;
    }

    private void initFields() {
      bossId_ = "";
      sessionId_ = 0L;
      loginTime_ = 0;
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      if (!hasBossId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasSessionId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLoginTime()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getBossIdBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeInt64(2, sessionId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeInt32(3, loginTime_);
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getBossIdBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(2, sessionId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(3, loginTime_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.boss.BossDAOProtos.SessionKey parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.weizhu.service.boss.BossDAOProtos.SessionKey prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code weizhu.boss.dao.SessionKey}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:weizhu.boss.dao.SessionKey)
        com.weizhu.service.boss.BossDAOProtos.SessionKeyOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.weizhu.service.boss.BossDAOProtos.internal_static_weizhu_boss_dao_SessionKey_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.weizhu.service.boss.BossDAOProtos.internal_static_weizhu_boss_dao_SessionKey_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.weizhu.service.boss.BossDAOProtos.SessionKey.class, com.weizhu.service.boss.BossDAOProtos.SessionKey.Builder.class);
      }

      // Construct using com.weizhu.service.boss.BossDAOProtos.SessionKey.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        bossId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        sessionId_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000002);
        loginTime_ = 0;
        bitField0_ = (bitField0_ & ~0x00000004);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.weizhu.service.boss.BossDAOProtos.internal_static_weizhu_boss_dao_SessionKey_descriptor;
      }

      public com.weizhu.service.boss.BossDAOProtos.SessionKey getDefaultInstanceForType() {
        return com.weizhu.service.boss.BossDAOProtos.SessionKey.getDefaultInstance();
      }

      public com.weizhu.service.boss.BossDAOProtos.SessionKey build() {
        com.weizhu.service.boss.BossDAOProtos.SessionKey result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.weizhu.service.boss.BossDAOProtos.SessionKey buildPartial() {
        com.weizhu.service.boss.BossDAOProtos.SessionKey result = new com.weizhu.service.boss.BossDAOProtos.SessionKey(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.bossId_ = bossId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.sessionId_ = sessionId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.loginTime_ = loginTime_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.weizhu.service.boss.BossDAOProtos.SessionKey) {
          return mergeFrom((com.weizhu.service.boss.BossDAOProtos.SessionKey)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.weizhu.service.boss.BossDAOProtos.SessionKey other) {
        if (other == com.weizhu.service.boss.BossDAOProtos.SessionKey.getDefaultInstance()) return this;
        if (other.hasBossId()) {
          bitField0_ |= 0x00000001;
          bossId_ = other.bossId_;
          onChanged();
        }
        if (other.hasSessionId()) {
          setSessionId(other.getSessionId());
        }
        if (other.hasLoginTime()) {
          setLoginTime(other.getLoginTime());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasBossId()) {
          
          return false;
        }
        if (!hasSessionId()) {
          
          return false;
        }
        if (!hasLoginTime()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.weizhu.service.boss.BossDAOProtos.SessionKey parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.weizhu.service.boss.BossDAOProtos.SessionKey) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private java.lang.Object bossId_ = "";
      /**
       * <code>required string boss_id = 1;</code>
       */
      public boolean hasBossId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required string boss_id = 1;</code>
       */
      public java.lang.String getBossId() {
        java.lang.Object ref = bossId_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            bossId_ = s;
          }
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string boss_id = 1;</code>
       */
      public com.google.protobuf.ByteString
          getBossIdBytes() {
        java.lang.Object ref = bossId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          bossId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string boss_id = 1;</code>
       */
      public Builder setBossId(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        bossId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string boss_id = 1;</code>
       */
      public Builder clearBossId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        bossId_ = getDefaultInstance().getBossId();
        onChanged();
        return this;
      }
      /**
       * <code>required string boss_id = 1;</code>
       */
      public Builder setBossIdBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        bossId_ = value;
        onChanged();
        return this;
      }

      private long sessionId_ ;
      /**
       * <code>required int64 session_id = 2;</code>
       */
      public boolean hasSessionId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required int64 session_id = 2;</code>
       */
      public long getSessionId() {
        return sessionId_;
      }
      /**
       * <code>required int64 session_id = 2;</code>
       */
      public Builder setSessionId(long value) {
        bitField0_ |= 0x00000002;
        sessionId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required int64 session_id = 2;</code>
       */
      public Builder clearSessionId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        sessionId_ = 0L;
        onChanged();
        return this;
      }

      private int loginTime_ ;
      /**
       * <code>required int32 login_time = 3;</code>
       */
      public boolean hasLoginTime() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>required int32 login_time = 3;</code>
       */
      public int getLoginTime() {
        return loginTime_;
      }
      /**
       * <code>required int32 login_time = 3;</code>
       */
      public Builder setLoginTime(int value) {
        bitField0_ |= 0x00000004;
        loginTime_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required int32 login_time = 3;</code>
       */
      public Builder clearLoginTime() {
        bitField0_ = (bitField0_ & ~0x00000004);
        loginTime_ = 0;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:weizhu.boss.dao.SessionKey)
    }

    static {
      defaultInstance = new SessionKey(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:weizhu.boss.dao.SessionKey)
  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_weizhu_boss_dao_SessionKey_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_weizhu_boss_dao_SessionKey_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\016boss_dao.proto\022\017weizhu.boss.dao\"E\n\nSes" +
      "sionKey\022\017\n\007boss_id\030\001 \002(\t\022\022\n\nsession_id\030\002" +
      " \002(\003\022\022\n\nlogin_time\030\003 \002(\005B(\n\027com.weizhu.s" +
      "ervice.bossB\rBossDAOProtos"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_weizhu_boss_dao_SessionKey_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_weizhu_boss_dao_SessionKey_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_weizhu_boss_dao_SessionKey_descriptor,
        new java.lang.String[] { "BossId", "SessionId", "LoginTime", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
