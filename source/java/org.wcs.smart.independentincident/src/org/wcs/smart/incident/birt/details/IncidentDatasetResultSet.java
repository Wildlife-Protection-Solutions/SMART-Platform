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
package org.wcs.smart.incident.birt.details;

import java.util.UUID;

import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.incident.birt.AbstractIncidentResultSet;
import org.wcs.smart.incident.birt.SmartIncidentConnection;
import org.wcs.smart.incident.birt.details.IncidentDatasetResultSetMetadata.Column;
import org.wcs.smart.observation.model.Waypoint;

/**
 * SMRAT Plan target result set
 * @author Emily
 * @since 2.0.0
 *
 */
public class IncidentDatasetResultSet extends AbstractIncidentResultSet<IncidentDatasetResultSetMetadata>{
	
	private Waypoint incident;
	
	/**
	 * Creates a new summary results set
	 * 
	 * @param query
	 *            the summary query
	 * @param metadata
	 *            the metadata
	 */
	public IncidentDatasetResultSet(UUID incidentUuid,
			IncidentDatasetResultSetMetadata metadata, SmartIncidentConnection connection) {
		super(connection, metadata);
		
		incident = session.get(Waypoint.class, incidentUuid);
		if (incident == null) {
			m_maxRows = 0;
		}else {
			m_maxRows = 1;
		}
	}

	@Override
	public Object findCurrentValue(int index) {
		return Column.values()[index-1].getValue(incident);
	}
	
}