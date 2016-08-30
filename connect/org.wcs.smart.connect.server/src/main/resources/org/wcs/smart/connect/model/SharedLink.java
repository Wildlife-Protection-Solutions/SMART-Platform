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


import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/*
 * An Alert entity
 *
 * @Author Jeff
 */
@Entity
@Table(name = "connect.shared_links")
public class SharedLink extends ConnectUuidItem{
	
	private UUID ownerUuid;
	private UUID caUuid;
	private Timestamp expiresAt;
	private String url; 
	private int expiresAfter;
	
	
	@Column(name="owner_uuid")
	public UUID getOwnerUuid() {
		return ownerUuid;
	}
	public void setOwnerUuid(UUID ownerUuid) {
		this.ownerUuid = ownerUuid;
	}
	
	@Column(name="expires_at")
	public Timestamp getExpiresAt() {
		return expiresAt;
	}
	public void setExpiresAt(Timestamp expiresAt) {
		this.expiresAt = expiresAt;
	}
	
	@Column(name="url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(name="expires_after")
	public int getExpiresAfter() {
		return expiresAfter;
	}
	public void setExpiresAfter(int expiresAfter) {
		this.expiresAfter = expiresAfter;
	}
	
	@Column(name="ca_uuid")
	public UUID getCaUuid() {
		return caUuid;
	}
	public void setCaUuid(UUID caUuid) {
		this.caUuid = caUuid;
	}
	
}
