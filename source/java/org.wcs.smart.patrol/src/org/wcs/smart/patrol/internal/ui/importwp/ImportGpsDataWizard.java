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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.gpx.WptType;
import org.wcs.smart.patrol.internal.ui.importwp.GPSDataImport.ImportType;
import org.wcs.smart.patrol.internal.ui.importwp.gpsbabel.GPSBabel;

/**
 * Wizard for importing data from GPS Device
 * @author Emily
 * @since 1.0.0
 */
public class ImportGpsDataWizard extends Wizard implements IPageChangingListener{

	
	private GPSDataImport.ImportType type ;
	
	private boolean completedOK = false;
	private boolean canFinish = false;
	private Session session = null;
	private Date currentDay;
	
	//a track or a list of waypoints
	private Object importedData = null;
	private List<WptType> allWaypoints = null;	//list of waypoint for either importing waypoints or importing track points
	private boolean allData = false;
	/**
	 * Creates a new wizard.
	 */
	public ImportGpsDataWizard(Date currentDay, GPSDataImport.ImportType type) {
		setWindowTitle("Import " + type + " Data");
		this.currentDay = currentDay;
		this.type = type;
		super.setForcePreviousAndNextButtons(true);
	}
		
	/**
	 * Sets if the wizard can finish
	 * @param canFinish if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish){		
		this.canFinish = canFinish;
	}
	
	public Date getCurrentDate(){
		return this.currentDay;
	}
	
	@Override
	public boolean canFinish(){
		return this.canFinish;
	}

	public ImportType getType(){
		return this.type;
	}

	/**
	 * Creates a new session and attaches
	 * the current conservation area.
	 * 
	 * @return
	 */
	public Session getSession(){
		if (session == null || !session.isOpen()){
			session = PatrolHibernateManager.openSession();
		}
		return session;
	}
	
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		super.addPage(new ImportWpTypeWizardPage());
		
		setCanFinish(false);
	}
	
	/**
	 * 
	 * @return true if the wizard completed okay with no errors; false if error occurred
	 * while finishing wizard
	 */
	public boolean isCompletedOk(){
		return completedOK;
	}

	public void setAllWaypoints(List<WptType> allWaypoints){
		this.allWaypoints = allWaypoints;
	}
	public List<WptType> getWaypoints(){
		return this.allWaypoints;
	}
	
	/**
	 * 
	 */
	@Override
	public boolean performFinish() {
		if (lastPage == null){
			return false;
		}
		if (lastPage instanceof ImportGpxWizardPage){
			//finished; import data from file for current date
			allData = ((ImportGpxWizardPage)lastPage).getImportAll();
			final String filename = ((ImportGpxWizardPage)lastPage).getFileName();
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			
			try {
				pmd.run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						if (allData){
							importedData = (GPSDataImport.convertGpx(new File(filename), null, Collections.singleton(type), monitor)).get(type);
						}else{
							importedData = (GPSDataImport.convertGpx(new File(filename), currentDay, Collections.singleton(type), monitor)).get(type);
								
						}
					}
				});
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog("Could not import data from pgx file. " + ex.getMessage(), ex);
				return false;
			}
		}else if (lastPage instanceof ImportGPSWizardPage ){
			//import data from gps device
			this.importedData = null;
			final String deviceType = ((ImportGPSWizardPage)lastPage).getDeviceType();
			allData  = ((ImportGPSWizardPage)lastPage).getImportAll();
			
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			
			try {
				pmd.run(false, false, new IRunnableWithProgress(){
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException{
						try{
						if (allData){
							importedData = ( GPSDataImport.importGpsData(deviceType, null, Collections.singleton(type), monitor) ).get(type);
						}else{
							importedData = ( GPSDataImport.importGpsData(deviceType, currentDay, Collections.singleton(type), monitor) ).get(type);
						}
						}catch(Exception ex){
							SmartPatrolPlugIn.displayLog("Could not import data from gps device. " + ex.getMessage(), ex);
						}
					}
				});
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog("Could not import data from gps device. " + ex.getMessage(), ex);
				return false;
			}
		
		}else if (lastPage instanceof ImportWpSelectWizardPage ){
			allData = false;
			//select data to import
			if (type == GPSDataImport.ImportType.WAYPOINT){
				importedData = GPSDataImport.convertWaypoints( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
			}else if (type == GPSDataImport.ImportType.TRACK){
				importedData = GPSDataImport.convertPointsToTrack( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
			}
			
		};
		
		if (importedData  == null || (importedData instanceof Collection &&  ((Collection)importedData).size() == 0 )){
			MessageDialog.openWarning(getShell(), "Import", "No " + this.type + " were found to import. This could be due to date formats or other GPS issues.  Try importing all " + this.type + " and selecting desired waypoints.");
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @return the data imported by the wizard.  May be null if no data imported.
	 */
	public Object getImportedData(){
		return this.importedData;
	}
	
	/**
	 * 
	 * @return <code>true</code> if all data is imported and should be assigned to correct leg day; <code>false</code> if only
	 * data for current leg dat was imported
	 */
	public boolean getImportAll(){
		return this.allData;
	}
	

	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		allWaypoints = null;
		if (event.getTargetPage() instanceof ImportWpSelectWizardPage){
			if (event.getCurrentPage() instanceof ImportGpxWizardPage){
				//read all waypoints from gpx file
				final String filename = ((ImportGpxWizardPage)event.getCurrentPage()).getFileName();
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						if (type == GPSDataImport.ImportType.WAYPOINT){
							allWaypoints = GPSDataImport.getWaypointsGpx(new File(filename), monitor);
						}else if (type == GPSDataImport.ImportType.TRACK){
							allWaypoints = GPSDataImport.getTrackPoints(new File(filename), monitor);
						}
					}
				});
				}catch (Exception ex){
					SmartPatrolPlugIn.displayLog("Could not import data from GPX file", ex);
					event.doit = false;
				}
				
			}else if (event.getCurrentPage() instanceof ImportGPSWizardPage){
				final String deviceType = ((ImportGPSWizardPage)event.getCurrentPage()).getDeviceType();
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try{
							monitor.setTaskName("Importing Data from GPS Device");
							File f = GPSBabel.getData(deviceType, Collections.singleton(type));
							if (type == ImportType.WAYPOINT){
								allWaypoints = GPSDataImport.getWaypointsGpx(f, monitor);
							}else if (type == ImportType.TRACK){
								allWaypoints = GPSDataImport.getTrackPoints(f, monitor);
							}
						}catch (Exception ex){
							SmartPatrolPlugIn.displayLog("Could not import data from GPX file", ex);
						}
					}
				});
				}catch (Exception ex){
					SmartPatrolPlugIn.displayLog("Could not import data from GPX file", ex);
					event.doit = false;
				}
				
	
			}
			if (allWaypoints == null){
				event.doit = false;
			}else if (allWaypoints.size() == 0){
				MessageDialog.openInformation(getShell(), "Import", "No " + type.guiName + "s were found.  Ensure you have selected the correct file or device and try again.");
				event.doit = false;
			}else{
				((ImportWpSelectWizardPage)event.getTargetPage()).setWaypoints(allWaypoints);
			}
		}
		
		
		if (event.doit){
			lastPage = (WizardPage)event.getTargetPage();
		}
	}
	private WizardPage lastPage = null;

	
}
