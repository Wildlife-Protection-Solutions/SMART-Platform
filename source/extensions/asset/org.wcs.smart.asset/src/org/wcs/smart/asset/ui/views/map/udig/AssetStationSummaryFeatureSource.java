/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.map.udig;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.StationData;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Feature source for entity locations
 * 
 * @author Emily
 *
 */
public class AssetStationSummaryFeatureSource extends ContentFeatureSource {

	public AssetStationSummaryFeatureSource(ContentEntry entry) {
		super(entry, null);
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			return DataUtilities.createType(entry.getTypeName(), getFeatureSchemaString(getColumns()));
		} catch (SchemaException e) {
			throw new IOException(e);
		}

	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		return getData().size();
	}

	private List<IOverviewTableColumn> getColumns(){
		return ((AssetStationSummaryDataSource)super.entry.getDataStore()).getColumns();
	}
	
	private List<StationData> getData(){
		return ((AssetStationSummaryDataSource)super.entry.getDataStore()).getData();
	}
	
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		return new AssetStationSummaryFeatureReader(getSchema(), getColumns(), getData());
	}

	public static SimpleFeature toFeature(SimpleFeatureType type, List<IOverviewTableColumn> columns, StationData sdata){
		Object[] data = new Object[2 + columns.size()];
		double x = Double.NaN;
		double y = Double.NaN;
		UUID uuid = null;
		if (sdata.getStation() != null) {
			x = sdata.getStation().getX();
			y = sdata.getStation().getY();
			uuid = sdata.getStation().getUuid();
		}else if (sdata.getStationLocation() != null) {
			x = sdata.getStationLocation().getX();
			y = sdata.getStationLocation().getY();
			uuid = sdata.getStationLocation().getUuid();
		}
		data[0] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(x,y));
		data[1] = UuidUtils.uuidToString(uuid);
		int i = 2;
		for (IOverviewTableColumn c : columns) {
			data[i++] = c.getValue(sdata);
		}
		return SimpleFeatureBuilder.build(type, data, (String)data[1]);
	}
	
	public static String getFeatureSchemaString(List<IOverviewTableColumn> columns){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:");
		sb.append("Point:srid=4326,");
		sb.append("fid:String,");
		for (IOverviewTableColumn c : columns) {
			//key is used so it is the same through all languages or if the user changes the name
			sb.append(c.getKey());
			sb.append(":");
			sb.append(c.getType().geotoolsType);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
}
