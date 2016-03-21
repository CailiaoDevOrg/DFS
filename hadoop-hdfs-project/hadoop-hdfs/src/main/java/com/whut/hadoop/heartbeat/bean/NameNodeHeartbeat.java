package com.whut.hadoop.heartbeat.bean;

/**
 * Created by niuyang on 3/15/16.
 */
public class NameNodeHeartbeat {

    private String namenodeId;

    private boolean isHealthy;

	public String getNamenodeId() {
		return namenodeId;
	}

	public void setNamenodeId(String namenodeId) {
		this.namenodeId = namenodeId;
	}

	public boolean isHealthy() {
		return isHealthy;
	}

	public void setHealthy(boolean isHealthy) {
		this.isHealthy = isHealthy;
	}
    
}
