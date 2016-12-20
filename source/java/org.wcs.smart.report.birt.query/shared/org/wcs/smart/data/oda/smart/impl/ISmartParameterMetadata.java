package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Extension to IParameterMetaData class to support SMART requirements
 * for BIRT. 
 *  
 * @author Emily
 *
 */
public interface ISmartParameterMetadata extends IParameterMetaData{
	/**
	 * Finds a parameter at a given index
	 * 
	 * @param index
	 *            the parameter index
	 * @return the parameter of null if index not found
	 */
	public Object findParameter(int index) throws OdaException;

	/**
	 * Finds a parameter by name
	 * 
	 * @param name
	 *            the parameter name
	 * @return the parameter or null if not found
	 */
	public Object findParameter(String name) throws OdaException;
	
}
