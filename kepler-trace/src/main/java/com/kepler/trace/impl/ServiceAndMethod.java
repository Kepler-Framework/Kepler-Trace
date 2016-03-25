package com.kepler.trace.impl;

import java.util.Arrays;

import com.kepler.org.apache.commons.lang.ClassUtils;
import com.kepler.org.apache.commons.lang.builder.ToStringBuilder;
import com.kepler.service.Service;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class ServiceAndMethod {

	private final Service service;

	private final String method;

	private final Class<?>[] methodTypes;

	public ServiceAndMethod(Service service, String method, Class<?>[] methodTypes) {
		this.service = service;
		this.method = method;
		this.methodTypes = methodTypes;
		for (int i = 0; i < this.methodTypes.length; i++) {
			if (this.methodTypes[i].isPrimitive()) {
				this.methodTypes[i] = ClassUtils.primitiveToWrapper(this.methodTypes[i]);
			}
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += 31 * hash + service.hashCode();
		hash += 31 * hash + method.hashCode();
		for (Class<?> methodType : methodTypes) {
			hash += 31 * hash + methodType.hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object that) {
		if (!(that instanceof ServiceAndMethod)) {
			return false;
		}
		if (that == this) {
			return true;
		}
		ServiceAndMethod other = (ServiceAndMethod) that;
		return this.service.equals(other.service) && this.method.equals(other.method) && Arrays.equals(this.methodTypes, other.methodTypes);
	}

	public Service getService() {
		return this.service;
	}

	public String getMethod() {
		return this.method;
	}

	public Class<?>[] getMethodTypes() {
		return this.methodTypes;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
