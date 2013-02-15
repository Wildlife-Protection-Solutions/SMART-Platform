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
		DISTANCE(Messages.NumericPlanTarget_TargetType_DistanceTraveled),
		PATROL_HOURS(Messages.NumericPlanTarget_TargetType_PatrolHours),
		PATROL_DAYS(Messages.NumericPlanTarget_TargetType_PatrolDays),
		PATROL_MANHOURS(Messages.NumericPlanTarget_TargetType_PatrolManHours);
		
		public String guiName;
		private TargetType(String guiName){
			this.guiName = guiName;
		}
		public String getName(){
			return guiName;
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
