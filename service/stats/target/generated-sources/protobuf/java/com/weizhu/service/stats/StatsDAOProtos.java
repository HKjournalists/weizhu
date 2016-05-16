// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: stats_dao.proto

package com.weizhu.service.stats;

public final class StatsDAOProtos {
  private StatsDAOProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface StatsLogOrBuilder extends
      // @@protoc_insertion_point(interface_extends:weizhu.stats.dao.StatsLog)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>required string log_name = 1;</code>
     */
    boolean hasLogName();
    /**
     * <code>required string log_name = 1;</code>
     */
    java.lang.String getLogName();
    /**
     * <code>required string log_name = 1;</code>
     */
    com.google.protobuf.ByteString
        getLogNameBytes();

    /**
     * <code>required int64 log_id = 2;</code>
     */
    boolean hasLogId();
    /**
     * <code>required int64 log_id = 2;</code>
     */
    long getLogId();

    /**
     * <code>required int64 timestamp = 3;</code>
     */
    boolean hasTimestamp();
    /**
     * <code>required int64 timestamp = 3;</code>
     */
    long getTimestamp();

    /**
     * <code>required string message = 4;</code>
     */
    boolean hasMessage();
    /**
     * <code>required string message = 4;</code>
     */
    java.lang.String getMessage();
    /**
     * <code>required string message = 4;</code>
     */
    com.google.protobuf.ByteString
        getMessageBytes();
  }
  /**
   * Protobuf type {@code weizhu.stats.dao.StatsLog}
   */
  public static final class StatsLog extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:weizhu.stats.dao.StatsLog)
      StatsLogOrBuilder {
    // Use StatsLog.newBuilder() to construct.
    private StatsLog(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private StatsLog(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final StatsLog defaultInstance;
    public static StatsLog getDefaultInstance() {
      return defaultInstance;
    }

    public StatsLog getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private StatsLog(
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
              logName_ = bs;
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              logId_ = input.readInt64();
              break;
            }
            case 24: {
              bitField0_ |= 0x00000004;
              timestamp_ = input.readInt64();
              break;
            }
            case 34: {
              com.google.protobuf.ByteString bs = input.readBytes();
              bitField0_ |= 0x00000008;
              message_ = bs;
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
      return com.weizhu.service.stats.StatsDAOProtos.internal_static_weizhu_stats_dao_StatsLog_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.weizhu.service.stats.StatsDAOProtos.internal_static_weizhu_stats_dao_StatsLog_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.weizhu.service.stats.StatsDAOProtos.StatsLog.class, com.weizhu.service.stats.StatsDAOProtos.StatsLog.Builder.class);
    }

    public static com.google.protobuf.Parser<StatsLog> PARSER =
        new com.google.protobuf.AbstractParser<StatsLog>() {
      public StatsLog parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new StatsLog(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<StatsLog> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    public static final int LOG_NAME_FIELD_NUMBER = 1;
    private java.lang.Object logName_;
    /**
     * <code>required string log_name = 1;</code>
     */
    public boolean hasLogName() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required string log_name = 1;</code>
     */
    public java.lang.String getLogName() {
      java.lang.Object ref = logName_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          logName_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string log_name = 1;</code>
     */
    public com.google.protobuf.ByteString
        getLogNameBytes() {
      java.lang.Object ref = logName_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        logName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int LOG_ID_FIELD_NUMBER = 2;
    private long logId_;
    /**
     * <code>required int64 log_id = 2;</code>
     */
    public boolean hasLogId() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required int64 log_id = 2;</code>
     */
    public long getLogId() {
      return logId_;
    }

    public static final int TIMESTAMP_FIELD_NUMBER = 3;
    private long timestamp_;
    /**
     * <code>required int64 timestamp = 3;</code>
     */
    public boolean hasTimestamp() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>required int64 timestamp = 3;</code>
     */
    public long getTimestamp() {
      return timestamp_;
    }

    public static final int MESSAGE_FIELD_NUMBER = 4;
    private java.lang.Object message_;
    /**
     * <code>required string message = 4;</code>
     */
    public boolean hasMessage() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>required string message = 4;</code>
     */
    public java.lang.String getMessage() {
      java.lang.Object ref = message_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          message_ = s;
        }
        return s;
      }
    }
    /**
     * <code>required string message = 4;</code>
     */
    public com.google.protobuf.ByteString
        getMessageBytes() {
      java.lang.Object ref = message_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        message_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private void initFields() {
      logName_ = "";
      logId_ = 0L;
      timestamp_ = 0L;
      message_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      if (!hasLogName()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasLogId()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasTimestamp()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasMessage()) {
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
        output.writeBytes(1, getLogNameBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeInt64(2, logId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeInt64(3, timestamp_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeBytes(4, getMessageBytes());
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
          .computeBytesSize(1, getLogNameBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(2, logId_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(3, timestamp_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(4, getMessageBytes());
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

    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.stats.StatsDAOProtos.StatsLog parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.weizhu.service.stats.StatsDAOProtos.StatsLog prototype) {
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
     * Protobuf type {@code weizhu.stats.dao.StatsLog}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:weizhu.stats.dao.StatsLog)
        com.weizhu.service.stats.StatsDAOProtos.StatsLogOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.weizhu.service.stats.StatsDAOProtos.internal_static_weizhu_stats_dao_StatsLog_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.weizhu.service.stats.StatsDAOProtos.internal_static_weizhu_stats_dao_StatsLog_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.weizhu.service.stats.StatsDAOProtos.StatsLog.class, com.weizhu.service.stats.StatsDAOProtos.StatsLog.Builder.class);
      }

      // Construct using com.weizhu.service.stats.StatsDAOProtos.StatsLog.newBuilder()
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
        logName_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        logId_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000002);
        timestamp_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000004);
        message_ = "";
        bitField0_ = (bitField0_ & ~0x00000008);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.weizhu.service.stats.StatsDAOProtos.internal_static_weizhu_stats_dao_StatsLog_descriptor;
      }

      public com.weizhu.service.stats.StatsDAOProtos.StatsLog getDefaultInstanceForType() {
        return com.weizhu.service.stats.StatsDAOProtos.StatsLog.getDefaultInstance();
      }

      public com.weizhu.service.stats.StatsDAOProtos.StatsLog build() {
        com.weizhu.service.stats.StatsDAOProtos.StatsLog result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.weizhu.service.stats.StatsDAOProtos.StatsLog buildPartial() {
        com.weizhu.service.stats.StatsDAOProtos.StatsLog result = new com.weizhu.service.stats.StatsDAOProtos.StatsLog(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.logName_ = logName_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.logId_ = logId_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.timestamp_ = timestamp_;
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        result.message_ = message_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.weizhu.service.stats.StatsDAOProtos.StatsLog) {
          return mergeFrom((com.weizhu.service.stats.StatsDAOProtos.StatsLog)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.weizhu.service.stats.StatsDAOProtos.StatsLog other) {
        if (other == com.weizhu.service.stats.StatsDAOProtos.StatsLog.getDefaultInstance()) return this;
        if (other.hasLogName()) {
          bitField0_ |= 0x00000001;
          logName_ = other.logName_;
          onChanged();
        }
        if (other.hasLogId()) {
          setLogId(other.getLogId());
        }
        if (other.hasTimestamp()) {
          setTimestamp(other.getTimestamp());
        }
        if (other.hasMessage()) {
          bitField0_ |= 0x00000008;
          message_ = other.message_;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasLogName()) {
          
          return false;
        }
        if (!hasLogId()) {
          
          return false;
        }
        if (!hasTimestamp()) {
          
          return false;
        }
        if (!hasMessage()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.weizhu.service.stats.StatsDAOProtos.StatsLog parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.weizhu.service.stats.StatsDAOProtos.StatsLog) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private java.lang.Object logName_ = "";
      /**
       * <code>required string log_name = 1;</code>
       */
      public boolean hasLogName() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required string log_name = 1;</code>
       */
      public java.lang.String getLogName() {
        java.lang.Object ref = logName_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            logName_ = s;
          }
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string log_name = 1;</code>
       */
      public com.google.protobuf.ByteString
          getLogNameBytes() {
        java.lang.Object ref = logName_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          logName_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string log_name = 1;</code>
       */
      public Builder setLogName(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        logName_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string log_name = 1;</code>
       */
      public Builder clearLogName() {
        bitField0_ = (bitField0_ & ~0x00000001);
        logName_ = getDefaultInstance().getLogName();
        onChanged();
        return this;
      }
      /**
       * <code>required string log_name = 1;</code>
       */
      public Builder setLogNameBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        logName_ = value;
        onChanged();
        return this;
      }

      private long logId_ ;
      /**
       * <code>required int64 log_id = 2;</code>
       */
      public boolean hasLogId() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required int64 log_id = 2;</code>
       */
      public long getLogId() {
        return logId_;
      }
      /**
       * <code>required int64 log_id = 2;</code>
       */
      public Builder setLogId(long value) {
        bitField0_ |= 0x00000002;
        logId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required int64 log_id = 2;</code>
       */
      public Builder clearLogId() {
        bitField0_ = (bitField0_ & ~0x00000002);
        logId_ = 0L;
        onChanged();
        return this;
      }

      private long timestamp_ ;
      /**
       * <code>required int64 timestamp = 3;</code>
       */
      public boolean hasTimestamp() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>required int64 timestamp = 3;</code>
       */
      public long getTimestamp() {
        return timestamp_;
      }
      /**
       * <code>required int64 timestamp = 3;</code>
       */
      public Builder setTimestamp(long value) {
        bitField0_ |= 0x00000004;
        timestamp_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required int64 timestamp = 3;</code>
       */
      public Builder clearTimestamp() {
        bitField0_ = (bitField0_ & ~0x00000004);
        timestamp_ = 0L;
        onChanged();
        return this;
      }

      private java.lang.Object message_ = "";
      /**
       * <code>required string message = 4;</code>
       */
      public boolean hasMessage() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      /**
       * <code>required string message = 4;</code>
       */
      public java.lang.String getMessage() {
        java.lang.Object ref = message_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          if (bs.isValidUtf8()) {
            message_ = s;
          }
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>required string message = 4;</code>
       */
      public com.google.protobuf.ByteString
          getMessageBytes() {
        java.lang.Object ref = message_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          message_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>required string message = 4;</code>
       */
      public Builder setMessage(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000008;
        message_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required string message = 4;</code>
       */
      public Builder clearMessage() {
        bitField0_ = (bitField0_ & ~0x00000008);
        message_ = getDefaultInstance().getMessage();
        onChanged();
        return this;
      }
      /**
       * <code>required string message = 4;</code>
       */
      public Builder setMessageBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000008;
        message_ = value;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:weizhu.stats.dao.StatsLog)
    }

    static {
      defaultInstance = new StatsLog(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:weizhu.stats.dao.StatsLog)
  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_weizhu_stats_dao_StatsLog_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_weizhu_stats_dao_StatsLog_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\017stats_dao.proto\022\020weizhu.stats.dao\032\014wei" +
      "zhu.proto\032\013stats.proto\"P\n\010StatsLog\022\020\n\010lo" +
      "g_name\030\001 \002(\t\022\016\n\006log_id\030\002 \002(\003\022\021\n\ttimestam" +
      "p\030\003 \002(\003\022\017\n\007message\030\004 \002(\tB*\n\030com.weizhu.s" +
      "ervice.statsB\016StatsDAOProtos"
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
          com.weizhu.proto.WeizhuProtos.getDescriptor(),
          com.weizhu.proto.StatsProtos.getDescriptor(),
        }, assigner);
    internal_static_weizhu_stats_dao_StatsLog_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_weizhu_stats_dao_StatsLog_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_weizhu_stats_dao_StatsLog_descriptor,
        new java.lang.String[] { "LogName", "LogId", "Timestamp", "Message", });
    com.weizhu.proto.WeizhuProtos.getDescriptor();
    com.weizhu.proto.StatsProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}