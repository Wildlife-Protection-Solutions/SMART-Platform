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

import org.wcs.smart.query.internal.Messages;

/**
 * Filter expression operator
 * 
 * @author Emily
 * @since 1.0.0
 */
public class Operator {

	/**
	 * Equals (=) operator 
	 */
	public static Operator EQUALS = new Operator(Messages.Operator_Equals, "="); //$NON-NLS-1$
	/**
	 * Less Than (<) operator 
	 */
	public static Operator LESSTHAN = new Operator(Messages.Operator_LessThan, "<"); //$NON-NLS-1$
	/**
	 * Less Than Equals (<=) operator 
	 */
	public static Operator LESSTHANEQUALS = new Operator(Messages.Operator_LessThanEqual, "<="); //$NON-NLS-1$
	/**
	 * Great Than (>) operator 
	 */
	public static Operator GREATERTHAN = new Operator(Messages.Operator_GreaterThan, ">"); //$NON-NLS-1$
	/**
	 * Great Than Equals (>=) operator 
	 */
	public static Operator GREATERTHANEQUALS = new Operator(Messages.Operator_GreaterThanEqual, ">="); //$NON-NLS-1$
	/**
	 * Not Equals (!=) operator 
	 */
	public static Operator NOTEQUALS = new Operator(Messages.Operator_NotEqual, "!="); //$NON-NLS-1$
	/**
	 * String like operator 
	 */
	public static Operator STR_EQUALS = new Operator(Messages.Operator_StrEquals, "equals"); //$NON-NLS-1$
	/**
	 * String contains operator 
	 */
	public static Operator STR_CONTAINS = new Operator(Messages.Operator_StrContains, "contains"); //$NON-NLS-1$
	/**
	 * String not contains operator
	 */
	public static Operator STR_NOTCONTAINS = new Operator(Messages.Operator_StrNotContains, "notcontains"); //$NON-NLS-1$
	
	/**
	 * Boolean and operator
	 */
	public static Operator AND = new Operator(Messages.Operator_AND, "and"); //$NON-NLS-1$
	/**
	 * Boolean or operator
	 */
	public static Operator OR = new Operator(Messages.Operator_OR, "or"); //$NON-NLS-1$
	/**
	 * Boolean not operator
	 */
	public static Operator NOT = new Operator(Messages.Operator_NOT, "not"); //$NON-NLS-1$

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
		return null;
	}
	
	private String guiValue;
	private String smartValue;
	
	/**
	 * @param guiValue the operator gui value
	 * @param sqlOperator the sql operator
	 */
	public Operator(String guiValue, String smartValue){
		this.guiValue = guiValue;
		this.smartValue = smartValue;
	}

	/**
	 * @return gui representation of operator
	 */
	public String getGuiValue(){
		return this.guiValue;
	}
	
	/**
	 * @return the smart query representation of the operator
	 */
	public String asSmartValue(){
		return this.smartValue;
	}
	
}
