package com.whut.hadoop.nnplugin;

import com.whut.hadoop.heartbeat.NameNodeHeartbeatProtocol;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by niuyang on 3/16/16.
 */
public class NNHealthChecker {

    private NameNodeHeartbeatProtocol[] monitorProxyArr;

    private NNHealthScriptChecker nnHealthScriptChecker;

    private String namenodeId;

    private boolean isHealthy;

    private Timer timer;

    public NNHealthChecker(Configuration conf, NameNodeHeartbeatProtocol[] monitorProxyArr, String namenodeId) {
        boolean shouldRun = conf.getBoolean("nn.healthy.run.script", false);
        if (monitorProxyArr != null) {
            isHealthy = true;
            this.monitorProxyArr = monitorProxyArr;
            if (conf != null && StringUtils.isNotBlank(namenodeId)) {
                this.namenodeId = namenodeId;
                if (shouldRun = NNHealthScriptChecker.shouldRun(conf)) {
                    nnHealthScriptChecker = new NNHealthScriptChecker(conf, monitorProxyArr, namenodeId);
                }
            }
            if (!shouldRun) {
                timer = new Timer("NNHealthMonitor-Timer", true);
                timer.scheduleAtFixedRate(new DefaultCheckStrategyExecutor(), 0, 1000);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean isHealthy() {
        boolean scriptHealthStatus = (nnHealthScriptChecker == null) ? true
                : nnHealthScriptChecker.isHealthy();
        return scriptHealthStatus && isHealthy;
    }

    private class DefaultCheckStrategyExecutor extends TimerTask {
        @Override
        public void run() {
            for (NameNodeHeartbeatProtocol monitorProxy : monitorProxyArr) {
                if (monitorProxy == null) {
                    throw new IllegalArgumentException();
                }
                monitorProxy.sendHeartbeat(namenodeId, isHealthy());
            }
        }
    }

}
