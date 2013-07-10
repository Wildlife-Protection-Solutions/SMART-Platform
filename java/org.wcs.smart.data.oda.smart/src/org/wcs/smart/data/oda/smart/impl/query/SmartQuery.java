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
package org.wcs.smart.data.oda.smart.impl.query;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

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
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.IMemoryQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.parser.PatrolQueryOptions.DATE_FILTER_OP;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.summary.DateGroupBy;
import org.wcs.smart.query.parser.internal.summary.GroupByPart;
import org.wcs.smart.query.parser.internal.summary.IGroupBy;
import org.wcs.smart.util.SmartUtils;

/**
 * Implementation class of IQuery for the SMART ODA runtime driver. <br>
 * This wraps around any smart query (ncluding summaries, patrol, waypoint
 * queries).
 */
public class SmartQuery implements IQuery {

	public static final String SMART_DATASET_TYPE = "org.wcs.smart.data.oda.smart.smartQueryDataset"; //$NON-NLS-1$

	private int m_maxRows;

	//the query uuid and type
	private byte[] uuid;
	private QueryType queryType;
	
	//the loaded query
	private Query smartQuery;

	//dataset parameters
	private HashMap<SmartParameterMetaData.Parameter, Object> parameters = null;
	
	//dataset metadata
	private SmartParameterMetaData pMetadata = null;

	/**
	 * Job to load the query from the database.
	 */
	private Job loadQueryJob = new Job(Messages.SmartQuery_LoadQuery_JobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {

			Session session = HibernateManager.openSession();
			try {
				session.beginTransaction();
				smartQuery = QueryHibernateManager.getInstance().findQuery(session,  uuid, queryType);
			} finally {
				if (session.getTransaction().isActive()){
					session.getTransaction().rollback();
				}
				session.close();
			}
			return Status.OK_STATUS;
		}
	};

	/**
	 * Creates a new smart query
	 */
	public SmartQuery() {
		this.queryType = null;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p>Here the queryText contains the hex encoded uuid
	 * of the query.  The query is loaded from the database and
	 * parsed to ensure it is valid.
	 * </p>
	 */
	public void prepare(String queryText) throws OdaException {
		
		parameters = new HashMap<SmartParameterMetaData.Parameter, Object>();
		try {
			String[] bits = queryText.split(":"); //$NON-NLS-1$
			this.queryType = QueryType.valueOf(bits[0]);
			this.uuid = SmartUtils.decodeHex(bits[1]);
			
		} catch (Exception e1) {
			throw new OdaException(e1);
		}

		if (smartQuery == null) {
			loadQueryJob.schedule();
			try {
				loadQueryJob.join();
				if (smartQuery == null) {
					throw new OdaException(Messages.SmartQuery_Error_CouldNoLoadQuery);
				}

				// attempt to parse query
				if (smartQuery instanceof SimpleQuery) {
					((SimpleQuery) smartQuery).getFilter();
				} else if (smartQuery instanceof SummaryQuery) {
					SummaryQuery sumQuery = (SummaryQuery)smartQuery;
					
					//date group by problem with reports 
					GroupByPart part = sumQuery.getQueryDefinition().getColumnGroupByPart();
					List<IGroupBy> headers = part.getGroupBys();
					for (IGroupBy h : headers){
						if (h instanceof DateGroupBy){
							throw new OdaException(Messages.SmartQuery_Warning_SummaryGroupByDates);
						}
					}
				} else if (smartQuery instanceof GriddedQuery){
					((GriddedQuery)smartQuery).getQueryDefinition();
				}

			} catch (InterruptedException e) {
				throw new OdaException(e);
			}
		}
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setAppContext(java.lang
	 * .Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		// do nothing; assumes no support for pass-through context
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#close()
	 */
	public void close() throws OdaException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException {
		if (smartQuery.getType() == QueryType.OBSERVATION
				|| smartQuery.getType() == QueryType.PATROL) {
			return new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery);
		} else if (smartQuery.getType() == QueryType.SUMMARY) {
			return new SummaryQueryResultSetMetadata((SummaryQuery) smartQuery);
		} else if (smartQuery.getType() == QueryType.GRIDDED ){
			return new SimpleQueryResultSetMetadata( (GriddedQuery) smartQuery);
		}
		throw new OdaException(Messages.SmartQuery_Error_CouldNoLoadMetadata);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException {
		IResultSet resultSet = null;

		//create date filter
		Date startDate = (Date) parameters.get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) parameters.get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			if (smartQuery.getType() == QueryType.SUMMARY){
				//we choose to run summaries in order to get 
				//all header information
				Calendar cal = GregorianCalendar.getInstance();
				cal.set(1900, Calendar.JANUARY, 1);
				startDate = new Date( cal.getTimeInMillis() );
				endDate = new Date(startDate.getTime());
			}else{
				//all others will just return an empty
				return EmptyResultSet.INSTANCE;
			}
			
		}
		DateFilter dateFilter = new DateFilter(
				DateFilter.DATE_FIELD_OP.WAYPOINT, DATE_FILTER_OP.CUSTOM,
				startDate, endDate);

		//the result set
		if (smartQuery.getType() == QueryType.OBSERVATION) {
			((SimpleQuery) smartQuery).setDateFilter(dateFilter);
			resultSet = new PagedQueryResultSet((IPagedQuery) smartQuery, (SimpleQueryResultSetMetadata)getMetaData());
		}else if (smartQuery.getType() == QueryType.PATROL) {
			((SimpleQuery) smartQuery).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet((IMemoryQuery) smartQuery,
					(SimpleQueryResultSetMetadata)getMetaData());
		}else if (smartQuery.getType() == QueryType.GRIDDED){
			((GriddedQuery) smartQuery).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet((GriddedQuery) smartQuery,
					(SimpleQueryResultSetMetadata)getMetaData());
		} else if (smartQuery.getType() == QueryType.SUMMARY) {
			((SummaryQuery) smartQuery).setDateFilter(dateFilter);
			resultSet = new SummaryQueryResultSet(
					(SummaryQuery) smartQuery,
					new SummaryQueryResultSetMetadata((SummaryQuery) smartQuery));
		}
		
		return resultSet;
	}

	/**
	 * Properties not supported.
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setProperty(java.lang.String
	 * , java.lang.String)
	 */
	public void setProperty(String name, String value) throws OdaException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setMaxRows(int)
	 */
	public void setMaxRows(int max) throws OdaException {
		m_maxRows = max;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMaxRows()
	 */
	public int getMaxRows() throws OdaException {
		return m_maxRows;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#clearInParameters()
	 */
	public void clearInParameters() throws OdaException {
		parameters.clear();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setInt(java.lang.String,
	 * int)
	 */
	public void setInt(String parameterName, int value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(int, int)
	 */
	public void setInt(int parameterId, int value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDouble(java.lang.String,
	 * double)
	 */
	public void setDouble(String parameterName, double value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(int, double)
	 */
	public void setDouble(int parameterId, double value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(java.lang
	 * .String, java.math.BigDecimal)
	 */
	public void setBigDecimal(String parameterName, BigDecimal value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(int,
	 * java.math.BigDecimal)
	 */
	public void setBigDecimal(int parameterId, BigDecimal value)
			throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setString(java.lang.String,
	 * java.lang.String)
	 */
	public void setString(String parameterName, String value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int,
	 * java.lang.String)
	 */
	public void setString(int parameterId, String value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDate(java.lang.String,
	 * java.sql.Date)
	 */
	public void setDate(String parameterName, Date value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(int,
	 * java.sql.Date)
	 */
	public void setDate(int parameterId, Date value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTime(java.lang.String,
	 * java.sql.Time)
	 */
	public void setTime(String parameterName, Time value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(int,
	 * java.sql.Time)
	 */
	public void setTime(int parameterId, Time value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(java.lang.
	 * String, java.sql.Timestamp)
	 */
	public void setTimestamp(String parameterName, Timestamp value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(int,
	 * java.sql.Timestamp)
	 */
	public void setTimestamp(int parameterId, Timestamp value)
			throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(java.lang.String
	 * , boolean)
	 */
	public void setBoolean(String parameterName, boolean value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(int,
	 * boolean)
	 */
	public void setBoolean(int parameterId, boolean value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setObject(java.lang.String,
	 * java.lang.Object)
	 */
	public void setObject(String parameterName, Object value)
			throws OdaException {
		parameters
				.put(getParameterMetaDataLocal().findParameter(parameterName),
						value);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setObject(int,
	 * java.lang.Object)
	 */
	public void setObject(int parameterId, Object value) throws OdaException {
		parameters.put(getParameterMetaDataLocal().findParameter(parameterId),
				value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setNull(java.lang.String)
	 */
	public void setNull(String parameterName) throws OdaException {
		setObject(parameterName, null);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setNull(int)
	 */
	public void setNull(int parameterId) throws OdaException {
		setObject(parameterId, null);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#findInParameter(java.lang
	 * .String)
	 */
	public int findInParameter(String parameterName) throws OdaException {
		return getParameterMetaDataLocal().findParameter(parameterName).index;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public IParameterMetaData getParameterMetaData() throws OdaException {
		if (pMetadata == null) {
			pMetadata = new SmartParameterMetaData();
		}
		return pMetadata;
	}

	/**
	 * @return
	 * @throws OdaException
	 */
	private SmartParameterMetaData getParameterMetaDataLocal()
			throws OdaException {
		return (SmartParameterMetaData) getParameterMetaData();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSortSpec(org.eclipse
	 * .datatools.connectivity.oda.SortSpec)
	 */
	public void setSortSpec(SortSpec sortBy) throws OdaException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSortSpec()
	 */
	public SortSpec getSortSpec() throws OdaException {
		return null;
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSpecification(org.eclipse
	 * .datatools.connectivity.oda.spec.QuerySpecification)
	 */
	public void setSpecification(QuerySpecification querySpec)
			throws OdaException, UnsupportedOperationException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSpecification()
	 */
	public QuerySpecification getSpecification() {
		// assumes no support
		return null;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#getEffectiveQueryText()
	 */
	public String getEffectiveQueryText() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#cancel()
	 */
	public void cancel() throws OdaException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
