package org.wcs.smart.connect.security;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;

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
	 * @param actionKey the actionkey to list resources for
	 * @param s current session with open transaction
	 * @return List of resource options for given connect action.
	 * Will return null if no resource options for this action.
	 * 
	 */
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l);
	
	/**
	 * 
	 * @param resource
	 * @param s current session with open transaction
	 * @return the name associated with the resource uuid
	 */
	public String getResourceName(UUID resource, Session s, Locale l);
}
