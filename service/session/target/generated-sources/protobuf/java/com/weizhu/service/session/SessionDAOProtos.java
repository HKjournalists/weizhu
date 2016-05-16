// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: session_dao.proto

package com.weizhu.service.session;

public final class SessionDAOProtos {
  private SessionDAOProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface SessionDataListOrBuilder extends
      // @@protoc_insertion_point(interface_extends:weizhu.session.dao.SessionDataList)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    java.util.List<com.weizhu.proto.SessionProtos.SessionData> 
        getSessionDataList();
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    com.weizhu.proto.SessionProtos.SessionData getSessionData(int index);
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    int getSessionDataCount();
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    java.util.List<? extends com.weizhu.proto.SessionProtos.SessionDataOrBuilder> 
        getSessionDataOrBuilderList();
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    com.weizhu.proto.SessionProtos.SessionDataOrBuilder getSessionDataOrBuilder(
        int index);
  }
  /**
   * Protobuf type {@code weizhu.session.dao.SessionDataList}
   */
  public static final class SessionDataList extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:weizhu.session.dao.SessionDataList)
      SessionDataListOrBuilder {
    // Use SessionDataList.newBuilder() to construct.
    private SessionDataList(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private SessionDataList(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final SessionDataList defaultInstance;
    public static SessionDataList getDefaultInstance() {
      return defaultInstance;
    }

    public SessionDataList getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private SessionDataList(
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
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                sessionData_ = new java.util.ArrayList<com.weizhu.proto.SessionProtos.SessionData>();
                mutable_bitField0_ |= 0x00000001;
              }
              sessionData_.add(input.readMessage(com.weizhu.proto.SessionProtos.SessionData.PARSER, extensionRegistry));
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
        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
          sessionData_ = java.util.Collections.unmodifiableList(sessionData_);
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.weizhu.service.session.SessionDAOProtos.internal_static_weizhu_session_dao_SessionDataList_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.weizhu.service.session.SessionDAOProtos.internal_static_weizhu_session_dao_SessionDataList_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.weizhu.service.session.SessionDAOProtos.SessionDataList.class, com.weizhu.service.session.SessionDAOProtos.SessionDataList.Builder.class);
    }

    public static com.google.protobuf.Parser<SessionDataList> PARSER =
        new com.google.protobuf.AbstractParser<SessionDataList>() {
      public SessionDataList parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new SessionDataList(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<SessionDataList> getParserForType() {
      return PARSER;
    }

    public static final int SESSION_DATA_FIELD_NUMBER = 1;
    private java.util.List<com.weizhu.proto.SessionProtos.SessionData> sessionData_;
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    public java.util.List<com.weizhu.proto.SessionProtos.SessionData> getSessionDataList() {
      return sessionData_;
    }
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    public java.util.List<? extends com.weizhu.proto.SessionProtos.SessionDataOrBuilder> 
        getSessionDataOrBuilderList() {
      return sessionData_;
    }
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    public int getSessionDataCount() {
      return sessionData_.size();
    }
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    public com.weizhu.proto.SessionProtos.SessionData getSessionData(int index) {
      return sessionData_.get(index);
    }
    /**
     * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
     */
    public com.weizhu.proto.SessionProtos.SessionDataOrBuilder getSessionDataOrBuilder(
        int index) {
      return sessionData_.get(index);
    }

    private void initFields() {
      sessionData_ = java.util.Collections.emptyList();
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      for (int i = 0; i < getSessionDataCount(); i++) {
        if (!getSessionData(i).isInitialized()) {
          memoizedIsInitialized = 0;
          return false;
        }
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      for (int i = 0; i < sessionData_.size(); i++) {
        output.writeMessage(1, sessionData_.get(i));
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      for (int i = 0; i < sessionData_.size(); i++) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(1, sessionData_.get(i));
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

    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.session.SessionDAOProtos.SessionDataList parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.weizhu.service.session.SessionDAOProtos.SessionDataList prototype) {
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
     * Protobuf type {@code weizhu.session.dao.SessionDataList}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:weizhu.session.dao.SessionDataList)
        com.weizhu.service.session.SessionDAOProtos.SessionDataListOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.weizhu.service.session.SessionDAOProtos.internal_static_weizhu_session_dao_SessionDataList_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.weizhu.service.session.SessionDAOProtos.internal_static_weizhu_session_dao_SessionDataList_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.weizhu.service.session.SessionDAOProtos.SessionDataList.class, com.weizhu.service.session.SessionDAOProtos.SessionDataList.Builder.class);
      }

      // Construct using com.weizhu.service.session.SessionDAOProtos.SessionDataList.newBuilder()
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
          getSessionDataFieldBuilder();
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        if (sessionDataBuilder_ == null) {
          sessionData_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          sessionDataBuilder_.clear();
        }
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.weizhu.service.session.SessionDAOProtos.internal_static_weizhu_session_dao_SessionDataList_descriptor;
      }

      public com.weizhu.service.session.SessionDAOProtos.SessionDataList getDefaultInstanceForType() {
        return com.weizhu.service.session.SessionDAOProtos.SessionDataList.getDefaultInstance();
      }

      public com.weizhu.service.session.SessionDAOProtos.SessionDataList build() {
        com.weizhu.service.session.SessionDAOProtos.SessionDataList result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.weizhu.service.session.SessionDAOProtos.SessionDataList buildPartial() {
        com.weizhu.service.session.SessionDAOProtos.SessionDataList result = new com.weizhu.service.session.SessionDAOProtos.SessionDataList(this);
        int from_bitField0_ = bitField0_;
        if (sessionDataBuilder_ == null) {
          if (((bitField0_ & 0x00000001) == 0x00000001)) {
            sessionData_ = java.util.Collections.unmodifiableList(sessionData_);
            bitField0_ = (bitField0_ & ~0x00000001);
          }
          result.sessionData_ = sessionData_;
        } else {
          result.sessionData_ = sessionDataBuilder_.build();
        }
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.weizhu.service.session.SessionDAOProtos.SessionDataList) {
          return mergeFrom((com.weizhu.service.session.SessionDAOProtos.SessionDataList)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.weizhu.service.session.SessionDAOProtos.SessionDataList other) {
        if (other == com.weizhu.service.session.SessionDAOProtos.SessionDataList.getDefaultInstance()) return this;
        if (sessionDataBuilder_ == null) {
          if (!other.sessionData_.isEmpty()) {
            if (sessionData_.isEmpty()) {
              sessionData_ = other.sessionData_;
              bitField0_ = (bitField0_ & ~0x00000001);
            } else {
              ensureSessionDataIsMutable();
              sessionData_.addAll(other.sessionData_);
            }
            onChanged();
          }
        } else {
          if (!other.sessionData_.isEmpty()) {
            if (sessionDataBuilder_.isEmpty()) {
              sessionDataBuilder_.dispose();
              sessionDataBuilder_ = null;
              sessionData_ = other.sessionData_;
              bitField0_ = (bitField0_ & ~0x00000001);
              sessionDataBuilder_ = 
                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                   getSessionDataFieldBuilder() : null;
            } else {
              sessionDataBuilder_.addAllMessages(other.sessionData_);
            }
          }
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        for (int i = 0; i < getSessionDataCount(); i++) {
          if (!getSessionData(i).isInitialized()) {
            
            return false;
          }
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.weizhu.service.session.SessionDAOProtos.SessionDataList parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.weizhu.service.session.SessionDAOProtos.SessionDataList) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private java.util.List<com.weizhu.proto.SessionProtos.SessionData> sessionData_ =
        java.util.Collections.emptyList();
      private void ensureSessionDataIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          sessionData_ = new java.util.ArrayList<com.weizhu.proto.SessionProtos.SessionData>(sessionData_);
          bitField0_ |= 0x00000001;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilder<
          com.weizhu.proto.SessionProtos.SessionData, com.weizhu.proto.SessionProtos.SessionData.Builder, com.weizhu.proto.SessionProtos.SessionDataOrBuilder> sessionDataBuilder_;

      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public java.util.List<com.weizhu.proto.SessionProtos.SessionData> getSessionDataList() {
        if (sessionDataBuilder_ == null) {
          return java.util.Collections.unmodifiableList(sessionData_);
        } else {
          return sessionDataBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public int getSessionDataCount() {
        if (sessionDataBuilder_ == null) {
          return sessionData_.size();
        } else {
          return sessionDataBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public com.weizhu.proto.SessionProtos.SessionData getSessionData(int index) {
        if (sessionDataBuilder_ == null) {
          return sessionData_.get(index);
        } else {
          return sessionDataBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder setSessionData(
          int index, com.weizhu.proto.SessionProtos.SessionData value) {
        if (sessionDataBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSessionDataIsMutable();
          sessionData_.set(index, value);
          onChanged();
        } else {
          sessionDataBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder setSessionData(
          int index, com.weizhu.proto.SessionProtos.SessionData.Builder builderForValue) {
        if (sessionDataBuilder_ == null) {
          ensureSessionDataIsMutable();
          sessionData_.set(index, builderForValue.build());
          onChanged();
        } else {
          sessionDataBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder addSessionData(com.weizhu.proto.SessionProtos.SessionData value) {
        if (sessionDataBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSessionDataIsMutable();
          sessionData_.add(value);
          onChanged();
        } else {
          sessionDataBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder addSessionData(
          int index, com.weizhu.proto.SessionProtos.SessionData value) {
        if (sessionDataBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSessionDataIsMutable();
          sessionData_.add(index, value);
          onChanged();
        } else {
          sessionDataBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder addSessionData(
          com.weizhu.proto.SessionProtos.SessionData.Builder builderForValue) {
        if (sessionDataBuilder_ == null) {
          ensureSessionDataIsMutable();
          sessionData_.add(builderForValue.build());
          onChanged();
        } else {
          sessionDataBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder addSessionData(
          int index, com.weizhu.proto.SessionProtos.SessionData.Builder builderForValue) {
        if (sessionDataBuilder_ == null) {
          ensureSessionDataIsMutable();
          sessionData_.add(index, builderForValue.build());
          onChanged();
        } else {
          sessionDataBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder addAllSessionData(
          java.lang.Iterable<? extends com.weizhu.proto.SessionProtos.SessionData> values) {
        if (sessionDataBuilder_ == null) {
          ensureSessionDataIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, sessionData_);
          onChanged();
        } else {
          sessionDataBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder clearSessionData() {
        if (sessionDataBuilder_ == null) {
          sessionData_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
          onChanged();
        } else {
          sessionDataBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public Builder removeSessionData(int index) {
        if (sessionDataBuilder_ == null) {
          ensureSessionDataIsMutable();
          sessionData_.remove(index);
          onChanged();
        } else {
          sessionDataBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public com.weizhu.proto.SessionProtos.SessionData.Builder getSessionDataBuilder(
          int index) {
        return getSessionDataFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public com.weizhu.proto.SessionProtos.SessionDataOrBuilder getSessionDataOrBuilder(
          int index) {
        if (sessionDataBuilder_ == null) {
          return sessionData_.get(index);  } else {
          return sessionDataBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public java.util.List<? extends com.weizhu.proto.SessionProtos.SessionDataOrBuilder> 
           getSessionDataOrBuilderList() {
        if (sessionDataBuilder_ != null) {
          return sessionDataBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(sessionData_);
        }
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public com.weizhu.proto.SessionProtos.SessionData.Builder addSessionDataBuilder() {
        return getSessionDataFieldBuilder().addBuilder(
            com.weizhu.proto.SessionProtos.SessionData.getDefaultInstance());
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public com.weizhu.proto.SessionProtos.SessionData.Builder addSessionDataBuilder(
          int index) {
        return getSessionDataFieldBuilder().addBuilder(
            index, com.weizhu.proto.SessionProtos.SessionData.getDefaultInstance());
      }
      /**
       * <code>repeated .weizhu.session.SessionData session_data = 1;</code>
       */
      public java.util.List<com.weizhu.proto.SessionProtos.SessionData.Builder> 
           getSessionDataBuilderList() {
        return getSessionDataFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilder<
          com.weizhu.proto.SessionProtos.SessionData, com.weizhu.proto.SessionProtos.SessionData.Builder, com.weizhu.proto.SessionProtos.SessionDataOrBuilder> 
          getSessionDataFieldBuilder() {
        if (sessionDataBuilder_ == null) {
          sessionDataBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
              com.weizhu.proto.SessionProtos.SessionData, com.weizhu.proto.SessionProtos.SessionData.Builder, com.weizhu.proto.SessionProtos.SessionDataOrBuilder>(
                  sessionData_,
                  ((bitField0_ & 0x00000001) == 0x00000001),
                  getParentForChildren(),
                  isClean());
          sessionData_ = null;
        }
        return sessionDataBuilder_;
      }

      // @@protoc_insertion_point(builder_scope:weizhu.session.dao.SessionDataList)
    }

    static {
      defaultInstance = new SessionDataList(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:weizhu.session.dao.SessionDataList)
  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_weizhu_session_dao_SessionDataList_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_weizhu_session_dao_SessionDataList_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021session_dao.proto\022\022weizhu.session.dao\032" +
      "\rsession.proto\"D\n\017SessionDataList\0221\n\014ses" +
      "sion_data\030\001 \003(\0132\033.weizhu.session.Session" +
      "DataB.\n\032com.weizhu.service.sessionB\020Sess" +
      "ionDAOProtos"
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
          com.weizhu.proto.SessionProtos.getDescriptor(),
        }, assigner);
    internal_static_weizhu_session_dao_SessionDataList_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_weizhu_session_dao_SessionDataList_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_weizhu_session_dao_SessionDataList_descriptor,
        new java.lang.String[] { "SessionData", });
    com.weizhu.proto.SessionProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
