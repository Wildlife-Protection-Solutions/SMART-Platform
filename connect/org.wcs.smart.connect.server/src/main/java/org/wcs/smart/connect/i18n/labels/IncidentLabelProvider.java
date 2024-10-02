/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.incident.json.IncidentJsonFeatureProcessor;
import org.wcs.smart.incident.model.IncidentType;

/**
 * Label provider for incident plugin.
 * @author Emily
 *
 */
public class IncidentLabelProvider implements IIncidentLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == IncidentType.DefaultType.INCIDENT) return Messages.getString("IncidentLabelProvider.IncidentLabel", l); //$NON-NLS-1$
		if (item == IncidentType.DefaultType.INTEGRATE) return Messages.getString("IncidentLabelProvider.SmartIntegrateIncident", l); //$NON-NLS-1$
		if (item == IncidentType.DefaultType.INTEGRATE_MOVE) return Messages.getString("IncidentLabelProvider.IntegrateMoveToPatrolIncident", l); //$NON-NLS-1$
		if (item == IncidentType.DefaultType.INTEGRATE_LINK) return Messages.getString("IncidentLabelProvider.IntegrateLinkToPatrolIncident", l); //$NON-NLS-1$
		
		if (item == IncidentJsonFeatureProcessor.Messages.COMPLETE_MSG) return Messages.getString("IncidentLabelProvider.createIncidentMsg",l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.INVALID_DATA_TYPE) return Messages.getString("IncidentLabelProvider.invalidSmartDataTypeJson",l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.INVALID_FEATURE_TYPE) return Messages.getString("IncidentLabelProvider.invalidSmartFeatureTypeJson",l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.OBSERVATION_EXISTS) return Messages.getString("IncidentLabelProvider.ObservationExists", l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.MISSING_PROPERTY) return Messages.getString("IncidentLabelProvider.MissionProperty", l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.WAYPOINT_NOT_FOUND) return Messages.getString("IncidentLabelProvider.WaypointNotFound", l); //$NON-NLS-1$
		if (item == IncidentJsonFeatureProcessor.Messages.OBSERVATION_NOT_FOUND) return Messages.getString("IncidentLabelProvider.ObservationNotFound", l); //$NON-NLS-1$
		
		return null;
	}

}
