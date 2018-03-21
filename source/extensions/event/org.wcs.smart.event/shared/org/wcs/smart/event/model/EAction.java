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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Action to perform.  Action is of a specified type and has a collection of paramater attribute values
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.e_action")
public class EAction extends UuidItem{
	
	public static final int MAX_ID_LENGTH = 128; 
			
	private ConservationArea ca;
	private String id;
	private String actionTypeKey;
	private List<EActionParameterValue> parameterValues;
	
	/**
	 * The conservation area 
	 * @return
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
	 * The action identifier
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
	 * The action type associated with this action
	 * 
	 * @return
	 */
	@Column(name="type_key")
	public String getActionTypeKey() {
		return this.actionTypeKey;
	}
	
	public void setActionTypeKey(String actionTypeKey) {
		this.actionTypeKey = actionTypeKey;
	}
	
	/**
	 * Sets the parameter values for the action type
	 * 
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.action", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<EActionParameterValue> getParameters(){
		return this.parameterValues;
	}
	
	public void setParameters(List<EActionParameterValue> values) {
		this.parameterValues = values;
	}
	
	/**
	 * Searches the parameter value for the given parameter.  Will return null
	 * if value not found.
	 * @param parameterKey
	 * @return
	 */
	@Transient
	public EActionParameterValue findParameter(String parameterKey) {
		for (EActionParameterValue v : getParameters()) {
			if (v.getId().getParameterKey().equals(parameterKey))  return v;
		}
		return null;
	}
}
