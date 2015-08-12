package org.wcs.smart.connect.model;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.JdkDeserializers.UUIDDeserializer;

@Entity
@Table(name = "connect.alerts")
public class Alert extends UuidItem{
	
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
//	@JsonDeserialize(using = CustomUuidDeserializer.class)
//	@JsonSerialize(using = CustomUuidSerializer.class)
	private Integer level;
	private AlertStatusEnum status;
	private Double x;
	private Double y;
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
	
}
