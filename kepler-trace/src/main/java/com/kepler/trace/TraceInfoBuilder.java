package com.kepler.trace;

import com.kepler.protocol.Request;
import com.kepler.protocol.Response;
import com.kepler.trace.collector.TraceInfo;

public class TraceInfoBuilder {

	public static TraceInfo build(Request request, Response response, String local, String remote, long waiting, long elapse, long receivedTime) {
		TraceInfo traceInfo = new TraceInfo();
		traceInfo.setElapse(elapse);
		traceInfo.setWaiting(waiting);
		traceInfo.setLocal(local);
		traceInfo.setRemote(remote);
		traceInfo.setRequest(request.args()); 
		traceInfo.setResponse(response.valid() ? response.response() : null);
		traceInfo.setThrowable(!response.valid() ? response.throwable().getMessage() : null); 
		traceInfo.setStartTime(SpanContext.get().getStartTime());
		traceInfo.setParentSpan(SpanContext.get().getParentSpan());
		traceInfo.setSpan(SpanContext.get().getSpan());
		traceInfo.setTrace(SpanContext.get().getTrace());
		traceInfo.setService(request.service().toString());
		traceInfo.setMethod(request.method());
		traceInfo.setReceivedTime(receivedTime);
		return traceInfo;
	}
	
}
