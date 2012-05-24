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
package org.wcs.smart.query.parser.internal.summary;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;

/**
 * A category value item that represents computing
 * the total number of observations with the given
 * category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryValueItem implements IValueItem {


	/**
	 * Creates a new category value item of the form
	 * "category:sum:<hkey>"
	 * 
	 * @param key
	 * @return
	 */
	public static CategoryValueItem createItem(String key){
		return new CategoryValueItem(key);
	}
	
	private String key;
	private String categoryHkey;
	
	/**
	 * Creates a new category value item.
	 * 
	 * @param key category value key
	 */
	public CategoryValueItem(String key){
		this.key = key;
		this.categoryHkey = key.split(":")[2];
	}
	
	/**
	 * @return the category hkey
	 */
	public String getCategoryHKey(){
		return this.categoryHkey;
	}
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asString()
	 */
	public String asString(){
		return this.key;
	}

	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(Session session){
		Category c = QueryHibernateManager.getCategory(session, categoryHkey);
		if (c == null){
			return this.key;
		}
		return "Count " +  c.getName();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) {
		Category category = QueryHibernateManager.getCategory(session, categoryHkey);
		if (category != null){
			return DropItemFactory.INSTANCE.createCategoryValueDropItem(category);
		}
		return null;
		
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#isCategory()
	 */
	public boolean isCategory(){
		return true;
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#validateDatabase(org.hibernate.Session)
	 */
	public void validateDatabase(Session session) throws Exception{
		//ensure category key exists
		QueryHibernateManager.validateCategory(categoryHkey, session);
	}
}
