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
package org.wcs.smart.connect.model;

import java.sql.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.GlobalFireWatchApi;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalFireWatchProxy {

	private UUID uuid = null;
	private UUID alertUuid = null;
	private Date lastDataDate = null;
	private String alertName = null;
	
	private String smartUrl = null;
	
	public static String generateUrl(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		sb.append("https://"); //$NON-NLS-1$
		sb.append(request.getServerName());
		sb.append(":");
		sb.append(request.getServerPort());
		sb.append(request.getContextPath());
		sb.append("/");
		sb.append(ConnectRESTApplication.NO_AUTH_PATH );
		sb.append("/"); //$NON-NLS-1$
		sb.append(GlobalFireWatchApi.PATH);
		return sb.toString();
		
	}
	
	public GlobalFireWatchProxy() {
		
	}
	
	public GlobalFireWatchProxy(GlobalFireWatch gfw, String urlPrefix) {
		this.uuid = gfw.getUuid();
		this.alertUuid = gfw.getAlertType().getUuid();
		this.alertName = gfw.getAlertType().getLabel();
		this.lastDataDate = gfw.getLastDataDate();
		this.smartUrl = urlPrefix + "/" + UuidUtils.uuidToString(this.uuid); //$NON-NLS-1$
	}
	
	public String getSmartUrl() {
		return this.smartUrl;
	}
	
	public void setSmartUrl(String url) {
		this.smartUrl = url;
	}
	
	
	public UUID getUuid() {
		return this.uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getAlertUuid() {
		return this.alertUuid;
	}
	public void setAlertUuid(UUID alertUuid) {
		this.alertUuid = alertUuid;
	}
	
	public String getAlertName() {
		return this.alertName;
	}
	public void setAlertName(String name) {
		this.alertName = name;
	}
	
	public Date getLastDataDate() {
		return this.lastDataDate;
	}
	public void setLastDataDate(Date date) {
		this.lastDataDate = date;
	}
	
	
	
}
