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

import java.util.HashMap;
import java.util.HashSet;

/**
 * Boolean filter expression.
 * <p>
 * Filter takes the form:
 * <p>
 * BooleanExpression = Filter BooleanOperator Filter
 * </p>
 * <p>Where BooleanOperator: {AND | OP }</p>
 *  
 * @author Emily
 * @since 1.0.0
 */
public class BooleanExpression implements Filter{

	private Filter e1;
	private Filter e2;
	private BooleanOperator op;

	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 * @return
	 */
	public static BooleanExpression create(Filter e1, Filter e2, BooleanOperator op){
		return new BooleanExpression(e1, e2, op);
	}
	
	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 */
	private BooleanExpression(Filter e1, Filter e2, BooleanOperator op){
		this.e1 = e1;
		this.op = op;
		this.e2 = e2;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#asString()
	 */
	@Override
	public String asString(){
		return e1.asString() + " " + op.asSql() + " " +e2.asString() ;
	}

	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping){
		return e1.asSql(tableMapping) + " " + op.asSql() + " " + e2.asSql(tableMapping);
	}
	
	/**	
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return e1.hasEmployeeFilter() || e2.hasEmployeeFilter();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return e1.hasCategoryFilter() || e2.hasCategoryFilter();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return e1.hasAttributeFilter() || e2.hasAttributeFilter();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.Filter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		e1.getAttributeFilters(attributes);
		e2.getAttributeFilters(attributes);
	}
}

