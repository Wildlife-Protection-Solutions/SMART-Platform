package org.wcs.smart.connect.exceptions;

import java.net.HttpURLConnection;

public class SmartConnectException extends RuntimeException {

	private String message;
	private String detail;
	private int responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
	
	/**
	 * Creates a exception that will return an internal error response code
	 * 
	 * @param message
	 */
	public SmartConnectException(String message, Exception ex){
		this(HttpURLConnection.HTTP_INTERNAL_ERROR, message, ex);
	}
	
	public SmartConnectException(String message){
		this(HttpURLConnection.HTTP_INTERNAL_ERROR, message, null);
	}
	
	public SmartConnectException(int responseCode, String message){
		this(responseCode, message, null);
		
	}
	/**
	 * Creates an exception that will return the specified response code
	 * with default server generated message
	 * @param responseCode
	 */
	public SmartConnectException(int responseCode, Exception ex){
		this(responseCode, null, ex);
	}
	
	public SmartConnectException(int responseCode){
		this(responseCode, null, null);
	}
	
	/**
	 * Creates an exception that will return the specified response code
	 * @param responseCode
	 * @param message
	 */
	public SmartConnectException(int responseCode, String message, Exception ex){
		this.message = message;
		this.responseCode = responseCode;
		if (ex != null){
			this.detail = ex.getMessage();
		}
	}
		
	public String getMessage(){
		return this.message;
	}
	
	public int getResponseCode(){
		return this.responseCode;
	}
	
	public String getDescription(){
		return this.detail;
	}
}
