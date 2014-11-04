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
package org.wcs.smart.query.common.engine.visitors;

import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.ObserverFilter;

/**
 * Determines if an observation filter (category or attribute filter)
 * are used in the filter.
 * 
 * @author Emily
 *
 */
public class HasObservationFilterVisitor implements IFilterVisitor{

	private boolean hasCategory = false;
	private boolean hasAttribute = false;
	private boolean hasObserver = false;
	
	@Override
	public void visit(IFilter filter) {
		if (hasCategory && hasAttribute) return;
		if (filter instanceof AttributeFilter){
			hasAttribute = true;
		}else if (filter instanceof CategoryFilter){
			hasCategory = true;
		}else if (filter instanceof ObserverFilter){
			hasObserver = true;
		}
	}
	
	/**
	 * 
	 * @return true if the filter contains a category filter
	 */
	public boolean hasCategoryFilter(){
		return this.hasCategory;
	}

	/**
	 * 
	 * @return true if filter contains an attribute filter
	 */
	public boolean hasAttributeFilter(){
		return this.hasAttribute;
	}
	
	/**
	 * 
	 * @return true if observer filter
	 */
	public boolean hasObserverFilter(){
		return this.hasObserver;
	}
	
	
	/**
	 * clears the state of the visitor so it can be re-used
	 */
	public void clear(){
		hasCategory = false;
		hasAttribute = false;
		hasObserver = false;
	}
}
