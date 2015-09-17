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
package org.wcs.smart.query.model.filter;

import java.util.Locale;

import org.wcs.smart.SmartContext;


/**
 * Filter expression operator
 * 
 * @author Emily
 * @since 1.0.0
 */
public enum Operator {

	/**
	 * Equals (=) operator 
	 */
	EQUALS("="), //$NON-NLS-1$
	/**
	 * Less Than (<) operator 
	 */
	LESSTHAN("<"), //$NON-NLS-1$
	/**
	 * Less Than Equals (<=) operator 
	 */
	LESSTHANEQUALS("<="), //$NON-NLS-1$
	/**
	 * Great Than (>) operator 
	 */
	GREATERTHAN(">"), //$NON-NLS-1$
	/**
	 * Great Than Equals (>=) operator 
	 */
	GREATERTHANEQUALS(">="), //$NON-NLS-1$
	/**
	 * Not Equals (!=) operator 
	 */
	NOTEQUALS("!="), //$NON-NLS-1$
	/**
	 * String like operator 
	 */
	STR_EQUALS("equals"), //$NON-NLS-1$
	/**
	 * String contains operator 
	 */
	STR_CONTAINS("contains"), //$NON-NLS-1$
	/**
	 * String not contains operator
	 */
	STR_NOTCONTAINS("notcontains"), //$NON-NLS-1$
	
	/**
	 * The between operator
	 */
	BETWEEN("between"), //$NON-NLS-1$
	
	/**
	 * The not between operator
	 */
	NOT_BETWEEN("not between"), //$NON-NLS-1$
	
	/**
	 * Boolean and operator
	 */
	AND("and"), //$NON-NLS-1$
	/**
	 * Boolean or operator
	 */
	OR("or"), //$NON-NLS-1$
	/**
	 * Boolean not operator
	 */
	NOT("not"), //$NON-NLS-1$

	/**
	 * Brackets items 
	 */
	BRACKETS("()"); //$NON-NLS-1$ 
	/**
	 * Valid numeric operators
	 */
	public static Operator[] NUMERIC_OPS = {EQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, NOTEQUALS};
	/**
	 * Valid string operators
	 */
	public static Operator[] STRING_OPS = {STR_EQUALS, STR_CONTAINS, STR_NOTCONTAINS};
	/**
	 * Valid boolean operators
	 */
	public static Operator[] BOOLEANS_OPS = {AND, OR, NOT};
	/**
	 * Valid date operators
	 */
	public static Operator[] DATE_OPS = {BETWEEN, NOT_BETWEEN};
	/**
	 * Parses an operator from the string value 
	 * @param value the string value
	 * @return the operator
	 */
	public static Operator parseOperator(String value){
		for (int i = 0; i < STRING_OPS.length; i ++){
			if (STRING_OPS[i].asSmartValue().equalsIgnoreCase(value)){
				return STRING_OPS[i];
			}
		}
		for (int i = 0; i < NUMERIC_OPS.length; i ++){
			if (NUMERIC_OPS[i].asSmartValue().equalsIgnoreCase(value)){
				return NUMERIC_OPS[i];
			}
		}
		if (value.equalsIgnoreCase("<>")){ //$NON-NLS-1$
			return NOTEQUALS;
		}
		for (int i = 0; i < BOOLEANS_OPS.length; i ++){
			if (BOOLEANS_OPS[i].asSmartValue().equalsIgnoreCase(value)){
				return BOOLEANS_OPS[i];
			}
		}
		for (int i = 0; i < DATE_OPS.length; i ++){
			if (DATE_OPS[i].asSmartValue().equalsIgnoreCase(value)){
				return DATE_OPS[i];
			}
		}
		return null;
	}
	
	private String smartValue;
	
	/**
	 * @param guiValue the operator gui value
	 * @param sqlOperator the sql operator
	 */
	Operator(String smartValue){
		this.smartValue = smartValue;
	}

	/**
	 * @return gui representation of operator
	 */
	public String getGuiValue(){
		return getGuiValue(Locale.getDefault());
	}
	
	/**
	 * @return gui representation of operator
	 */
	public String getGuiValue(Locale l){
		return SmartContext.INSTANCE.getClass(IOperatorLabelProvider.class).getLabel(this, l);
	}
	
	/**
	 * @return the smart query representation of the operator
	 */
	public String asSmartValue(){
		return this.smartValue;
	}
	
}
