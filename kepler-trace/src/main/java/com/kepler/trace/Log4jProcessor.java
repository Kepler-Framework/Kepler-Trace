package com.kepler.trace;

import org.apache.log4j.MDC;

import com.kepler.config.PropertiesUtils;
import com.kepler.header.Headers;
import com.kepler.protocol.Request;
import com.kepler.protocol.RequestProcessor;

public class Log4jProcessor implements RequestProcessor {
	
	private static final int SORT = PropertiesUtils.get(Log4jProcessor.class.getName().toLowerCase() + ".sort", Integer.MAX_VALUE);

	private final boolean log4jSupported;
	
	public Log4jProcessor() {
		try {
			Class.forName("org.apache.log4j.MDC");
		} catch (Exception e) {
			log4jSupported = false;
			return;
		}
		log4jSupported = true;
	}
	
	@Override
	public Request process(Request request) {
		if (log4jSupported) {
			Headers headers = request.headers();
			if (headers != null) {
				String trace = headers.get(Trace.TRACE);
				MDC.put(Trace.TRACE, trace);
			}
		}
		return request;
	}

	@Override
	public int sort() {
		return SORT;
	}

}
