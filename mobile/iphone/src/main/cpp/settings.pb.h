// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: settings.proto

#ifndef PROTOBUF_settings_2eproto__INCLUDED
#define PROTOBUF_settings_2eproto__INCLUDED

#include <string>

#include <google/protobuf/stubs/common.h>

#if GOOGLE_PROTOBUF_VERSION < 2006000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please update
#error your headers.
#endif
#if 2006001 < GOOGLE_PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/message_lite.h>
#include <google/protobuf/repeated_field.h>
#include <google/protobuf/extension_set.h>
#include "weizhu.pb.h"
// @@protoc_insertion_point(includes)

namespace weizhu {
namespace settings {

// Internal implementation detail -- do not call these.
void  protobuf_AddDesc_settings_2eproto();
void protobuf_AssignDesc_settings_2eproto();
void protobuf_ShutdownFile_settings_2eproto();

class Settings;
class Settings_DoNotDisturb;
class SetDoNotDisturbRequest;
class SettingsResponse;
class GetUserSettingsRequest;
class GetUserSettingsResponse;

// ===================================================================

class Settings_DoNotDisturb : public ::google::protobuf::MessageLite {
 public:
  Settings_DoNotDisturb();
  virtual ~Settings_DoNotDisturb();

  Settings_DoNotDisturb(const Settings_DoNotDisturb& from);

  inline Settings_DoNotDisturb& operator=(const Settings_DoNotDisturb& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const Settings_DoNotDisturb& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const Settings_DoNotDisturb* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(Settings_DoNotDisturb* other);

  // implements Message ----------------------------------------------

  Settings_DoNotDisturb* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const Settings_DoNotDisturb& from);
  void MergeFrom(const Settings_DoNotDisturb& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required bool enable = 1;
  inline bool has_enable() const;
  inline void clear_enable();
  static const int kEnableFieldNumber = 1;
  inline bool enable() const;
  inline void set_enable(bool value);

  // optional int32 begin_time = 2;
  inline bool has_begin_time() const;
  inline void clear_begin_time();
  static const int kBeginTimeFieldNumber = 2;
  inline ::google::protobuf::int32 begin_time() const;
  inline void set_begin_time(::google::protobuf::int32 value);

  // optional int32 end_time = 3;
  inline bool has_end_time() const;
  inline void clear_end_time();
  static const int kEndTimeFieldNumber = 3;
  inline ::google::protobuf::int32 end_time() const;
  inline void set_end_time(::google::protobuf::int32 value);

  // @@protoc_insertion_point(class_scope:weizhu.settings.Settings.DoNotDisturb)
 private:
  inline void set_has_enable();
  inline void clear_has_enable();
  inline void set_has_begin_time();
  inline void clear_has_begin_time();
  inline void set_has_end_time();
  inline void clear_has_end_time();

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  bool enable_;
  ::google::protobuf::int32 begin_time_;
  ::google::protobuf::int32 end_time_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static Settings_DoNotDisturb* default_instance_;
};
// -------------------------------------------------------------------

class Settings : public ::google::protobuf::MessageLite {
 public:
  Settings();
  virtual ~Settings();

  Settings(const Settings& from);

  inline Settings& operator=(const Settings& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const Settings& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const Settings* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(Settings* other);

  // implements Message ----------------------------------------------

  Settings* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const Settings& from);
  void MergeFrom(const Settings& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  typedef Settings_DoNotDisturb DoNotDisturb;

  // accessors -------------------------------------------------------

  // required int64 user_id = 1;
  inline bool has_user_id() const;
  inline void clear_user_id();
  static const int kUserIdFieldNumber = 1;
  inline ::google::protobuf::int64 user_id() const;
  inline void set_user_id(::google::protobuf::int64 value);

  // optional .weizhu.settings.Settings.DoNotDisturb do_not_disturb = 2;
  inline bool has_do_not_disturb() const;
  inline void clear_do_not_disturb();
  static const int kDoNotDisturbFieldNumber = 2;
  inline const ::weizhu::settings::Settings_DoNotDisturb& do_not_disturb() const;
  inline ::weizhu::settings::Settings_DoNotDisturb* mutable_do_not_disturb();
  inline ::weizhu::settings::Settings_DoNotDisturb* release_do_not_disturb();
  inline void set_allocated_do_not_disturb(::weizhu::settings::Settings_DoNotDisturb* do_not_disturb);

  // @@protoc_insertion_point(class_scope:weizhu.settings.Settings)
 private:
  inline void set_has_user_id();
  inline void clear_has_user_id();
  inline void set_has_do_not_disturb();
  inline void clear_has_do_not_disturb();

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  ::google::protobuf::int64 user_id_;
  ::weizhu::settings::Settings_DoNotDisturb* do_not_disturb_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static Settings* default_instance_;
};
// -------------------------------------------------------------------

class SetDoNotDisturbRequest : public ::google::protobuf::MessageLite {
 public:
  SetDoNotDisturbRequest();
  virtual ~SetDoNotDisturbRequest();

  SetDoNotDisturbRequest(const SetDoNotDisturbRequest& from);

  inline SetDoNotDisturbRequest& operator=(const SetDoNotDisturbRequest& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const SetDoNotDisturbRequest& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const SetDoNotDisturbRequest* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(SetDoNotDisturbRequest* other);

  // implements Message ----------------------------------------------

  SetDoNotDisturbRequest* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const SetDoNotDisturbRequest& from);
  void MergeFrom(const SetDoNotDisturbRequest& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required .weizhu.settings.Settings.DoNotDisturb do_not_disturb = 1;
  inline bool has_do_not_disturb() const;
  inline void clear_do_not_disturb();
  static const int kDoNotDisturbFieldNumber = 1;
  inline const ::weizhu::settings::Settings_DoNotDisturb& do_not_disturb() const;
  inline ::weizhu::settings::Settings_DoNotDisturb* mutable_do_not_disturb();
  inline ::weizhu::settings::Settings_DoNotDisturb* release_do_not_disturb();
  inline void set_allocated_do_not_disturb(::weizhu::settings::Settings_DoNotDisturb* do_not_disturb);

  // @@protoc_insertion_point(class_scope:weizhu.settings.SetDoNotDisturbRequest)
 private:
  inline void set_has_do_not_disturb();
  inline void clear_has_do_not_disturb();

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  ::weizhu::settings::Settings_DoNotDisturb* do_not_disturb_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static SetDoNotDisturbRequest* default_instance_;
};
// -------------------------------------------------------------------

class SettingsResponse : public ::google::protobuf::MessageLite {
 public:
  SettingsResponse();
  virtual ~SettingsResponse();

  SettingsResponse(const SettingsResponse& from);

  inline SettingsResponse& operator=(const SettingsResponse& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const SettingsResponse& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const SettingsResponse* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(SettingsResponse* other);

  // implements Message ----------------------------------------------

  SettingsResponse* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const SettingsResponse& from);
  void MergeFrom(const SettingsResponse& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required .weizhu.settings.Settings settings = 1;
  inline bool has_settings() const;
  inline void clear_settings();
  static const int kSettingsFieldNumber = 1;
  inline const ::weizhu::settings::Settings& settings() const;
  inline ::weizhu::settings::Settings* mutable_settings();
  inline ::weizhu::settings::Settings* release_settings();
  inline void set_allocated_settings(::weizhu::settings::Settings* settings);

  // @@protoc_insertion_point(class_scope:weizhu.settings.SettingsResponse)
 private:
  inline void set_has_settings();
  inline void clear_has_settings();

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  ::weizhu::settings::Settings* settings_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static SettingsResponse* default_instance_;
};
// -------------------------------------------------------------------

class GetUserSettingsRequest : public ::google::protobuf::MessageLite {
 public:
  GetUserSettingsRequest();
  virtual ~GetUserSettingsRequest();

  GetUserSettingsRequest(const GetUserSettingsRequest& from);

  inline GetUserSettingsRequest& operator=(const GetUserSettingsRequest& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const GetUserSettingsRequest& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const GetUserSettingsRequest* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(GetUserSettingsRequest* other);

  // implements Message ----------------------------------------------

  GetUserSettingsRequest* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const GetUserSettingsRequest& from);
  void MergeFrom(const GetUserSettingsRequest& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // repeated int64 user_id = 1;
  inline int user_id_size() const;
  inline void clear_user_id();
  static const int kUserIdFieldNumber = 1;
  inline ::google::protobuf::int64 user_id(int index) const;
  inline void set_user_id(int index, ::google::protobuf::int64 value);
  inline void add_user_id(::google::protobuf::int64 value);
  inline const ::google::protobuf::RepeatedField< ::google::protobuf::int64 >&
      user_id() const;
  inline ::google::protobuf::RepeatedField< ::google::protobuf::int64 >*
      mutable_user_id();

  // @@protoc_insertion_point(class_scope:weizhu.settings.GetUserSettingsRequest)
 private:

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  ::google::protobuf::RepeatedField< ::google::protobuf::int64 > user_id_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static GetUserSettingsRequest* default_instance_;
};
// -------------------------------------------------------------------

class GetUserSettingsResponse : public ::google::protobuf::MessageLite {
 public:
  GetUserSettingsResponse();
  virtual ~GetUserSettingsResponse();

  GetUserSettingsResponse(const GetUserSettingsResponse& from);

  inline GetUserSettingsResponse& operator=(const GetUserSettingsResponse& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::std::string& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::std::string* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const GetUserSettingsResponse& default_instance();

  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  // Returns the internal default instance pointer. This function can
  // return NULL thus should not be used by the user. This is intended
  // for Protobuf internal code. Please use default_instance() declared
  // above instead.
  static inline const GetUserSettingsResponse* internal_default_instance() {
    return default_instance_;
  }
  #endif

  void Swap(GetUserSettingsResponse* other);

  // implements Message ----------------------------------------------

  GetUserSettingsResponse* New() const;
  void CheckTypeAndMergeFrom(const ::google::protobuf::MessageLite& from);
  void CopyFrom(const GetUserSettingsResponse& from);
  void MergeFrom(const GetUserSettingsResponse& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  void DiscardUnknownFields();
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:
  ::std::string GetTypeName() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // repeated .weizhu.settings.Settings settings = 1;
  inline int settings_size() const;
  inline void clear_settings();
  static const int kSettingsFieldNumber = 1;
  inline const ::weizhu::settings::Settings& settings(int index) const;
  inline ::weizhu::settings::Settings* mutable_settings(int index);
  inline ::weizhu::settings::Settings* add_settings();
  inline const ::google::protobuf::RepeatedPtrField< ::weizhu::settings::Settings >&
      settings() const;
  inline ::google::protobuf::RepeatedPtrField< ::weizhu::settings::Settings >*
      mutable_settings();

  // @@protoc_insertion_point(class_scope:weizhu.settings.GetUserSettingsResponse)
 private:

  ::std::string _unknown_fields_;

  ::google::protobuf::uint32 _has_bits_[1];
  mutable int _cached_size_;
  ::google::protobuf::RepeatedPtrField< ::weizhu::settings::Settings > settings_;
  #ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  friend void  protobuf_AddDesc_settings_2eproto_impl();
  #else
  friend void  protobuf_AddDesc_settings_2eproto();
  #endif
  friend void protobuf_AssignDesc_settings_2eproto();
  friend void protobuf_ShutdownFile_settings_2eproto();

  void InitAsDefaultInstance();
  static GetUserSettingsResponse* default_instance_;
};
// ===================================================================


// ===================================================================

// Settings_DoNotDisturb

// required bool enable = 1;
inline bool Settings_DoNotDisturb::has_enable() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void Settings_DoNotDisturb::set_has_enable() {
  _has_bits_[0] |= 0x00000001u;
}
inline void Settings_DoNotDisturb::clear_has_enable() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void Settings_DoNotDisturb::clear_enable() {
  enable_ = false;
  clear_has_enable();
}
inline bool Settings_DoNotDisturb::enable() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.Settings.DoNotDisturb.enable)
  return enable_;
}
inline void Settings_DoNotDisturb::set_enable(bool value) {
  set_has_enable();
  enable_ = value;
  // @@protoc_insertion_point(field_set:weizhu.settings.Settings.DoNotDisturb.enable)
}

// optional int32 begin_time = 2;
inline bool Settings_DoNotDisturb::has_begin_time() const {
  return (_has_bits_[0] & 0x00000002u) != 0;
}
inline void Settings_DoNotDisturb::set_has_begin_time() {
  _has_bits_[0] |= 0x00000002u;
}
inline void Settings_DoNotDisturb::clear_has_begin_time() {
  _has_bits_[0] &= ~0x00000002u;
}
inline void Settings_DoNotDisturb::clear_begin_time() {
  begin_time_ = 0;
  clear_has_begin_time();
}
inline ::google::protobuf::int32 Settings_DoNotDisturb::begin_time() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.Settings.DoNotDisturb.begin_time)
  return begin_time_;
}
inline void Settings_DoNotDisturb::set_begin_time(::google::protobuf::int32 value) {
  set_has_begin_time();
  begin_time_ = value;
  // @@protoc_insertion_point(field_set:weizhu.settings.Settings.DoNotDisturb.begin_time)
}

// optional int32 end_time = 3;
inline bool Settings_DoNotDisturb::has_end_time() const {
  return (_has_bits_[0] & 0x00000004u) != 0;
}
inline void Settings_DoNotDisturb::set_has_end_time() {
  _has_bits_[0] |= 0x00000004u;
}
inline void Settings_DoNotDisturb::clear_has_end_time() {
  _has_bits_[0] &= ~0x00000004u;
}
inline void Settings_DoNotDisturb::clear_end_time() {
  end_time_ = 0;
  clear_has_end_time();
}
inline ::google::protobuf::int32 Settings_DoNotDisturb::end_time() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.Settings.DoNotDisturb.end_time)
  return end_time_;
}
inline void Settings_DoNotDisturb::set_end_time(::google::protobuf::int32 value) {
  set_has_end_time();
  end_time_ = value;
  // @@protoc_insertion_point(field_set:weizhu.settings.Settings.DoNotDisturb.end_time)
}

// -------------------------------------------------------------------

// Settings

// required int64 user_id = 1;
inline bool Settings::has_user_id() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void Settings::set_has_user_id() {
  _has_bits_[0] |= 0x00000001u;
}
inline void Settings::clear_has_user_id() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void Settings::clear_user_id() {
  user_id_ = GOOGLE_LONGLONG(0);
  clear_has_user_id();
}
inline ::google::protobuf::int64 Settings::user_id() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.Settings.user_id)
  return user_id_;
}
inline void Settings::set_user_id(::google::protobuf::int64 value) {
  set_has_user_id();
  user_id_ = value;
  // @@protoc_insertion_point(field_set:weizhu.settings.Settings.user_id)
}

// optional .weizhu.settings.Settings.DoNotDisturb do_not_disturb = 2;
inline bool Settings::has_do_not_disturb() const {
  return (_has_bits_[0] & 0x00000002u) != 0;
}
inline void Settings::set_has_do_not_disturb() {
  _has_bits_[0] |= 0x00000002u;
}
inline void Settings::clear_has_do_not_disturb() {
  _has_bits_[0] &= ~0x00000002u;
}
inline void Settings::clear_do_not_disturb() {
  if (do_not_disturb_ != NULL) do_not_disturb_->::weizhu::settings::Settings_DoNotDisturb::Clear();
  clear_has_do_not_disturb();
}
inline const ::weizhu::settings::Settings_DoNotDisturb& Settings::do_not_disturb() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.Settings.do_not_disturb)
#ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  return do_not_disturb_ != NULL ? *do_not_disturb_ : *default_instance().do_not_disturb_;
#else
  return do_not_disturb_ != NULL ? *do_not_disturb_ : *default_instance_->do_not_disturb_;
#endif
}
inline ::weizhu::settings::Settings_DoNotDisturb* Settings::mutable_do_not_disturb() {
  set_has_do_not_disturb();
  if (do_not_disturb_ == NULL) do_not_disturb_ = new ::weizhu::settings::Settings_DoNotDisturb;
  // @@protoc_insertion_point(field_mutable:weizhu.settings.Settings.do_not_disturb)
  return do_not_disturb_;
}
inline ::weizhu::settings::Settings_DoNotDisturb* Settings::release_do_not_disturb() {
  clear_has_do_not_disturb();
  ::weizhu::settings::Settings_DoNotDisturb* temp = do_not_disturb_;
  do_not_disturb_ = NULL;
  return temp;
}
inline void Settings::set_allocated_do_not_disturb(::weizhu::settings::Settings_DoNotDisturb* do_not_disturb) {
  delete do_not_disturb_;
  do_not_disturb_ = do_not_disturb;
  if (do_not_disturb) {
    set_has_do_not_disturb();
  } else {
    clear_has_do_not_disturb();
  }
  // @@protoc_insertion_point(field_set_allocated:weizhu.settings.Settings.do_not_disturb)
}

// -------------------------------------------------------------------

// SetDoNotDisturbRequest

// required .weizhu.settings.Settings.DoNotDisturb do_not_disturb = 1;
inline bool SetDoNotDisturbRequest::has_do_not_disturb() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void SetDoNotDisturbRequest::set_has_do_not_disturb() {
  _has_bits_[0] |= 0x00000001u;
}
inline void SetDoNotDisturbRequest::clear_has_do_not_disturb() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void SetDoNotDisturbRequest::clear_do_not_disturb() {
  if (do_not_disturb_ != NULL) do_not_disturb_->::weizhu::settings::Settings_DoNotDisturb::Clear();
  clear_has_do_not_disturb();
}
inline const ::weizhu::settings::Settings_DoNotDisturb& SetDoNotDisturbRequest::do_not_disturb() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.SetDoNotDisturbRequest.do_not_disturb)
#ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  return do_not_disturb_ != NULL ? *do_not_disturb_ : *default_instance().do_not_disturb_;
#else
  return do_not_disturb_ != NULL ? *do_not_disturb_ : *default_instance_->do_not_disturb_;
#endif
}
inline ::weizhu::settings::Settings_DoNotDisturb* SetDoNotDisturbRequest::mutable_do_not_disturb() {
  set_has_do_not_disturb();
  if (do_not_disturb_ == NULL) do_not_disturb_ = new ::weizhu::settings::Settings_DoNotDisturb;
  // @@protoc_insertion_point(field_mutable:weizhu.settings.SetDoNotDisturbRequest.do_not_disturb)
  return do_not_disturb_;
}
inline ::weizhu::settings::Settings_DoNotDisturb* SetDoNotDisturbRequest::release_do_not_disturb() {
  clear_has_do_not_disturb();
  ::weizhu::settings::Settings_DoNotDisturb* temp = do_not_disturb_;
  do_not_disturb_ = NULL;
  return temp;
}
inline void SetDoNotDisturbRequest::set_allocated_do_not_disturb(::weizhu::settings::Settings_DoNotDisturb* do_not_disturb) {
  delete do_not_disturb_;
  do_not_disturb_ = do_not_disturb;
  if (do_not_disturb) {
    set_has_do_not_disturb();
  } else {
    clear_has_do_not_disturb();
  }
  // @@protoc_insertion_point(field_set_allocated:weizhu.settings.SetDoNotDisturbRequest.do_not_disturb)
}

// -------------------------------------------------------------------

// SettingsResponse

// required .weizhu.settings.Settings settings = 1;
inline bool SettingsResponse::has_settings() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void SettingsResponse::set_has_settings() {
  _has_bits_[0] |= 0x00000001u;
}
inline void SettingsResponse::clear_has_settings() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void SettingsResponse::clear_settings() {
  if (settings_ != NULL) settings_->::weizhu::settings::Settings::Clear();
  clear_has_settings();
}
inline const ::weizhu::settings::Settings& SettingsResponse::settings() const {
  // @@protoc_insertion_point(field_get:weizhu.settings.SettingsResponse.settings)
#ifdef GOOGLE_PROTOBUF_NO_STATIC_INITIALIZER
  return settings_ != NULL ? *settings_ : *default_instance().settings_;
#else
  return settings_ != NULL ? *settings_ : *default_instance_->settings_;
#endif
}
inline ::weizhu::settings::Settings* SettingsResponse::mutable_settings() {
  set_has_settings();
  if (settings_ == NULL) settings_ = new ::weizhu::settings::Settings;
  // @@protoc_insertion_point(field_mutable:weizhu.settings.SettingsResponse.settings)
  return settings_;
}
inline ::weizhu::settings::Settings* SettingsResponse::release_settings() {
  clear_has_settings();
  ::weizhu::settings::Settings* temp = settings_;
  settings_ = NULL;
  return temp;
}
inline void SettingsResponse::set_allocated_settings(::weizhu::settings::Settings* settings) {
  delete settings_;
  settings_ = settings;
  if (settings) {
    set_has_settings();
  } else {
    clear_has_settings();
  }
  // @@protoc_insertion_point(field_set_allocated:weizhu.settings.SettingsResponse.settings)
}

// -------------------------------------------------------------------

// GetUserSettingsRequest

// repeated int64 user_id = 1;
inline int GetUserSettingsRequest::user_id_size() const {
  return user_id_.size();
}
inline void GetUserSettingsRequest::clear_user_id() {
  user_id_.Clear();
}
inline ::google::protobuf::int64 GetUserSettingsRequest::user_id(int index) const {
  // @@protoc_insertion_point(field_get:weizhu.settings.GetUserSettingsRequest.user_id)
  return user_id_.Get(index);
}
inline void GetUserSettingsRequest::set_user_id(int index, ::google::protobuf::int64 value) {
  user_id_.Set(index, value);
  // @@protoc_insertion_point(field_set:weizhu.settings.GetUserSettingsRequest.user_id)
}
inline void GetUserSettingsRequest::add_user_id(::google::protobuf::int64 value) {
  user_id_.Add(value);
  // @@protoc_insertion_point(field_add:weizhu.settings.GetUserSettingsRequest.user_id)
}
inline const ::google::protobuf::RepeatedField< ::google::protobuf::int64 >&
GetUserSettingsRequest::user_id() const {
  // @@protoc_insertion_point(field_list:weizhu.settings.GetUserSettingsRequest.user_id)
  return user_id_;
}
inline ::google::protobuf::RepeatedField< ::google::protobuf::int64 >*
GetUserSettingsRequest::mutable_user_id() {
  // @@protoc_insertion_point(field_mutable_list:weizhu.settings.GetUserSettingsRequest.user_id)
  return &user_id_;
}

// -------------------------------------------------------------------

// GetUserSettingsResponse

// repeated .weizhu.settings.Settings settings = 1;
inline int GetUserSettingsResponse::settings_size() const {
  return settings_.size();
}
inline void GetUserSettingsResponse::clear_settings() {
  settings_.Clear();
}
inline const ::weizhu::settings::Settings& GetUserSettingsResponse::settings(int index) const {
  // @@protoc_insertion_point(field_get:weizhu.settings.GetUserSettingsResponse.settings)
  return settings_.Get(index);
}
inline ::weizhu::settings::Settings* GetUserSettingsResponse::mutable_settings(int index) {
  // @@protoc_insertion_point(field_mutable:weizhu.settings.GetUserSettingsResponse.settings)
  return settings_.Mutable(index);
}
inline ::weizhu::settings::Settings* GetUserSettingsResponse::add_settings() {
  // @@protoc_insertion_point(field_add:weizhu.settings.GetUserSettingsResponse.settings)
  return settings_.Add();
}
inline const ::google::protobuf::RepeatedPtrField< ::weizhu::settings::Settings >&
GetUserSettingsResponse::settings() const {
  // @@protoc_insertion_point(field_list:weizhu.settings.GetUserSettingsResponse.settings)
  return settings_;
}
inline ::google::protobuf::RepeatedPtrField< ::weizhu::settings::Settings >*
GetUserSettingsResponse::mutable_settings() {
  // @@protoc_insertion_point(field_mutable_list:weizhu.settings.GetUserSettingsResponse.settings)
  return &settings_;
}


// @@protoc_insertion_point(namespace_scope)

}  // namespace settings
}  // namespace weizhu

// @@protoc_insertion_point(global_scope)

#endif  // PROTOBUF_settings_2eproto__INCLUDED
