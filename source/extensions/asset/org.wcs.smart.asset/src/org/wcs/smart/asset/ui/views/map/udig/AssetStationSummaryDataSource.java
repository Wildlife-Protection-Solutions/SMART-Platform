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
package org.wcs.smart.asset.ui.views.map.udig;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn;
import org.wcs.smart.asset.ui.views.map.StationData;

/**
 * Geotools data store for entity locations
 * 
 * @author Emily
 */
public class AssetStationSummaryDataSource extends ContentDataStore{

	private AssetStationSummaryService service;
	
	public AssetStationSummaryDataSource(AssetStationSummaryService service){
		this.service = service;
	}
	
	public List<StationData> getData(){
		return this.service.getData();
	}
	
	public List<IOverviewTableColumn> getColumns(){
		return this.service.getColumns();
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new AssetStationSummaryFeatureSource(entry);
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		names.add(generateName());
		return names;
	}
	
	public static Name generateName(){
		return new NameImpl("org.wcs.smart.asset.summary", "statistics"); //$NON-NLS-1$ //$NON-NLS-2$
	}

//	public static Filter createDateFilter(LocalDate startDate, LocalDate endDate){
//		if (startDate == null || endDate == null) return Filter.INCLUDE;
//		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
//		return ff.between(ff.property("date"), ff.literal(startDate.atStartOfDay()), ff.literal(endDate.atTime(LocalTime.MAX))); //$NON-NLS-1$
//	}
}
