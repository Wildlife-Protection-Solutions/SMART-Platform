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
package org.wcs.smart.ca.datamodel;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "smart.dm_aggregation_i18n")
@AssociationOverrides({
	@AssociationOverride(name = "id.name", 
		joinColumns = @JoinColumn(name = "name")),
	@AssociationOverride(name = "id.lang_code", 
		joinColumns = @JoinColumn(name = "lang_code")) })
/**
 * i18N labels for summary aggregations
 * 
 * @author egouge
 *
 */
public class AggregationLabel {

	
	private AggregationLabelPk id;
	private String guiName;
	
	public AggregationLabel(){
	}
	
	@EmbeddedId
	public AggregationLabelPk getId(){
		return this.id;
	}
	
	public void setId(AggregationLabelPk id){
		this.id = id;
	}
	
	@Column(name="gui_name")
	public String getGuiName(){
		return this.guiName;
	}
	
	public void setGuiName(String guiName){
		this.guiName = guiName;
	}
	
	@Transient
	public String getCode(){
		return id.getCode();
	}
	
	@Embeddable
	public static class AggregationLabelPk implements Serializable{
		private static final long serialVersionUID = 1L;
		
		private String aggregation;
		private String code;
		
		public AggregationLabelPk(String agg, String code){
			this.aggregation = agg;
			this.code = code;
		}
		
		public AggregationLabelPk(){
			super();
		}
		
		public String getName(){
			return this.aggregation;
		}
		
		@Column(name="lang_code")
		public String getCode(){
			return this.code;
		}
		
		public void setName(String name){
			this.aggregation = name;
		}
		
		public void setCode(String code){
			this.code = code;			
		}
		
		@Override
		public boolean equals(Object other){
			if (other == this) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			AggregationLabelPk o = (AggregationLabelPk) other;
			return Objects.equals(aggregation, o.aggregation) && Objects.equals(code, o.code);
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(aggregation, code);
		}
	}
}
