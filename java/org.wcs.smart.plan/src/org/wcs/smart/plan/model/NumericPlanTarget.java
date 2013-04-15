package org.wcs.smart.plan.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import org.wcs.smart.plan.internal.Messages;

/**
 * Represents a NumericPlanTarget object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("NUMERIC")
public class NumericPlanTarget extends PlanTarget {

	public final static String TARGET_GUI_NAME = Messages.NumericPlanTarget_GuiName;
	
	/*
	 * Valid target types
	 */
	public enum TargetType{
		DISTANCE(Messages.NumericPlanTarget_TargetType_DistanceTraveled, Messages.NumericPlanTarget_km),
		PATROL_HOURS(Messages.NumericPlanTarget_TargetType_PatrolHours, Messages.NumericPlanTarget_hours),
		PATROL_DAYS(Messages.NumericPlanTarget_TargetType_PatrolDays, Messages.NumericPlanTarget_days),
		PATROL_MANHOURS(Messages.NumericPlanTarget_TargetType_PatrolManHours, Messages.NumericPlanTarget_hours);
		
		public String guiName;
		public String units;
		private TargetType(String guiName, String units){
			this.guiName = guiName;
			this.units = units;
		}
		public String getName(){
			return guiName;
		}
		public String getUnits(){
			return this.units;
		}
	}
	
	/*
	 * Valid operators
	 */
	public enum Operator{
		GREATER(">"), //$NON-NLS-1$
		LESS("<"), //$NON-NLS-1$
		EQUAL("="), //$NON-NLS-1$
		NOEQUAL("!="); //$NON-NLS-1$

		public String guiName;
		private Operator(String guiName){
			this.guiName = guiName;
		}
	}
	
	private Double value;
	private Operator op;
	private TargetType type;
	private String description;

	@Override
	@Transient
	public String getSummary() {
		return "[" + Messages.NumericPlanTarget_CategoryName + "] " + type.guiName + " " + op.guiName + " " + value; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	}
	
	@Column(name = "op")
	@Enumerated(EnumType.STRING)
	public Operator getOp() {
		return this.op;
	}
	public void setOp(Operator op) {
		this.op = op;
	}	
	
	@Column(name = "type")
	@Enumerated(EnumType.STRING)
	public TargetType getType() {
		return this.type;
	}
	public void setType(TargetType type) {
		this.type = type;
	}

	@Column(name = "value")
	public Double getValue() {
		return value;
	}
	public void setValue(Double value) {
		this.value = value;
		
	}

	@Column(name = "description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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
