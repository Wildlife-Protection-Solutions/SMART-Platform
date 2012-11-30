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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;

/**
 * A data model category filter. Of the form<br>
 * category:<category_hkey>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CategoryFilter implements IFilter {

	private String categoryIdentifier;  //category:category_hkey
	
	/**
	 * Creates new category filter
	 * @param categoryIdentifier the category key part of the form "category:<categoryhkey>"
	 * @return
	 */
	public static CategoryFilter createFilter(String categoryIdentifier){
		return new CategoryFilter(categoryIdentifier);
	}
	
	/**
	 * Creates new category filter
	 * @param categoryIdentifier the category key part of the form "category:<categoryhkey>"
	 */
	public CategoryFilter(String categoryIdentifier){
		this.categoryIdentifier = categoryIdentifier;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return categoryIdentifier;
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		String keyPart = categoryIdentifier.split(":")[1]; //$NON-NLS-1$
		
		String prefix = tableMapping.get(Category.class);
		if (prefix == null){
			throw new IllegalStateException(Messages.CategoryFilter_InvalidPrefix);
		}
		
		return "( " + prefix + ".hkey >= '" + keyPart + "' and " + prefix + ".hkey < '" + keyPart.substring(0,  keyPart.length() -1) + "/') "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ 
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * 
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		Category cat = getCategory(session);
		DropItem it = DropItemFactory.INSTANCE.createCategoryDropItem(cat);
		return new DropItem[]{it};
	}
	
	/**
	 * Loads the full category item from the database.
	 * 
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public Category getCategory(Session session) throws Exception{
		String keyPart = categoryIdentifier.split(":")[1]; //$NON-NLS-1$
		Category cat = QueryHibernateManager.getCategory(session, keyPart);
		if (cat == null){
			throw new Exception(MessageFormat.format(Messages.CategoryFilter_CategoryNotFound, new Object[]{keyPart}));
		}
		return cat;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
}
