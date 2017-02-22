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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.IImportWizardPage;
import org.wcs.smart.observation.common.importwp.ImportCsvWizardPage;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.csv.CsvImportEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;

/**
 * Import engine for importing waypoints from a CSV File.
 * 
 * @author Emily
 * @author elitvin
 * @since 3.0.0
 */
public class PatrolCsvImportEngine extends CsvImportEngine {

	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> waypoints, IProgressMonitor monitor)
			throws Exception {
		if(type == ImportType.WAYPOINT){
			PatrolLegDay currentLeg = (PatrolLegDay) object;
			Patrol patrol = currentLeg.getPatrolLeg().getPatrol();

			//if no ID was given, get the largest ID from the patrol 
			//so far and reset the id's of the points about to be saved. 
			if (getConfiguration().getIdColumn() == -1){
				int max = 0;
				Patrol p = currentLeg.getPatrolLeg().getPatrol();
				for( PatrolLeg pl : p.getLegs() ){
					for(PatrolLegDay pld : pl.getPatrolLegDays()){
						for(PatrolWaypoint wp : pld.getWaypoints()){
							if (wp.getWaypoint().getId() > max){
								max = wp.getWaypoint().getId();
							}
						}
					}
				}
				for(Waypoint wp : waypoints){
					wp.setId(max + 1);
					max++;
				}
			}
			monitor.setTaskName(MessageFormat.format(Messages.CsvImportEngine_SaveProgressMessage, new Object[]{waypoints.size()}));
			return PatrolGPSDataImport.saveWaypoints(option, patrol, currentLeg, waypoints);
		}
		return null;
	}

	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		ImportCsvWizardPage page = new ImportCsvWizardPage(wizard);
		PatrolImportGpsDataWizard w = (PatrolImportGpsDataWizard) wizard;
		PatrolOptionsLabelUtil.updateLabels(page, w);
		return page;
	}
	
}
