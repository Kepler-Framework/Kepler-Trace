package com.kepler.trace.collector.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.xerial.snappy.Snappy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.collector.TraceInfos;
import com.kepler.trace.collector.TraceTransferService;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;

@com.kepler.annotation.Autowired
public class TraceTransferServiceImpl implements TraceTransferService {

	@Autowired
	private MongoDatabase db;

	private static Log LOGGER = LogFactory.getLog(TraceTransferServiceImpl.class);

	static final String COLLECTION_NAME = "trace";

	private Documents documentsHolder = new Documents();
	
	private ObjectMapper om = new ObjectMapper();
	
	private InsertManyOptions options = new InsertManyOptions().ordered(false);

	private AtomicLong counter = new AtomicLong(0);
	
	private final int SAMPLE_RATE = Integer.parseInt(System.getProperty("sampleRate", "1000"));

	private MongoCollection<Document> dbCollection;
	
	static class Documents extends ThreadLocal<List<Document>> {

		@Override
		protected List<Document> initialValue() {
			return new ArrayList<Document>();
		}

		public void clear() {
			this.get().clear();
		}

	}
	
	public void init() {
		dbCollection = this.db.getCollection(COLLECTION_NAME).withWriteConcern(WriteConcern.W1);
	}
	
	@Override
	public void transferTraceInfos(TraceInfos traceInfos) {
		if (traceInfos == null) {
			return;
		}
		boolean isSample = (counter.getAndIncrement() % SAMPLE_RATE ) == 0;
		long startTime = 0, endTime = 0;
		List<Document> documents = documentsHolder.get();
		try {
			documents = prepareDataForBatchInsert(traceInfos.getList(), documents);
			// 统计时长
			startTime = System.currentTimeMillis();
			insertToDB(documents);
			endTime = System.currentTimeMillis();
			
			if (isSample || (endTime - startTime) > 50) {
				StringBuilder msg = new StringBuilder("Cost " + (endTime - startTime) + "ms.");
				msg.append('[');
				for (TraceInfo trace : traceInfos.getList()) {
					msg.append("{trace: ").append(trace.getTrace())
						.append(", span: ").append(trace.getSpan()).append("},");
				}
				msg.append(']');
				LOGGER.info(msg.toString());
			}
		} finally {
			documents.clear();
		}
	}

	private List<Document> prepareDataForBatchInsert(List<TraceInfo> traceInfos, List<Document> documents) {
		for (TraceInfo traceInfo : traceInfos) {
			try {
				if (!StringUtils.isEmpty(traceInfo.getTrace())) {
					documents.add(createDocument(traceInfo));
				}
			} catch (JsonProcessingException e) {
				LOGGER.error("Fail deserializing document: " + traceInfo);
			}
		}
		return documents;
	}

	private void insertToDB(List<Document> documents) {
		dbCollection.insertMany(documents, options);
	}

	private Document createDocument(TraceInfo traceInfo) throws JsonProcessingException {
		Document document = convertTraceInfo(traceInfo);
		document.put("_id", traceInfo.getSpan());
		return document;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Document convertTraceInfo(TraceInfo traceInfo) throws JsonProcessingException {
		Document document = new Document();
		document.put("trace", traceInfo.getTrace());
		document.put("span", traceInfo.getSpan());
		document.put("parentSpan", traceInfo.getParentSpan());
		document.put("service", traceInfo.getService());
		document.put("method", traceInfo.getMethod());
		document.put("local", traceInfo.getLocal());
		document.put("remote", traceInfo.getRemote());
		document.put("transferTime", traceInfo.getTransferTime());
		document.put("waiting", traceInfo.getWaiting());
		document.put("elapse", traceInfo.getElapse());
		document.put("startTime", new Date(traceInfo.getStartTime()));
		document.put("receivedTime", traceInfo.getReceivedTime());
		List params = new ArrayList();
		for (Object param : traceInfo.getRequest()) {
			params.add(param == null ? null :param.getClass().cast(param));
		}
		try {
			document.put("request", params == null ? null : Snappy.compress(om.writerWithDefaultPrettyPrinter().writeValueAsString(params)));
			document.put("response", traceInfo.getResponse() == null ? null : Snappy.compress(om.writerWithDefaultPrettyPrinter().writeValueAsString(traceInfo.getResponse())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		document.put("throwable", traceInfo.getThrowable());
		document.put("useSnappy", true);
		return document;
	}

}
