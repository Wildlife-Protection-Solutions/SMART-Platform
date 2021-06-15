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
package org.wcs.smart.incident.birt.attachments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.incident.birt.AbstractIncidentResultSet;
import org.wcs.smart.incident.birt.SmartIncidentConnection;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Incident attachmenta dataset result set.
 * @author Emily
 *
 */
public class IncidentAttachmentDatasetResultSet extends AbstractIncidentResultSet<IncidentAttachmentDatasetResultSetMetadata>{
	
	private List<ISmartAttachment> allAttachments;
	
	/**
	 * new dataset
	 * @param incidentUuid
	 * @param metadata
	 * @param connection
	 */
	public IncidentAttachmentDatasetResultSet(UUID incidentUuid,
			IncidentAttachmentDatasetResultSetMetadata metadata, SmartIncidentConnection connection) {
		super(connection, metadata);
		
		Waypoint incident = session.get(Waypoint.class, incidentUuid);
		if (incident == null) {
			m_maxRows = 0;
		}else {
			
			allAttachments = new ArrayList<>();
			incident.getAttachments().forEach(wa->{
				try {
					wa.computeFileLocation(session);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				allAttachments.add(wa);
			});
			
			for (WaypointObservation wo : incident.getAllObservations()) {
				for (ObservationAttachment oa : wo.getAttachments()) {
					try {
						oa.computeFileLocation(session);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					allAttachments.add(oa);
				}
			}
			m_maxRows = allAttachments.size();
		}
	}

	@Override
	public Object findCurrentValue(int index) {
		ISmartAttachment item = allAttachments.get(currentRow);
		return IncidentAttachmentDatasetResultSetMetadata.Column.values()[index-1].getValue(item);
	}
	
}