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

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * Interface for group by parts of summary queries.
 * @author egouge
 * @since 1.0.0
 */
public interface IGroupBy {

	/**
	 * The type of the group by part.  This represents the 
	 * data type of the unique key of group by items.
	 * 
	 * @author egouge
	 * @since 1.0.0
	 */
	public enum GroupByType{
		STRING, BYTE, DATE
	}
	
	/**
	 * @return converts the group by to the string
	 * representation
	 */
	public String asString();

	/**
	 * @return the group by type
	 */
	public GroupByType getType();
	
	/**
	 * @param session
	 * @return a collection of items that make up the group by part
	 */
	public List<ListItem> getItems(Session session);
	
	/**
	 * Converts the value item to a drop item
	 * @param session
	 * @return
	 */
	public DropItem asDropItem(Session session);
}
