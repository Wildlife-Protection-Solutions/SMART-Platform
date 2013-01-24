package org.wcs.smart.plan.model;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Represents a NumericPlanTarget object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("ALPHANUMERIC")
public class NumericPlanTarget extends PlanTarget {

	private Double value;
	private String op;
	private String type;
	
	
	@Transient
	public String getSummary() {
		return "[Numeric] " +type + " " + op + " " + value;
	}
	
	@Column(name = "op")
	public String getOp() {
		return this.op;
	}
	public void setOp(String op) {
		this.op = op;
	}	
	
	@Column(name = "type")
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "value")
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
		
	}


	@Override
	public NumericPlanTarget clone() {
		NumericPlanTarget n = new NumericPlanTarget();
		super.clone(n);
		n.op = this.op;
		n.type = this.type;
		n.value = this.value;
		return n;
	}


}
