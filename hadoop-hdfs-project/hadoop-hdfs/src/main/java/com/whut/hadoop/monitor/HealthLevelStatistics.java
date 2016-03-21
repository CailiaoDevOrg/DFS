package com.whut.hadoop.monitor;


import com.whut.hadoop.heartbeat.bean.NameNodeHeartbeat;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class HealthLevelStatistics {

	private static final int lineNum = 240;

	private List<NameNodeHeartbeat> getNameNodeHeartbeatList() {
		Scanner scanner = null;
		List<NameNodeHeartbeat> nameNodeHeartbeats = new ArrayList<>();
		try {
			scanner = new Scanner(new FileInputStream(new File(Monitor.nnHeartbeatLogPath)));
			scanner.useDelimiter("\n");
			int i = 0;
			String str;
			while (i <= lineNum && scanner.hasNext()) {
				str = scanner.next();
				if (StringUtils.isBlank(str)) {
					continue;
				}
				String[] nnHeartbeatInfo = str.trim().split(" ");
				if (nnHeartbeatInfo.length != 2 ||
						(!"nn1".equalsIgnoreCase(nnHeartbeatInfo[0]) && !"nn2".equalsIgnoreCase(nnHeartbeatInfo[0]))
						|| (!"true".equalsIgnoreCase(nnHeartbeatInfo[1]) && !"false".equalsIgnoreCase(nnHeartbeatInfo[1]))) {
					continue;
				}
				NameNodeHeartbeat nameNodeHeartbeat = new NameNodeHeartbeat();
				nameNodeHeartbeat.setNamenodeId(nnHeartbeatInfo[0]);
				nameNodeHeartbeat.setHealthy(BooleanUtils.toBoolean(nnHeartbeatInfo[1]));
				nameNodeHeartbeats.add(nameNodeHeartbeat);
				i++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return Collections.emptyList();
		} finally {
			if (scanner != null) {
				scanner.close();	
			}
		}
		return nameNodeHeartbeats;
	}

	public class StatisticsResult {
		private int nn1HealthLevel;
		private int nn2HealthLevel;

		public StatisticsResult() {
			nn1HealthLevel = 0;
			nn2HealthLevel = 0;
		}

		public int getNn1HealthLevel() {
			return nn1HealthLevel;
		}

		public int getNn2HealthLevel() {
			return nn2HealthLevel;
		}

		public void setNn1HealthLevel(int nn1HealthLevel) {
			this.nn1HealthLevel = nn1HealthLevel;
		}

		public void setNn2HealthLevel(int nn2HealthLevel) {
			this.nn2HealthLevel = nn2HealthLevel;
		}
	}


	public StatisticsResult statistics() {
		StatisticsResult result = new StatisticsResult();

		int nn1Healthy = 0;
		int nn1Total = 0;
		int nn2Healthy = 0;
		int nn2Total = 0;

		List<NameNodeHeartbeat> nameNodeHeartbeats = getNameNodeHeartbeatList();
		if (CollectionUtils.isEmpty(nameNodeHeartbeats)) {
			return result;
		}
		for (NameNodeHeartbeat nameNodeHeartbeat : nameNodeHeartbeats) {
			if (nameNodeHeartbeat == null) {
				continue;
			}
			if ("nn1".equalsIgnoreCase(nameNodeHeartbeat.getNamenodeId())) {
				if (nameNodeHeartbeat.isHealthy()) {
					nn1Healthy++;
				}
				nn1Total++;
			} else if ("nn2".equalsIgnoreCase(nameNodeHeartbeat.getNamenodeId())) {
				if (nameNodeHeartbeat.isHealthy()) {
					nn2Healthy++;
				}
				nn2Total++;
			}
		}

		float nn1HealthRate = 0f;
		float nn2HealthRate = 0f;

		if (nn1Total != 0) {
			nn1HealthRate = (float) nn1Healthy / nn1Total;
		}

		if (nn2Total != 0) {
			nn2HealthRate = (float) nn2Healthy / nn2Total;
		}

		if (nn1HealthRate >= 0.75f) {
			result.setNn1HealthLevel(4);
		} else if (nn1HealthRate >= 0.6f) {
			result.setNn1HealthLevel(3);
		} else if (nn1HealthRate >= 0.3f) {
			result.setNn1HealthLevel(2);
		} else {
			result.setNn1HealthLevel(1);
		}

		if (nn2HealthRate >= 0.75f) {
			result.setNn2HealthLevel(4);
		} else if (nn2HealthRate >= 0.6f) {
			result.setNn2HealthLevel(3);
		} else if (nn2HealthRate >= 0.3f) {
			result.setNn2HealthLevel(2);
		} else if (nn2HealthRate >= 0.05){
			result.setNn2HealthLevel(1);
		} else {
			result.setNn2HealthLevel(4);
		}

		return result;
	}

}
