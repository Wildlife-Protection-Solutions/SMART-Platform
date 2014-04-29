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

import org.hibernate.Session;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;


/**
 * A not filter expression of the form:
 * <p>
 * NOT <Filter>
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class NotExpression implements IFilter {

	
	/**
	 * Creates a new not expression 
	 * @param filter the not filter
	 * @return
	 */
	public static NotExpression createNotExpression(IFilter filter){
		return new NotExpression(filter);
	}
	
	private IFilter filter;
	
	/**
	 * @param filter the not expression
	 */
	public NotExpression(IFilter filter){
		this.filter = filter;
	}

	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return "NOT " + filter.asString(); //$NON-NLS-1$
	}
	
	@Override
	public void accept(IFilterVisitor visitor) {
		filter.accept(visitor);
		visitor.visit(this);
	}
	
	public IFilter getFilter(){
		return this.filter;
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		DropItem[] its1 = filter.getDropItems(session);
		
		DropItem[] results = new DropItem[its1.length + 1];
		for (int i = 0; i < its1.length; i ++){
			results[i+1] = its1[i];
		}
		results[0] =  BasicDropItemFactory.createNotDropItem();
		
		return results;
	}

}
