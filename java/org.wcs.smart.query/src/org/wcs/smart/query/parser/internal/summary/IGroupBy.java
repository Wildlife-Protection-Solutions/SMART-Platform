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

import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.xml.model.UuidItemType;

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
		STRING, BYTE, DATE, KEY
	}
	
	/**
	 * @return converts the group by to the string
	 * representation
	 */
	public String asString();
	
	/**
	 * 
	 * The key part of a group by is 
	 * the part of the group by that uniquely identifies
	 * what is being grouped by but does not include
	 * which particular items should be included in
	 * the results.  For example for CategoryGoupBys
	 * <  CATEGORY_GROUP_BY : "category:" < LEVEL > ":" ( < DM_KEY > ":")* >
	 * the key is the <  CATEGORY_GROUP_BY : "category:" < LEVEL> >.
	 * 
	 * 
	 * @return only the key part of the 
	 * group by
	 */
	public String getKeyPart();

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
	
	/**
	 * @return <code>true</code> if group by part includes category
	 */
	public boolean hasCategory();
	
	/**
	 * @return <code>true</code> if group by part includes attribute
	 */
	public boolean hasAttribute();
	
	/**
	 * Validates the current value item against the database.  This includes
	 * ensuring that any keys/uuids exist in the database and updating
	 * if required.
	 * 
	 * <p>Warnings are created when the process can continue but
	 * incorrect values my be substituted.  Errors are thrown 
	 * if the process cannot continue (ie attribute key not found in database).
	 * </p>
	 * 
	 * @param langCode - the language code of the uuid lookup items
	 * @param uuidLookup - a mapping from uuid to the associated name items can attempt to
	 * be matched to names if uuids do not match
	 * @param session
	 * @return list of warnings or null if no warnings
	 * @throws Exception if the item cannot be validated
	 */
	public List<String> validateAndImport(String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception;
}
