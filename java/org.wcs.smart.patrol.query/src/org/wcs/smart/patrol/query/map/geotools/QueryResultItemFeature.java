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
package org.wcs.smart.patrol.query.map.geotools;

import java.text.MessageFormat;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Class to convert a query result item to 
 * a feature.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryResultItemFeature {

	private static GeometryFactory gf = new GeometryFactory();
	
	
	/**
	 * Converts a query result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createObservationFeature(PatrolQueryResultItem it, List<QueryColumn> columns, SimpleFeatureType ftype){
		
		Object[] data = new Object[columns.size() + 2];
		data[0] = gf.createPoint(new Coordinate(it.getWaypointX(), it.getWaypointY()));
		data[1] = it.getPatrolId() + "." + it.getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (int i = 0; i < columns.size(); i ++){
			data[i+2] = QueryColumnUtils.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
	}
	
	
	/**
	 * Converts a query result item track feature.
	 * 
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	public static SimpleFeature createTrackFeature(PatrolQueryResultItem it, List<QueryColumn> columns, SimpleFeatureType ftype){
		
		Object[] data = new Object[columns.size() + 2];
		
		if (it.getTrack() == null || it.getTrack().size() == 0) {
			data[0] = gf.createLineString((Coordinate[]) null);
		} else {
			try {
				WKBReader reader = new WKBReader();
				List<byte[]> tracks = it.getTrack();
				LineString[] lss = new LineString[tracks.size()];
				for (int i = 0; i < lss.length; i ++){
					lss[i] = (LineString)reader.read(tracks.get(i));
				}
				data[0] = gf.createMultiLineString(lss);
			} catch (ParseException e) {
				data[0] = gf.createLineString((Coordinate[]) null);
				PatrolQueryPlugIn.log(MessageFormat.format(Messages.QueryResultItemFeature_GeomParseError, new Object[]{it.getPatrolId()}), e);
			}
		}
		
		data[1] = it.getPatrolId() + "." + System.nanoTime(); //$NON-NLS-1$
		
		for (int i = 0; i < columns.size(); i ++){
			data[i+2] = QueryColumnUtils.getValue(it, columns.get(i), ftype.getDescriptor(i + 1));
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data[1]);
		
	}
}
