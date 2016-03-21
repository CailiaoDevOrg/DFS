package com.whut.hadoop.test;

import com.whut.hadoop.heartbeat.NameNodeHeartbeatProtocol;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RPC.Server;

public class MonitorServer implements NameNodeHeartbeatProtocol {
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Server server = new RPC.Builder(conf)
				.setProtocol(NameNodeHeartbeatProtocol.class)
				.setInstance(new MonitorServer())
				.setBindAddress("127.0.0.1")
				.setPort(8000)
				.build();  
        server.start();
	}

	@Override
	public void sendHeartbeat(String nameNodeId, boolean isHealth) {
		System.out.println("namenodeId = " + nameNodeId);
		System.out.println("isHealth = " + isHealth);
	}
}
