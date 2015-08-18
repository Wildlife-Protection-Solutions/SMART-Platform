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
package org.wcs.smart.patrol.query.parser.internal.filter;

import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * Patrol uuid filter.  This is not available as
 * a drop item.
 * 
 * @author Emily
 *
 */
public class PatrolUuidFilter implements IFilter {

	
	/**
	 * Creates a patrol filter
	 * 
	 * @param op patrol filter operator
	 * @param value patrol filter value
	 * @return
	 */
	public static PatrolUuidFilter createStringFilter(Operator op, Object value){
		if (op.equals(Operator.STR_EQUALS)){
			return new PatrolUuidFilter(op, value);
		}
		throw new RuntimeException("Operator not supported for patrol uuid filter (only equals supported)."); //$NON-NLS-1$
	}

	private Operator op= null;
	private Object value= null;

	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol key
	 * @param op operator
	 * @param value filter value
	 */
	public PatrolUuidFilter (Operator op, Object value){
		this.op = op;
		this.value = value;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return "patrol:uuid " + op.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$
	}
	
	
	/**
	 * <p>Quotes have been removed</p>
	 * @return the filter value as a string
	 */
	public String getValue(){
		return  SharedUtils.stripQuotes((String)value) ;
	}
	
	public Operator getOperator(){
		return this.op;
	}
	
	/**
	 * The value to set the filter to (without quotes).
	 * @param value
	 */
	public void setValue(String value){
		this.value = "\"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);		
	}
}
