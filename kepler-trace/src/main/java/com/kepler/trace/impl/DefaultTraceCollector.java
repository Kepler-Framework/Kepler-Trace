package com.kepler.trace.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepler.annotation.Config;
import com.kepler.config.PropertiesUtils;
import com.kepler.trace.TraceCollector;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.collector.TraceInfos;
import com.kepler.trace.collector.TraceTransferService;
import com.kepler.trace.filequeue.DecodeException;
import com.kepler.trace.filequeue.FileQueue;

public class DefaultTraceCollector implements TraceCollector {

	private static final Log LOGGER = LogFactory.getLog(DefaultTraceCollector.class);

	private int capacity = PropertiesUtils.get(TraceCollector.class.getName().toLowerCase() + ".capacity", 100);

	private final String queueDir = PropertiesUtils.get("com.kepler.trace.filequeue.dir", "trace");

	private final LinkedBlockingQueue<TraceInfo> queue = new LinkedBlockingQueue<>(capacity);

	private final TraceTransferService traceTransferService;

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final TraceInfoByteReader reader = new TraceInfoByteReader();

	private final FileQueue fileQueue = new FileQueue(queueDir);
	
	private volatile boolean shutdown = false;

	private Thread transferTaskService = new Thread(new TransferTask());
	
	private Thread recoverService = new Thread(new RecoverService());

	public DefaultTraceCollector(TraceTransferService traceTransferService) {
		this.traceTransferService = traceTransferService;
	}

	public void init() throws Exception {
		this.transferTaskService.start();
		this.recoverService.start();
		this.fileQueue.load();
	}

	public void destroy() throws Exception {
		this.shutdown = true;
		this.transferTaskService.interrupt();
		this.transferTaskService.join();
		this.recoverService.interrupt();
		this.recoverService.join();
		this.fileQueue.destroy();
	}

	@Config(value = "com.kepler.trace.tracecollector.trace_timeout")
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	private final class TransferTask implements Runnable {

		ArrayList<TraceInfo> transferingTraceInfos = new ArrayList<>(capacity);

		@Override
		public void run() {
			while (!shutdown) {
				try {
					transferingTraceInfos.add(queue.take());
					queue.drainTo(transferingTraceInfos);
					LOGGER.debug("Transfering " + transferingTraceInfos.size() + " traceInfo.");
					traceTransferService.transferTraceInfos(new TraceInfos(transferingTraceInfos));
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					logTraceInfo();
					LOGGER.error(e.getMessage(), e);
				} finally {
					transferingTraceInfos.clear();
				}
			}
			LOGGER.info("Shutting down transfering trace task.");
		}

		private void logTraceInfo() {
			for (TraceInfo traceInfo : transferingTraceInfos) {
				LOGGER.error("Error transfering service " + traceInfo.getService() + " method " + traceInfo.getMethod());
			}
		}

	}

	@Override
	public void put(TraceInfo traceInfo) {
		try {
			boolean inserted = queue.offer(traceInfo);
			if (!inserted) {
				LOGGER.debug("Failed putting to the memory queue. Now try to put to the file queue");
				fileQueue.offer(objectMapper.writeValueAsBytes(traceInfo));
			}
		} catch (Exception e) {
			// Ignore. Just record for god's sake
			LOGGER.info(e.getMessage(), e);
		}
	}

	class RecoverService implements Runnable {

		@Override
		public void run() {
			while (!shutdown) {
				try {
					TraceInfo traceInfo = fileQueue.poll(reader);
					DefaultTraceCollector.this.traceTransferService.transferTraceInfos(new TraceInfos(Arrays.asList(traceInfo)));
				} catch (DecodeException e) {
					LOGGER.error(e.getMessage(), e);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

	}

}
