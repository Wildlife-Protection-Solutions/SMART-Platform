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
package org.wcs.smart.query.parser.internal;

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
	public static Operator EQUALS = new Operator("=", "=");
	/**
	 * Less Than (<) operator 
	 */
	public static Operator LESSTHAN = new Operator("<", "<");
	/**
	 * Less Than Equals (<=) operator 
	 */
	public static Operator LESSTHANEQUALS = new Operator("<=", "<=");
	/**
	 * Great Than (>) operator 
	 */
	public static Operator GREATERTHAN = new Operator(">", ">");
	/**
	 * Great Than Equals (>=) operator 
	 */
	public static Operator GREATERTHANEQUALS = new Operator(">=", ">=");
	/**
	 * Not Equals (!=) operator 
	 */
	public static Operator NOTEQUALS = new Operator("!=", "<>");
	/**
	 * String like operator 
	 */
	public static Operator STR_EQUALS = new Operator("equals", "=");
	/**
	 * String contains operator 
	 */
	public static Operator STR_CONTAINS = new Operator("contains", "like");
	/**
	 * String not contains operator
	 */
	public static Operator STR_NOTCONTAINS = new Operator("not contains", "not like");
	
	/**
	 * Valid numeric operators
	 */
	public static Operator[] NUMERIC_OPS = {EQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, NOTEQUALS};
	/**
	 * Valid string operators
	 */
	public static Operator[] STRING_OPS = {STR_EQUALS, STR_CONTAINS, STR_NOTCONTAINS};
	
	/**
	 * Parses an operator from the string value 
	 * @param value the string value
	 * @return the operator
	 */
	public static Operator parseOperator(String value){
		if (value.equals("=")) return EQUALS;
		if (value.equals("<")) return LESSTHAN;
		if (value.equals("<=")) return LESSTHANEQUALS;
		if (value.equals(">")) return GREATERTHAN;
		if (value.equals(">=")) return GREATERTHANEQUALS;
		if (value.equals("!=")) return NOTEQUALS;
		if (value.equals("<>")) return NOTEQUALS;
		
		if (value.equals("equals")) return STR_EQUALS;
		if (value.equals("contains")) return STR_CONTAINS;
		if (value.equals("notcontains")) return STR_NOTCONTAINS;
		
		return null;
	}
	
	private String guiValue;
	private String sqlOperator;
	
	/**
	 * @param guiValue the operator gui value
	 * @param sqlOperator the sql operator
	 */
	public Operator(String guiValue, String sqlOperator){
		this.guiValue = guiValue;
		this.sqlOperator = sqlOperator;
	}

	/**
	 * @return gui representation of operator
	 */
	public String getGuiValue(){
		return this.guiValue;
	}
	
	/**
	 * @return the sql operator
	 */
	public String asSql(){
		return sqlOperator;
	}
	
	/**
	 * @return string representation of operator
	 */
	public String asString(){
		return sqlOperator;
	}
}
