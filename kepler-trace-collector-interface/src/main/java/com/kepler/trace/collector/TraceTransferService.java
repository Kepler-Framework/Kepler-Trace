package com.kepler.trace.collector;

import com.kepler.annotation.Async;
import com.kepler.annotation.Service;

@Service(version="0.0.1")
public interface TraceTransferService {

	@Async
	void transferTraceInfos(TraceInfos traceInfos);
	
}
