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
 * Implementation class of IParameterMetaData for an ODA runtime driver.
 * <br>
 * For demo purpose, the auto-generated method stubs have
 * hard-coded implementation that returns a pre-defined set
 * of meta-data and query results.
 * A custom ODA driver is expected to implement own data source specific
 * behavior in its place. 
 */
public class ParameterMetaData implements IParameterMetaData 
{

	public enum Parameter{
		STARTDATE("Start Date", 1, java.sql.Types.DATE),
		ENDDATE("End Date", 2, java.sql.Types.DATE);
		
		public String guiName;
		public int index;
		public int type;
		
		private Parameter(String name, int index, int type){
			this.guiName = name;
			this.index = index;
			this.type = type;
		}
	}
	
	public Parameter findParameter(int index){
		for (int i = 0; i < Parameter.values().length; i ++){
    		if (Parameter.values()[i].index == index){
    			return Parameter.values()[i];
    		}
    	}
		return null;
	}
	
	public Parameter findParameter(String name){
		for (int i = 0; i < Parameter.values().length; i ++){
    		if (Parameter.values()[i].guiName.equals(name)){
    			return Parameter.values()[i];
    		}
    	}
		return null;
	}
	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterCount()
	 */
	public int getParameterCount() throws OdaException 
	{
		return Parameter.values().length;
	}

    /*
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterMode(int)
	 */
	public int getParameterMode( int param ) throws OdaException 
	{
		return IParameterMetaData.parameterModeIn;
	}

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterName(int)
     */
    public String getParameterName( int param ) throws OdaException
    {
    	Parameter p = findParameter(param);
    	if (p != null){
    		return p.guiName;
    	}
        return null;    // name is not available
    }

	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterType(int)
	 */
	public int getParameterType( int param ) throws OdaException 
	{
    	Parameter p = findParameter(param);
    	if (p != null){
    		return p.type;
    	}
        return -1;    
	}

	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterTypeName(int)
	 */
	public String getParameterTypeName( int param ) throws OdaException 
	{
        int nativeTypeCode = getParameterType( param );
        return SmartDriver.getNativeDataTypeName( nativeTypeCode );
	}

	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getPrecision(int)
	 */
	public int getPrecision( int param ) throws OdaException 
	{
        // TODO Auto-generated method stub
		return -1;
	}

	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getScale(int)
	 */
	public int getScale( int param ) throws OdaException 
	{
        // TODO Auto-generated method stub
		return -1;
	}

	/* 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#isNullable(int)
	 */
	public int isNullable( int param ) throws OdaException 
	{
		return IParameterMetaData.parameterNullableUnknown;
	}

}
