/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt.observations;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.incident.birt.SmartIncidentDriver;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.util.UuidUtils;

/**
 * Incident observation result set metadata
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class IncidentObservationDatasetResultSetMetadata implements IResultSetMetaData {

	public enum Column {
		OBS_UUID(Messages.IncidentObservationDatasetResultSetMetadata_obsuuidcolumnname, "obs:uuid", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		GROUP_UUID(Messages.IncidentObservationDatasetResultSetMetadata_groupuuidcolumnname, "group:uuid", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		LEAF_CATEOGRY(Messages.IncidentObservationDatasetResultSetMetadata_catleafcolumnname, "category:leaf", java.sql.Types.VARCHAR), //$NON-NLS-1$
		OTHER_CATEOGRY(Messages.IncidentObservationDatasetResultSetMetadata_catothercolumnname, "category:other", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CATEOGRY(Messages.IncidentObservationDatasetResultSetMetadata_catfullcolumnname, "category:full", java.sql.Types.VARCHAR); //$NON-NLS-1$
		
		public String name;
		public String key;
		public int type;
		
		private Column(String name, String key, int type){
			this.name = name;
			this.key = key;
			this.type = type;
		}
		
		public Object getValue(WaypointObservation wo){
			switch(this){
			case CATEOGRY: return wo.getCategory().getFullCategoryName();
			case GROUP_UUID: return UuidUtils.uuidToString(wo.getObservationGroup().getUuid());
			case LEAF_CATEOGRY: return wo.getCategory().getName();
			case OBS_UUID: return UuidUtils.uuidToString(wo.getUuid());
			case OTHER_CATEOGRY:{
				if (wo.getCategory().getParent() == null) return ""; //$NON-NLS-1$
				return wo.getCategory().getParent().getFullCategoryName();
			}
			default:
				break;
			}
			return null;
		}
	}
	
	public IncidentObservationDatasetResultSetMetadata(){
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
		return Column.values()[index-1].name;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].key;
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
	     return SmartIncidentDriver.getNativeDataTypeName( nativeTypeCode, IncidentObservationDataset.DATASET_TYPE );
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