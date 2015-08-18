/*
 * Copyright (C) 2015 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.exceptions;

import java.net.HttpURLConnection;

/**
 * A exception for SMART Connect that gets translated into the correct
 * HTTP Response.
 * 
 * @author Emily
 *
 */
public class SmartConnectException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
