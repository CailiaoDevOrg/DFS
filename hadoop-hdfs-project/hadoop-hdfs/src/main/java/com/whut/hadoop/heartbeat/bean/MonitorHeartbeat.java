package com.whut.hadoop.heartbeat.bean;

/**
 * Created by niuyang on 3/15/16.
 */
public class MonitorHeartbeat {

    private String monitorId;

	public String getMonitorId() {
		return monitorId;
	}

	public void setMonitorId(String monitorId) {
		this.monitorId = monitorId;
	}

	@Override
	public String toString() {
		return "MonitorHeartbeat [monitorId=" + monitorId + "]";
	}

}
