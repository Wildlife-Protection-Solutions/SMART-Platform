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

import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.SharedUtils;

/**
 * A patrol filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolFilter implements IFilter {

	
	/**
	 * Creates a patrol filter
	 * 
	 * @param key patrol filter key
	 * @param op patrol filter operator 
	 * @param value patrol filter value
	 * @return
	 */
	public static PatrolFilter createStringFilter(String key, Operator op, Object value){
		return new PatrolFilter(key, op, value);
	}
	
	/**
	 * Creates a patrol filter for a boolean patrol filter option
	 * 
	 * @param key the patrol key 
	 * @return
	 */
	public static PatrolFilter createBooleanFilter(String key){
		return new PatrolFilter(key);
	}
	

	private String patrolKey = null;
	private Operator op= null;
	private Object value= null;
	private PatrolQueryOption option ;

	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol key
	 * @param op operator
	 * @param value filter value
	 */
	public PatrolFilter (String patrolKey, Operator op, Object value){
		this(patrolKey);
		this.op = op;
		this.value = value;
	}
	
	
	/**
	 * Creates a new patrol filter
	 * @param patrolKey patrol filter key
	 */
	private PatrolFilter (String patrolKey){
		this.patrolKey = patrolKey;
		
		String patrolItem = patrolKey.split(":")[1]; //$NON-NLS-1$
        option = PatrolQueryOptions.findPatrolQueryOption(patrolItem);
	}
	
	/**
	 * 
	 * @return the patrol option represented by this filter
	 */
	public PatrolQueryOption getPatrolOption(){
		return option;
	}
	/**
	 * @see org.wcs.smart.patrol.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		if (value == null){
			return patrolKey;
		}else{
			return patrolKey + " " + op.asSmartValue() + " " + value;  //$NON-NLS-1$  //$NON-NLS-2$
		}
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
