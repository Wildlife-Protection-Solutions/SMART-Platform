package org.wcs.smart.er.query.filter;

import org.hibernate.Session;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;

public class SurveyIdFilter implements IFilter {

	
	/**
	 * Creates a survey id filter.
	 * 
	 * @return
	 */
	public static SurveyIdFilter createFilter(Operator op, String id){
		boolean ok = false;
		for (Operator s : Operator.STRING_OPS){
			if (s.equals(op)){
				ok = true;
				break;
			}
		}
		if (!ok){
			throw new RuntimeException("String operator not supported for survey id filter."); //$NON-NLS-1$
		}
		return new SurveyIdFilter(op, id);
	}
	
	private Operator op;
	private String value;
	
	public SurveyIdFilter(Operator op, String value){
		this.op = op;
		this.value = value;
	}
	
	@Override
	public String asString() {
		return "survey:id " + op.asSmartValue() + " \"" + value + "\"";
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		return null;
	}

}
