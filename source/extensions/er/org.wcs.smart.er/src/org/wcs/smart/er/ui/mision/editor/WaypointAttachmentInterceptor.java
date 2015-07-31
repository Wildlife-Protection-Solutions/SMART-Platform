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
package org.wcs.smart.er.ui.mision.editor;

import java.io.Serializable;

import org.hibernate.type.Type;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * An extension of the default attachment interceptor
 * that also deletes waypoint attachments from patrol leg days.
 *  
 * @author Emily
 * @since 1.0.0
 */
public class WaypointAttachmentInterceptor extends AttachmentInterceptor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * When a parent object is deleted it also deletes the file on disk.
	 */
	@Override
    public void onDelete(Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types) {
		
		super.onDelete(entity, id, state, propertyNames, types);
		
		/**
		 * PatrolLegDay deletes are cascaded to waypoints in the
		 * database and not in hibernate; therefore we need
		 * to make sure we delete the attachments
		 * here; otherwise they get left behind
		 */
		if (entity instanceof Mission){
			for (MissionDay md : ((Mission) entity).getMissionDays()){
				if (md.getWaypoints() != null){
					for (SurveyWaypoint wp : md.getWaypoints()){
						if (wp.getWaypoint().getAttachments() != null){
							for (ISmartAttachment att : wp.getWaypoint().getAttachments()){
								try {
									toDelete.add(att.getFullFile());
								} catch (Exception e) {
									EcologicalRecordsPlugIn.log(e.getMessage(), e);
								}
							}
						}
						if (wp.getWaypoint().getObservations() != null){
							for (WaypointObservation wo : wp.getWaypoint().getObservations()){
								if (wo.getAttachments()!= null){
									for (ObservationAttachment att : wo.getAttachments()){
										try {
											toDelete.add(att.getFullFile());
										} catch (Exception e) {
											EcologicalRecordsPlugIn.log(e.getMessage(), e);
										}
									}
								}
							}
						}
					}
				}
			}
		}
    	
    }


}
