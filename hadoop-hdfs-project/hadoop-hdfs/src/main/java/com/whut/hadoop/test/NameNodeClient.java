package com.whut.hadoop.test;

import java.net.InetSocketAddress;

import com.whut.hadoop.heartbeat.NameNodeHeartbeatProtocol;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

public class NameNodeClient implements NameNodeHeartbeatProtocol {

	public static void main(String []args) throws Exception {
		NameNodeHeartbeatProtocol proxy =
				RPC.getProxy(NameNodeHeartbeatProtocol.class,
						101L,
						new InetSocketAddress("127.0.0.1", 8000),
						new Configuration());
		int i = 5;
		while (i > 0) {
			Thread.sleep(3000);
	        proxy.sendHeartbeat("hello namenode", true);
	        i--;
		}
        RPC.stopProxy(proxy);  
	}

	@Override
	public void sendHeartbeat(String namenodeId, boolean isHealth) {
		return;
	}
}
