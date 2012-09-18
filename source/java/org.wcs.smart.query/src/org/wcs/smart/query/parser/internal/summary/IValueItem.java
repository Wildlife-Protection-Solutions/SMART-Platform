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
package org.wcs.smart.query.parser.internal.summary;

import org.hibernate.Session;
import org.wcs.smart.query.ui.formulaDnd.DropItem;


/**
 * Interface that represents a value item
 * in a summary query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IValueItem {
	/**
	 * @return the string representation of the item
	 */
	public String asString();

	/**
	 * Give a hibernate connection returns the 
	 * human readable representation of the name
	 * for the value item.  This is displayed in the
	 * results table.
	 * 
	 * @param session hibernate session
	 * @return 
	 */
	public String getName(Session session);
	
	/**
	 * Converts the value item to a drop item
	 * @param session
	 * @return
	 */
	public DropItem asDropItem(Session session);
	
	/**
	 * @return <code>true</code> if value item includes category
	 */
	public boolean hasCategory();
	
	/**
	 * @return <code>true</code> if value item includes a datamodel attribute
	 */
	public boolean hasAttribute();
	
	/**
	 * Validates the current value item against the database.  This includes
	 * ensuring that any keys/uuids exist in the database.
	 *  
	 * @param session
	 * @throws Exception if the item cannot be validated
	 */
	public void validateDatabase(Session session) throws Exception;
	
	/**
	 * @return the object used to initailize the drop item
	 */
	public Object getDropItemInitializeData();
}
