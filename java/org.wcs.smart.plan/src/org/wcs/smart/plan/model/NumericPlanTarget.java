package org.wcs.smart.plan.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.util.List;
import org.wcs.smart.plan.PlanHibernateManager;

/**
 * Represents a NumericPlanTarget object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("NUMERIC")
public class NumericPlanTarget extends PlanTarget {

	public final static String TARGET_GUI_NAME = "Numeric";
	
	/*
	 * Valid target types
	 */
	public enum TargetType{
		DISTANCE("Distance Travelled"),
		PATROL_HOURS("Patrol Hours"),
		PATROL_DAYS("Patrol Days"),
		PATROL_MANHOURS("Patrol Man-Hours");
		
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
		GREATER(">"),
		LESS("<"),
		EQUAL("="),
		NOEQUAL("!=");

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
		return "[Numeric] " + type.guiName + " " + op.guiName + " " + value;
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

	@Override
	@Transient
	public String getStatus() {
		
		Double total = calculateTargetStatusValue(this.getPlan());

		if(op == Operator.EQUAL){
			if(total == value){
				return "Completed (" + total + ")";
			}else{
				return "Incomplete ("+ total + ")";
			}
		}else if(op == Operator.GREATER){
			if(total > value){
				return "Completed (" + total + ")";
			}else{
				return "Incomplete ("+ total + ")";
			}
		}else if(op == Operator.LESS){
			if(total < value){
				return "Completed (" + total + ")";
			}else{
				return "Missed (" + total + ")";
			}
		}else if(op == Operator.NOEQUAL){
			if(total != value){
				return "Completed (" + total + ")";
			}else{
				return "Incomplete (" + total + ")";
			}
		}
		return "Unsupported Target Type";
	}

	private Double calculateTargetStatusValue(Plan plan){
		List<Plan> children = plan.getChildren();
		Double total = PlanHibernateManager.getTargetTotalValue(this.type, plan);
		for (Plan p : children){
			total += calculateTargetStatusValue(p);
		}
		return total;
	}

}
