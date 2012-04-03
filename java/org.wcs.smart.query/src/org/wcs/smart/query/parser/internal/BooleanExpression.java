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
public class BooleanExpression implements Filter{

	private Filter e1;
	private Filter e2;
	private BooleanOperator op;

	public BooleanExpression(Filter e1, Filter e2, BooleanOperator op){
		this.e1 = e1;
		this.op = op;
		this.e2 = e2;
	}
	
	public static BooleanExpression create(Filter e1, Filter e2, BooleanOperator op){
		return new BooleanExpression(e1, e2, op);
	}
	
	public String asString(){
		return e1.asString() + " " + op.asString() + " " +e2.asString() ;
	}
	
	public String asHql(HashMap<Class<?>, String> tableMapping, HashMap<String, Object> parameters){
		return e1.asHql(tableMapping, parameters) + " " + op.asString() + " " + e2.asHql(tableMapping, parameters);
	}
	
	
	public String asSql(HashMap<Class<?>, String> tableMapping){
		return e1.asSql(tableMapping) + " " + op.asString() + " " + e2.asSql(tableMapping);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return e1.hasAttributeTreeItemFilter() | e2.hasAttributeTreeItemFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return e1.hasAttributeTreeItemFilter() | e2.hasAttributeTreeItemFilter();
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return e1.hasEmployeeFilter() | e2.hasEmployeeFilter();
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return e1.hasCategoryFilter() || e2.hasCategoryFilter();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return e1.hasAttributeFilter() || e2.hasAttributeFilter();
	}
}

