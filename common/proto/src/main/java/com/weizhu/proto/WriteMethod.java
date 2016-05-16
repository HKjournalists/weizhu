package com.weizhu.proto;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 标记服务方法是写操作
 */
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface WriteMethod {
}
