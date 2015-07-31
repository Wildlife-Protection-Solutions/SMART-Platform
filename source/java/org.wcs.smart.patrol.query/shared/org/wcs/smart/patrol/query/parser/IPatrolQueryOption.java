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
package org.wcs.smart.patrol.query.parser;

import java.util.Locale;
import java.util.UUID;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.patrol.query.model.PatrolQueryOptionType;

/**
 * Interface that represents patrol query options
 * 
 * @author elitvin
 * @author egouge
 * @since 1.0.0
 */
public interface IPatrolQueryOption {

	/**
	 * MANDATORY prefix for the key for all boolean contribution options
	 */
	public static final String BOOLEAN_CONTRIBUTION_KEY_PREFIX = "contribution:boolean:"; //$NON-NLS-1$

	/**
	 * MANDATORY prefix for the key for all string contribution options
	 */
	public static final String STRING_CONTRIBUTION_KEY_PREFIX = "contribution:string:";  //$NON-NLS-1$
	
	/**
	 * @return <code>true</code> if this option involved employees
	 */
	public boolean isEmployeeItem();
	
	/**
	 * @return gui name
	 */
	public String getGuiName(Locale l);
	
	/**
	 * @return the option key
	 */
	public String getKey();
	
	/**
	 * @return the database column name
	 */
	public String getColumnName();
	
	/**
	 * @return option type
	 */
	public PatrolQueryOptionType getType();
	
	/**
	 * 
	 * @return the class this patrol option is an attribute of
	 */
	public Class<?> getPatrolAttributeClass();

	/**
	 * @return the class that represents the option
	 */
	public Class<?> getSourceClass();
//
//	/**
//	 * @return the image that represents a particular patrol filter option
//	 */
//	public Image getImage();
	
	/**
	 * Given a particular uuid (key) determine the string
	 * name for the given option.
	 * 
	 * @param session
	 * @param uuid
	 * @return
	 */
	public String getName(Session session, UUID uuid, Locale l);
	
	/**
	 * Return an array of names that represent
	 * the uuid for a given option.  Name provided in 
	 * the conservation area default language.
	 * 
	 * @param session
	 * @param uuid
	 * @return if this option represents employee then
	 * an array of the employeeid, givenname, familyname is returned,
	 * otherwise a simple element array with the object 
	 * name is returned.
	 */
	public String[] getNames(Session session, UUID uuid, Locale l);
	
	/**
	 * Give a particular uuid return the source 
	 * object (returns a Team, Station etc. object)
	 * @param session
	 * @param uuid
	 * @return
	 */
	public Object getObject(Session session, UUID uuid);
	
//	/**
//	 * Given a set of keys (hex encoded uuids or string keys), returns
//	 * a list of listitems that represent the objects
//	 * with the given keys.
//	 * 
//	 * @param session
//	 * @param keys
//	 * @return
//	 */
//	public List<ListItem> getValues(Session session, String[] keys);
//	
//	/**
//	 * @param session
//	 * @return a list of listitems that represent all
//	 * active values for a given object 
//	 */
//	public List<ListItem> getAllActiveValues(Session session);
//	
//	/**
//	 * Specifies default list item that will be selected by default if option
//	 * was added to the query.
//	 * 
//	 * @return list item selected by default
//	 */
//	public ListItem getDefaultListItem();
}
