package com.kepler.trace.collector.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.collector.TraceInfos;
import com.kepler.trace.collector.TraceTransferService;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@com.kepler.annotation.Autowired
public class TraceTransferServiceImpl implements TraceTransferService {

	@Autowired
	private MongoDatabase db;

	private static Log LOGGER = LogFactory.getLog(TraceTransferServiceImpl.class);

	static final String COLLECTION_NAME = "trace";

	private Documents documentsHolder = new Documents();

	static class Documents extends ThreadLocal<List<Document>> {

		@Override
		protected List<Document> initialValue() {
			return new ArrayList<Document>();
		}

		public void clear() {
			this.get().clear();
		}

	}

	@Override
	public void transferTraceInfos(TraceInfos traceInfos) {
		if (traceInfos == null) {
			return;
		}
		List<Document> documents = documentsHolder.get();
		try {
			documents = prepareDataForBatchInsert(traceInfos.getList(), documents);
			insertToDB(documents);
		} finally {
			documents.clear();
		}
	}

	private List<Document> prepareDataForBatchInsert(List<TraceInfo> traceInfos, List<Document> documents) {
		for (TraceInfo traceInfo : traceInfos) {
			try {
				documents.add(createDocument(traceInfo));
			} catch (JsonProcessingException e) {
				LOGGER.error("Fail deserializing document: " + traceInfo);
			}
		}
		return documents;
	}

	private void insertToDB(List<Document> documents) {
		traceCollection().insertMany(documents);
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
		document.put("startTime", traceInfo.getStartTime());
		document.put("receivedTime", traceInfo.getReceivedTime());
		List params = new ArrayList();
		for (Object param : traceInfo.getRequest()) {
			params.add(param == null ? null :param.getClass().cast(param));
		}
		document.put("request", params);
		document.put("response", traceInfo.getResponse() == null ? null : traceInfo.getResponse().getClass().cast(traceInfo.getResponse()));
		document.put("throwable", traceInfo.getThrowable());
		return document;
	}

	private MongoCollection<Document> traceCollection() {
		return this.db.getCollection(COLLECTION_NAME).withWriteConcern(WriteConcern.MAJORITY);
	}

}
