/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol;

import java.util.Locale;

import org.wcs.smart.cybertracker.patrol.json.PatrolJsonImportWarning;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonProcessor;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;


/**
 * @since 8.0
 */
public class PatrolCyberTrackerLabelProvider implements IPatrolCyberTrackerLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == PatrolJsonImportWarning.WarningType.STATION_NOT_FOUND) return "Station value not found. Station will be empty.";
		if (item == PatrolJsonImportWarning.WarningType.TEAM_NOT_FOUND) return "Team value not found. Station will be empty.";
		if (item == PatrolJsonImportWarning.WarningType.MANDATE_NOT_FOUND) return "Mandate value not found. Station will be empty.";
		if (item == PatrolJsonImportWarning.WarningType.MEMBER_NOT_FOUND) return "Member not found. Member will not be added to patrol.";
		if (item == PatrolJsonImportWarning.WarningType.TT_NOT_FOUND_ERROR) return "Patrol transport type not found. Patrol part will not be imported.";
		if (item == PatrolJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES) return "The track point {0} matches multiple patrols [{1}].  Ensure the patrol days and times do not overlap and try again.";
		if (item == PatrolJsonImportWarning.WarningType.PATROL_NOT_FOUND) return "No patrol found for 'add to previous waypoint' observation.";
		if (item == PatrolJsonImportWarning.WarningType.DUPLICATE) return "Possible duplicate processing of file. The patrol {0} linked with this SMART Mobile data already exists in the database with an observation counter greater than the observation counter in the file ({1} > {2})";
		
		
		if (item == PatrolJsonProcessor.StatusMessage.ADDED) return "Created {0} patrols";
		if (item == PatrolJsonProcessor.StatusMessage.MODIFIED) return "Modified {0} patrols";
		
		if (item == PatrolJsonProcessor.CA_ERROR) return "The Conservation Area associated with the file ({0}), does not match the Conservation Area of the patrol currently linked to this data ({1})";
		
		return null;
	}

}
