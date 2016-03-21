package com.whut.hadoop.monitor;

import com.whut.hadoop.heartbeat.MonitorHeartbeatProtocol;
import com.whut.hadoop.heartbeat.NameNodeHeartbeatProtocol;
import com.whut.hadoop.raft.NodeWatcher;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.HadoopIllegalArgumentException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by niuyang on 3/15/16.
 */
public class Monitor implements NameNodeHeartbeatProtocol, MonitorHeartbeatProtocol {
	
	public static final String nnHeartbeatLogPath = "/home/niuyang/Documents/nnhb.log";

	/**
	 * m1, m2
	 */
    private String monitorId;

    private String machineAddress;
    private int nnhServicePort;

    private String mhServiceAddressInActiveLB;
    private int mhServicePortInActiveLB;
    private String mhServiceAddressInStandbyLB;
    private int mhServicePortInStandbyLB;

    private HealthLevelStatistics healthLevelStatistics;

    private MonitorHeartbeatProtocol activeLBNodeProxy;

    private MonitorHeartbeatProtocol standbyLBNodeProxy;
    
    private Configuration conf;

    private Timer timer;

    private TimerTask heartbeatToLBNExecutor;
    
    private long lastNN1HeartbeatTime = 0L;
    private long lastNN2HeartbeatTime = 0L;
    private long heartbeatInterval = 0L;

    public Monitor() {
		conf = new Configuration();

		// check format of name
		// check name is no reuse

        initMonitor();

		// two lb node proxy, master and standby
		// send heartbeat at the same time
		try {
			this.activeLBNodeProxy = RPC.getProxy(MonitorHeartbeatProtocol.class, MonitorHeartbeatProtocol.versionID,
					new InetSocketAddress(mhServiceAddressInActiveLB, mhServicePortInActiveLB), conf);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			this.standbyLBNodeProxy = RPC.getProxy(MonitorHeartbeatProtocol.class, MonitorHeartbeatProtocol.versionID,
					new InetSocketAddress(mhServiceAddressInStandbyLB, mhServicePortInStandbyLB), conf);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create healthLevelStatistics object
		this.healthLevelStatistics = new HealthLevelStatistics();

		// start send heartbeat by timer
		timer = new Timer("NNHeartbeatMonitor-Timer", true);
		this.heartbeatToLBNExecutor = new HeartbeatToLBNExecutor();

    }

    private void initMonitor() {
        this.monitorId = conf.getTrimmed("monitor.current.id");
        if (!"m1".equalsIgnoreCase(this.monitorId)
                && !"m2".equalsIgnoreCase(this.monitorId)) {
            throw new IllegalArgumentException();
        }
        this.monitorId = this.monitorId.toLowerCase();
        machineAddress = conf.getTrimmed("monitor.address." + monitorId, null);
        nnhServicePort = conf.getInt("monitor.nnhs.port." + monitorId, -1);

        mhServicePortInActiveLB = conf.getInt("lb.mhs.port.active", -1);
        mhServicePortInStandbyLB = conf.getInt("lb.mhs.port.standby", -1);

        mhServiceAddressInActiveLB = conf.get("lb.address.active", null);
        mhServiceAddressInStandbyLB = conf.get("lb.address.standby", null);

        heartbeatInterval = conf.getLong("heartbeat.interval", 10000);
        // reft
        // can not double node have same name
    }

    public void run() {
        // register rpc server
        try {
			new RPC.Builder(conf)
                    .setProtocol(NameNodeHeartbeatProtocol.class)
                    .setInstance(this)
					.setBindAddress(machineAddress)
                    .setPort(nnhServicePort).build().start();
        } catch (HadoopIllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // launch timer schedule
        timer.scheduleAtFixedRate(heartbeatToLBNExecutor, 0, 5000);

        new Timer("NNHTimeOutMonitor-Timer", true).scheduleAtFixedRate(new NNHTimeOutMonitorExecutor(), heartbeatInterval / 2, heartbeatInterval);
    }

    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        monitor.run();
    }

    private class HeartbeatToLBNExecutor extends TimerTask {
        @Override
        public void run() {
            HealthLevelStatistics.StatisticsResult statisticsResult = healthLevelStatistics.statistics();
            if (StringUtils.isNotBlank(monitorId)
                    && statisticsResult != null) {
                // send heartbeat to two lb node at the same time
            	try {
            		activeLBNodeProxy.sendHeartbeat(
                            monitorId,
                            statisticsResult.getNn1HealthLevel(),
                            statisticsResult.getNn2HealthLevel());
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
                try {
                	standbyLBNodeProxy.sendHeartbeat(
                            monitorId,
                            statisticsResult.getNn1HealthLevel(),
                            statisticsResult.getNn2HealthLevel());
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        }
    }

    @Override
    public void sendHeartbeat(String nameNodeId, boolean isHealth) {
        // System.out.println("namenodeId = " + nameNodeId);
        // System.out.println("isHealth = " + isHealth);
        boolean nn1EqNN1 = "nn1".equalsIgnoreCase(nameNodeId);
        boolean nn2EqNN2 = "nn2".equalsIgnoreCase(nameNodeId);
        if (!nn1EqNN1 && !nn2EqNN2) {
        	return;
        }
        try {
            File file = new File(nnHeartbeatLogPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter out = new PrintWriter(new File(nnHeartbeatLogPath));
            String str = nameNodeId + " " + isHealth;
            out.append(str);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isHealth) {
        	if (nn1EqNN1) {
                lastNN1HeartbeatTime = System.currentTimeMillis();
            } else {
                lastNN2HeartbeatTime = System.currentTimeMillis();
            }	
        }
    }

    private class NNHTimeOutMonitorExecutor extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - lastNN1HeartbeatTime > heartbeatInterval) {
                sendHeartbeat("nn1", false);
            }
            if (now - lastNN2HeartbeatTime > heartbeatInterval) {
                sendHeartbeat("nn2", false);
                // System.out.println("==================================");
            }
        }
    }

    @Override
    public void sendHeartbeat(String monitorId, int namenode1HealthLevel, int namenode2HealthLevel) {
        return;
    }
}
