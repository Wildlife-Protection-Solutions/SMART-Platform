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
package org.wcs.smart.query.parser.internal.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.ui.formulaDnd.BracketDropItem.BracketType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;

/**
 * A bracketed expression.  Of the form:
 * ( Filter )
 * 
 * @author Emily
 * @since 1.0.0
 */
public class BracketFilter implements IFilter{

	private IFilter filter;
	
	/**
	 * Creates new bracket filter expression
	 * 
	 * @param f bracketed expression 
	 * @return
	 */
	public static BracketFilter createFilter(IFilter f){
		return new BracketFilter(f);
	}
	
	
	/**
	 * Creates new bracket filter expression
	 * 
	 * @param f bracketed expression
	 */
	private BracketFilter(IFilter filter){
		this.filter = filter;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return "(" + filter.asString() + ")";
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping){
		return "(" + filter.asSql(tableMapping) + ")";
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return filter.hasEmployeeFilter();
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return filter.hasCategoryFilter();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return filter.hasAttributeFilter();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		filter.getAttributeFilters(attributes);
	}
	
	public DropItem[] getDropItems(Session session) throws Exception{
		DropItem[] its1 = filter.getDropItems(session);
		
		DropItem[] results = new DropItem[its1.length + 2];
		for (int i = 0; i < its1.length; i ++){
			results[i+1] = its1[i];
		}
		results[0] = DropItemFactory.INSTANCE.createOtherSingleBracketDropItem(BracketType.OPEN);
		results[results.length - 1] = DropItemFactory.INSTANCE.createOtherSingleBracketDropItem(BracketType.CLOSE);
		
		return results;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		List<IFilter> kids = new ArrayList<IFilter>();
		kids.add(filter);
		return kids;
	}
}
