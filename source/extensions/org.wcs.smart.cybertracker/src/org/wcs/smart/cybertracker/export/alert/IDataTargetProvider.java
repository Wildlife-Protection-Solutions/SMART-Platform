/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.export.alert;

import org.wcs.smart.cybertracker.model.CyberTrackerProperties;

/**
 * Interface for providing a data target for Cybertracker data.  Only one plugin should
 * provide one of these, if multiple plugins provide them then one is picked at random.
 * 
 * @author Emily
 *
 */
public interface IDataTargetProvider {

	/**
	 * Gets the data target information
	 * @return
	 * @throws Exception
	 */
	public DataTarget getTarget() throws Exception;
	
	/**
	 * Data target information class that includes url, username, password, frequency 
	 * and protocol information
	 * 
	 * @author Emily
	 *
	 */
	public class DataTarget{
		
		private String url; //target url
		private String username; //target username
		private String password; //target password
		private Integer frequency; //how often to send data
		private CyberTrackerProperties.Protocol protocol; //data protocol
		
		public DataTarget(String url, String username, String password, int frequency){
			this(url, username, password, frequency, CyberTrackerProperties.Protocol.GEOJSON_COMPRESSED);
		}
		
		public DataTarget(String url, String username, String password, int frequency, CyberTrackerProperties.Protocol protocol){
			this.url = url;
			this.username = username;
			this.password = password;
			this.frequency = frequency;
			this.protocol = protocol;
		}
		
		public String getUrl(){
			return this.url;
		}
		
		public String getUsername(){
			return this.username;
		}
		
		public String getPassword(){
			return this.password;
		}
		
		public Integer getFrequency(){
			return this.frequency;
		}
		
		public CyberTrackerProperties.Protocol getProtocol(){
			return this.protocol;
		}
	}
}
