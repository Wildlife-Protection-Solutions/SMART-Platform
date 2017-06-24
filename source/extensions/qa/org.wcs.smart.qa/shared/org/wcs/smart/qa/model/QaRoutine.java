/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model;

import java.util.Collection;

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
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.routine.IQaRoutineType;

/**
 * A QA routine created by the user. 
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.qa_routine")
public class QaRoutine extends NamedItem{

	public static final int MAX_DESC_LENGTH = 32600;
	
	private ConservationArea conservationArea;
	private String type;
	private Boolean autoCheck;
	private Collection<QaRoutineParameter> parameters;
	private String description;

	@Transient
	private IQaRoutineType rtype;
	
	/**
	 * Constructor.
	 */
	public QaRoutine() {
	}

	/**
	 * Get the conservation area.
	 * 
	 * @return conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	
	/**
	 * Set the conservation area.
	 * 
	 * @param conservationArea
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}

	/**
	 * Get the routine type identifier
	 * 
	 * @return id
	 */
	@Column(name="routine_type_id")
	public String getRoutineTypeId() {
		return this.type;
	}

	/**
	 * Set the routine type identifier
	 * 
	 * @param id
	 *            id
	 */
	public void setRoutineTypeId(String type) {
		this.type = type;
	}

	/**
	 * A user defined description for the routine
	 * 
	 * @return description
	 */
	@Column(name="description")
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the user defined description for the routine
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the auto_check value
	 * 
	 * @return auto_check
	 */
	@Column(name="auto_check")
	public Boolean getAutoCheck() {
		return this.autoCheck;
	}
	
	/**
	 * Set the auto check value
	 * 
	 * @param autoCheck
	 */
	public void setAutoCheck(Boolean autoCheck) {
		this.autoCheck = autoCheck;
	}

	/**
	 * Get the set of the parameters defined for the routine
	 * 
	 * @return The set of qa_routine_parameter
	 */
	@OneToMany(fetch = FetchType.LAZY, cascade={CascadeType.ALL}, mappedBy="qaRoutine", orphanRemoval=true)
	public Collection<QaRoutineParameter> getParameters() {
		return this.parameters;
	}
	/**
	 * Set the set of the qa_routine_parameter.
	 * 
	 * @param qaRoutineParameterSet
	 *            The set of qa_routine_parameter
	 */
	public void setParameters(Collection<QaRoutineParameter> parameters) {
		this.parameters = parameters;
	}


	@Transient
	public QaRoutineParameter findParameter(String parameterId){
		if (parameters == null) return null;
		for (QaRoutineParameter p : parameters){
			if (p.getParameterId().equals(parameterId)) return p;
		}
		return null;
	}

	/**
	 * Get the routine type identifier
	 * 
	 * @return id
	 */
	@Transient
	public IQaRoutineType getRoutineType() {
		if (this.rtype == null){
			rtype = RoutineExtensionManager.INSTANCE.findRoutineType(getRoutineTypeId());
		}
		return this.rtype;
	}

}
