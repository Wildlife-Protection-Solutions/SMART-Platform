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

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;

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
	protected String categoryHkey;
	protected IValueItem.ValueType type;
	
	/**
	 * Creates a new category value item.
	 * 
	 * @param key category value key
	 */
	public CategoryValueItem(String key){
		this.key = key;
		String[] bits = key.split(":"); //$NON-NLS-1$
		this.type = ValueType.OBSERVATION;
		for (ValueType vt : ValueType.values()){
			if (vt.key.endsWith(bits[2])){
				this.type = vt;
				break;
			}
		}
		if (bits.length >= 4){
			this.categoryHkey = bits[3];
		}else{
			this.categoryHkey = null;
		}
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
	 * 
	 * @return the type of category value
	 */
	public ValueType getType(){
		return this.type;
	}

	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(Session session){
		if (categoryHkey == null){
			return type.guiLabel + " " + AllCategory.INSTANCE.getName(); //$NON-NLS-1$
		}
		Category c = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
		if (c == null){
			return this.key;
		}
		return type.guiLabel + " " + c.getName(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getFullName(org.hibernate.Session)
	 */
	public String getFullName(Session session){
		if (categoryHkey == null){
			return type.guiLabel + " " + AllCategory.INSTANCE.getName(); //$NON-NLS-1$
		}
		Category c = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
		if (c == null){
			return this.key;
		}
		return type.guiLabel + " " + c.getName(); //$NON-NLS-1$
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#asDropItem(org.hibernate.Session)
	 */
	@Override
	public DropItem asDropItem(Session session) throws Exception{
		try{
			DropItem di = null;
			if (categoryHkey == null){
				di = BasicDropItemFactory.INSTANCE.createCategoryValueDropItem(null);
			}else{
				Category category = QueryDataModelManager.getInstance().getCategory(session, categoryHkey);
				if (category == null){
					throw new Exception(MessageFormat.format(Messages.CategoryValueItem_categorynotfound, new Object[]{categoryHkey}));
				}
				category.getFullCategoryName();		//cache this
				di = BasicDropItemFactory.INSTANCE.createCategoryValueDropItem(category);
			}
			di.initializeData(getDropItemInitializeData());
			return di;
		} catch (Exception ex) {
			return new ErrorDropItem(ex.getMessage());
		}
		
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getInitializeData()
	 */
	public Object getDropItemInitializeData(){
		return type.key;
	}
	
	public void accept(IValueVisitor visitor){
		visitor.visit(this);
	}
	
}
