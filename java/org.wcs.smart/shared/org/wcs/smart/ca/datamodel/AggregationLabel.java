package org.wcs.smart.ca.datamodel;

import java.io.Serializable;

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
	}
}
