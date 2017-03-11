/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;

/**
 * Query operators.
 * 
 * @author Emily
 *
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
	BRACKETS("()"), //$NON-NLS-1$
	
	/**
	 * Open Bracket
	 */
	BRACKET_OPEN("("), //$NON-NLS-1$ 
	
	/**
	 * Close bracket
	 */
	BRACKET_CLOSE(")"); //$NON-NLS-1$ 
	/**
	 * Valid numeric operators
	 */
	public static Operator[] NUMERIC_OPS = {EQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, NOTEQUALS};
	/**
	 * Valid string operators
	 */
	public static Operator[] STRING_OPS = {STR_EQUALS, STR_CONTAINS, STR_NOTCONTAINS};
	/**
	 * Valid date operators
	 */
	public static Operator[] DATE_OPS = {BETWEEN, NOT_BETWEEN};
	/**
	 * Valid boolean operators
	 */
	public static Operator[] BOOLEANS_OPS = {AND, OR, NOT};
	
	private String key;
	
	private Operator(String key){
		this.key = key;
	}
	
	public String getLabel(Locale l){
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
	}
	
	public String getKey(){
		return this.key;
	}
	
	public static Operator parse(String op){
		for (Operator o : Operator.values()){
			if (op.equalsIgnoreCase(o.key)) return o;
		}
		throw new IllegalStateException("Operator not supported: " + op); //$NON-NLS-1$
	}
}


