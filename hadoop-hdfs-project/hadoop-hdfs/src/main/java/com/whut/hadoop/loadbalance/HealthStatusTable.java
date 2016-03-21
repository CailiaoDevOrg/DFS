package com.whut.hadoop.loadbalance;

/**
 * Created by niuyang on 3/15/16.
 */
public class HealthStatusTable {

	public enum HealthLevel {
		HEALTH(4),
		SL(3),
		HL(2),
		RSS(1),
		NA(0);

		public int value;

		HealthLevel(int value) {
			this.value = value;
		}
	}

    private boolean monitor1isAlive;

    private boolean monitor2isAlive;

    private HealthLevel namenode1HealthLevel;

    private HealthLevel namenode2HealthLevel;

	public boolean isMonitor1isAlive() {
		return monitor1isAlive;
	}

	public void setMonitor1isAlive(boolean monitor1isAlive) {
		this.monitor1isAlive = monitor1isAlive;
	}

	public boolean isMonitor2isAlive() {
		return monitor2isAlive;
	}

	public void setMonitor2isAlive(boolean monitor2isAlive) {
		this.monitor2isAlive = monitor2isAlive;
	}

	public HealthLevel getNamenode1HealthLevel() {
		return namenode1HealthLevel;
	}

	public void setNamenode1HealthLevel(int namenode1HealthLevel) {
		this.namenode1HealthLevel = getHealthLevel(namenode1HealthLevel);
	}

	public HealthLevel getNamenode2HealthLevel() {
		return namenode2HealthLevel;
	}

	public void setNamenode2HealthLevel(int namenode2HealthLevel) {
		this.namenode2HealthLevel = getHealthLevel(namenode2HealthLevel);
	}

	private HealthLevel getHealthLevel(int level) {
		HealthLevel healthLevel;
		switch (level) {
			case 4:
				healthLevel = HealthLevel.HEALTH;
				break;
			case 3:
				healthLevel = HealthLevel.SL;
				break;
			case 2:
				healthLevel = HealthLevel.HL;
				break;
			case 1:
				healthLevel = HealthLevel.RSS;
				break;
			case 0:
				healthLevel = HealthLevel.NA;
				break;
			default:
				healthLevel = HealthLevel.HEALTH;
		}
		return healthLevel;
	}
    
}
