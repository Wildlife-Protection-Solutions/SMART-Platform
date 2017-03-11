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
package org.wcs.smart.i2.udig.query;

import java.io.IOException;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.wcs.smart.i2.query.IQueryColumn;

/**
 * Query results feature source
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class QueryContentFeatureSource extends ContentFeatureSource {

	private QueryDataSource source;
	
	public QueryContentFeatureSource(ContentEntry entry, QueryDataSource source) {
		super(entry, null);
		this.source = source;	
	}
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		
		String geomType = IQueryColumn.Type.GEOMETRY.getFeatureType();
		if (entry.getName().equals(QueryDataSource.POINT_TYPE)){
			geomType = "Point"; //$NON-NLS-1$
		}else if (entry.getName().equals(QueryDataSource.POLYGON_TYPE)){
			geomType = "Polygon"; //$NON-NLS-1$
		}
		try{
			return FeatureGenerator.generateFeatureType(geomType, entry.getName(), source.getResultSet().getQueryColumns());
		}catch (SchemaException ex){
			throw new IOException(ex);
		}
	}

	@Override
	protected ReferencedEnvelope getBoundsInternal(Query arg0)
			throws IOException {
		final BoundingBox[] re = {null};
		getFeatures().accepts(new FeatureVisitor() {
			@Override
			public void visit(Feature feature) {
				if (re[0] == null){
					re[0] = feature.getBounds();
				}else{
					re[0].include(feature.getBounds());
				}
			}
		}, null);
		
		ReferencedEnvelope env = new ReferencedEnvelope(re[0]);
		return env;
	}

	/**
	 * Don't know the number of elements
	 * as the results contains both point and polygons; this always returns -1
	 */
	@Override
	protected int getCountInternal(Query arg0) throws IOException {
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(
			Query arg0) throws IOException {
		try{
			return new QueryFeatureReader(source.getResultSet(), getSchema());
		}catch (Exception ex){
			throw new IOException(ex);
		}
	}

}
