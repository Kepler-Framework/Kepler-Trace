package com.kepler.trace.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.kepler.config.PropertiesUtils;
import com.kepler.trace.Sampler;

public class DefaultSampler implements Sampler {

	private final AtomicLong counter = new AtomicLong(0);

	private static final int THRESHOLD = PropertiesUtils.get(DefaultSampler.class.getName().toLowerCase() + ".threshold", 10);
	
	@Override
	public boolean recordAndSampling() {
		if (THRESHOLD == 0) {
			return true;
		}
		return counter.incrementAndGet() % THRESHOLD == 0;
	}

}
