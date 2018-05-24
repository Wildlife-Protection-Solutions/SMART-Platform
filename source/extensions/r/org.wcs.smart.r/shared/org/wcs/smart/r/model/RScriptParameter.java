package org.wcs.smart.r.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name="smart.r_script_runparameter")
public class RScriptParameter extends UuidItem{

	public static final String R_PARAMETER = "R_PARAMETER"; //$NON-NLS-1$
	
	private RScript script;
	private String key;
	private String value;

	public RScriptParameter() {
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="script_uuid", referencedColumnName="uuid")
	public RScript getScript() {
		return this.script;
	}
	
	public void setScript(RScript script) {
		this.script = script;
	}
	
	@Column(name="pkey")
	public String getKey() {
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	@Column(name="value")
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
