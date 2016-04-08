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
package org.wcs.smart.plan.report.oda;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.plan.internal.Messages;

/**
 * SMART plan target result set metadata.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class PlanTargetResultSetMetadata implements IResultSetMetaData {

	public static final String ID = "org.wcs.smart.plan.report.oda.SmartPlanTargets"; //$NON-NLS-1$
	public static final String GEOM_COLUMN_NAME = "geometry"; //$NON-NLS-1$
	
	public PlanTargetResultSetMetadata(){
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return 6;
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
		switch(index){
		case 1: return Messages.PlanTargetResultSetMetadata_TargetNameColumnLabel;
		case 2: return Messages.PlanTargetResultSetMetadata_TargetDescriptionColumnLabel;
		case 3: return Messages.PlanTargetResultSetMetadata_StatusDescriptionTargetLabel;
		case 4: return Messages.PlanTargetResultSetMetadata_StatusKeyTargetLabel;
		case 5: return Messages.PlanTargetResultSetMetadata_PlanIdColumnLabel;
		case 6: return Messages.PlanTargetResultSetMetadata_TargetPointsGeomColumnName;
		}
		return null;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		switch(index){
		case 1: return "TargetName"; //$NON-NLS-1$
		case 2: return "TargetDescription"; //$NON-NLS-1$
		case 3: return "StatusDescription"; //$NON-NLS-1$
		case 4: return "targetStatus"; //$NON-NLS-1$
		case 5: return "PlanId"; //$NON-NLS-1$
		case 6: return GEOM_COLUMN_NAME;
		}
		return null;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		switch(index){
		case 1: return java.sql.Types.VARCHAR;
		case 2: return java.sql.Types.VARCHAR;
		case 3: return java.sql.Types.VARCHAR;
		case 4: return java.sql.Types.VARCHAR;
		case 5: return java.sql.Types.VARCHAR;
		case 6: return java.sql.Types.JAVA_OBJECT;
		}
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartPlanDriver.getNativeDataTypeName( nativeTypeCode, ID );
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