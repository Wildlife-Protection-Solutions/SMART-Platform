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
public class BooleanExpression implements IFilter{

	private IFilter e1;
	private IFilter e2;
	private Operator op;

	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 * @return
	 */
	public static BooleanExpression create(IFilter e1, IFilter e2, Operator op){
		return new BooleanExpression(e1, e2, op);
	}
	
	/**
	 * Creates a new boolean expression 
	 * 
	 * @param e1 left expression
	 * @param e2 right expression 
	 * @param op boolean operator
	 */
	private BooleanExpression(IFilter e1, IFilter e2, Operator op){
		assert op == Operator.AND || op == Operator.OR;
		this.e1 = e1;
		this.op = op;
		this.e2 = e2;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return e1.asString() + " " + op.asSmartValue() + " " +e2.asString() ; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IFilter getFilter1(){
		return this.e1;
	}
	
	public IFilter getFilter2(){
		return this.e2;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	public void accept(IFilterVisitor visitor){
		e1.accept(visitor);
		e2.accept(visitor);
		visitor.visit(this);
	}
}

