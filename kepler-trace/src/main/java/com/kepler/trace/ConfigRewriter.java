package com.kepler.trace;

import java.util.Map;

import com.kepler.config.PropertiesUtils;
import com.kepler.serial.SerialID;
import com.kepler.trace.collector.TraceTransferService;

public class ConfigRewriter {

	public ConfigRewriter() {
		Map<String, String> properties = PropertiesUtils.memory();
		properties.put(TraceTransferService.class.getName().toLowerCase() + "." + SerialID.class.getName().toLowerCase() + ".serial", "snappy");
		PropertiesUtils.properties(properties);
	}

}
