package org.wcs.smart.r.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;

@Entity
@Table(name="smart.r_query")
public class RQuery extends NamedItem{

	private RScript script;
	private String configuration;
	private ConservationArea ca;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="config")
	public String getConfiguration() {
		return this.configuration;
	}
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="script_uuid", referencedColumnName="uuid")
	public RScript getScript() {
		return this.script;
	}
	
	public void setScript(RScript script) {
		this.script = script;
	}
	
}
