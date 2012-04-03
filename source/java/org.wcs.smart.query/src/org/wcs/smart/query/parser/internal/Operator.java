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
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class Operator {

	private String guiValue;
	private String sqlOperator;
	
	public Operator(String guiValue, String sqlOperator){
		this.guiValue = guiValue;
		this.sqlOperator = sqlOperator;
	}

	public String asString(){
		return guiValue;
	}

	public String asHql(){
		return sqlOperator;
	}
	public String asSql(){
		return sqlOperator;
	}
	
	public static Operator EQUALS = new Operator("=", "=");
	public static Operator LESSTHAN = new Operator("<", "<");
	public static Operator LESSTHANEQUALS = new Operator("<=", "<=");
	public static Operator GREATERTHAN = new Operator(">", ">");
	public static Operator GREATERTHANEQUALS = new Operator(">=", ">=");
	public static Operator NOT_EQUALS = new Operator("!=", "<>");
	
	public static Operator STR_EQUALS = new Operator("equals", "like");
	public static Operator STR_CONTAINS = new Operator("contains", "like");
	public static Operator STR_NOTCONTAINS = new Operator("notcontains", "not like");
	
	public static Operator parseOperator(String value){
		if (value.equals("=")) return EQUALS;
		if (value.equals("<")) return LESSTHAN;
		if (value.equals("<=")) return LESSTHANEQUALS;
		if (value.equals(">")) return GREATERTHAN;
		if (value.equals(">=")) return GREATERTHANEQUALS;
		if (value.equals("!=")) return NOT_EQUALS;
		if (value.equals("<>")) return NOT_EQUALS;
		
		if (value.equals("equals")) return STR_EQUALS;
		if (value.equals("contains")) return STR_CONTAINS;
		if (value.equals("notcontains")) return STR_NOTCONTAINS;
		
		return null;
	}
}
