/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.security;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;

/**
 * An action in smart connect allows users to perform a set of tasks.
 * Actions can be associated with a set of resources to limit access to some resources.
 * @author Emily
 *
 */
public interface ISmartConnectAction {

	/**
	 * 
	 * @param actionKey
	 * @return the name for the given action key
	 */
	public String getActionName(String actionKey, Locale l);
	
	/**
	 * 
	 * @return list of supported action keys 
	 */
	public String[] getActionKeys();
	
	/**
	 * 
	 * @return list of CA-Administrator supported action keys 
	 */
	public String[] getCaAdminAccessibleActionKeys();
	
	/**
	 * @param actionKey the actionkey to list resources for
	 * @param s current session with open transaction
	 * @return List of resource options for given connect action.
	 * Will return null if no resource options for this action.
	 * 
	 */
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l);
	
	/**
	 * @param actionKey the actionkey to list resources for
	 * @param s current session with open transaction
	 * @param uuidList is the list of CAs this Ca-Admin user has access to. Return list will only include resources withing these CAs
	 * @return List of resource options for given connect action.
	 * 
	 * Will return null if no resource options for this action.
	 * 
	 */
	public List<ResourceOption> getResourceOptionsForCas(String actionKey, Session s, Locale l, List<UUID> uuidList);
	
	
	/**
	 * 
	 * @param resource
	 * @param s current session with open transaction
	 * @return the name associated with the resource uuid
	 */
	public String getResourceName(UUID resource, Session s, Locale l);
}
