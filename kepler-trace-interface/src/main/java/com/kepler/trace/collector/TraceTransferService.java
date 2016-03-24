package com.kepler.trace.collector;

import com.kepler.annotation.Service;

@Service(version="0.0.1")
public interface TraceTransferService {

	void transferTraceInfos(TraceInfos traceInfos);
	
}
