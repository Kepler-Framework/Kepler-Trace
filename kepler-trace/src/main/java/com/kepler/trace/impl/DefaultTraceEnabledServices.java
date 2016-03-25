package com.kepler.trace.impl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import com.kepler.trace.TraceEnabledServices;
import com.kepler.trace.TraceLogger;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class DefaultTraceEnabledServices implements BeanPostProcessor, TraceEnabledServices {

	private final Set<Object> traceLogEnabledServices = new HashSet<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		TraceLogger annotation = AnnotationUtils.findAnnotation(bean.getClass(), TraceLogger.class);
		if (null != annotation) {
			this.traceLogEnabledServices.add(bean);
		}
		return bean;
	}

	@Override
	public Set<Object> getTraceLogEnabledService() {
		return this.traceLogEnabledServices;
	}
}
