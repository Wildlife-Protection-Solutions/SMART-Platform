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
 * Attribute expression for asset overview map category column filter.
 * 
 * @author Emily
 *
 */
public class AttributeExpression implements IFilter{

	public static AttributeExpression parse (IFilter filter1, Operator op, IFilter filter2) {
		return new AttributeExpression(filter1, op, filter2);
	}
	
	private IFilter filter1;
	private Operator op;
	private IFilter filter2;
	
	public AttributeExpression(IFilter filter1, Operator op, IFilter filter2) {
		this.filter1 = filter1;
		this.op = op;
		this.filter2 = filter2;
	}
	
	public IFilter getFilter1() { 
		return filter1;
	}

	public IFilter getFilter2() {
		return filter2;
	}
	
	public Operator getOperator() {
		return this.op;
	}
	
	@Override
	public String toString() {
		return filter1.toString() + " " + op.operator.sql + " " + filter2.toString();
	}
	
	@Override
	public void accept(IFilterVisitor visitor) {
		filter1.accept(visitor);
		filter2.accept(visitor);
		visitor.visit(this);
	}
}
