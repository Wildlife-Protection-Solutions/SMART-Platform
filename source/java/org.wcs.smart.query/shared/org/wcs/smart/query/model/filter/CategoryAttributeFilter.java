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

import org.wcs.smart.util.SharedUtils;

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
	 * Creates a new date category attribute filter
	 * 
	 * Date filters are of the form: <DATE> BETWEEN <DATE1> AND <DATE2>
	 * 
	 * @param attributeIdentifier the attribute identifier in the form "attribute:s:<key>"
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return
	 */
	public static CategoryAttributeFilter createDateFilter(String catAttributeIdentifier, String date1, String date2, Operator op){
		
		String bits[] = catAttributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		CategoryFilter cat = CategoryFilter.createFilter(catPart);
		AttributeFilter att = AttributeFilter.createDateFilter(attPart, date1, date2, op);
		
		return new CategoryAttributeFilter(cat, att);
	}
	
	/**
	 * 
	 */
	public static CategoryAttributeFilter createStringFilter(String catAttributeIdentifier, Operator op, String value){
		String bits[] = catAttributeIdentifier.split(":"); //$NON-NLS-1$
		String catPart = bits[0] + ":" + bits[1]; //$NON-NLS-1$
		String attPart = bits[2] + ":" + bits[3] + ":" + bits[4]; //$NON-NLS-1$ //$NON-NLS-2$
		
		value = SharedUtils.stripQuotes(value);
		
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

	

	
}
