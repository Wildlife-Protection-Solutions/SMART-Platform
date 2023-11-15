/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt.observations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.incident.birt.AbstractIncidentResultSet;
import org.wcs.smart.incident.birt.SmartIncidentConnection;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * SMRAT Incident observation attributes result set
 * 
 * @author Emily
 *
 */
public class IncidentObservationAttributeDatasetResultSet extends AbstractIncidentResultSet<IncidentObservationAttributeDatasetResultSetMetadata>{
	
	private List<WaypointObservationAttribute> allObservations;
	/**
	 * Creates a new summary results set
	 * 
	 * @param query
	 *            the summary query
	 * @param metadata
	 *            the metadata
	 */
	public IncidentObservationAttributeDatasetResultSet(UUID incidentUuid,
			IncidentObservationAttributeDatasetResultSetMetadata metadata, SmartIncidentConnection connection) {
		super(connection, metadata);
		
		Waypoint incident = session.get(Waypoint.class, incidentUuid);
		if (incident == null) {
			m_maxRows = 0;
		}else {
			allObservations = new ArrayList<>();
			for (WaypointObservation wo : incident.getAllObservations()) {
				allObservations.addAll(wo.getAttributesSorted());
			}
			m_maxRows = allObservations.size();
		}
	}

	@Override
	public Object findCurrentValue(int index) {
		WaypointObservationAttribute item = allObservations.get(currentRow);
		return IncidentObservationAttributeDatasetResultSetMetadata.Column.values()[index-1].getValue(item);
	}
	
}