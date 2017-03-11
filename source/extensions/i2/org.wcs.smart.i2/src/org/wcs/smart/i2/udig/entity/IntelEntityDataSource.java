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
package org.wcs.smart.i2.udig.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.util.UuidUtils;

/**
 * Geotools data store for entity locations
 * 
 * @author Emily
 */
public class IntelEntityDataSource extends ContentDataStore{

	private UUID entityUuid;
	
	public IntelEntityDataSource(UUID entityUuid){
		this.entityUuid = entityUuid;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new IntelEntityFeatureSource(entry, entityUuid);
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		for (LocationLayerType layertype : LocationLayerType.values()){
			names.add(generateName(layertype, entityUuid));
		}
		return names;
	}
	
	public static Name generateName(LocationLayerType type, UUID entityUuid){
		if (type == LocationLayerType.ATTRIBUTE){
			return new NameImpl("org.wcs.smart.i2.entity." + UuidUtils.uuidToString(entityUuid), type.name());	 //$NON-NLS-1$
		}
		return new NameImpl("org.wcs.smart.i2.entity.location." + UuidUtils.uuidToString(entityUuid), type.name()); //$NON-NLS-1$
	}

	public static Filter createDateFilter(Date startDate, Date endDate){
		if (startDate == null || endDate == null) return Filter.INCLUDE;
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		return ff.between(ff.property("date"), ff.literal(startDate), ff.literal(endDate)); //$NON-NLS-1$
	}
}
