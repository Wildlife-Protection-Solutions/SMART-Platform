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
package org.wcs.smart.incident;

import java.util.Locale;

import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.json.IncidentJsonFeatureProcessor;
import org.wcs.smart.incident.model.IncidentType;
/**
 * Label provider for independent incidents
 * 
 * @author Emily
 *
 */
public class IncidentLabelProvider implements IIncidentLabelProvider {
	
	public static final String TYPE_NAME = Messages.IncidentLabelProvider_IncidentTypeLabel;
	
	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof IndepedentIncidentSource ){
			 return Messages.IndepedentIncidentSource_IndIncidentWaypointsourceName;
		}
				
		if (item == IncidentType.DefaultType.INCIDENT)  return  Messages.IndepedentIncidentSource_IndIncidentWaypointsourceName;
		if (item == IncidentType.DefaultType.INTEGRATE) return Messages.IncidentLabelProvider_SmartIntegrateIncident;
		if (item == IncidentType.DefaultType.INTEGRATE_LINK) return Messages.IncidentLabelProvider_LinkToPatrolIncidentSource;
		if (item == IncidentType.DefaultType.INTEGRATE_MOVE) return Messages.IncidentLabelProvider_MoveToPatrolIncidentSource;
		
		if (item == IncidentJsonFeatureProcessor.Messages.COMPLETE_MSG) return Messages.IncidentLabelProvider_jsonloaded;
		if (item == IncidentJsonFeatureProcessor.Messages.INVALID_DATA_TYPE) return Messages.IncidentLabelProvider_invaliddatatype;
		if (item == IncidentJsonFeatureProcessor.Messages.INVALID_FEATURE_TYPE) return Messages.IncidentLabelProvider_invalidfeaturetype;
		if (item == IncidentJsonFeatureProcessor.Messages.OBSERVATION_EXISTS) return Messages.IncidentLabelProvider_ObservationExistsCannotUpdate;
		if (item == IncidentJsonFeatureProcessor.Messages.MISSING_PROPERTY) return Messages.IncidentLabelProvider_JsonMissing;
		if (item == IncidentJsonFeatureProcessor.Messages.WAYPOINT_NOT_FOUND) return Messages.IncidentLabelProvider_WpNotFound;
		if (item == IncidentJsonFeatureProcessor.Messages.OBSERVATION_NOT_FOUND) return Messages.IncidentLabelProvider_ObservationNotFound;
		
		return null;
	}
}
