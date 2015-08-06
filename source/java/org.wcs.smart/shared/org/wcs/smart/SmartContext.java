package org.wcs.smart;

import java.io.File;
import java.util.HashMap;

public enum SmartContext {

	INSTANCE;
	
	public static final String FILESTORE_KEY = "filestore_location";
	
	public static final String TEMP_FILESTORE_KEY = "temp_filestore_location";
	
	private java.util.HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();
	private java.util.HashMap<String, String> pairs = new HashMap<String, String>();
	
	
	public void setClass(Class<?> clazz, Object object){
		map.put(clazz, object);
	}
	
	public <T> T getClass(Class<T> clazz){
		return (T)map.get(clazz);
	}
	
	public String getFilestoreLocation(){
		return pairs.get(FILESTORE_KEY);
	}
	
	public void setFilestoreLocation(String rootLocation){
		pairs.put(FILESTORE_KEY, rootLocation);
	}
	
	public File getTempFilestoreLocation(){
		return new File(pairs.get(TEMP_FILESTORE_KEY));
	}
	
	public void setTempFilestoreLocation(File location){
		pairs.put(TEMP_FILESTORE_KEY, location.getAbsolutePath());
	}
	
	public void setPair(String key, String value){
		pairs.put(key,  value);
	}
	
	public String getPair(String key){
		return pairs.get(key);
	}
}
