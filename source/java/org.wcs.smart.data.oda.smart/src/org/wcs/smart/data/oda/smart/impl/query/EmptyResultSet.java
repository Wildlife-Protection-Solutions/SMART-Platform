package org.wcs.smart.data.oda.smart.impl.query;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class EmptyResultSet implements IResultSet {

	public final static EmptyResultSet INSTANCE = new  EmptyResultSet();
	
	private EmptyResultSet(){}
	
	@Override
	public void close() throws OdaException {
		// TODO Auto-generated method stub

	}

	@Override
	public int findColumn(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BigDecimal getBigDecimal(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBlob getBlob(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBlob getBlob(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getBoolean(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getBoolean(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IClob getClob(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClob getClob(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDate(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDouble(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDouble(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getInt(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IResultSetMetaData getMetaData() throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getObject(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRow() throws OdaException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getString(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Time getTime(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(int arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp getTimestamp(String arg0) throws OdaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean next() throws OdaException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setMaxRows(int arg0) throws OdaException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean wasNull() throws OdaException {
		// TODO Auto-generated method stub
		return false;
	}

}
