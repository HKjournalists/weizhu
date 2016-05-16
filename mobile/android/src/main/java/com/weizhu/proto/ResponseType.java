package com.weizhu.proto;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.protobuf.MessageLite;

@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface ResponseType {

	Class<? extends MessageLite> value();
	
}
