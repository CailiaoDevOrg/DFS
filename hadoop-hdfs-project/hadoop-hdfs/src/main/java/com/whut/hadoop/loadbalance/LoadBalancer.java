package com.whut.hadoop.loadbalance;

import com.whut.hadoop.heartbeat.MonitorHeartbeatProtocol;
import com.whut.hadoop.loadbalance.HealthStatusTable.HealthLevel;
import com.whut.hadoop.raft.NodeWatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.HadoopIllegalArgumentException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by niuyang on 3/15/16.
 */
public class LoadBalancer implements LoadBalanceProtocol, MonitorHeartbeatProtocol {

    private enum Status {
        ACTIVE,
        STANDBY
    }

    private String machineAddress;
    private int lbServicePort;
    private int mhServicePort;

    private Status status;
    private boolean isUseDefaultStrategy;
    private CustomLBStrategy customLBStrategy;

    private long heartbeatInterval = 0L;

    private HealthStatusTable healthStatusTable;
    private long lastHeartbeatTimeByM1 = 0L;
    private long lastHeartbeatTimeByM2 = 0L;

    private Configuration conf;

    public LoadBalancer() {
    	conf = new Configuration();

        String role = conf.getTrimmed("lb.role", "").toLowerCase();
        if (!"active".equalsIgnoreCase(role) && !"standby".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException();
        }
        status = "active".equals(role) ? Status.ACTIVE : Status.STANDBY;
        // check syn
        // status

        isUseDefaultStrategy = conf.getBoolean("lb.isUseDefaultStrategy", true);

        machineAddress = conf.get("lb.address." + role, null);
        lbServicePort = conf.getInt("lb.lbs.port." + role, -1);
        mhServicePort = conf.getInt("lb.mhs.port." + role, -1);

        customLBStrategy = new CustomLBStrategy();
        customLBStrategy.setStrategyList(conf.getTrimmed("lb.customLBStrategy"));

        heartbeatInterval = conf.getLong("heartbeat.interval", 10000);

        healthStatusTable = new HealthStatusTable();

    }

    public static void main(String[] args) {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.run();
    }

    // register server
    public void run() {
        try {
			new RPC.Builder(conf)
                    .setProtocol(LoadBalanceProtocol.class)
                    .setInstance(this)
                    .setBindAddress(machineAddress)
					.setPort(lbServicePort).build().start();
        } catch (HadoopIllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
			new RPC.Builder(conf)
                    .setProtocol(MonitorHeartbeatProtocol.class)
                    .setInstance(this)
                    .setBindAddress(machineAddress)
					.setPort(mhServicePort).build().start();
		} catch (HadoopIllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        new Timer("MHBTimeOutMonitor-Timer", true).scheduleAtFixedRate(new MHBTimeOutMonitorExecutor(), heartbeatInterval / 2, heartbeatInterval);
    }

    @Override
    public String doLoadBalance(String jobName) {
        if (!baseCheck()) {
            return "ERROR";
        }
        if (isUseDefaultStrategy) {
            return doLoadBalancebyDefault();
        } else {
            return doLoadBalancebyCustom(jobName);
        }
    }

    private String doLoadBalancebyDefault() {
        if (healthStatusTable.getNamenode1HealthLevel() == HealthLevel.NA) {
            return "nn2";
        }
        if (healthStatusTable.getNamenode2HealthLevel() == HealthLevel.NA) {
            return "nn1";
        }
        int namenode1Level = healthStatusTable.getNamenode1HealthLevel().value;
        int namenode2Level = healthStatusTable.getNamenode2HealthLevel().value;
        int levelSum = namenode1Level + namenode2Level;
        if (levelSum <= 0 || namenode1Level <= 0 || namenode2Level <= 0) {
            throw new IllegalArgumentException();
        }
        return (int) (Math.random() * levelSum) < namenode1Level ? "nn1" : "nn2";
    }

    private String doLoadBalancebyCustom(String jobName) {
    	if (StringUtils.isBlank(jobName) || customLBStrategy == null || CollectionUtils.isEmpty(customLBStrategy.getStrategyList())) {
    		return doLoadBalancebyDefault();
    	}
        for (CustomLBStrategy.Strategy strategy : customLBStrategy.getStrategyList()) {
            if (strategy != null && jobName.equalsIgnoreCase(strategy.getJobName())) {
                return strategy.getNamenodeId();
            }
        }
        return doLoadBalancebyDefault();
    }

    private boolean baseCheck() {
        if (status == Status.STANDBY) {
            return false;
        }
        if (healthStatusTable == null) {
            throw new IllegalArgumentException();
        }
        if (!(healthStatusTable.isMonitor1isAlive() || healthStatusTable.isMonitor2isAlive())) {
            return false;
        }
        if (healthStatusTable.getNamenode1HealthLevel() == null
                && healthStatusTable.getNamenode2HealthLevel() == null) {
            return false;
        }
        if (healthStatusTable.getNamenode1HealthLevel() == HealthLevel.NA
                && healthStatusTable.getNamenode2HealthLevel() == HealthLevel.NA) {
            return false;
        }
        return true;
    }

    @Override
    public void sendHeartbeat(String monitorId, int namenode1HealthLevel, int namenode2HealthLevel) {
        // System.out.println("LoadBalancer ----> monitorId = " + monitorId + "; nn1hLevel = " + namenode1HealthLevel + "; nn2hlevel =" + namenode2HealthLevel);
        if (healthStatusTable == null) {
            healthStatusTable = new HealthStatusTable();
        }
        boolean flag = false;
        if ("m1".equalsIgnoreCase(monitorId)) {
            healthStatusTable.setMonitor1isAlive(true);
            lastHeartbeatTimeByM1 = System.currentTimeMillis();
            // System.out.println("M1 success");
            flag = true;
        } else if ("m2".equalsIgnoreCase(monitorId)) {
            healthStatusTable.setMonitor2isAlive(true);
            lastHeartbeatTimeByM2 = System.currentTimeMillis();
            // System.out.println("M2 success");
            flag = true;
        }
        if (flag) {
            healthStatusTable.setNamenode1HealthLevel(namenode1HealthLevel);
            healthStatusTable.setNamenode2HealthLevel(namenode2HealthLevel);
        }
    }

    private class MHBTimeOutMonitorExecutor extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - lastHeartbeatTimeByM1 > heartbeatInterval) {
                healthStatusTable.setMonitor1isAlive(false);
                //System.out.println("M1 error");
            }
            if (now - lastHeartbeatTimeByM2 > heartbeatInterval) {
                healthStatusTable.setMonitor2isAlive(false);
                //System.out.println("M2 error");
            }
        }
    }
}