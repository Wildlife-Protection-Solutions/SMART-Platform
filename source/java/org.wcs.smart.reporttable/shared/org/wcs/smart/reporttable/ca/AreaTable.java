/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.reporttable.ca;

import java.util.List;
import java.util.Locale;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Wrapper to convert Conservation Area area objects
 * to a BIRT table data source.
 * 
 * @author Emily
 *
 */
public class AreaTable  extends SmartBirtTable {

	public enum Column{
		CA_ID(ICoreLabelProvider.AREATABLE_CAID_KEY, "Conservation Area ID", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CA_NAME(ICoreLabelProvider.AREATABLE_CANAME_KEY, "Conservation Area Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AREA_NAME(ICoreLabelProvider.AREATABLE_NAME_KEY, "Name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AREA_KEY(ICoreLabelProvider.AREATABLE_KEY_KEY, "Key", java.sql.Types.VARCHAR), //$NON-NLS-1$
		AREA(ICoreLabelProvider.AREATABLE_AREA_KEY, "Area (m2)", java.sql.Types.DOUBLE), //$NON-NLS-1$
		GEOMETRY(ICoreLabelProvider.AREATABLE_GEOMETRY_KEY, "Geometry", java.sql.Types.JAVA_OBJECT); //$NON-NLS-1$
		
		private String name;
		private int type;
		private String key;
		
		private Column(String key, String name, int type){
			this.name = name;
			this.type = type;
			this.key = key;
		}
		public String getName(){
			return this.name;
		}
		public String getLabel(Locale l){
			return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(key, l);			
		}
		public int getType(){
			return this.type;
		}
		
		public Object getValue(Area area){
			switch(this){
			case AREA: 
				try {
					Coordinate c = area.getGeometry().getCentroid().getCoordinate();
					CoordinateReferenceSystem targetCRS = CRS.decode("AUTO2:42001,"+c.x+","+c.y); //$NON-NLS-1$ //$NON-NLS-2$
					Geometry t = JTS.transform(area.getGeometry(), ReprojectUtils.findMathTransform(targetCRS));
					return t.getArea();
				}catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			case AREA_KEY: return area.getKeyId();
			case AREA_NAME: return area.getName();
			case CA_ID: return area.getConservationArea().getId();
			case CA_NAME: return area.getConservationArea().getName();
			case GEOMETRY: return area.getGeometry();			
			}
			return null;
		}
	}
	
	
	private Area.AreaType type;
	/**
	 * Creates a new area birt table source
	 */
	public AreaTable(Area.AreaType type) {
		super("smartareas:" + type.name()); //$NON-NLS-1$
		this.type = type;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames(SmartConnection connection) {
		String[] name = new String[Column.values().length];
		for (int i = 0; i < Column.values().length; i ++){
			name[i] = Column.values()[i].getName();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnLabels()
	 */
	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		String[] name = new String[Column.values().length];
		for (int i = 0; i < Column.values().length; i ++){
			name[i] = Column.values()[i].getLabel(connection.getCurrentLocale());
		}
		return name;
	}
	
	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getColumnTypes()
	 */
	@Override
	public int[] getColumnTypes(SmartConnection connection) {
		int[] name = new int[Column.values().length];
		for (int i = 0; i < Column.values().length; i ++){
			name[i] = Column.values()[i].getType();
		}
		return name;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public List<? extends Object> getValues (SmartConnection connection) {
		String sql = "FROM Area a WHERE a.type = :type and a.conservationArea in (:ca)"; //$NON-NLS-1$
		Query<Area> q  = connection.getSession().createQuery(sql, Area.class);
		q.setParameterList("ca", connection.getConservationAreas()); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		return q.list();
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		return Column.values()[index].getValue((Area)object);
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {
	}

	@Override
	public String getTableFullName(Locale l) {
		return type.getGuiName(l);
	}

	@Override
	public String getTableShortName(Locale l) {
		return getTableFullName(l);
	}

}
