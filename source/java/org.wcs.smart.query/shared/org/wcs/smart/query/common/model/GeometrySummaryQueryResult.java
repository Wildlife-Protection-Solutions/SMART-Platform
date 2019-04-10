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
package org.wcs.smart.query.common.model;

import java.util.HashMap;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;

/**
 * Summary query result that include a single row group by
 * with coordinates associated with the group by item for
 * display the results on a map 
 * 
 * @author Emily
 *
 */
public class GeometrySummaryQueryResult extends SummaryQueryResult {
	
	public final static String GEOMETRY_COLUMN_KEY = "summary:geometry"; //$NON-NLS-1$
	public final static String GEOMETRY_COLUMN_NAME = "Geometry"; //$NON-NLS-1$
	
	private HashMap<UUID, Coordinate> positions;
	
	public GeometrySummaryQueryResult() {
		super();
	}
	
	/**
	 * Adds a position for the row element identified by the uuid
	 * @param uuid
	 * @param c
	 */
	public void addCoordinateDetails(UUID uuid, Coordinate c) {
		if (positions == null) positions = new HashMap<>();
		positions.put(uuid, c);
	}

	/**
	 * Finds the position element for the row element
	 * identified by the uuid
	 * 
	 * @param uuid
	 * @return
	 */
	public Coordinate getCoordinate(UUID uuid) {
		if (positions == null) return null;
		return positions.get(uuid);
	}
	
}
