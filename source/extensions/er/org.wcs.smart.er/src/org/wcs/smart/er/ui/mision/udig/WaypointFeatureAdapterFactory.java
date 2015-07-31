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
package org.wcs.smart.er.ui.mision.udig;

import java.util.UUID;

import org.eclipse.core.runtime.IAdapterFactory;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * Adpater factory for adapting SimpleFeature with WaypointFeatureType
 * to Waypoints.
 * 
 * @author egouge
 *
 */
public class WaypointFeatureAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == Waypoint.class) {
			if (adaptableObject instanceof SurveyFeature){
				SurveyFeature sf = (SurveyFeature)adaptableObject;
				if (sf.getFeatureType().getTypeName().equals(MissionDataSource.MISSIONWAYPOINT_TYPE)){
					String key = sf.getID();
					String uuids = key.substring(key.lastIndexOf('.') + 1);
					UUID wpuuid;
					try {
						wpuuid = UuidUtils.stringToUuid(uuids);
					} catch (Exception e) {
						EcologicalRecordsPlugIn.log("Could not determine waypoint for uuid " + uuids, e); //$NON-NLS-1$
						return null;
					}
					Waypoint wp = new Waypoint();
					wp.setUuid(wpuuid);
					return wp;
				}
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[]{Waypoint.class};
	}

}
