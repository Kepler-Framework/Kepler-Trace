package com.kepler.trace.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.kepler.service.Service;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class Tracers {

	private final ConcurrentMap<Service, TraceConfig> serviceLogConfigs = new ConcurrentHashMap<>();

	private final ConcurrentMap<ServiceAndMethod, TraceConfig> methodLogConfigs = new ConcurrentHashMap<>();

	public static class Builder {

		private final Tracers trace = new Tracers();

		public Builder addServiceTraceConfig(Service service, TraceConfig traceConfig) {
			this.trace.serviceLogConfigs.put(service, traceConfig);
			return this;
		}

		public Builder addMethodTraceConfig(ServiceAndMethod method, TraceConfig traceConfig) {
			this.trace.methodLogConfigs.put(method, traceConfig);
			return this;
		}

		public Tracers build() {
			for (ServiceAndMethod method : this.trace.methodLogConfigs.keySet()) {
				TraceConfig methodTraceConfig = this.trace.methodLogConfigs.get(method);
				if (null == methodTraceConfig.getLogger()) {
					TraceConfig serviceConfig = this.trace.serviceLogConfigs.get(method.getService());
					methodTraceConfig.setLogger(serviceConfig.getLogger());
				}
			}
			return this.trace;
		}

	}

	public TraceConfig getLogConfig(ServiceAndMethod method) {
		return this.methodLogConfigs.get(method);
	}

	public TraceConfig getLogConfig(Service service) {
		return this.serviceLogConfigs.get(service);
	}
}
