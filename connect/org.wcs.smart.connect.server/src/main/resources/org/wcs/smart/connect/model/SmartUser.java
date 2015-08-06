package org.wcs.smart.connect.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name="connect.users")
public class SmartUser extends ConnectUuidItem {

	public static final int MAX_USERNAME_LENGTH = 32;
	public static final int MIN_USERNAME_LENGTH = 4;
	public static final int MAX_PASS_LENGTH = 32;
	public static final int MIN_PASS_LENGTH = 8;
	
	private String username;
	private String email;
	
	private String password;
	private String oldpassword;
	private String resetId;
	private Date resetDate;
	
	
	@Column(name="username")
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Column(name="email")
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	@JsonIgnore
	@Column(name="password")
	public String getPassword() {
		return password;
	}
	
	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}
	
	@JsonIgnore
	@Transient
	public String getOldpassword() {
		return oldpassword;
	}
	
	@JsonProperty
	public void setOldpassword(String password) {
		this.oldpassword = password;
	}
	
	@JsonIgnore
	@Column(name="resetid")
	public String getResetId() {
		return resetId;
	}
	public void setResetId(String resetId) {
		this.resetId = resetId;
	}
	
	@JsonIgnore
	@Column(name="resetdatetime")
	public Date getResetDatetime() {
		return resetDate;
	}
	public void setResetDatetime(Date reset) {
		this.resetDate = reset;
	}
	
}
