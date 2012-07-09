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
package org.wcs.smart.ca.internal.datamodel;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelAdvisor;

/**
 * A data model advisor the ensure the data model will remain
 * valid after modifications.
 *
 * @author egouge
 * @since 1.0.0
 */
public class DataModelAdvisor implements IDataModelAdvisor {

	/**
	 * <p>
	 * Validates that the category has no attributes and no
	 * children categories.
	 * </p>
	 * <p>The given category is not guaranteed to exist
	 * in the database so implementors
	 * should check verify that the uuid is not null before
	 * using in database queries.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Category, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Category category, Session session) {
		if (category.getAttributes() != null && category.getAttributes().size() > 0){
			return "Category cannot be removed until all attributes are also removed.";
		}else if (category.getChildren() != null && category.getChildren().size() > 0){
			return "Category cannot be removed until all children are also removed.";
		}
		return null;
	}

	/**
	 * <p>
	 * Validates the attribute is not associated with
	 * any categories.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Attribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Attribute attribute, Session session) {
		if (attribute.getUuid() == null){
			return null;
		}
		Criteria query = session.createCriteria(CategoryAttribute.class);
		query.add(Restrictions.eq("id.attribute", attribute));
		@SuppressWarnings("unchecked")
		List<CategoryAttribute> items = query.list();
		if (items.size() == 0){
			return null;
		}else{
			StringBuilder sb = new StringBuilder();
			sb.append("Attribute cannot be deleted until the following category relationships are removed: \n" );
			for (CategoryAttribute it : items){
				sb.append(it.getCategory().getFullCategoryName() + "\n");
			}
			return sb.toString();
		}
	}

	/**
	 * <p>
	 * Always returns null as deleting this relationship
	 * does not invalidate the data model.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.CategoryAttribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(CategoryAttribute categoryAttribute, Session session) {
		return null;
	}

	/**
	 * <p>
	 * Always returns null as deleting a list item
	 * does not invalidate the data model.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.AttributeListItem, org.hibernate.Session)
	 */
	@Override
	public String canDelete(AttributeListItem item, Session session) {
		return null;
	}

	/**
	 * <p>
	 * Validates that the tree node has no children.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.AttributeTreeNode, org.hibernate.Session)
	 */
	@Override
	public String canDelete(AttributeTreeNode node, Session session) {
		if (node.getChildren().size() > 0){
			return "Attribute tree node cannot be deleted until all children are also removed.";
		}
		return null;
	}

}
