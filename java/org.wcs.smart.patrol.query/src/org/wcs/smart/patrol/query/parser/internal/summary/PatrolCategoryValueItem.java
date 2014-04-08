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
package org.wcs.smart.patrol.query.parser.internal.summary;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * A category value item that represents computing
 * the total number of observations with the given
 * category.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolCategoryValueItem extends CategoryValueItem {
	
	/**
	 * Creates a new category value item of the form
	 * "category:sum:<hkey>"
	 * 
	 * @param key
	 * @return
	 */
	public static PatrolCategoryValueItem createItem(String key){
		return new PatrolCategoryValueItem(key);
	}
	
	
	/**
	 * Creates a new category value item.
	 * 
	 * @param key category value key
	 */
	public PatrolCategoryValueItem(String key){
		super(key);		
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		try{
			DropItem di = null;
			if (categoryHkey == null){
				di = PatrolDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
			}else{
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null){
					throw new Exception(MessageFormat.format(Messages.PatrolCategoryValueItem_CategoryNotFound, new Object[]{categoryHkey}));
				}
				category.getFullCategoryName();		//cache this
				di = PatrolDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			}
			
			di.initializeData(new Object[]{getDropItemInitializeData(), null});
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
		
	}
	
	
}
