package com.kepler.trace;

import org.springframework.util.StringUtils;

import com.kepler.config.PropertiesUtils;
import com.kepler.header.Headers;
import com.kepler.protocol.Request;
import com.kepler.protocol.RequestProcessor;
import com.kepler.trace.Trace;

public class ServerTraceProcessor implements RequestProcessor {

	private static final int SORT = PropertiesUtils.get(ServerTraceProcessor.class.getName().toLowerCase() + ".sort", Integer.MAX_VALUE);
	
	public ServerTraceProcessor() {
		
	}
	
	@Override
	public Request process(Request request) {
		Headers headers = request.headers();
		if (headers != null) {
			SpanContext.set(newSpan(headers));
			headers.put(Trace.TRACE_TO_COVER, headers.get(Trace.TRACE));
			headers.put(Trace.PARENT_SPAN, headers.get(Trace.SPAN));
		} else {
			SpanContext.set(new Span());
		}
		return request;
	}

	private Span newSpan(Headers headers) {
		Span span = new Span();
		span.setTrace(headers.get(Trace.TRACE));
		span.setSpan(headers.get(Trace.SPAN));
		span.setParentSpan(headers.get(Trace.PARENT_SPAN));
		span.setStartTime(StringUtils.isEmpty(headers.get(Trace.START_TIME)) ? 0 : Long.parseLong(headers.get(Trace.START_TIME)));
		return span;
	}

	@Override
	public int sort() {
		return SORT;
	}

}
