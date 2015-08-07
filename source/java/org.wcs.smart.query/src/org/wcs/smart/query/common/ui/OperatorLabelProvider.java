/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.query.common.ui;

import java.util.Locale;

import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IOperatorLabelProvider;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Operator label provider implementation.
 * @author Emily
 *
 */
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
				case BRACKETS:{ return "( )"; } //$NON-NLS-1$
			}
		}
		return null;
	}

}
