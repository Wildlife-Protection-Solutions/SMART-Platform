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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.Operator;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.query.ui.formulaDnd.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;


/**
 * A filter which consists of a category and attribute.
 * <p>
 * Of the form:<br>
 * "category:<hkey>:attribute:<type>:<key>"
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CategoryAttributeFilter implements IFilter{

	private CategoryFilter categoryFilter;
	private AttributeFilter attributeFilter;
	
	
	/**
	 * 
	 */
	public static CategoryAttributeFilter createStringFilter(String catAttributeIdentifier, Operator op, String value){
		String bits[] = catAttributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		value = SmartUtils.stripQuotes(value);
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createStringFilter(attPart, op, value);
		
		return new CategoryAttributeFilter(cat, att);
	}
	
	/**
	 * Creates a new value attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:n:<key>"
	 * @param op the operator
	 * @param Double value the filter value
	 * @return
	 */
	public static CategoryAttributeFilter createValueFilter(String catAttributeIdentifier, Operator op, Double value){
		String bits[] = catAttributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createValueFilter(attPart, op, value);
		
		return new CategoryAttributeFilter(cat, att);
	}
	
	/**
	 * Creates a new boolean attribute filter
	 * @param attributeIdentifier the attribute identifier in the form "attribute:b:<key>"
	 * @return
	 */
	public static CategoryAttributeFilter createBooleanFilter(String catAtributeIdentifier){
		String bits[] = catAtributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createBooleanFilter(attPart);
		return new CategoryAttributeFilter(cat, att);
		
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:l:<key>"
	 * @param op the list operator
	 * @param attributeItemKey the list item key
	 * @return
	 */
	public static CategoryAttributeFilter createListItemFilter(String catAtributeIdentifier, Operator op, String attributeItemKey){
		String bits[] = catAtributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createListItemFilter(attPart, op, attributeItemKey);
		return new CategoryAttributeFilter(cat, att);
	}
	
	/**
	 * Creates a new list item attribute filter 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:t:<hkey>"
	 * @param op the list operator
	 * @param attributeItemKey the tree item hkey
	 * @return
	 */
	public static CategoryAttributeFilter createTreeItemFilter(String catAtributeIdentifier, Operator op, String attributeItemKey){
		String bits[] = catAtributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createTreeItemFilter(attPart, op, attributeItemKey);
		return new CategoryAttributeFilter(cat, att);
	}
	
	
	public CategoryAttributeFilter(CategoryFilter cat, AttributeFilter att){
		this.categoryFilter = cat;
		this.attributeFilter = att;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		return categoryFilter.asString() + ":" + attributeFilter.asString(); //$NON-NLS-1$
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping, HashMap<IFilter, String> filterTables){
		String col = filterTables.get(this);
		if (col != null){
			return col + ".wp_uuid is not null "; //$NON-NLS-1$
		}
		return "( " + categoryFilter.asSql( tableMapping, filterTables ) + Operator.AND.asSql() + attributeFilter.asSql(tableMapping,filterTables) + " )"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return true;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return true;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
		attributeFilter.getAttributeFilters(attributes);
		categoryFilter.getAttributeFilters(attributes);
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getDropItems(org.hibernate.Session)
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		Category c = null;
		Attribute att = null;
		
		try{
			c = categoryFilter.getCategory(session);
			att = attributeFilter.getAttribute(session);

			boolean found = false;
			for (Attribute a : QueryDataModelManager.getInstance().getAttributes(session, c.getHkey())){
				if (a.getKeyId().equals(att.getKeyId())){
					found = true;
					break;
				}
			}
			if (!found){
				throw new Exception(MessageFormat.format(Messages.CategoryAttributeFilter_MissingCategoryAttribute, new Object[]{c.getKeyId(), att.getKeyId()}));
			}
		
			CategoryAttribute ca = new CategoryAttribute(c,  att);
			DropItem it = DropItemFactory.INSTANCE.createAttributeDropItem(ca);
			attributeFilter.initDropItem(it, session);
		
			return new DropItem[]{it};
		}catch (Exception ex){
			return new DropItem[]{new ErrorDropItem(ex.getMessage())};
		}
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		List<IFilter> kids = new ArrayList<IFilter>();
		kids.add(categoryFilter);
		kids.add(attributeFilter);
		return kids;
	}
}
