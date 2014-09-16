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
package org.wcs.smart.er.ui.mision.importwp;

import java.sql.Time;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.editor.SaveWaypointJob;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Class of utilities that support
 * the importing of waypoints and tracks from a variety 
 * of sources 
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDataImport {

	/**
	 * Saves a set of waypoints to the database
	 * 
	 * @param op import option
	 * @param mission mission
	 * @param waypoints set of waypoints
	 * @return status message
	 * @throws InterruptedException
	 */
	public static String saveWaypoints(ImportOption op, final Mission mission, Date date, List<Waypoint> waypoints) throws InterruptedException {
		String message = null;
		final List<SurveyWaypoint> addedWaypoints = new ArrayList<SurveyWaypoint>();
		for (Waypoint w : waypoints) {
			SurveyWaypoint swp = new SurveyWaypoint();
			swp.setMission(mission);
			swp.setWaypoint(w);				
			
			mission.getWaypoints().add(swp);
			addedWaypoints.add(swp);
			if (op == ImportOption.SELECT && date != null) {
				Date wpdt = date;
				if (swp.getWaypoint().getDateTime() != null) {
					wpdt = SmartUtils.combineDateTime(wpdt, new Time(swp.getWaypoint().getDateTime().getTime()));
				}
				swp.getWaypoint().setDateTime(wpdt);
			}
		}
		message = MessageFormat.format("Imported {0} waypoints.", new Object[]{addedWaypoints.size()});
		
		//start up a save job
		SaveWaypointJob saveJob = new SaveWaypointJob();
		saveJob.setWaypoints(addedWaypoints);
		saveJob.schedule();
		saveJob.join();
		
		//fire events
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
			}});
		
		return message;
	}
	
}
