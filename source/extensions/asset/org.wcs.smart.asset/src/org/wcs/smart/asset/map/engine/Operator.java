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

import org.wcs.smart.asset.internal.Messages;

/**
 * Various supported operators for asset overview map
 * query expressions.
 * 
 * @author Emily
 *
 */
public class Operator {

	enum Op{
		AND(Messages.Operator_And, "AND"), //$NON-NLS-1$
		OR(Messages.Operator_Or, "OR"), //$NON-NLS-1$
		LG("<", "<"), //$NON-NLS-1$ //$NON-NLS-2$
		GT(">", ">"), //$NON-NLS-1$ //$NON-NLS-2$
		LGE("<=", "<="), //$NON-NLS-1$ //$NON-NLS-2$
		GTE(">=", ">="), //$NON-NLS-1$ //$NON-NLS-2$
		EQ("=", "="), //$NON-NLS-1$ //$NON-NLS-2$
		NOTEQ("<>", "!="), //$NON-NLS-1$ //$NON-NLS-2$
		STR_EQUAL(Messages.Operator_StrEquals, "="), //$NON-NLS-1$
		STR_CONTAINS(Messages.Operator_strContains, "like"), //$NON-NLS-1$
		BEFORE(Messages.Operator_Before, "<"), //$NON-NLS-1$
		AFTER(Messages.Operator_After, ">"), //$NON-NLS-1$
		
		PLUS("+", "+"), //$NON-NLS-1$ //$NON-NLS-2$
		MINUS("-", "-"), //$NON-NLS-1$ //$NON-NLS-2$
		TIMES("*", "*"), //$NON-NLS-1$ //$NON-NLS-2$
		DIVIDE("/", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		
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
