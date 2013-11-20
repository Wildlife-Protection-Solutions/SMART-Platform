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

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.CategoryDropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

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
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString(){
		return categoryIdentifier;
	}
	
	public String getCategoryKey(){
		return categoryIdentifier.split(":")[1]; //$NON-NLS-1$
	}

	/**
	 * @return {@link CategoryDropItem} or {@link ErrorDropItem} if 
	 * category cannot be found.
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		DropItem it = null;
		try{
			Category cat = getCategory(session);
			it = BasicDropItemFactory.INSTANCE.createCategoryDropItem(cat);
		}catch (Exception ex){
			it = new ErrorDropItem(ex.getMessage());
		}
		
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
		Category cat = QueryDataModelManager.getInstance().getCategory(session, keyPart);
		if (cat == null){
			throw new Exception(MessageFormat.format(Messages.CategoryFilter_CategoryNotFound, new Object[]{keyPart}));
		}
		return cat;
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}
}
