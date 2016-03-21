package com.whut.hadoop.heartbeat;

import org.apache.hadoop.io.retry.Idempotent;

/**
 * @author niuyang
 */
public interface MonitorHeartbeatProtocol {
	
	public static final long versionID = 101L;

	/**
	 * @param monitorId
	 * @param namenode1HealthLevel
	 * @param namenode2HealthLevel
     */
	@Idempotent
	public void sendHeartbeat(
			String monitorId,
			int namenode1HealthLevel,
			int namenode2HealthLevel);

}
