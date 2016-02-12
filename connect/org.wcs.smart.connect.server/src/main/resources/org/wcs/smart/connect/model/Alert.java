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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/*
 * An Alert entity
 *
 * @Author Jeff
 */
@Entity
@Table(name = "connect.alerts")
public class Alert extends ConnectUuidItem{
	
	public enum AlertStatusEnum {
		ACTIVE("ACTIVE"),
		DISABLED("DISABLED");
	 
		protected String value;
			
		private AlertStatusEnum(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}
	
	private String userGeneratedId; 
	private Date date;
	private String description;
	private Integer level;
	private AlertStatusEnum status;
	private Double x;
	private Double y;
	private String track;
	public UUID typeUuid;
	private UUID caUuid;
	private UUID creatorUuid;
	
	@Column(name="date")
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Column(name="description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Column(name="type_uuid")
	public UUID getTypeUuid() {
		return typeUuid;
	}
	public void setTypeUuid(UUID type_uuid) {
		this.typeUuid = type_uuid;
	}
	
	@Column(name="level")
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	
	@Column(name="ca_uuid")
	public UUID getCaUuid() {
		return caUuid;
	}
	public void setCaUuid(UUID ca_uuid) {
		this.caUuid = ca_uuid;
	}
	
	@Column(name="status")
	@Type(type="org.wcs.smart.connect.model.AlertStatusType")
	public AlertStatusEnum getStatus() {
		return status;
	}
	public void setStatus(AlertStatusEnum status) {
		this.status = status;
	}
	
	@Column(name="x")
	public Double getX() {
		return x;
	}
	public void setX(Double x) {
		this.x = x;
	}
	
	@Column(name="y")
	public Double getY() {
		return y;
	}
	public void setY(Double y) {
		this.y = y;
	}
	
	@Column(name="creator_uuid")
	public UUID getCreatorUuid() {
		return creatorUuid;
	}
	public void setCreatorUuid(UUID creator_uuid) {
		this.creatorUuid = creator_uuid;
	}
	
	@Column(name="user_generated_id")
	public String getUserGeneratedId() {
		return userGeneratedId;
	}
	public void setUserGeneratedId(String userGeneratedId) {
		this.userGeneratedId = userGeneratedId;
	}
	
	@Column(name="track")
	public String getTrack() {
		return track;
	}
	public void setTrack(String track) {
		this.track = track;
	}
	
}
