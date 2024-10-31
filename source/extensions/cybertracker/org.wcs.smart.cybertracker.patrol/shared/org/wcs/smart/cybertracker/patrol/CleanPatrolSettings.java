package org.wcs.smart.cybertracker.patrol;

public class CleanPatrolSettings {

	
	public static final String DEFAULT_DAYS = "7"; //$NON-NLS-1$
	public static final String DEFAULT_DISTANCE = "250"; //$NON-NLS-1$
	public static final String DEFAULT_MINUTES = "30"; //$NON-NLS-1$
	
	public static final int MAX_DISTANCE = 5000;
	
	public static final String KEY = "org.wcs.smart.cybertracker.patrol.clean"; //$NON-NLS-1$
	
	public static final String DISTANCE_KEY = KEY + ".distance"; //$NON-NLS-1$
	public static final String DAYS_KEY = KEY + ".days"; //$NON-NLS-1$
	public static final String CLUSTER_MINUTES_KEY = KEY + ".clusterminutes"; //$NON-NLS-1$
	public static final String CLUSTER_DISTANCE_KEY = KEY + ".clusterdistance"; //$NON-NLS-1$
	
	
	private int days;
	private int validTrackDistance;
	private int clusterTrackDistance;
	private int clusterMinutes;
	
	
	public int getDays() {
		return days;
	}
	public void setDays(int days) {
		this.days = days;
	}
	public int getValidTrackDistance() {
		return validTrackDistance;
	}
	public void setValidTrackDistance(int validTrackDistance) {
		this.validTrackDistance = validTrackDistance;
	}
	public int getClusterTrackDistance() {
		return clusterTrackDistance;
	}
	public void setClusterTrackDistance(int clusterTrackDistance) {
		this.clusterTrackDistance = clusterTrackDistance;
	}
	public int getClusterMinutes() {
		return clusterMinutes;
	}
	public void setClusterMinutes(int clusterMinutes) {
		this.clusterMinutes = clusterMinutes;
	}
	
	
}
