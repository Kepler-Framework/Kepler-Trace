package com.kepler.trace.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kepler.trace.collector.TraceInfo;
import com.kepler.trace.filequeue.ByteBufferInputStream;
import com.kepler.trace.filequeue.DecodeException;
import com.kepler.trace.filequeue.MessageReader;

final class TraceInfoByteReader implements MessageReader<TraceInfo> {

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Override
	public TraceInfo readMessage(ByteBuffer buffer) throws DecodeException {
		try {
			return objectMapper.reader(TraceInfo.class).readValue(new ByteBufferInputStream(buffer));
		} catch (JsonProcessingException e) {
			throw new DecodeException(e);
		} catch (IOException e) {
			throw new DecodeException(e);
		}
	}

}