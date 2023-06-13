/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="paws_service", schema="smart")
public class PawsService extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private ConservationArea ca;
	private String pawsApiKey;
	private String pawsApi;
	private String taskApi;
	private String oauthURL;
	private String clientId;
	private String storageURL;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="paws_api_key")
	public String getApiKey() {
		return this.pawsApiKey;
	}
	
	public void setApiKey(String pawsApiKey) {
		this.pawsApiKey = pawsApiKey;
	}
	
	@Column(name="paws_api")
	public String getPawsApiUrl() {
		return this.pawsApi;
	}
	
	public void setPawsApiUrl(String pawsApi) {
		this.pawsApi = pawsApi;
	}
	
	@Column(name="task_api")
	public String getTaskApiUrl() {
		return this.taskApi;
	}
	
	public void setTaskApiUrl(String taskApi) {
		this.taskApi = taskApi;
	}
	
	@Column(name="oauth_url")
	public String getOAuthUrl() {
		return this.oauthURL;
	}
	
	public void setOAuthUrl(String oauthURL) {
		this.oauthURL = oauthURL;
	}
	
	@Column(name="client_id")
	public String getClientId() {
		return this.clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	@Column(name="storage_account_url")
	public String getStorageUrl() {
		return this.storageURL;
	}
	
	public void setStorageUrl(String storageURL) {
		this.storageURL = storageURL;
	}
	
	@Transient
	public boolean isConfigured() {
		if (taskApi == null || pawsApi== null || pawsApi== null ||
				clientId == null || storageURL == null || oauthURL == null ||
				taskApi.isEmpty() || pawsApi.isBlank() || pawsApiKey.isBlank() ||
				clientId.isBlank() || storageURL.isBlank() || oauthURL.isBlank() ) return false;
		return true;
	}
}
