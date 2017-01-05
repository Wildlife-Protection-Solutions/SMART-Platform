package org.wcs.smart.i2.query.observation.filter;

import org.wcs.smart.i2.query.Operator;

public class BooleanFilter implements IQueryFilter {
	
	private IQueryFilter e1;
	private IQueryFilter e2;
	private Operator op;

	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 * @return
	 */
	public static BooleanFilter create(IQueryFilter e1, IQueryFilter e2, Operator op){
		return new BooleanFilter(e1, e2, op);
	}
	
	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 */
	private BooleanFilter(IQueryFilter e1, IQueryFilter e2, Operator op){
		assert op == Operator.AND || op == Operator.OR;
		this.e1 = e1;
		this.op = op;
		this.e2 = e2;
	}

	public IQueryFilter getFilter1(){
		return this.e1;
	}
	
	public IQueryFilter getFilter2(){
		return this.e2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
}

