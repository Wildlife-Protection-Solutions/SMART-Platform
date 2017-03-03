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

import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTargetPoint;

import com.vividsolutions.jts.geom.Coordinate;

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
	

	public enum Column {
		
		TARGE_TNAME(Messages.PlanTargetResultSetMetadata_TargetNameColumnLabel, "TargetName", java.sql.Types.VARCHAR),
		TARGET_DESCRIPTION(Messages.PlanTargetResultSetMetadata_TargetDescriptionColumnLabel, "TargetDescription", java.sql.Types.VARCHAR),
		STATUS_DESCRIPTION(Messages.PlanTargetResultSetMetadata_StatusDescriptionTargetLabel, "StatusDescription", java.sql.Types.VARCHAR),
		STATUS_KEY(Messages.PlanTargetResultSetMetadata_StatusKeyTargetLabel, "targetStatus", java.sql.Types.VARCHAR),
		PLAN_ID(Messages.PlanTargetResultSetMetadata_PlanIdColumnLabel, "PlanId", java.sql.Types.VARCHAR),
		GEOMETRY(Messages.PlanTargetResultSetMetadata_TargetPointsGeomColumnName, GEOM_COLUMN_NAME, java.sql.Types.JAVA_OBJECT),
	
		PLAN_START_DATE("Plan Start Date", "PlanStartDate", java.sql.Types.DATE),
		PLAN_END_DATE("Plan End Date", "PlanEndDate", java.sql.Types.DATE);
		
		public String name;
		public String key;
		public int type;
		
		private Column(String name, String key, int type){
			this.name = name;
			this.key = key;
			this.type = type;
		}
		
		public Object getValue(PlanTarget pt){
	
			switch(this){
				case GEOMETRY:
					if (pt instanceof SpatialPlanTarget){
						SpatialPlanTarget sp = (SpatialPlanTarget)pt;
						Coordinate[] pnts = new Coordinate[sp.getPoints().size()];
						int i =0;
						for (SpatialPlanTargetPoint pnt : sp.getPoints()){
							pnts[i++] = new Coordinate(pnt.getX(), pnt.getY()); 
						}
						return GeometryFactoryProvider.getFactory().createMultiPoint(pnts);
					}
					return null;
				case PLAN_END_DATE:
					return pt.getPlan().getEndDate();
				case PLAN_ID:
					return  pt.getPlan().getId();
				case PLAN_START_DATE:
					return pt.getPlan().getStartDate();
				case STATUS_DESCRIPTION:
					return pt.getCurrentStatus().getDisplayString(Locale.getDefault());
				case STATUS_KEY:
					return pt.getCurrentStatus().getStatus().key;
				case TARGET_DESCRIPTION:
					return pt.getSummary(Locale.getDefault());
				case TARGE_TNAME:
					return pt.getName();
			}
			return null;
		}
	}
	
	public PlanTargetResultSetMetadata(){
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