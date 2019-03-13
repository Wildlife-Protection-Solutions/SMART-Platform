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
package org.wcs.smart.entity.map;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.entity.query.EntitySightingQuery;
/**
 * Data source for entity sighting queries.
 * @author Emily
 *
 */
public class EntityQueryDataSource extends ContentDataStore{

	public static final String TYPENAME = "ENTITY_QUERY"; //$NON-NLS-1$

	private EntitySightingQuery query;
	
	public EntityQueryDataSource(EntitySightingQuery query){
		this.query = query;
	}
	
	public EntitySightingQuery getQuery() {
		return this.query;
	}
	
	@Override
	public void dispose(){
		super.dispose();
	}

	public void refresh(EntitySightingQuery query){
		removeEntry(new NameImpl(TYPENAME));
	}


	@Override
	protected List<Name> createTypeNames() throws IOException {
		return Collections.singletonList(new NameImpl(TYPENAME));
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new EntityQueryFeatureSource(entry);
	}
}

