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
package org.wcs.smart.i2.birt.record.location;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelLocation;

import com.vividsolutions.jts.io.ParseException;

/**
 * Entity record datasets results metadata
 * @author Emily
 *
 */
public class RecordLocationDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		RECORD_UUID("recordlocation:record_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ID("recordlocation:id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GEOM("recordlocation:geom", java.sql.Types.JAVA_OBJECT), //$NON-NLS-1$
		DATE("recordlocation:date", java.sql.Types.DATE), //$NON-NLS-1$
		COMMENT("recordlocation:comment", java.sql.Types.VARCHAR), //$NON-NLS-1$
		OBSERVATION("recordlocation:observation", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		String id;
		int type;
		
		Column(String id, int type){
			this.id = id;
			this.type = type;
		}
		public String getColumnName(Locale l){
			return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
		}
		public String getId(){
			return this.id;
		}
		public Object getValue(IntelLocation location) {
			if (this == RECORD_UUID) return location.getRecord().getUuid();
			if (this == ID) return location.getId();
			if (this == GEOM) {
				try{
					return location.getGeometry();
				}catch (ParseException e){
					Logger.getLogger(RecordLocationDatasetResultSetMetadata.class.getName()).log(Level.INFO, e.getMessage(), e); 
				}
				return null;
			}
			if (this == DATE) return location.getDateTime();
			if (this == COMMENT) return location.getComment();
			if (this == OBSERVATION) return "TODO:"; //$NON-NLS-1$
			return null;
		}
	}
	
	private Locale l;
	public RecordLocationDatasetResultSetMetadata(Locale l){
		this.l = l;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return Column.values().length;
		
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	@Override
	public int getColumnDisplayLength(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		return Column.values()[index-1].getColumnName(l);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].id;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		return Column.values()[index-1].type;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, RecordLocationDataset.DATASET_TYPE );
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 */
	@Override
	public int getPrecision(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int arg0) throws OdaException {
		return columnNullableUnknown;
	}

}
