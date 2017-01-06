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
package org.wcs.smart.i2.query.observation.filter;

import org.wcs.smart.i2.query.Operator;

/**
 * Boolean filter for two expressions with operator.
 * 
 * @author Emily
 *
 */
public class BooleanFilter implements IQueryFilter {
	
	private IQueryFilter e1;
	private IQueryFilter e2;
	private Operator op;

	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 * @return
	 */
	public static BooleanFilter create(IQueryFilter e1, IQueryFilter e2, Operator op){
		return new BooleanFilter(e1, e2, op);
	}
	
	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 */
	private BooleanFilter(IQueryFilter e1, IQueryFilter e2, Operator op){
		assert op == Operator.AND || op == Operator.OR;
		this.e1 = e1;
		this.op = op;
		this.e2 = e2;
	}

	public IQueryFilter getFilter1(){
		return this.e1;
	}
	
	public IQueryFilter getFilter2(){
		return this.e2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	@Override
	public void accept(IFilterVisitor visitor){
		e1.accept(visitor);
		visitor.visitElement(this);
		e2.accept(visitor);
	}
}

