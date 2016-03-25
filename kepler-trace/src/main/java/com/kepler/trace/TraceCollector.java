package com.kepler.trace;

import com.kepler.trace.collector.TraceInfo;

public interface TraceCollector {

	void put(TraceInfo traceInfo);

}