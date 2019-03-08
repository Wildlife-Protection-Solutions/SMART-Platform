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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityType;

/**
 * Data source for fixed entity type locations.  Displays
 * the locations of all the entities.
 * <p>Data source supports all 
 * fixed entity type in database for a given conservation area.  Each
 * fixed entity type represents a different type.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedEntityDataSource extends ContentDataStore{

	private HashMap<String, EntityType> cachedTypes = new HashMap<String, EntityType>();
	
	public FixedEntityDataSource(){
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	/**
	 * Updates the schema for a given entity type
	 * @param eType
	 */
	public void refresh(EntityType eType){
		String key = eType.getKeyId();
		//remove schemas from cache
		super.removeEntry(new NameImpl(key));
		cachedTypes.put(key, eType);
	}


	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<>();
		
		for (EntityType et : EntityHibernateManager.getInstance().getActiveEntityTypes()){
			if (et.getType() == EntityType.Type.FIXED){
				names.add(new NameImpl(et.getKeyId()));
			}
		}
		return names;
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		EntityType et = cachedTypes.get(entry.getTypeName());
		return new FixedEntityFeatureSource(entry, et);
	}
	

}
