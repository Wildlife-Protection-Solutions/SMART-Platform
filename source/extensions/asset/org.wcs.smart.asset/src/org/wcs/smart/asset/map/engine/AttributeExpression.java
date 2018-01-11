package org.wcs.smart.asset.map.engine;

public class AttributeExpression implements IFilter{

	public static AttributeExpression parse(IFilter exp) {
		return new AttributeExpression(exp);
	}
	
	
	public static AttributeExpression parse (IFilter filter1, Operator op, IFilter filter2) {
		return new AttributeExpression(filter1, op, filter2);
	}
	
	IFilter filter1;
	Operator op;
	IFilter filter2;
	
	public AttributeExpression(IFilter exp) {
		this.filter1 = exp;
		
	}
	
	public AttributeExpression(IFilter filter1, Operator op, IFilter filter2) {
		this.filter1 = filter1;
		this.op = op;
		this.filter2 = filter2;
	}

	@Override
	public String toString() {
		if (filter2 == null) {
			return " ( " + filter1.toString() + " ) ";
		}else {
			return filter1.toString() + " " + op.operator.sql + " " + filter2.toString();
		}
	}
}
