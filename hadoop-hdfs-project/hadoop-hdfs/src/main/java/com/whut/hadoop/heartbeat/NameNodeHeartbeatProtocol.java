package com.whut.hadoop.heartbeat;

import org.apache.hadoop.io.retry.Idempotent;

/**
 * NameNode
 * @author niuyang
 *
 */
public interface NameNodeHeartbeatProtocol {
	
	public static final long versionID = 101L;

	/**
	 * @param nameNodeId
	 * @param isHealth
     */
	@Idempotent
	public void sendHeartbeat(String nameNodeId, boolean isHealth);
}
