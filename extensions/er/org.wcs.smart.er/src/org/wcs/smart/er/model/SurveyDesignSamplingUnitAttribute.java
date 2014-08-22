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

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Link between a survey design and associated sampling unit attributes.
 * @author Emily
 *
 */
@Entity
@Table(name="smart.survey_design_sampling_unit")
public class SurveyDesignSamplingUnitAttribute {
	
	private SurveyDesignSamplingUnitAttributePk id = 
			new SurveyDesignSamplingUnitAttributePk();


	public SurveyDesignSamplingUnitAttribute() {

	}

	@EmbeddedId
	public SurveyDesignSamplingUnitAttributePk getId() {
		return this.id;
	}

	public void setId(SurveyDesignSamplingUnitAttributePk id) {
		this.id = id;
	}

	@Transient
	public SurveyDesign getSurveyDesign() {
		return id.getSurveyDesign();
	}

	public void setSurveyDesign(SurveyDesign surveyDesign) {
		id.setSurveyDesign(surveyDesign);
	}

	@Transient
	public SamplingUnitAttribute getSamplingUnitAttribute() {
		return id.getSamplingUnitAttribute();
	}

	public void setSamplingUnitAttribute(SamplingUnitAttribute attribute) {
		id.setSamplingUnitAttribute(attribute);
	}

	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof SurveyDesignSamplingUnitAttribute) {
			return this.id.equals(((SurveyDesignSamplingUnitAttribute) o).id);
		}
		return false;
	}

	/**
	 * @return
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Embeddable
	private static class SurveyDesignSamplingUnitAttributePk implements Serializable {
		/**
	 * 
	 */
		private static final long serialVersionUID = 1L;


		private SamplingUnitAttribute attribute;
		private SurveyDesign design;

		public SurveyDesignSamplingUnitAttributePk() {
		}

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
		@JoinColumn(name = "survey_design_uuid", referencedColumnName = "uuid")
		public SurveyDesign getSurveyDesign() {
			return design;
		}

		public void setSurveyDesign(SurveyDesign design) {
			this.design = design;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "su_attribute_uuid", referencedColumnName = "uuid")
		public SamplingUnitAttribute getSamplingUnitAttribute() {
			return attribute;
		}

		public void setSamplingUnitAttribute(SamplingUnitAttribute attribute) {
			this.attribute = attribute;
		}

		@Override
		public boolean equals(Object key) {
			if (!(key instanceof SurveyDesignSamplingUnitAttributePk)) {
				return false;
			}
			SurveyDesignSamplingUnitAttributePk p = (SurveyDesignSamplingUnitAttributePk) key;

			if (p.design == null || this.design == null
					|| p.attribute == null || this.attribute == null) {

				if (p.design == null && this.design == null
						&& p.attribute == null && this.attribute == null) {
					return true;
				}
				return false;
			}

			return p.design.equals(this.design)
					&& p.attribute.equals(this.attribute);
		}

		@Override
		public int hashCode() {
			int code = 0;
			if (design != null) {
				code += design.hashCode();
			}
			code *= 31;
			if (attribute != null) {
				code += attribute.hashCode();
			}
			return code;
		}
	}
}
