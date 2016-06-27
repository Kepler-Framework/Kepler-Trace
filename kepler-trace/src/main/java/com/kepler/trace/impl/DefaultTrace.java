package com.kepler.trace.impl;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import com.kepler.config.ConfigAware;
import com.kepler.config.PropertiesUtils;
import com.kepler.org.apache.commons.lang.ClassUtils;
import com.kepler.protocol.Request;
import com.kepler.protocol.Response;
import com.kepler.service.Service;
import com.kepler.service.exported.ExportedServices;
import com.kepler.trace.ObjectPrinter;
import com.kepler.trace.Trace;
import com.kepler.trace.TraceCollector;
import com.kepler.trace.TraceEnabled;
import com.kepler.trace.TraceEnabledServices;
import com.kepler.trace.TraceInfoBuilder;
import com.kepler.trace.TraceLogger;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.impl.Tracers.Builder;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class DefaultTrace implements Trace, ApplicationListener<ContextRefreshedEvent>, ConfigAware {

	private final static Log LOGGER = LogFactory.getLog(DefaultTrace.class);

	private final AnnotationPropertyResolver annotationPropertyResolver = new AnnotationPropertyResolver();

	private final TraceEnabledServices traceEnabledServices;

	private final ExportedServices exportedServices;

	private final ObjectPrinter objectPrinter;

	private final TraceCollector traceCollector;
	
	private volatile Tracers tracers;
	
	public DefaultTrace(TraceEnabledServices traceEnabledServices, ExportedServices exportedServices, ObjectPrinter objectPrinter, TraceCollector traceCollector) {
		this.traceEnabledServices = traceEnabledServices;
		this.exportedServices = exportedServices;
		this.objectPrinter = objectPrinter;
		this.traceCollector = traceCollector;
	}

	@Override
	public void trace(Request request, Response response, String local, String remote, long waiting, long elapse, long receivedTime) {
		if (!PropertiesUtils.get(Trace.ENABLED_KEY, Trace.ENABLED_DEF)) {
			return;
		}
		
		if (null == this.tracers) {
			DefaultTrace.LOGGER.warn("Traces hasn't been initialized");
			return;
		}
		
		TraceInfo traceInfo = TraceInfoBuilder.build(request, response, local, remote, waiting, elapse, receivedTime);
		
		if (request.headers() != null && traceInfo != null && !StringUtils.isEmpty(traceInfo.getTrace()) ) {
			// 是否启用Trace，启动则放入收集器收集		
			this.traceCollector.put(traceInfo);
		}
		
		ServiceAndMethod method = new ServiceAndMethod(request.service(), request.method(), request.types());
		TraceConfig logConfig = this.tracers.getLogConfig(method);
		if (null == logConfig || null == logConfig.getExpression()) {
			return;
		}
		
		try {
			// 条件match才打日志
			if (this.traceEnabled(logConfig, new ArgWrapper(waiting, elapse, response.valid() ? response.response() : response.throwable(), request.args()))) {
				logConfig.getLogger().info(traceMessage(traceInfo));
			}
		} catch (EvaluationException e) {
			DefaultTrace.LOGGER.error("Error evaluating: " + logConfig.getExpression());
		}
	}

	private String traceMessage(TraceInfo traceInfo) {
		return this.objectPrinter.print(traceInfo);
	}

	private Boolean traceEnabled(TraceConfig logConfig, ArgWrapper args) {
		Expression expression = logConfig.getExpression();
		if (expression != null) {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(args);
			Boolean traceEnabled = expression.getValue(context, Boolean.class);
			return (null == traceEnabled ? Boolean.FALSE : traceEnabled);
		} else {
			return false;
		}
	}
	

	@Override
	public void changed(Map<String, String> prevConfig, Map<String, String> currentConfig) {
		boolean needRefreshed = false;
		for (String key : this.annotationPropertyResolver.keys) {
			// 当前没有指定表达式但新配置存在指定表达式则刷新
			if (!prevConfig.containsKey(key) && currentConfig.containsKey(key)) {
				needRefreshed = true;
				break;
			}
			// 当前指定表达式的Value改变
			if (!prevConfig.get(key).equals(currentConfig.get(key))) {
				needRefreshed = true;
				break;
			}
		}
		if (needRefreshed) {
			this.rebuildTracers();
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// 容器启动完毕后初始化表达式
		this.rebuildTracers();
	}

	private void rebuildTracers() {
		Tracers.Builder tracersBuilder = new Tracers.Builder();
		Set<Object> tracerEnabledServices = this.traceEnabledServices.getTraceLogEnabledService();
		for (Entry<Service, Object> exportedServiceEntry : this.exportedServices.services().entrySet()) {
			Object exportedServiceInstance = exportedServiceEntry.getValue();
			if (tracerEnabledServices.contains(exportedServiceInstance)) {
				Service exportedService = exportedServiceEntry.getKey();
				this.addToTracers(tracersBuilder, exportedService, exportedServiceInstance);
			}
		}
		this.tracers = tracersBuilder.build();
	}

	private void addToTracers(Tracers.Builder tracersBuilder, Service exportedService, Object exportedServiceInstance) {
		Class<?> beanClass = Advised.class.isAssignableFrom(exportedServiceInstance.getClass()) ? Advised.class.cast(exportedServiceInstance).getTargetClass() : exportedServiceInstance.getClass();
		TraceLogger loggerAnnotation = AnnotationUtils.findAnnotation(beanClass, TraceLogger.class);
		if (loggerAnnotation != null) {
			TraceConfig config = new TraceConfig();
			config.setLogger(LogFactory.getLog((String) this.annotationPropertyResolver.resolve(loggerAnnotation.logger())));
			tracersBuilder.addServiceTraceConfig(exportedService, config);
			Method[] methods = beanClass.getDeclaredMethods();
			for (Method method : methods) {
				this.addToTracers(tracersBuilder, exportedService, method);
			}
		}
	}

	private void addToTracers(Builder tracersBuilder, Service service, Method method) {
		TraceEnabled traceEnabledAnnotation = AnnotationUtils.findAnnotation(method, TraceEnabled.class);
		if (null == traceEnabledAnnotation) {
			return;
		}
		ServiceAndMethod serviceAndMethod = new ServiceAndMethod(service, method.getName(), getParamTypes(method));
		TraceConfig config = new TraceConfig();
		config.setTraceCondition((String) this.annotationPropertyResolver.resolve(traceEnabledAnnotation.when()));
		TraceLogger loggerAnnotation;
		if (null != (loggerAnnotation = AnnotationUtils.findAnnotation(method, TraceLogger.class))) {
			config.setLogger(LogFactory.getLog((String) this.annotationPropertyResolver.resolve(loggerAnnotation.logger())));
		}
		tracersBuilder.addMethodTraceConfig(serviceAndMethod, config);
	}

	private Class<?>[] getParamTypes(Method method) {
		Class<?>[] types =  method.getParameterTypes();
		for (int i = 0; i < types.length; i++) {
			if (types[i].isPrimitive()) {
				types[i] = ClassUtils.primitiveToWrapper(types[i]);
			}
		}
		return types;
	}

	private class ArgWrapper {

		private final Object response;

		private final Object[] args;

		private final long waiting;

		private final long elapse;

		ArgWrapper(long waiting, long elapse, Object response, Object... args) {
			this.waiting = waiting;
			this.elapse = elapse;
			this.response = response;
			this.args = args;
		}

		@SuppressWarnings("unused")
		public Object[] getArgs() {
			return this.args;
		}

		@SuppressWarnings("unused")
		public Object getResponse() {
			return this.response;
		}

		@SuppressWarnings("unused")
		public long getWaiting() {
			return this.waiting;
		}

		@SuppressWarnings("unused")
		public long getElapse() {
			return this.elapse;
		}
	}

	private class AnnotationPropertyResolver {

		// 支持${XXXX}
		private Pattern pattern = Pattern.compile("^\\$\\{(.*)\\}");

		private Set<String> keys = new HashSet<>();

		public Object resolve(String item) {
			if (item == null) {
				return item;
			}
			Matcher matcher = this.pattern.matcher(item);
			if (!matcher.find()) {
				return item;
			}
			String key = matcher.group(1);
			this.keys.add(key);
			return PropertiesUtils.get(key);
		}
	}
}
