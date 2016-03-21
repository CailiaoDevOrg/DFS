package com.whut.hadoop.test;

public interface HeartBeatService {
	public static final long versionID = 10100L;  
	
	public void sendHeartBeat(boolean isHealth);

}
