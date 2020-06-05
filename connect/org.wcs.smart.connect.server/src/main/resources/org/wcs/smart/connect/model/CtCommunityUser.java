package org.wcs.smart.connect.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.cybertracker.community.model.CommunityUser;


@Entity
@Table(name="connect.ct_community_user")
public class CtCommunityUser extends UuidItem{
	
	private CommunityUser.State state;
	private String source;
	
	private Date validationSentDate;
	private String validationKey;

	@Enumerated(EnumType.STRING)
	@Column(name="state")
	public CommunityUser.State getState() {
		return this.state;
	}
	public void setState(CommunityUser.State state) {
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
	public Date getValidateSentDate() {
		return this.validationSentDate;
	}
	public void setValidateSentDate(Date date) {
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
