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
package org.wcs.smart.internal.ca.advisors;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.internal.Messages;

/**
 * Advisor for deleting categories from the data model.
 * @author egouge
 *
 */
public class CategoryDMAdvisor implements IDeleteAdvisor {

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
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Category)){
			return Messages.CategoryDMAdvisor_Error_NotCategory;
		}
		Category category = (Category)object;
		if (category.getRootAttributes() != null && category.getRootAttributes().size() > 0){
			return MessageFormat.format(Messages.CategoryDMAdvisor_Error_CategoryReferencedByAttributes, new Object[]{category.getName()});
		}else if (category.getChildren() != null && category.getChildren().size() > 0){
			return MessageFormat.format(Messages.CategoryDMAdvisor_Error_CategoryReferencedByChildren, new Object[]{category.getName()});
		}
		return null;
	}
}

