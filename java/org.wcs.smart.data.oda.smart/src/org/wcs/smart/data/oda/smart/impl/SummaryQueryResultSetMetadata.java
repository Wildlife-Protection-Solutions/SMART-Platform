package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.engine.DerbySummaryEngine;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.model.SummaryQueryResult;
import org.wcs.smart.query.model.observation.QueryColumn;

public class SummaryQueryResultSetMetadata implements IResultSetMetaData {

	private SummaryQueryResult results;
	
	public SummaryQueryResultSetMetadata(final SummaryQuery query){
		
		
		results =  new SummaryQueryResult();
		Job parseQuery = new Job("Parsing Query") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				try{
					DerbySummaryEngine.getHeaderInfo(query, results, session);
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
		};
		parseQuery.schedule();
		
		try {
			parseQuery.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return results.getNumDataColumns() + results.getRowHeaders().size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	@Override
	public int getColumnDisplayLength(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		index = index - 1;
		if (index < results.getRowHeaders().size()){
			return "";
		}else{
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				sb.append(results.getColumnHeaderValues()[i][index - results.getRowHeaders().size()].getName());
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		String name = getColumnLabel(index);
		if(name.length() == 0){
			return "Header";
		}
		return getColumnLabel(index);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		index--;
		if (index < results.getRowHeaders().size()){
			return QueryColumn.ColumnType.STRING.getSqlType();
		}else{
			return QueryColumn.ColumnType.NUMBER.getSqlType();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartDriver.getNativeDataTypeName( nativeTypeCode );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 */
	@Override
	public int getPrecision(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int index) throws OdaException {
		return columnNullableUnknown;
	}

}
