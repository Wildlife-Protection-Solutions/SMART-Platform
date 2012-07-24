/*
 *************************************************************************
 * Copyright (c) 2012 <<Your Company Name here>>
 *  
 *************************************************************************
 */

package org.wcs.smart.data.oda.smart.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.SortSpec;
import org.eclipse.datatools.connectivity.oda.spec.QuerySpecification;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.model.patrol.PatrolQuery;
import org.wcs.smart.query.parser.PatrolQueryOptions.DATE_FILTER_OP;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.util.SmartUtils;

/**
 * Implementation class of IQuery for an ODA runtime driver.
 * <br>
 * For demo purpose, the auto-generated method stubs have
 * hard-coded implementation that returns a pre-defined set
 * of meta-data and query results.
 * A custom ODA driver is expected to implement own data source specific
 * behavior in its place. 
 */
public class SmartQuery implements IQuery
{
	
	public static final String SMART_DATASET_TYPE = "org.wcs.smart.data.oda.smart.smartDataset";
	
	
	private int m_maxRows;
    
	
    private byte[] uuid;
    private org.wcs.smart.query.model.Query smartQuery;
    
    private org.wcs.smart.query.model.Query.QueryType queryType;
    
    private HashMap<ParameterMetaData.Parameter, Object> parameters = null;
    private ParameterMetaData pMetadata = null;
    
    private Job loadQueryJob = new Job("load query job") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			Session session = HibernateManager.openSession();
			try{
				if (queryType == null){
					smartQuery = (org.wcs.smart.query.model.Query) session.get(org.wcs.smart.query.model.observation.ObservationQuery.class, uuid);
					if (smartQuery == null){
						smartQuery = (org.wcs.smart.query.model.Query) session.get(PatrolQuery.class, uuid);	
					}
					if (smartQuery == null){
						smartQuery = (org.wcs.smart.query.model.Query) session.get(SummaryQuery.class, uuid);	
					}					
				}else{
					if (queryType == QueryType.OBSERVATION){
						smartQuery = (org.wcs.smart.query.model.Query) session.get(org.wcs.smart.query.model.observation.ObservationQuery.class, uuid);
					}else if (queryType == QueryType.PATROL){
						smartQuery = (org.wcs.smart.query.model.Query) session.get(PatrolQuery.class, uuid);	
					}else if (queryType == QueryType.SUMMARY){
						smartQuery = (org.wcs.smart.query.model.Query) session.get(SummaryQuery.class, uuid);
					}
				}
			}finally{
				session.close();
			}
			return Status.OK_STATUS;
		}
	};
    
	public SmartQuery (){
		this.queryType = null;
	}
    public SmartQuery (org.wcs.smart.query.model.Query.QueryType queryType){
    	this.queryType = queryType;
    }
    
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 */
	public void prepare( String queryText ) throws OdaException
	{
		
		parameters = new HashMap<ParameterMetaData.Parameter, Object>();
		try {
			this.uuid = SmartUtils.decodeHex( (String)queryText );
		} catch (Exception e1) {
			throw new OdaException(e1);
		}
		
		if (smartQuery == null) {
			loadQueryJob.schedule();
			try {
				loadQueryJob.join();
				if (smartQuery == null) {
					throw new OdaException("Query could not be loaded.");
				}
			
				// attempt to parse query
				//TODO:
				if (smartQuery instanceof SimpleQuery){
					((SimpleQuery) smartQuery).getFilter();
				}else if (smartQuery instanceof SummaryQuery){
					//((SummaryQuery)smartQuery).getQueryDefinition().getQueryFilter();
				}

			} catch (InterruptedException e) {
				throw new OdaException(e);
			}
		}
	}
	
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setAppContext(java.lang.Object)
	 */
	public void setAppContext( Object context ) throws OdaException
	{
	    // do nothing; assumes no support for pass-through context
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#close()
	 */
	public void close() throws OdaException
	{
        // TODO Auto-generated method stub
        
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException
	{
        /* TODO Auto-generated method stub
         * Replace with implementation to return an instance 
         * based on this prepared query.
         */
		if (smartQuery.getType() == QueryType.OBSERVATION || 
				smartQuery.getType() == QueryType.PATROL){
			return new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery);	
		}else if (smartQuery.getType() == QueryType.SUMMARY){
			return new SummaryQueryResultSetMetadata((SummaryQuery) smartQuery);
		}
		return new ResultSetMetaData();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException
	{
        /* TODO Auto-generated method stub
         * Replace with implementation to return an instance 
         * based on this prepared query.
         */
		IResultSet resultSet = null;
		
		DateFilter dateFilter = new DateFilter(DateFilter.DATE_FIELD_OP.WAYPOINT, 
				DATE_FILTER_OP.CUSTOM, 
				(Date)parameters.get(ParameterMetaData.Parameter.STARTDATE), 
				(Date)parameters.get(ParameterMetaData.Parameter.ENDDATE));
		
		if (smartQuery.getType() == QueryType.OBSERVATION || 
				smartQuery.getType() == QueryType.PATROL){
			((SimpleQuery)smartQuery).setDateFilter(dateFilter);
			resultSet = new SimpleQueryResultSet((SimpleQuery)smartQuery,  new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery));	
		}else if (smartQuery.getType() == QueryType.SUMMARY){
			((SummaryQuery)smartQuery).setDateFilter(dateFilter);
			resultSet = new SummaryQueryResultSet((SummaryQuery)smartQuery, new SummaryQueryResultSetMetadata((SummaryQuery)smartQuery));
		}
		
//		resultSet.setMaxRows( getMaxRows() );
		return resultSet;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setProperty(java.lang.String, java.lang.String)
	 */
	public void setProperty( String name, String value ) throws OdaException
	{
		// do nothing; assumes no data set query property
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setMaxRows(int)
	 */
	public void setMaxRows( int max ) throws OdaException
	{
	    m_maxRows = max;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMaxRows()
	 */
	public int getMaxRows() throws OdaException
	{
		return m_maxRows;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#clearInParameters()
	 */
	public void clearInParameters() throws OdaException
	{
		parameters.clear();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(java.lang.String, int)
	 */
	public void setInt( String parameterName, int value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(int, int)
	 */
	public void setInt( int parameterId, int value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(java.lang.String, double)
	 */
	public void setDouble( String parameterName, double value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(int, double)
	 */
	public void setDouble( int parameterId, double value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(java.lang.String, java.math.BigDecimal)
	 */
	public void setBigDecimal( String parameterName, BigDecimal value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(int, java.math.BigDecimal)
	 */
	public void setBigDecimal( int parameterId, BigDecimal value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(java.lang.String, java.lang.String)
	 */
	public void setString( String parameterName, String value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int, java.lang.String)
	 */
	public void setString( int parameterId, String value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(java.lang.String, java.sql.Date)
	 */
	public void setDate( String parameterName, Date value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(int, java.sql.Date)
	 */
	public void setDate( int parameterId, Date value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(java.lang.String, java.sql.Time)
	 */
	public void setTime( String parameterName, Time value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(int, java.sql.Time)
	 */
	public void setTime( int parameterId, Time value ) throws OdaException
	{
		setObject(parameterId, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(java.lang.String, java.sql.Timestamp)
	 */
	public void setTimestamp( String parameterName, Timestamp value ) throws OdaException
	{
		setObject(parameterName, value);
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(int, java.sql.Timestamp)
	 */
	public void setTimestamp( int parameterId, Timestamp value ) throws OdaException
	{
		setObject(parameterId, value);
	}

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(java.lang.String, boolean)
     */
    public void setBoolean( String parameterName, boolean value )
            throws OdaException
    {
    	setObject(parameterName, value);
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(int, boolean)
     */
    public void setBoolean( int parameterId, boolean value )
            throws OdaException
    {
    	setObject(parameterId, value);
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setObject(java.lang.String, java.lang.Object)
     */
    public void setObject( String parameterName, Object value )
            throws OdaException
    {
    	parameters.put(getParameterMetaDataLocal().findParameter(parameterName), value);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setObject(int, java.lang.Object)
     */
    public void setObject( int parameterId, Object value ) throws OdaException
    {
    	parameters.put(getParameterMetaDataLocal().findParameter(parameterId), value);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setNull(java.lang.String)
     */
    public void setNull( String parameterName ) throws OdaException
    {
        setObject(parameterName, null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setNull(int)
     */
    public void setNull( int parameterId ) throws OdaException
    {
    	setObject(parameterId, null);
    }

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#findInParameter(java.lang.String)
	 */
	public int findInParameter( String parameterName ) throws OdaException
	{
        return getParameterMetaDataLocal().findParameter(parameterName).index;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public IParameterMetaData getParameterMetaData() throws OdaException
	{
        /* TODO Auto-generated method stub
         * Replace with implementation to return an instance 
         * based on this prepared query.
         */
		if (pMetadata == null){
			pMetadata = new ParameterMetaData();
		}
		return pMetadata;
	}

	private ParameterMetaData getParameterMetaDataLocal() throws OdaException{
		return (ParameterMetaData) getParameterMetaData();
	}
	
	
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setSortSpec(org.eclipse.datatools.connectivity.oda.SortSpec)
	 */
	public void setSortSpec( SortSpec sortBy ) throws OdaException
	{
		// only applies to sorting, assumes not supported
        throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSortSpec()
	 */
	public SortSpec getSortSpec() throws OdaException
	{
		// only applies to sorting
		return null;
	}

	
    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#setSpecification(org.eclipse.datatools.connectivity.oda.spec.QuerySpecification)
     */
    public void setSpecification( QuerySpecification querySpec )
            throws OdaException, UnsupportedOperationException
    {
        // assumes no support
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#getSpecification()
     */
    public QuerySpecification getSpecification()
    {
        // assumes no support
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#getEffectiveQueryText()
     */
    public String getEffectiveQueryText()
    {
    	throw new UnsupportedOperationException( );
    }

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IQuery#cancel()
     */
    public void cancel() throws OdaException, UnsupportedOperationException
    {
        // assumes unable to cancel while executing a query
        throw new UnsupportedOperationException();
    }
    
}
