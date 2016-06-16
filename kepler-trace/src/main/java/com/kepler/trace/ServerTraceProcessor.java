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
			backupHeader(headers);
			headers.put(Trace.TRACE_COVER, headers.get(Trace.TRACE));
			headers.put(Trace.SPAN_PARENT, headers.get(Trace.SPAN));
		}
		return request;
	}

	private void backupHeader(Headers headers) {
		headers.put(Trace.TRACE + "_orig",  headers.get(Trace.TRACE));
		headers.put(Trace.SPAN + "_orig",  headers.get(Trace.SPAN));
		headers.put(Trace.SPAN_PARENT + "_orig", headers.get(Trace.SPAN_PARENT));
		headers.put(Trace.START_TIME + "_orig", StringUtils.isEmpty(headers.get(Trace.START_TIME)) ? "0" : headers.get(Trace.START_TIME));
	}

	@Override
	public int sort() {
		return SORT;
	}

}
