package com.kepler.trace.collector;

import java.io.Serializable;

@SuppressWarnings("unused")
public class TraceInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private String trace;
	
	private String span;

	private String parentSpan;
	
	private String service;
	
	private String method;

	private String local;

	private String remote;

	private long transferTime;
	
	private long waiting;

	private long elapse;

	private long startTime;
	
	private long receivedTime;
	
	private Object[] request;

	private Object response;

	private String throwable;

	public TraceInfo() {

	}
	
	public String getSpan() {
		return span;
	}

	public void setSpan(String span) {
		this.span = span;
	}

	public String getParentSpan() {
		return parentSpan;
	}

	public void setParentSpan(String parentSpan) {
		this.parentSpan = parentSpan;
	}

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public String getRemote() {
		return remote;
	}

	public void setRemote(String remote) {
		this.remote = remote;
	}

	public long getWaiting() {
		return waiting;
	}

	public void setWaiting(long waiting) {
		this.waiting = waiting;
	}

	public long getElapse() {
		return elapse;
	}

	public void setElapse(long elapse) {
		this.elapse = elapse;
	}

	public Object[] getRequest() {
		return request;
	}

	public void setRequest(Object[] request) {
		this.request = request;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

	public String getThrowable() {
		return throwable;
	}

	public void setThrowable(String throwable) {
		this.throwable = throwable;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(long receivedTime) {
		this.receivedTime = receivedTime;
	}

	public String getTrace() {
		return trace;
	}

	public void setTrace(String trace) {
		this.trace = trace;
	}

	public long getTransferTime() {
		return this.receivedTime - this.startTime;
	}

	public void setTransferTime(long transferTime) {
		this.transferTime = this.receivedTime - this.startTime;
	}

}