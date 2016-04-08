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
package org.wcs.smart.report.birt.map.udig;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.report.birt.map.MapLayerInfo;

/**
 * Datastore for SMART Birt Query results
 * @author Emily
 *
 */
public class BirtDataStore extends ContentDataStore{

	private IQueryResults queryResults;
	private Name typeName;
	private MapLayerInfo mapInfo;
	
	private ContentFeatureSource source;
	private OdaDataSetHandle dataSetHandle;
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public BirtDataStore(IQueryResults queryResults, String typeName, MapLayerInfo mapInfo){
		this.queryResults = queryResults;
		this.typeName = new NameImpl(typeName);
		this.mapInfo = mapInfo;
	}
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public BirtDataStore(OdaDataSetHandle dataSetHandle, String typeName, MapLayerInfo mapInfo){
		this((IQueryResults)null, typeName, mapInfo);
		this.dataSetHandle = dataSetHandle;
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		return Collections.singletonList(typeName);
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		if (source == null){
			synchronized (this) {
				if (queryResults != null){
					source = new QueryFeatureSource(entry, queryResults, mapInfo);
				}else{
					source = new EmptyFeatureSource(entry, dataSetHandle, mapInfo);
				}	
			}
		}
		return source;
	}
	
}
