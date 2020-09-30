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


import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.connect.util.LocalDateTimeDeserializer;
import org.wcs.smart.connect.util.LocalDateTimeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An Shared Link entity
 *
 * @Author Jeff
 */
@Entity
@Table(name = "connect.shared_links")
public class SharedLink extends ConnectUuidItem{
	
	private UUID ownerUuid;
	private UUID permissionUserUuid;
	private UUID caUuid;
	private LocalDateTime expiresAt;
	private LocalDateTime dateCreated;
	private String url;
	private boolean isUserToken;
	private String allowedIp;
	
	@Transient
	private int expiresAfter; //for json objects in minutes
	
	@Transient
	public String permissionUsername;	//the username of the permission user
	@Transient
	public String ownerUsername;	//the username of the creator of this link
	
	
	@Column(name="allowed_ip")
	public String getAllowedIp() {
		return allowedIp;
	}
	public void setAllowedIp(String allowedIp) {
		this.allowedIp = allowedIp;
	}
	
	@Column(name="owner_uuid")
	public UUID getOwnerUuid() {
		return ownerUuid;
	}
	public void setOwnerUuid(UUID ownerUuid) {
		this.ownerUuid = ownerUuid;
	}
	
	@Column(name="permissionuser_uuid")
	public UUID getPermissionUserUuid() {
		return permissionUserUuid;
	}
	public void setPermissionUserUuid(UUID permissionUserUuid) {
		this.permissionUserUuid = permissionUserUuid;
	}
	
	@Column(name="expires_at")
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)  
	@JsonSerialize(using = LocalDateTimeSerializer.class) 
	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}
	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
	
	@Column(name="url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(name="date_created")
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)  
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	public LocalDateTime getDateCreated(){
		return this.dateCreated;
	}

	public void setDateCreated(LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	@Column(name="is_user_token")
	public boolean isUserToken() {
		return isUserToken;
	}
	public void setUserToken(boolean isUserToken) {
		this.isUserToken = isUserToken;
	}
	
	@Column(name="ca_uuid")
	public UUID getConservationArea() {
		return caUuid;
	}
	public void setConservationArea(UUID caUuid) {
		this.caUuid = caUuid;
	}
	
	@Transient
	public String getPermissionUsername(){
		return this.permissionUsername;
	}
	public void setPermissionUsername(String permissionUsername){
		this.permissionUsername = permissionUsername;
	}
	
	@Transient
	public String getOwnerUsername(){
		return this.ownerUsername;
	}
	public void setOwnerUsername(String ownerUsername){
		this.ownerUsername = ownerUsername;
	}
	
	@Transient
	/**
	 * The amount of time the link should be valid
	 * for in minutes.
	 * @return
	 */
	public int getExpiresAfter(){
		return this.expiresAfter;
	}

	@Transient
	/**
	 * The amount of time the link should be valid
	 * for in minutes.
	 * @return
	 */
	public void setExpiresAfter(int expiresAfter){
		this.expiresAfter = expiresAfter;
	}

}
