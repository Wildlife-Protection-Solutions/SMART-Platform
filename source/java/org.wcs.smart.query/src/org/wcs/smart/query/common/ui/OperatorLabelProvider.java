package org.wcs.smart.query.common.ui;

import java.util.Locale;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IOperatorLabelProvider;
import org.wcs.smart.query.model.filter.Operator;

public class OperatorLabelProvider implements IOperatorLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof Operator){
			switch((Operator)item){
				case EQUALS:{ return Messages.Operator_Equals;}
				case LESSTHAN:{ return Messages.Operator_LessThan;}
				case LESSTHANEQUALS:{ return Messages.Operator_LessThanEqual;}
				case GREATERTHAN:{ return Messages.Operator_GreaterThan;}
				case GREATERTHANEQUALS:{ return Messages.Operator_GreaterThanEqual;}
				case NOTEQUALS:{ return Messages.Operator_NotEqual;}
				case STR_EQUALS:{ return Messages.Operator_StrEquals;}
				case STR_CONTAINS:{ return Messages.Operator_StrContains;}
				case STR_NOTCONTAINS:{ return Messages.Operator_StrNotContains;}
				case BETWEEN:{ return Messages.Operator_BetweenOp;}
				case NOT_BETWEEN:{ return Messages.Operator_NotBetweenOp;}
				case AND:{ return Messages.Operator_AND;}
				case OR:{ return Messages.Operator_OR;}
				case NOT:{ return Messages.Operator_NOT;}
				case BRACKETS:{ return "( )"; }
			}
		}
		return null;
	}

}
