package com.kepler.trace;

import com.kepler.serial.SerialID;
import com.kepler.trace.collector.TraceTransferService;

public class ConfigRewriter {

	public ConfigRewriter() {
		System.setProperty(TraceTransferService.class.getName().toLowerCase() + "." + SerialID.class.getName().toLowerCase() + ".serial", "snappy");
	}

}
