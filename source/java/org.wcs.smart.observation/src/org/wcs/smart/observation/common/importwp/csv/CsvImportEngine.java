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
package org.wcs.smart.observation.common.importwp.csv;

import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.IImportEngine;
import org.wcs.smart.observation.common.importwp.IImportWizardPage;
import org.wcs.smart.observation.common.importwp.ImportCsvWizardPage;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
/**
 * Import engine for importing waypoints from a CSV File.
 * 
 * @author Emily
 *
 */
public abstract class CsvImportEngine implements IImportEngine {

	private CSVImportConfiguration configuration;
	
	@Override
	public String getName() {
		return Messages.ImportWpTypeWizardPage_0;
	}

	/**
	 * Supports only waypoints
	 */
	@Override
	public boolean supportsType(ImportType type) {
		return (type == ImportType.WAYPOINT);
	}

	/**
	 * 
	 * @return the current waypoint configuration; creates
	 * a new one if not yet set
	 */
	public CSVImportConfiguration getConfiguration(){
		if (configuration == null){
			configuration = new CSVImportConfiguration();
		}
		return configuration;
	}
	
	/**
	 * reads the waypoints based on the configuration
	 */
	@Override
	public List<Waypoint> getWaypoints(ImportOption option, ImportType type, 
			Date currentDate, IProgressMonitor monitor) throws Exception{
		if (option == ImportOption.DATE){
			return getConfiguration().getWaypoints(monitor, currentDate);
		}else{
			return getConfiguration().getWaypoints(monitor, null);
		}
	}

	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		return new ImportCsvWizardPage(wizard);
	}

//	/**
//	 * Saves waypoints to patrol
//	 */
//	@Override
//	public String updatePatrol(ImportOption option, ImportType type,
//			Patrol patrol, PatrolLegDay currentLeg, List<Waypoint> waypoints,
//			IProgressMonitor monitor) throws Exception {
//
//		if(type == ImportType.WAYPOINT){
//
//			//if no ID was given, get the largest ID from the patrol 
//			//so far and reset the id's of the points about to be saved. 
//			if (configuration.getIdColumn() == -1){
//				int max = 0;
//				Patrol p = currentLeg.getPatrolLeg().getPatrol();
//				for( PatrolLeg pl : p.getLegs() ){
//					for(PatrolLegDay pld : pl.getPatrolLegDays()){
//						for(PatrolWaypoint wp : pld.getWaypoints()){
//							if (wp.getWaypoint().getId() > max){
//								max = wp.getWaypoint().getId();
//							}
//						}
//					}
//				}
//				for(Waypoint wp : waypoints){
//					wp.setId(max + 1);
//					max++;
//				}
//			}
//			monitor.setTaskName(MessageFormat.format(Messages.CsvImportEngine_SaveProgressMessage, new Object[]{waypoints.size()}));
//			return GPSDataImport.saveWaypoints(option, patrol, currentLeg, waypoints);
//		}
//		return null;
//	}
}
