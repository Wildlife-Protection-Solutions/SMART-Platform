/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

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
				case EQUALS:{ return "=";}
				case LESSTHAN:{ return "<";}
				case LESSTHANEQUALS:{ return "<=";}
				case GREATERTHAN:{ return ">";}
				case GREATERTHANEQUALS:{ return ">=";}
				case NOTEQUALS:{ return "!=";}
				case STR_EQUALS:{ return "equals";}
				case STR_CONTAINS:{ return "contains";}
				case STR_NOTCONTAINS:{ return "not contains";}
				case BETWEEN:{ return "Between";}
				case NOT_BETWEEN:{ return "Not Between";}
				case AND:{ return "AND";}
				case OR:{ return "OR";}
				case NOT:{ return "NOT";}
				case BRACKETS:{ return "( )"; } //$NON-NLS-1$
			}
		}
		return null;
	}

}
