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
package org.wcs.smart.data.oda.smart.query.common;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Empty result set
 * @author Emily
 *
 */
public class EmptyResultSet implements IResultSet {

	public final static EmptyResultSet INSTANCE = new  EmptyResultSet();
	
	private EmptyResultSet(){}
	
	@Override
	public void close() throws OdaException {
	}

	@Override
	public int findColumn(String arg0) throws OdaException {
		return 0;
	}

	@Override
	public BigDecimal getBigDecimal(int arg0) throws OdaException {
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(String arg0) throws OdaException {
		return null;
	}

	@Override
	public IBlob getBlob(int arg0) throws OdaException {
		return null;
	}

	@Override
	public IBlob getBlob(String arg0) throws OdaException {
		return null;
	}

	@Override
	public boolean getBoolean(int arg0) throws OdaException {
		return false;
	}

	@Override
	public boolean getBoolean(String arg0) throws OdaException {
		return false;
	}

	@Override
	public IClob getClob(int arg0) throws OdaException {
		return null;
	}

	@Override
	public IClob getClob(String arg0) throws OdaException {
		return null;
	}

	@Override
	public Date getDate(int arg0) throws OdaException {
		return null;
	}

	@Override
	public Date getDate(String arg0) throws OdaException {
		return null;
	}

	@Override
	public double getDouble(int arg0) throws OdaException {
		return 0;
	}

	@Override
	public double getDouble(String arg0) throws OdaException {
		return 0;
	}

	@Override
	public int getInt(int arg0) throws OdaException {
		return 0;
	}

	@Override
	public int getInt(String arg0) throws OdaException {
		return 0;
	}

	@Override
	public IResultSetMetaData getMetaData() throws OdaException {
		return null;
	}

	@Override
	public Object getObject(int arg0) throws OdaException {
		return null;
	}

	@Override
	public Object getObject(String arg0) throws OdaException {
		return null;
	}

	@Override
	public int getRow() throws OdaException {
		return 0;
	}

	@Override
	public String getString(int arg0) throws OdaException {
		return null;
	}

	@Override
	public String getString(String arg0) throws OdaException {
		return null;
	}

	@Override
	public Time getTime(int arg0) throws OdaException {
		return null;
	}

	@Override
	public Time getTime(String arg0) throws OdaException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(int arg0) throws OdaException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(String arg0) throws OdaException {
		return null;
	}

	@Override
	public boolean next() throws OdaException {
		return false;
	}

	@Override
	public void setMaxRows(int arg0) throws OdaException {
	}

	@Override
	public boolean wasNull() throws OdaException {
		return false;
	}

}
