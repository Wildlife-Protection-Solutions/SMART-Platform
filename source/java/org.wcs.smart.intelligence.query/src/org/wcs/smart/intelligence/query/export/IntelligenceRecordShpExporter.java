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
package org.wcs.smart.intelligence.query.export;

import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.query.map.udig.IntelQueryDataSource;
import org.wcs.smart.intelligence.query.map.udig.IntelQueryFeatureReader;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * Intelligence Record query shape exporter.  Exports
 * the results from an intelligence record query to 
 * shapefile.
 * @author Emily
 *
 */
public class IntelligenceRecordShpExporter extends ShapeQueryExporter{

	@Override
	public boolean canExport(Query query) {
		if (IntelligenceRecordQuery.KEY.equals(query.getTypeKey())){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType)
			throws Exception {
		throw new UnsupportedOperationException("createFeature not supported for intelligence shp exporter.");  //$NON-NLS-1$
	}
	
	/**
	 * Intelligence records are a bit different as for
	 * each result item row we have multiple features.
	 */
	@Override
	protected void writeRow(IResultItem row) throws Exception {
		IntelligenceRecordResultItem currentIntel = (IntelligenceRecordResultItem)row;
		SimpleFeatureType ftype = shapefile.getSchema(shapefile.getTypeNames()[0]);
		Session s = HibernateManager.openSession();
		try{
			Intelligence i = (Intelligence) s.load(Intelligence.class, currentIntel.getUuid());
			for (IntelligencePoint ip : i.getPoints()){
				features.add(IntelQueryFeatureReader.toSimpleFeature(queryColumns, ftype, currentIntel, ip));
			}
		}finally{
			s.close();
		}
	}

	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType)
			throws Exception {
		return IntelQueryDataSource.createIntelligenceRecordSchema(this.queryColumns);
	}

}
