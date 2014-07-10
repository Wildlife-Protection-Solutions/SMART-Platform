/*
 *************************************************************************
 * Copyright (c) 2012 <<Your Company Name here>>
 *  
 *************************************************************************
 */

package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Implementation class of IParameterMetaData for the SMART ODA runtime driver. <br>
 * Currently only smart and end data parameters are supports.
 * 
 */
// In the future it may be possible to view the query
// and parameterize certain parts of the queries (stations/teams etc)
public class SmartParameterMetaData implements IParameterMetaData {

	/**
	 * Fixed start and end date parameters
	 */
	public enum Parameter {
		STARTDATE("Start Date", 1, java.sql.Types.DATE),  //$NON-NLS-1$
		ENDDATE("End Date", 2, java.sql.Types.DATE); //$NON-NLS-1$
		public String guiName;
		public int index;
		public int type;

		private Parameter(String name, int index, int type) {
			this.guiName = name;
			this.index = index;
			this.type = type;
		}
	}

	/**
	 * Finds a parameter at a given index
	 * 
	 * @param index
	 *            the parameter index
	 * @return the parameter of null if index not found
	 */
	public Parameter findParameter(int index) {
		for (int i = 0; i < Parameter.values().length; i++) {
			if (Parameter.values()[i].index == index) {
				return Parameter.values()[i];
			}
		}
		return null;
	}

	/**
	 * Finds a parameter by name
	 * 
	 * @param name
	 *            the parameter name
	 * @return the parameter or null if not found
	 */
	public Parameter findParameter(String name) {
		for (int i = 0; i < Parameter.values().length; i++) {
			if (Parameter.values()[i].guiName.equals(name)) {
				return Parameter.values()[i];
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterCount()
	 */
	public int getParameterCount() throws OdaException {
		return Parameter.values().length;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterMode(int)
	 */
	public int getParameterMode(int param) throws OdaException {
		return IParameterMetaData.parameterModeIn;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterName(int)
	 */
	public String getParameterName(int param) throws OdaException {
		Parameter p = findParameter(param);
		if (p != null) {
			return p.guiName;
		}
		return null; // name is not available
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterType(int)
	 */
	public int getParameterType(int param) throws OdaException {
		Parameter p = findParameter(param);
		if (p != null) {
			return p.type;
		}
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterTypeName(int)
	 */
	public String getParameterTypeName(int param) throws OdaException {
		int nativeTypeCode = getParameterType(param);
		return SmartDriver.getNativeDataTypeName(nativeTypeCode, SmartQuery.SMART_DATASET_TYPE);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getPrecision(int)
	 * @return -1
	 */
	public int getPrecision(int param) throws OdaException {
		return -1;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IParameterMetaData#getScale(int)
	 * @return -1
	 */
	public int getScale(int param) throws OdaException {
		return -1;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IParameterMetaData#isNullable(int)
	 */
	public int isNullable(int param) throws OdaException {
		return IParameterMetaData.parameterNullableUnknown;
	}

}
