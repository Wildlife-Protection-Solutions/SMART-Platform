package org.wcs.smart.event.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name = "smart.e_event_filter")
public class EEventFilter extends UuidItem{
	
	public static final int MAX_ID_LENGTH = 128;
	
	private String id;
	private String filterString;
	private ConservationArea ca;
	
	/**
	 * 
	 * @return the conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * the filter identifier
	 * @return
	 */
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Filter string
	 * @return
	 */
	@Column(name="filter_string")
	public String getFilterString() {
		return this.filterString;
	}
	
	public void setFilterString(String filterString) {
		this.filterString = filterString;
	}

}
