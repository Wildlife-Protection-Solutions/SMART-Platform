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
 * Boolean operator
 * 
 * @author Emily
 * @since 1.0.0
 */
public class BooleanOperator {
	
	public static BooleanOperator AND = new BooleanOperator("AND", "AND");
	public static BooleanOperator OR = new BooleanOperator("OR", "OR");
	public static BooleanOperator NOT = new BooleanOperator("NOT", "NOT");

	
	/**
	 * Parses the boolean operator from the value 
	 * @param value the string value 
	 * @return
	 */
	public static BooleanOperator parseOperator(String value) {
		if (value.toLowerCase().equalsIgnoreCase("and"))
			return AND;
		if (value.toLowerCase().equalsIgnoreCase("or"))
			return OR;
		return null;
	}

	private String sqlOperator;
	private String guiValue ;
	
	/**
	 * Creates a new boolean operator.
	 * 
	 * @param guiValue the value to display to the user 
	 * @param sqlOperator the sql operator value 
	 */
	private BooleanOperator(String guiValue, String sqlOperator){
		this.sqlOperator = sqlOperator;
		this.guiValue = guiValue;
	}
	
	/**
	 * @return the operator gui value
	 */
	public String getGuiValue(){
		return this.guiValue;
	}
	
	/**
	 * @return the sql operator
	 */
	public String asSql(){
		return this.sqlOperator;
	}

}
