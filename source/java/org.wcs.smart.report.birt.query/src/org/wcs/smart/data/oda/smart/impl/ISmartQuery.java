package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

public interface ISmartQuery {

	public static final String SMART_QUERY_EXTENSION_ID = "org.wcs.smart.report.birt.query.queryDataset";
	
	
	public void prepare(SmartQuery smartQuery) throws OdaException;
	
	public IResultSet executeQuery(SmartQuery smartQuery) throws OdaException;
	
	public IResultSetMetaData getMetaData(SmartQuery smartQuery) throws OdaException;
	
	
}