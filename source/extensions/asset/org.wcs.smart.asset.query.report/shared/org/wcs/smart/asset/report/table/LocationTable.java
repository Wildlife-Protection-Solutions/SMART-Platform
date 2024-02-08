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
package org.wcs.smart.asset.report.table;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttribute.AttributeType;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * BIRT table for displaying all asset station locations in the system
 * 
 * @author Emily
 *
 */
public class LocationTable extends SmartBirtTable {

	private static final String ATTRIBUTE_COLUMN_PREFIX = "assetloc:"; //$NON-NLS-1$
	public static final String COLUMN_PREFIX = "c:" + ATTRIBUTE_COLUMN_PREFIX; //$NON-NLS-1$
	
	private List<AssetAttribute> attributeColumns = null;
	
	public enum Column{
		ID,
		STATUS,
		STATUS_KEY,
		POSITION;
		
		public String getName(Locale l) {
			switch(this){
			case ID:
				return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(IAssetLabelProvider.ID_COL_NAME, l);
			case STATUS:
				return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(IAssetLabelProvider.STATUS_COL_NAME, l);
			case STATUS_KEY:
				return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(IAssetLabelProvider.STATUSKEY_COL_NAME, l);
			case POSITION:
				return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(IAssetLabelProvider.POSITION_COL_NAME, l);
			}
			return ""; //$NON-NLS-1$
		}
		
		public Object getValue(AssetStationLocation a, Locale l) {
			switch(this) {
			case ID: return a.getId();
			case STATUS: return a.getCachedStatus().getGuiName(l);
			case STATUS_KEY: return a.getCachedStatus().name();
			case POSITION: return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(a.getX(),a.getY()));
			}
			return null;
		}
	}
	
	public LocationTable() {
		super("asset:assetstationlocation"); //$NON-NLS-1$
	}

	private synchronized void getAttributeColumn(SmartConnection connection) {
		if (attributeColumns != null) return;
		
		String sql = "SELECT distinct a.attribute FROM AssetStationLocationAttribute a join a.attribute b on b.conservationArea in (:cas)"; //$NON-NLS-1$

		List<AssetAttribute> assetAttributes = connection.getSession()
				.createQuery(sql, AssetAttribute.class)
				.setParameterList("cas", connection.getConservationAreas()) //$NON-NLS-1$
				.list();
		attributeColumns = assetAttributes;
		attributeColumns.sort((a,b)->a.getKeyId().compareTo(b.getKeyId()));
	}
	
	
	@Override
	public String[] getColumnNames(SmartConnection connection) {
		getAttributeColumn(connection);
		List<String> names = new ArrayList<>();
		for (Column c : Column.values()) {
			names.add(COLUMN_PREFIX + c.name().toLowerCase(Locale.ROOT));
		}
		for (AssetAttribute a : attributeColumns) {
			names.add(ATTRIBUTE_COLUMN_PREFIX + a.getKeyId());
		}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		getAttributeColumn(connection);
		List<String> names = new ArrayList<>();
		for (Column c : Column.values()) {
			names.add(c.getName(connection.getCurrentLocale()));
		}
		for (AssetAttribute a : attributeColumns) {
			names.add(a.getName());
		}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public String getTableFullName(Locale l) {
		return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(IAssetLabelProvider.STATIONLOCATION_TABLE_NAME, l);
	}

	@Override
	public String getTableShortName(Locale l) {
		return getTableFullName(l);
	}

	@Override
	public int[] getColumnTypes(SmartConnection connection) {
		getAttributeColumn(connection);
		int[] names = new int[Column.values().length + attributeColumns.size()];
		int i = 0;
		for (Column c : Column.values()) {
			if (c == Column.POSITION) {
				names[i++] = IGeometryColumn.Type.POINT.birtDataType;
			}else {
				names[i++] = Types.VARCHAR;
			}
		}
		for (AssetAttribute a : attributeColumns) {
			switch(a.getType()) {
			case BOOLEAN:
				names[i++] = Types.BOOLEAN;
				break;
			case DATE:
				names[i++] = Types.DATE;
				break;
			case LIST:
				names[i++] = Types.VARCHAR;
				break;
			case NUMERIC:
				names[i++] = Types.NUMERIC;
				break;
			case POSITION:
				names[i++] = Types.JAVA_OBJECT;
				break;
			case TEXT:
				names[i++] = Types.VARCHAR;
				break;
			default:
				names[i++] = Types.VARCHAR;
				break;
					
			}
		}
		return names;
	}

	@Override
	public List<? extends Object> getValues(SmartConnection connection) {
		List<AssetStationLocation> assets = connection.getSession()
				.createQuery("SELECT a FROM AssetStationLocation a join a.station b WHERE b.conservationArea in (:cas)", AssetStationLocation.class) //$NON-NLS-1$
				.setParameterList("cas", connection.getConservationAreas()) //$NON-NLS-1$
				.getResultList();
		assets.forEach(a->a.computeStatus(connection.getSession()));
		return assets;
	}

	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		if (! (object instanceof AssetStationLocation)) return null;
		
		if (index < Column.values().length) {
			return Column.values()[index].getValue((AssetStationLocation)object, connection.getCurrentLocale());
		}
		AssetAttribute attribute = attributeColumns.get(index - Column.values().length);
		AssetStationLocation asset = (AssetStationLocation) object;
		for (AssetStationLocationAttributeValue v : asset.getAttributeValues()) {
			if (v.getAttribute().equals(attribute)) {
				if (v.getAttribute().getType() == AttributeType.LIST) {
					if (v.getAttributeValue() == null) return null;
					return v.getAttributeListItem().getName();
				}
				return v.getAttributeValue();
			}
		}
		return null;
	}

	@Override
	public void openQuery() {
	}

	@Override
	public void closeQuery() {
	}

}
