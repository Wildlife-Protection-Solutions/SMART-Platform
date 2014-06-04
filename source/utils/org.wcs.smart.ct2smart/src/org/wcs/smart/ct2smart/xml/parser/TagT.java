package org.wcs.smart.ct2smart.xml.parser;

public class TagT {
	
	public static final String DEVICE_ID = "DeviceId"; //$NON-NLS-1$
	public static final String DATE      = "Date"; //$NON-NLS-1$
	public static final String TIME      = "Time"; //$NON-NLS-1$
	public static final String LATITUDE  = "Latitude"; //$NON-NLS-1$
	public static final String LONGITUDE = "Longitude"; //$NON-NLS-1$
	
	private String deviceId;
	private String date;
	private String time;
	private String latitude;
	private String longitude;
	
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

}
