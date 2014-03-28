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
import org.wcs.smart.query.model.filter.IGroupByVisitor;
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
public class CategoryGroupBy implements IGroupBy {

	/**
	 * Creates a new category group by of the form:
	 *  <  CATEGORY_GROUP_BY : "category:" < DM_KEY > ":" ( < DM_KEY > ":")* >
	 *  <p>The first DM_KEY is the parent class category hkey.  The
	 *  remaining hkeys must be direct children of the parent
	 *  hkey and are used to filter the results</p>
	 * 
	 * @param key
	 * @return
	 */
	public final static CategoryGroupBy createGroupBy(String key){
		return new CategoryGroupBy(key);
	}
	
	//private String categoryHkey = null;
	private int treeLevel = 0;
	private String[] filterHkeys = null;
	
	/**
	 * @param key
	 */
	protected CategoryGroupBy(String key){
		String bits[] = key.split(":"); //$NON-NLS-1$
//		this.categoryHkey = bits[1];
		this.treeLevel = Integer.parseInt(bits[1]);
		if (bits.length - 2 > 0){
			filterHkeys = new String[bits.length-2];
			for (int i = 2; i < bits.length; i ++){
				filterHkeys[i-2] = bits[i];
			}
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getKeyPart()
	 */
	public String getKeyPart(){
		StringBuilder sb = new StringBuilder();
		sb.append("category:"); //$NON-NLS-1$
//		sb.append(categoryHkey);
		sb.append(treeLevel);
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getKeyPart());
		sb.append(":"); //$NON-NLS-1$
		if (filterHkeys != null){
			for (int i =0; i < filterHkeys.length; i ++){
				sb.append(filterHkeys[i]);
				if (i < filterHkeys.length-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * @return the tree level category
	 */
	public int getTreeLevel(){
		return this.treeLevel;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IGroupBy#getType()
	 */
	public GroupByType getType(){
		return GroupByType.KEY;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.wp.sum.IGroupBy#getItems(org.hibernate.Session)
	 */
	@Override
	public List<ListItem> getItems(Session session) {
		//get children categories

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
	
	public void visit(IGroupByVisitor visitor){
		visitor.visit(this);
	}

	public String[] getFilterKeys(){
		return this.filterHkeys;
	}
}
