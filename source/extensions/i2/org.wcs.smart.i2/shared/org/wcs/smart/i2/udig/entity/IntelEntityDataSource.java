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
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.i2.udig.IntelRecordDataSource;
import org.wcs.smart.util.UuidUtils;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class IntelEntityDataSource extends ContentDataStore{

	private UUID entityUuid;
	private Date[] dateFilter;
	
	public IntelEntityDataSource(UUID entityUuid){
		this.entityUuid = entityUuid;
		this.dateFilter = null;
	}

	/**
	 * Two element date array representing the start and end day filters
	 */
	public void setDateFilter(Date[] dateFilter){
		this.dateFilter = dateFilter;
	}
	
	public Date[] getDateFilter(){
		return dateFilter;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new IntelEntityFeatureSource(entry, entityUuid);
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		for (IntelRecordDataSource.Type layertype : IntelRecordDataSource.Type.values()){
			names.add(generateName(layertype, entityUuid));
		}
		return names;
	}
	
	public static Name generateName(IntelRecordDataSource.Type type, UUID entityUuid){
		return new NameImpl("org.wcs.smart.i2.entity.location." + UuidUtils.uuidToString(entityUuid), type.name());
	}

}
