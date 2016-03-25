package com.kepler.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 控制是否需要输出 
 */
/**
 * @author zhangjiehao 2016年1月12日
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TraceEnabled {

	/*
	 * 日志输出的条件
	 */
	String when() default "true";
}
