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
package org.wcs.smart.patrol.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Geotools data store for SMART area layers.
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDataSource extends ContentDataStore{

	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	public static final String TRACK_PART_TYPE = "Track"; //$NON-NLS-1$
	
	private Patrol patrol;
	
	public PatrolDataSource(Patrol patrol){
		this.patrol = patrol;
	}

	/**
	 * updates the patrol object associated with the data source
	 * @param patrol
	 */
	public void updatePatrol(Patrol patrol){
		this.patrol = patrol;
	}

	public Patrol getPatrol() {
		return this.patrol;
	}
	
	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<Name>();
		names.add(new NameImpl(WAYPOINT_TYPE));
		names.add(new NameImpl(TRACK_PART_TYPE));
		return names;
	}
	

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new PatrolFeatureSource(entry);
	}
	


}
