package org.wcs.smart.asset.map.engine;

public class CombinedFilter implements IFilter{

	public static CombinedFilter parse (IFilter filter1, Operator op, IFilter filter2) {
		return new CombinedFilter(filter1, op, filter2);
	}
	
	private IFilter filter1;
	private Operator op;
	private IFilter filter2;
	
	public CombinedFilter(IFilter filter1, Operator op, IFilter filter2) {
		this.filter1 = filter1;
		this.op = op;
		this.filter2 = filter2;
	}
	
	public IFilter getFilter1() { 
		return filter1;
	}

	public IFilter getFilter2() {
		return filter2;
	}
	
	public Operator getOperator() {
		return this.op;
	}
	
	@Override
	public String toString() {
		return filter1.toString() + " " + op.operator.sql + " " + filter2.toString();
	}
	
	@Override
	public void accept(IFilterVisitor visitor) {
		filter1.accept(visitor);
		filter2.accept(visitor);
		visitor.visit(this);
	}
}
