package com.whut.hadoop.loadbalance;

import org.apache.hadoop.io.retry.Idempotent;


/**
 * Created by niuyang on 3/15/16.
 */
public interface LoadBalanceProtocol {
	
	public static final long versionID = 101L;

    /**
     * return namenodeId
     * @param jobName
     * @return
     */
    @Idempotent
    public String doLoadBalance(String jobName);

}
