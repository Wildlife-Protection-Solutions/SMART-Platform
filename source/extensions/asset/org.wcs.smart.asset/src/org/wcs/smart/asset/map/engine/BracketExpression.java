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
 * Bracket filter for column in the asset overview map.  Bracket
 * filter is represented by an open bracket and expression and a closed
 * bracket.
 * 
 * @author Emily
 *
 */
public class BracketExpression implements IExpression{
	
	public static BracketExpression parse(IExpression exp) {
		return new BracketExpression(exp);
	}
	
	private IExpression filter1;
	
	public BracketExpression(IExpression exp) {
		this.filter1 = exp;
	}
	
	public IExpression getFilter() {
		return this.filter1;
	}
	
	@Override
	public String toString() {
		return " ( " + filter1.toString() + " ) "; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public void accept(IExpressionVisitor visitor) {
		filter1.accept(visitor);
		visitor.visit(this);
	}
}
