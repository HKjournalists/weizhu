// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: upload_dao.proto

package com.weizhu.service.upload;

public final class UploadDAOProtos {
  private UploadDAOProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface ImageTagListOrBuilder extends
      // @@protoc_insertion_point(interface_extends:weizhu.upload.dao.ImageTagList)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated string tag = 1;</code>
     */
    com.google.protobuf.ProtocolStringList
        getTagList();
    /**
     * <code>repeated string tag = 1;</code>
     */
    int getTagCount();
    /**
     * <code>repeated string tag = 1;</code>
     */
    java.lang.String getTag(int index);
    /**
     * <code>repeated string tag = 1;</code>
     */
    com.google.protobuf.ByteString
        getTagBytes(int index);
  }
  /**
   * Protobuf type {@code weizhu.upload.dao.ImageTagList}
   */
  public static final class ImageTagList extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:weizhu.upload.dao.ImageTagList)
      ImageTagListOrBuilder {
    // Use ImageTagList.newBuilder() to construct.
    private ImageTagList(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private ImageTagList(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final ImageTagList defaultInstance;
    public static ImageTagList getDefaultInstance() {
      return defaultInstance;
    }

    public ImageTagList getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private ImageTagList(
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
              if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                tag_ = new com.google.protobuf.LazyStringArrayList();
                mutable_bitField0_ |= 0x00000001;
              }
              tag_.add(bs);
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
          tag_ = tag_.getUnmodifiableView();
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.weizhu.service.upload.UploadDAOProtos.internal_static_weizhu_upload_dao_ImageTagList_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.weizhu.service.upload.UploadDAOProtos.internal_static_weizhu_upload_dao_ImageTagList_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.weizhu.service.upload.UploadDAOProtos.ImageTagList.class, com.weizhu.service.upload.UploadDAOProtos.ImageTagList.Builder.class);
    }

    public static com.google.protobuf.Parser<ImageTagList> PARSER =
        new com.google.protobuf.AbstractParser<ImageTagList>() {
      public ImageTagList parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new ImageTagList(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<ImageTagList> getParserForType() {
      return PARSER;
    }

    public static final int TAG_FIELD_NUMBER = 1;
    private com.google.protobuf.LazyStringList tag_;
    /**
     * <code>repeated string tag = 1;</code>
     */
    public com.google.protobuf.ProtocolStringList
        getTagList() {
      return tag_;
    }
    /**
     * <code>repeated string tag = 1;</code>
     */
    public int getTagCount() {
      return tag_.size();
    }
    /**
     * <code>repeated string tag = 1;</code>
     */
    public java.lang.String getTag(int index) {
      return tag_.get(index);
    }
    /**
     * <code>repeated string tag = 1;</code>
     */
    public com.google.protobuf.ByteString
        getTagBytes(int index) {
      return tag_.getByteString(index);
    }

    private void initFields() {
      tag_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      for (int i = 0; i < tag_.size(); i++) {
        output.writeBytes(1, tag_.getByteString(i));
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      {
        int dataSize = 0;
        for (int i = 0; i < tag_.size(); i++) {
          dataSize += com.google.protobuf.CodedOutputStream
            .computeBytesSizeNoTag(tag_.getByteString(i));
        }
        size += dataSize;
        size += 1 * getTagList().size();
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

    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.weizhu.service.upload.UploadDAOProtos.ImageTagList parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.weizhu.service.upload.UploadDAOProtos.ImageTagList prototype) {
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
     * Protobuf type {@code weizhu.upload.dao.ImageTagList}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:weizhu.upload.dao.ImageTagList)
        com.weizhu.service.upload.UploadDAOProtos.ImageTagListOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.weizhu.service.upload.UploadDAOProtos.internal_static_weizhu_upload_dao_ImageTagList_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.weizhu.service.upload.UploadDAOProtos.internal_static_weizhu_upload_dao_ImageTagList_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.weizhu.service.upload.UploadDAOProtos.ImageTagList.class, com.weizhu.service.upload.UploadDAOProtos.ImageTagList.Builder.class);
      }

      // Construct using com.weizhu.service.upload.UploadDAOProtos.ImageTagList.newBuilder()
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
        tag_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.weizhu.service.upload.UploadDAOProtos.internal_static_weizhu_upload_dao_ImageTagList_descriptor;
      }

      public com.weizhu.service.upload.UploadDAOProtos.ImageTagList getDefaultInstanceForType() {
        return com.weizhu.service.upload.UploadDAOProtos.ImageTagList.getDefaultInstance();
      }

      public com.weizhu.service.upload.UploadDAOProtos.ImageTagList build() {
        com.weizhu.service.upload.UploadDAOProtos.ImageTagList result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.weizhu.service.upload.UploadDAOProtos.ImageTagList buildPartial() {
        com.weizhu.service.upload.UploadDAOProtos.ImageTagList result = new com.weizhu.service.upload.UploadDAOProtos.ImageTagList(this);
        int from_bitField0_ = bitField0_;
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          tag_ = tag_.getUnmodifiableView();
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.tag_ = tag_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.weizhu.service.upload.UploadDAOProtos.ImageTagList) {
          return mergeFrom((com.weizhu.service.upload.UploadDAOProtos.ImageTagList)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.weizhu.service.upload.UploadDAOProtos.ImageTagList other) {
        if (other == com.weizhu.service.upload.UploadDAOProtos.ImageTagList.getDefaultInstance()) return this;
        if (!other.tag_.isEmpty()) {
          if (tag_.isEmpty()) {
            tag_ = other.tag_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureTagIsMutable();
            tag_.addAll(other.tag_);
          }
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.weizhu.service.upload.UploadDAOProtos.ImageTagList parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.weizhu.service.upload.UploadDAOProtos.ImageTagList) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private com.google.protobuf.LazyStringList tag_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      private void ensureTagIsMutable() {
        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
          tag_ = new com.google.protobuf.LazyStringArrayList(tag_);
          bitField0_ |= 0x00000001;
         }
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public com.google.protobuf.ProtocolStringList
          getTagList() {
        return tag_.getUnmodifiableView();
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public int getTagCount() {
        return tag_.size();
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public java.lang.String getTag(int index) {
        return tag_.get(index);
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public com.google.protobuf.ByteString
          getTagBytes(int index) {
        return tag_.getByteString(index);
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public Builder setTag(
          int index, java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureTagIsMutable();
        tag_.set(index, value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public Builder addTag(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureTagIsMutable();
        tag_.add(value);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public Builder addAllTag(
          java.lang.Iterable<java.lang.String> values) {
        ensureTagIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, tag_);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public Builder clearTag() {
        tag_ = com.google.protobuf.LazyStringArrayList.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }
      /**
       * <code>repeated string tag = 1;</code>
       */
      public Builder addTagBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureTagIsMutable();
        tag_.add(value);
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:weizhu.upload.dao.ImageTagList)
    }

    static {
      defaultInstance = new ImageTagList(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:weizhu.upload.dao.ImageTagList)
  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_weizhu_upload_dao_ImageTagList_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_weizhu_upload_dao_ImageTagList_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020upload_dao.proto\022\021weizhu.upload.dao\032\014u" +
      "pload.proto\"\033\n\014ImageTagList\022\013\n\003tag\030\001 \003(\t" +
      "B,\n\031com.weizhu.service.uploadB\017UploadDAO" +
      "Protos"
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
          com.weizhu.proto.UploadProtos.getDescriptor(),
        }, assigner);
    internal_static_weizhu_upload_dao_ImageTagList_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_weizhu_upload_dao_ImageTagList_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_weizhu_upload_dao_ImageTagList_descriptor,
        new java.lang.String[] { "Tag", });
    com.weizhu.proto.UploadProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
