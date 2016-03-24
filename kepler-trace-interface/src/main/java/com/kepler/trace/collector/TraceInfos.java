package com.kepler.trace.collector;

import java.io.Serializable;
import java.util.List;

public class TraceInfos implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<TraceInfo> list;

	public TraceInfos() {
		
	} 
	
	public TraceInfos(List<TraceInfo> list) {
		this.list = list;
	}

	public List<TraceInfo> getList() {
		return list;
	}

	public void setList(List<TraceInfo> list) {
		this.list = list;
	}
	
}