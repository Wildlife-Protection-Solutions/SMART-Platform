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
package org.wcs.smart.ca.datamodel;

import org.hibernate.Session;

/**
 * An interface plugins can implement and register
 * with the DataModelManager that will allow
 * plugins to advise if certain actions
 * can occur to the datamodel. 
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IDataModelAdvisor {

	/**
	 * 
	 * @param category the category to delete
	 * @param session the current open hibernate session
	 * @return null if okay to delete category; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(Category category, Session session);
	
	
	/**
	 * 
	 * @param attribute the attribute to delete
	 * @param session the current open hibernate session 
	 * 
	 * @return null if okay to delete attribute; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(Attribute attribute, Session session);
	
	
	/**
	 * 
	 * @param categoryAttribute
	 * @param session the current open hibernate session
	 * @return null if okay to delete connection between category & attribute; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(CategoryAttribute categoryAttribute, Session session);

	/**
	 * 
	 * @param item the attribute list item to delete
	 * @param session the current open hibernate session
	 * @return null if okay to delete attribute list item; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(AttributeListItem item, Session session);
	
	
	/**
	 * 
	 * @param node the tree node to delete
	 * @param session the current open hibernate session
	 * @return null if okay to delete attribute tree node; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(AttributeTreeNode node, Session session);
	
}
