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

import org.wcs.smart.query.model.filter.IGroupByVisitor;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Visitor for group bys that determine if categories or attributes
 * are used in any group by clause.
 * 
 * @author Emily
 *
 */
public class HasObservationGroupByVisitor implements IGroupByVisitor {


	protected boolean hasCategory = false;
	protected boolean hasAttribute = false;
	
	@Override
	public void visit(IGroupBy item) {
		if (hasCategory && hasAttribute) return;
		if (item instanceof CategoryGroupBy){
			hasCategory = true;
		}else if (item instanceof AttributeGroupBy){
			hasAttribute = true;
			if (((AttributeGroupBy) item).getCategoryHkey() != null){
				hasCategory = true;
			}
		}		
	}
	
	/**
	 * 
	 * @return true if group by contains a category
	 */
	public boolean hasCategory(){
		return this.hasCategory;
	}
	
	/**
	 * 
	 * @return true if group by contains a attribute
	 */
	public boolean hasAttribute(){
		return this.hasAttribute;
	}

}
