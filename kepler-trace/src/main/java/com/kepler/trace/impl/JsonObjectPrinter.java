package com.kepler.trace.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kepler.config.PropertiesUtils;
import com.kepler.trace.ObjectPrinter;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class JsonObjectPrinter implements ObjectPrinter {

	private final static Log LOGGER = LogFactory.getLog(JsonObjectPrinter.class);
	
	private final static boolean prettyPrint = PropertiesUtils.get(JsonObjectPrinter.class.getSimpleName() + ".prettyPrint", false);

	private final ConcurrentHashMap<Class<?>, ObjectWriter> writerCache = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper = new ObjectMapper();

	public JsonObjectPrinter() {
		this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		this.objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
		this.objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true);
		this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint);
	}

	@Override
	public String print(Object object) {
		if (null == object) {
			return null;
		}
		try {
			ObjectWriter objectWriter = this.getObjectWriterOfClass(object);
			return objectWriter.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			JsonObjectPrinter.LOGGER.error(e.getMessage(), e);
		}
		return object.toString();
	}
	
	private ObjectWriter getObjectWriterOfClass(Object object) {
		ObjectWriter objectWriter = this.writerCache.get(object.getClass());
		if (null == objectWriter) {
			objectWriter = this.objectMapper.writer().withType(object.getClass());
			this.writerCache.putIfAbsent(objectWriter.getClass(), objectWriter);
		}
		return objectWriter;
	}
}
