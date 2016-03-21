package com.whut.hadoop.loadbalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Created by niuyang on 3/15/16.
 */
public class CustomLBStrategy {

	public class Strategy {
		
		private String jobName;
		private String namenodeId;

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public String getNamenodeId() {
			return namenodeId;
		}

		public void setNamenodeId(String namenodeId) {
			this.namenodeId = namenodeId;
		}

	}

	private List<Strategy> strategyList;

	public List<Strategy> getStrategyList() {
		return strategyList;
	}

	public void setStrategyList(List<Strategy> strategyList) {
		this.strategyList = strategyList;
	}
	
	public void setStrategyList(String strategyStrs) {
	    if (StringUtils.isBlank(strategyStrs)) {
	    	this.strategyList = Collections.emptyList();
			return;
	    }
		this.strategyList = new ArrayList<>();
	    String[] strategyArr = strategyStrs.split(";");
		for (String strategy : strategyArr) {
			if (StringUtils.isBlank(strategy)) {
				this.strategyList = Collections.emptyList();
				return;
			}
			String[] item = strategy.trim().split(":");
			if (item.length != 2) {
				this.strategyList = Collections.emptyList();
				return;
			}
			Strategy s = new Strategy();
			s.setJobName(item[0]);
			s.setNamenodeId(item[1]);
			this.strategyList.add(s);
		}
	}

}
