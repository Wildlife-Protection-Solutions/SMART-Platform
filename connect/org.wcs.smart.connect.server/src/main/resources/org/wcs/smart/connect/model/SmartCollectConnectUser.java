package org.wcs.smart.connect.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.smartcollect.model.SmartCollectUser;


@Entity
@Table(name="connect.smartcollect_user")
public class SmartCollectConnectUser extends UuidItem{
	
	private static final long serialVersionUID = 1L;
	
	private SmartCollectUser.State state;
	private String source;
	
	private LocalDateTime validationSentDate;
	private String validationKey;

	@Enumerated(EnumType.STRING)
	@Column(name="state")
	public SmartCollectUser.State getState() {
		return this.state;
	}
	public void setState(SmartCollectUser.State state) {
		this.state = state;
	}
	
	@Column(name="source")
	public String getSource() {
		return this.source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	@Column(name="validation_sent_date")
	public LocalDateTime getValidateSentDate() {
		return this.validationSentDate;
	}
	public void setValidateSentDate(LocalDateTime date) {
		this.validationSentDate = date;
	}

	@Column(name="validation_key")
	public String getValidationKey() {
		return this.validationKey;
	}
	public void setValidationKey(String key) {
		this.validationKey = key;
	}
}
