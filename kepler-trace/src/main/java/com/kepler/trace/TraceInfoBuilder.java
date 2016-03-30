package com.kepler.trace;

import java.util.StringTokenizer;

import com.kepler.config.PropertiesUtils;
import com.kepler.org.apache.commons.lang.SystemUtils;
import com.kepler.org.apache.commons.lang.exception.ExceptionUtils;
import com.kepler.protocol.Request;
import com.kepler.protocol.Response;
import com.kepler.trace.collector.TraceInfo;

public class TraceInfoBuilder {

	private static final int MAX_STACKTRACE_LINE = PropertiesUtils.get(TraceInfoBuilder.class.getSimpleName() + ".max_stacktrace_line", 5);

	public static TraceInfo build(Request request, Response response, String local, String remote, long waiting,
			long elapse, long receivedTime) {
		TraceInfo traceInfo = new TraceInfo();
		traceInfo.setElapse(elapse);
		traceInfo.setWaiting(waiting);
		traceInfo.setLocal(local);
		traceInfo.setRemote(remote);
		traceInfo.setRequest(request.args());
		traceInfo.setResponse(response.valid() ? response.response() : null);
		traceInfo.setThrowable(!response.valid() ? getException(response.throwable(), MAX_STACKTRACE_LINE) : null);
		traceInfo.setStartTime(SpanContext.get().getStartTime());
		traceInfo.setParentSpan(SpanContext.get().getParentSpan());
		traceInfo.setSpan(SpanContext.get().getSpan());
		traceInfo.setTrace(SpanContext.get().getTrace());
		traceInfo.setService(request.service().toString());
		traceInfo.setMethod(request.method());
		traceInfo.setReceivedTime(receivedTime);

		return traceInfo;
	}

	static String getException(Throwable throwable, int maxLine) {
		if (throwable == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		String stackTrace = ExceptionUtils.getStackTrace(throwable);
		String linebreak = SystemUtils.LINE_SEPARATOR;
		StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
		int lineno = 0;
		while (frames.hasMoreTokens() && lineno < maxLine) {
			sb.append(frames.nextToken()).append('\n');
			lineno += 1;
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

}
