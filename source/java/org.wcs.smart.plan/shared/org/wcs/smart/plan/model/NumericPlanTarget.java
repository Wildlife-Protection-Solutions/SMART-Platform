package org.wcs.smart.plan.model;

import java.text.MessageFormat;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.wcs.smart.SmartContext;
import org.wcs.smart.plan.IPlanLabelProvider;

/**
 * Represents a NumericPlanTarget object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("NUMERIC")
public class NumericPlanTarget extends PlanTarget {

	public static final String SUMMARY_KEY = "numericsummary"; //$NON-NLS-1$
	
	/*
	 * Valid target types
	 */
	public enum Unit{
		KM,
		HOURS,
		DAYS;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(this, l);
		}
	}
	public enum TargetType{
		DISTANCE(Unit.KM),
		PATROL_HOURS(Unit.HOURS),
		PATROL_DAYS(Unit.DAYS),
		PATROL_MANHOURS(Unit.HOURS);
		
		private Unit unit;
		private TargetType(Unit unit){
			this.unit = unit;
		}
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(this, l);
		}
		public Unit getUnits(){
			return this.unit;
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

		private String value;
		
		Operator(String value){
			this.value = value;
		}
		public String getSmartValue(){
			return value;
		}
	}
	
	private Double value;
	private Operator op;
	private TargetType type;
	private String description;

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

	@Override
	public String getSummary(Locale l) {
		return MessageFormat.format(
				SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(SUMMARY_KEY, l),
				getType().getGuiName(l) + " " + getOp().getSmartValue() + " " + getValue()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
