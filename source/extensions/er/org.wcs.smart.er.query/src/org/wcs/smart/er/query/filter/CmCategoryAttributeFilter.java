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
package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.AttributeDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.AttributeTreeDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;


/**
 * A filter for a configurable model attribute
 * <p>
 * Of the form:<br>
 * "category:<hkey>:cmattribute:<type>:<uuid>:<key>"
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CmCategoryAttributeFilter implements IFilter{

	private CategoryFilter categoryFilter;
	private AttributeFilter attributeFilter;
	private byte[] cmAttributeUuid;
	
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the category attribute identifier in the form "category:<key>:cmattribute:l:<key>:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 * @throws Exception 
	 */
	public static CmCategoryAttributeFilter createListItemFilter(String catAtributeIdentifier, Operator op, String attributeItemKey) {
		String bits[] = catAtributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[5]; //$NON-NLS-1$ //$NON-NLS-2$
		byte[] cmAttributeUuid = null;
		try{
			cmAttributeUuid = SmartUtils.decodeHex(bits[4]);
		}catch (Exception ex){
			//this should never happen if created correctly
		}
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createListItemFilter(attPart, op, attributeItemKey);
		return new CmCategoryAttributeFilter(cmAttributeUuid, cat, att);
	}
	
//	/**
//	 * Creates a new list item attribute filter 
//	 * @param attributeIdentifier the attribute identifier in the form "attribute:t:<hkey>"
//	 * @param op the list operator
//	 * @param attributeItemKey the tree item hkey
//	 * @return
//	 */
//	public static CmCategoryAttributeFilter createTreeItemFilter(String catAtributeIdentifier, Operator op, String attributeItemKey){
//		String bits[] = catAtributeIdentifier.split(":"); //$NON-NLS-1$
//		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
//		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
//		
//		CategoryFilter cat = CategoryFilter.createFilter(catPart);
//		AttributeFilter att = AttributeFilter.createTreeItemFilter(attPart, op, attributeItemKey);
//		return new CmCategoryAttributeFilter(cat, att);
//	}
	
	
	public CmCategoryAttributeFilter(byte[] cmAttribute, CategoryFilter cat, AttributeFilter att){
		this.categoryFilter = cat;
		this.attributeFilter = att;
		this.cmAttributeUuid = cmAttribute;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return categoryFilter.asString() + ":" + attributeFilter.asString(); //$NON-NLS-1$
	}
	
	/**
	 * visits the category filter, the attribute filter then this filter
	 */
	@Override
	public void accept(IFilterVisitor visitor) {
		categoryFilter.accept(visitor);
		attributeFilter.accept(visitor);
		visitor.visit(this);
	}
	
	/**
	 * The associated category filters
	 * @return
	 */
	public CategoryFilter getCategoryFilter(){
		return this.categoryFilter;
	}

	/**
	 * 
	 * @return the associated attribute filter
	 */
	public AttributeFilter getAttributeFilter(){
		return this.attributeFilter;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 * 
	 * @return {@link AttributeDropItem} or {@link AttributeListDropItem} 
	 * or {@link AttributeTreeDropItem} depending on 
	 * attribute type.
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		Category c = null;
		Attribute att = null;
		
		try{
			c = categoryFilter.getCategory(session);
			att = attributeFilter.getAttribute(session);

			CmAttribute node = (CmAttribute) session.load(CmAttribute.class, cmAttributeUuid);
			
			boolean found = false;
			for (Attribute a : QueryDataModelManager.getInstance().getAttributes(session, c.getHkey())){
				if (a.getKeyId().equals(att.getKeyId())){
					found = true;
					break;
				}
			}
			if (!found){
				throw new Exception(MessageFormat.format(Messages.CmCategoryAttributeFilter_invalidCatAttribute, new Object[]{c.getKeyId(), att.getKeyId()}));
			}
		
			DropItem it = null;
			if (node != null){
				it = SurveyDropItemFactory.INSTANCE.createCmAttribute(node);
			}else{
				//for whatever reason this configurable model node doesn't exist anymore but the category/attribute combination does so lets just create
				//a normal category/attribute drop item
				CategoryAttribute ca = new CategoryAttribute(c,  att);
				it = BasicDropItemFactory.INSTANCE.createAttributeDropItem(ca);
			}
			attributeFilter.initDropItem(it, session);
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
}
