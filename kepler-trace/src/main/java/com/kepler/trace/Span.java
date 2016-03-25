package com.kepler.trace;

public class Span {

	private long startTime;
	
	private String trace;
	
	private String span;
	
	private String parentSpan;

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
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

	@Override
	public String toString() {
		return "[trace]" + this.trace + "[span]" + this.span + "[parentSpan]" + this.parentSpan + "[startTime]" + this.startTime;
	}

	public String getTrace() {
		return trace;
	}

	public void setTrace(String trace) {
		this.trace = trace;
	}

}
