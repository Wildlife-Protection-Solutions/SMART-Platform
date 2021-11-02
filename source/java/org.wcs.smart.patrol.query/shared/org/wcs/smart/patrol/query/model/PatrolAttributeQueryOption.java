/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.model;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;

/**
 * Query option representing custom patrol attributes
 * 
 * @author Emily
 *
 */
public class PatrolAttributeQueryOption implements IPatrolQueryOption {
	
	private PatrolAttribute pattribute;
	
	public PatrolAttributeQueryOption(PatrolAttribute pattribute){
		this.pattribute = pattribute;
	}
	
	public PatrolAttribute getPatrolAttribute() {
		return this.pattribute;
	}
	
	/**
	 * @return <code>true</code> if this option involved employees
	 */
	@Override
	public boolean isEmployeeItem(){
		return false;
	}
	
	/**
	 * @return gui name
	 */
	@Override
	public String getGuiName(Locale l){
		return pattribute.getName();
	}
	
	/**
	 * @return the option key
	 */
	@Override
	public String getKey(){
		return "patrol:attribute:" + this.pattribute.getType().typeKey + ":" + this.pattribute.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * @return the database column name
	 */
	@Override
	public String getColumnName(){
		return null;
	}
	
	/**
	 * @return option type
	 */
	@Override
	public PatrolQueryOptionType getType(){
		switch(pattribute.getType()) {
		case BOOLEAN: return PatrolQueryOptionType.BOOLEAN;
		case DATE: return PatrolQueryOptionType.DATE;
		case LIST: return PatrolQueryOptionType.KEY;
		case NUMERIC: return PatrolQueryOptionType.NUMBER;
		case TEXT: return PatrolQueryOptionType.STRING;
		case MLIST:
		case TREE:
		}
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 
	 * @return the class this patrol option is an attribute of
	 */
	public Class<?> getPatrolAttributeClass(){
		return PatrolAttribute.class;
	}

	/**
	 * @return the class that represents the option
	 */
	public Class<?> getSourceClass(){
		return PatrolAttribute.class;
	}

	
	/**
	 * Given a particular uuid (key) determine the string
	 * name for the given option.
	 * 
	 * @param session
	 * @param uuid
	 * @return
	 */
	public String getName(Session session, UUID uuid, Locale l){
		PatrolAttribute temp = session.get(PatrolAttribute.class, pattribute.getUuid());
		if (temp == null) return null;
		if (temp.getType() == Attribute.AttributeType.LIST) {
			for (PatrolAttributeListItem item : temp.getAttributeList()) {
				if (item.getUuid().equals(uuid)) return item.getName();
			}
		}
		return null;
	}
	
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
	public String[] getNames(Session session, UUID uuid, Locale l){
		String name = getName(session, uuid, l);
		if (name != null) return new String[] {name};
		return null;
	}
	
	/**
	 * Given a particular uuid return the source 
	 * object (returns a Team, Station etc. object)
	 * @param session
	 * @param uuid
	 * @return
	 */
	public Object getObject(Session session, UUID uuid){
		return session.get(PatrolAttributeListItem.class, uuid);
	}
	

}
