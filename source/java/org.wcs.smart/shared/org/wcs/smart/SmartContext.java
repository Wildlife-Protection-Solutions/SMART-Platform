package org.wcs.smart;

import java.io.File;
import java.util.HashMap;

public enum SmartContext {

	INSTANCE;
	
	private java.util.HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();
	
	private String filestoreLocation;
	private File tempfilestoreLocation;
	
	public void setClass(Class<?> clazz, Object object){
		map.put(clazz, object);
	}
	
	public <T> T getClass(Class<T> clazz){
		return (T)map.get(clazz);
	}
	
	public String getFilestoreLocation(){
		return this.filestoreLocation;
	}
	
	public void setFilestoreLocation(String rootLocation){
		this.filestoreLocation = rootLocation;
	}
	
	public File getTempFilestoreLocation(){
		return this.tempfilestoreLocation;
	}
	
	public void setTempFilestoreLocation(File location){
		this.tempfilestoreLocation = location;
	}
}
