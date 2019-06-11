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
package org.wcs.smart.geotools.data.smart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class SmartDataSource extends ContentDataStore{

	private UUID ca = null;
	private IDatabaseConnectionProvider connectionProvider;
	
	public SmartDataSource(UUID ca, IDatabaseConnectionProvider connectionProvider){
		this.ca = ca;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public void dispose(){
		super.dispose();
	}

	public UUID getConservationArea() {
		return this.ca;
	}
	public IDatabaseConnectionProvider getDbConnection() {
		return this.connectionProvider;
	}
	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		for (int i = 0; i < Area.AreaType.values().length; i ++){
			names.add(new NameImpl("smart." + Area.AreaType.values()[i].name(), Area.AreaType.values()[i].name() )); //$NON-NLS-1$
		}
		
		return names;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		AreaType at = AreaType.valueOf(entry.getName().getLocalPart());
		return new SmartFeatureSource(entry, at);
	}

}
