package com.kepler.trace.filequeue;

import java.nio.ByteBuffer;

public interface MessageReader<T> {

	T readMessage(ByteBuffer buffer) throws DecodeException;
	
}
