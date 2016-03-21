package com.whut.hadoop.nnplugin;

import com.whut.hadoop.heartbeat.NameNodeHeartbeatProtocol;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.util.Shell.ShellCommandExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class NNHealthScriptChecker {

    private NameNodeHeartbeatProtocol[] monitorProxyArr;

    private boolean isHealthy;

    private String nodeHealthScript;

    private ShellCommandExecutor shexec = null;

    private long scriptTimeout;

    private TimerTask timerTask;

    private Timer timer;

    private String namenodeId;

    private static final String ERROR_PATTERN = "ERROR";

    private enum HealthCheckerExitStatus {
        SUCCESS,
        TIMED_OUT,
        FAILED_WITH_EXIT_CODE,
        FAILED_WITH_EXCEPTION,
        FAILED
    }

    public static boolean shouldRun(Configuration conf) {
        String nodeHealthScript = conf.get("nn.healthy.script.url");
        if (StringUtils.isBlank(nodeHealthScript)) {
            return false;
        }
        File f = new File(nodeHealthScript);
        return f.exists() && FileUtil.canExecute(f);
    }

    public NNHealthScriptChecker(Configuration conf, NameNodeHeartbeatProtocol[] monitorProxyArr, String namenodeId) {
        if (conf != null || monitorProxyArr != null || StringUtils.isBlank(namenodeId)) {
            init(conf, monitorProxyArr, namenodeId);
            run();
        }
    }

    private void init(Configuration conf, NameNodeHeartbeatProtocol[] monitorProxyArr, String namenodeId) {
        this.monitorProxyArr = monitorProxyArr;
        this.namenodeId = namenodeId;
        nodeHealthScript = conf.get("nn.healthy.script.url");
        isHealthy = true;
        scriptTimeout = 6000;
    }

    public void run() {
        timerTask = new NNHealthMonitorExecutor();
        timer = new Timer("NNHealthMonitor-Timer", true);
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    private class NNHealthMonitorExecutor extends TimerTask {

        public NNHealthMonitorExecutor() {
            ArrayList<String> execScript = new ArrayList<>();
            execScript.add(nodeHealthScript);
            shexec = new ShellCommandExecutor(execScript
                    .toArray(new String[execScript.size()]), null, null, scriptTimeout);
        }

        @Override
        public void run() {
            if (monitorProxyArr == null) {
                throw new IllegalArgumentException();
            }
            HealthCheckerExitStatus status = HealthCheckerExitStatus.SUCCESS;
            try {
                shexec.execute();
            } catch (Shell.ExitCodeException e) {
                status = HealthCheckerExitStatus.FAILED_WITH_EXIT_CODE;
                if (Shell.WINDOWS && shexec.isTimedOut()) {
                    status = HealthCheckerExitStatus.TIMED_OUT;
                }
            } catch (Exception e) {
                if (!shexec.isTimedOut()) {
                    status = HealthCheckerExitStatus.FAILED_WITH_EXCEPTION;
                } else {
                    status = HealthCheckerExitStatus.TIMED_OUT;
                }
            } finally {
                if (status == HealthCheckerExitStatus.SUCCESS) {
                    if (hasErrors(shexec.getOutput())) {
                        status = HealthCheckerExitStatus.FAILED;
                    }
                }
            }
            isHealthy = (status == HealthCheckerExitStatus.SUCCESS);
            for (NameNodeHeartbeatProtocol monitorProxy : monitorProxyArr) {
                if (monitorProxy != null) {
                	try {
                		monitorProxy.sendHeartbeat(namenodeId, isHealthy);
                		// System.out.println("NNHealthScriptChecker, namenodeId = " + namenodeId 
                			//	+ "; isHealthy = " + isHealthy);
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                }
            }
        }

        private boolean hasErrors(String output) {
            String[] splits = output.split("\n");
            for (String split : splits) {
                if (split.startsWith(ERROR_PATTERN)) {
                    return true;
                }
            }
            return false;
        }
    }
}
