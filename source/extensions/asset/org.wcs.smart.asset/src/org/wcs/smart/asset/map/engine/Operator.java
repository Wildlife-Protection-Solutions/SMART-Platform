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
package org.wcs.smart.asset.map.engine;

/**
 * Various supported operators for asset overview map
 * query expressions.
 * 
 * @author Emily
 *
 */
public class Operator {

	enum Op{
		AND("AND", "AND"),
		OR("OR", "OR"),
		LG("<", "<"),
		GT(">", ">"),
		LGE("<=", "<="),
		GTE(">=", ">="),
		EQ("=", "="),
		NOTEQ("<>", "!="),
		STR_EQUAL("equals", "="),
		STR_CONTAINS("contains", "like"),
		BEFORE("before", "<"),
		AFTER("after", ">"),
		
		PLUS("+", "+"),
		MINUS("-", "-"),
		TIMES("*", "*"),
		DIVIDE("/", "/");
		
		String key;
		String sql;
		
		Op(String key, String sql) {
			this.key = key;
			this.sql = sql;
		}
		
	}
	
	Op operator;
	
	private Operator(Op operator) {
		this.operator = operator;
	}
	
	public static Operator parse(String key){
		for (Op o : Op.values()) {
			if (o.key.equalsIgnoreCase(key)) return new Operator(o);
		}
		return null;
	}
	
}
