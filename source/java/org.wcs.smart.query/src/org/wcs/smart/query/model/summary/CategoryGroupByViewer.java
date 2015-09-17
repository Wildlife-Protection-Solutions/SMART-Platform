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
package org.wcs.smart.query.model.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

/**
 * Class that represents a category
 * group by class for a summary group by.
 *  
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CategoryGroupByViewer extends AbstractGroupByViewer<CategoryGroupBy> {
	
	public CategoryGroupByViewer(CategoryGroupBy gb) {
		super(gb);
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories
		String[] filterHkeys = groupBy.getFilterKeys();
		int treeLevel = groupBy.getTreeLevel();
		
		//find all categories with treeLevel + 1 . in them  
		List<ListItem> items = new ArrayList<ListItem>();
		if (filterHkeys != null && filterHkeys.length > 0){
			for (int i = 0; i < filterHkeys.length; i++){
				Category cat = QueryDataModelManager.getInstance().getCategory(session, filterHkeys[i]);
				if (cat == null){
					throw new IllegalStateException(MessageFormat.format(Messages.CategoryGroupBy_CategoryNotFound, new Object[]{filterHkeys[i]}));
				}
				items.add( new ListItem(null, cat.getFullCategoryName(), cat.getHkey()) );		
			}
		}else{
			for(Category child : QueryDataModelManager.getInstance().getCategories(session, treeLevel)){
				items.add(new ListItem(null, child.getFullCategoryName(), child.getHkey()));
			}
		}
		
		return items;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception {
		String[] filterHkeys = groupBy.getFilterKeys();
		int treeLevel = groupBy.getTreeLevel();
		try{
			DropItem it = BasicDropItemFactory.INSTANCE.createCategoryGroupByDropItem(treeLevel);
			if (filterHkeys != null){
				ArrayList<ListItem> inits = new ArrayList<ListItem>();
				for (int i = 0; i < filterHkeys.length; i ++){
					Category child = QueryDataModelManager.getInstance().getCategory(session, filterHkeys[i]);
					if (child == null){
						throw new Exception(MessageFormat.format(Messages.CategoryGroupBy_CategoryNotFoundError, new Object[]{filterHkeys[i]}));
					}
					inits.add(new ListItem(null, child.getName(), filterHkeys[i]));
				}
				it.initializeData(inits);
			}
			return it;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
	}
}
