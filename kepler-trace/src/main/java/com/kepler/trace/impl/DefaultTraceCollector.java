package com.kepler.trace.impl;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kepler.annotation.Config;
import com.kepler.config.PropertiesUtils;
import com.kepler.trace.TraceCollector;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.collector.TraceInfos;
import com.kepler.trace.collector.TraceTransferService;

public class DefaultTraceCollector implements TraceCollector {

	private static final Log LOGGER = LogFactory.getLog(DefaultTraceCollector.class);

	private int capacity = PropertiesUtils.get(TraceCollector.class.getName().toLowerCase() + ".capacity", 100);
	
	private static final long TRACE_TIMEOUT = PropertiesUtils.get(TraceCollector.class.getName().toLowerCase() + ".trace_timeout", 200);
	
	private final LinkedBlockingQueue<TraceInfo> queue = new LinkedBlockingQueue<>(capacity);

	private final TraceTransferService traceTransferService;

	private final ExecutorService transferTaskExecutor = Executors.newSingleThreadExecutor();

	public DefaultTraceCollector(TraceTransferService traceTransferService) {
		this.traceTransferService = traceTransferService;
	}

	public void init() {
		this.transferTaskExecutor.execute(new TransferTask());
	}
	
	@Config(value="com.kepler.trace.tracecollector.trace_timeout")
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	private final class TransferTask implements Runnable {

		ArrayList<TraceInfo> transferingTraceInfos = new ArrayList<>(capacity);

		@Override
		public void run() {
			while (true) {
				try {
					transferingTraceInfos.add(queue.take());
					queue.drainTo(transferingTraceInfos);
					LOGGER.info("Transfering "  + transferingTraceInfos.size() + " traceInfo.");
					traceTransferService.transferTraceInfos(new TraceInfos(transferingTraceInfos));
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				} finally {
					transferingTraceInfos.clear();
				}
			}
		}

	}

	@Override
	public void put(TraceInfo traceInfo) {
		try {
			boolean inserted = queue.offer(traceInfo, TRACE_TIMEOUT, TimeUnit.MILLISECONDS);
			if (!inserted) {
				LOGGER.warn("Input traceInfo timeout after " + TRACE_TIMEOUT + " milliseconds");
			}
		} catch (InterruptedException e) {
//			Ignore. Just record for god's sake
			LOGGER.info(e.getMessage(), e);
		}
	}


}
