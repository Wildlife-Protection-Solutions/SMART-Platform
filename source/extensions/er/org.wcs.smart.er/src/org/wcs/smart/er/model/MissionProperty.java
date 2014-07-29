/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.er.model;

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Link between a survey and the associated mission 
 * properties.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.mission_property")
@AssociationOverrides({
	@AssociationOverride(name = "id.surveyDesign", 
		joinColumns = @JoinColumn(name = "survey_design_uuid")),
	@AssociationOverride(name = "id.attribute", 
		joinColumns = @JoinColumn(name = "mission_attribute_uuid")) })
public class MissionProperty {


	
	private int order;
	private PropertyPk id = new PropertyPk();
	
	public MissionProperty(){
		
	}
	
	
	@EmbeddedId
	public PropertyPk getId(){
		return this.id;
	}
	public void setId(PropertyPk id){
		this.id = id;
	}
	
	/**
	 * The link to the datamodel
	 * attribute that represents this
	 * entity attribute
	 * 
	 * @return
	 */
	@Transient
	public MissionAttribute getAttribute() {
		return id.getAttribute();
	}


	public void setAttribute(MissionAttribute attribute) {
		id.setAttribute(attribute);
	}

	/**
	 * Link to the survey design
	 * @return
	 */
	@Transient
	public SurveyDesign getSurveyDesign() {
		return id.getSurveyDesign();
	}


	public void setSurveyDesign(SurveyDesign survey) {
		id.setSurveyDesign(survey);
	}


	/**
	 * The order of the attributes for the
	 * entity.  Allows users to sort
	 * the order the attributes are displayed
	 * in the UI.
	 * @return
	 */
	@Column(name="attribute_order")
	public int getOrder(){
		return this.order;
	}
	
	public void setOrder(int order){
		this.order = order;
	}
	
	@Embeddable
	private static class PropertyPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private SurveyDesign design;
		
		private MissionAttribute attribute;
		
		public PropertyPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="survey_design_uuid", referencedColumnName="uuid")
		public SurveyDesign getSurveyDesign(){
			return this.design;
		}
		public void setSurveyDesign(SurveyDesign design){
			this.design = design;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="mission_attribute_uuid", referencedColumnName="uuid")
		public MissionAttribute getAttribute(){
			return this.attribute;
		}
		public void setAttribute(MissionAttribute attribute){
			this.attribute  = attribute;
		}
	}
}
