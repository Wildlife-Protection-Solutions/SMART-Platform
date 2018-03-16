package org.wcs.smart.event.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

@Entity
@Table(name = "smart.e_event_action")
public class EActionEvent extends UuidItem {

	private EAction action;
	private EEventFilter filter;
	
	/**
	 * 
	 * @return the associated action
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="action_uuid", referencedColumnName="uuid")
	public EAction getAction() {
		return this.action;
	}
	
	public void setAction(EAction action) {
		this.action = action;
	}
	
	/**
	 * 
	 * @return the associated filter
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="filter_uuid", referencedColumnName="uuid")
	public EEventFilter getFilter() {
		return this.filter;
	}
	
	public void setFilter(EEventFilter filter) {
		this.filter = filter;
	}
}
