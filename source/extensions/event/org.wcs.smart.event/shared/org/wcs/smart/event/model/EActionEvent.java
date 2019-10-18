/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Action events to perform on new waypoint observations
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.e_event_action")
public class EActionEvent extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	private EAction action;
	private EFilter filter;
	
	private boolean isEnabled;
	
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
	public EFilter getFilter() {
		return this.filter;
	}
	
	public void setFilter(EFilter filter) {
		this.filter = filter;
	}
	
	@Column(name="is_enabled")
	public boolean isEnabled() {
		return this.isEnabled;
	}
	
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}
