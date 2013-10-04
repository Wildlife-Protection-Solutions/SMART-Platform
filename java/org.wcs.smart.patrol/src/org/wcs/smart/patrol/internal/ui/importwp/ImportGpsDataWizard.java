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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.gpx.WptType;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.GPSDataImport.ImportType;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CSVImportConfiguration;
import org.wcs.smart.patrol.internal.ui.importwp.gpsbabel.GPSBabel;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Waypoint;

/**
 * Wizard for importing data from GPS Device
 * @author Emily
 * @since 1.0.0
 */
public class ImportGpsDataWizard extends Wizard implements IPageChangingListener{

	
	public static final String CREATE_FROM_WAYPOINTS = "CreateFromWaypoints"; //$NON-NLS-1$

	private static final String GPX_FILE_ERROR = Messages.ImportGpsDataWizard_GPXFileImportError;
	private static final String GPS_DEVICE_ERROR = Messages.ImportGpsDataWizard_GPSDeviceImportError;
	private static final String IMPORT_DIALOG_TITLE = Messages.ImportGpsDataWizard_ImportDialog_Title;
	
	private GPSDataImport.ImportType type ;
	
	private boolean completedOK = false;
	private boolean canFinish = false;
	private PatrolLegDay currentDay;
	
	//a track or a list of waypoints
	private Object importedData = null;
	private List<WptType> allWaypoints = null;	//list of waypoint for either importing waypoints or importing track points
	private List<Waypoint> allWaypointsCsv = null;	//list of waypoints from csv import
	private boolean allData = false;
	
	//CSV options for getting all data, current day's data or select which point;
	//probably could use the same variables from other options, but for CSV it isn't on the previous page so I want to store the selected choice explicity.
	private boolean selectPoints = false;
	private boolean onlyTodays = false;
	private boolean allCsvData = true;
	
	private CSVImportConfiguration csvConfig;
	
	/**
	 * Creates a new wizard.
	 */
	public ImportGpsDataWizard(PatrolLegDay currentDay, GPSDataImport.ImportType type) {
		setWindowTitle(MessageFormat.format(Messages.ImportGpsDataWizard_DialogTitle, new Object[]{type.guiName}));
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
	
	public PatrolLegDay getCurrentDate(){
		return this.currentDay;
	}
	
	@Override
	public boolean canFinish(){
		return this.canFinish;
	}

	public ImportType getType(){
		return this.type;
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
	
	
	public void setAllWaypointsObj(List<Waypoint> allWaypointsObj){
		this.allWaypointsCsv = allWaypointsObj;
	}
	public List<Waypoint> getWaypointObj(){
		return this.allWaypointsCsv;
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
			allData = ((ImportGpxWizardPage)lastPage).getImportAll();
			final List<String> filenames = ((ImportGpxWizardPage)lastPage).getFiles();
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			
			try {
				pmd.run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						if (allData){
							importedData = (GPSDataImport.convertGpx(filenames, null, Collections.singleton(type), monitor)).get(type);
						}else{
							importedData = (GPSDataImport.convertGpx(filenames, currentDay.getDate(), Collections.singleton(type), monitor)).get(type);
						}
					}
				});
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog(GPX_FILE_ERROR + ex.getLocalizedMessage(), ex);
				return false;
			}
		}else if (lastPage instanceof ImportGPSWizardPage ){
			//import data from gps device
			this.importedData = null;
			final String deviceType = ((ImportGPSWizardPage)lastPage).getDeviceType();
			allData  = ((ImportGPSWizardPage)lastPage).getImportAll();
			
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			
			try {
				pmd.run(true, false, new IRunnableWithProgress(){
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException{
						try{
							if (allData){
								importedData = ( GPSDataImport.importGpsData(deviceType, null, Collections.singleton(type), monitor) ).get(type);
							}else{
								importedData = ( GPSDataImport.importGpsData(deviceType, currentDay.getDate(), Collections.singleton(type), monitor) ).get(type);
							}
						}catch(final Exception ex){
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
								}
							});
							
						}
					}
				});
				
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
				return false;
			}
		
		}else if (lastPage instanceof ImportWpSelectWizardPage ){
			// ---- IMPORT SELECTED WAYPOINTS/TRACKS ----
			allData = false;
			//select data to import
			if (type == GPSDataImport.ImportType.WAYPOINT){
				importedData = GPSDataImport.convertWaypoints( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
			}else if(type == GPSDataImport.ImportType.WAYPOINTCSV){
				importedData = ((ImportWpSelectWizardPage)lastPage).getSelectedWaypointsObj();
			}else if (type == GPSDataImport.ImportType.TRACK){
				importedData = GPSDataImport.convertPointsToTrack( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
			}
		}else if (lastPage instanceof ImportFromWaypointWizardPage){
			// ---- GENERATE TRACKS FROM WAYPOINTS ----
			allData = ((ImportFromWaypointWizardPage)lastPage).getImportAll();
			importedData = CREATE_FROM_WAYPOINTS;
		}else if (lastPage instanceof ImportCsvDetailsWizardPage ){
			setCsvConfigs((ImportCsvDetailsWizardPage)lastPage);
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			
			try {
				pmd.run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						if (onlyTodays){
							importedData = csvConfig.getWaypoints(monitor, currentDay.getDate());
						}else{
							importedData = csvConfig.getWaypoints(monitor, null);
						}
					}
				});
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog("Error Loading waypoints from CSV file" , ex);
				return false;
			}
			
		}
		
		//validation
		if (type == ImportType.TRACK){
			if (allData){
				boolean hasTracks = false;
				for(PatrolLeg pl : getCurrentDate().getPatrolLeg().getPatrol().getLegs()){
					for (PatrolLegDay pld : pl.getPatrolLegDays()){
						if (pld.getTrack() != null){
							hasTracks = true;
							break;
						}
					}
				}
				if (hasTracks){
					String warn = ""; //$NON-NLS-1$
					if  (lastPage instanceof ImportWpSelectWizardPage ){
						warn = Messages.ImportGpsDataWizard_TrackWarningOverwriteAll;
					}else{
						warn = Messages.ImportGpsDataWizard_TrackWarningOverwriteNew;
					}
					boolean cont= MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, warn);
					if (!cont){
						return false;
					}
				}
			}else{
				if (getCurrentDate().getTrack() != null){
					boolean cont= MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.PatrolLegDayInputComposite_SetTrackDialog_Message);
					if (!cont){
						return false;
					}
				}
			}
		}
		
		if (importedData  == null || (importedData instanceof Collection &&  ((Collection<?>)importedData).size() == 0 )){
			MessageDialog.openWarning(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new Object[]{this.type.guiName, this.type }));
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
	 * @return <code>true</code> if all data is imported and 
	 * should be assigned to correct leg day; <code>false</code> if only
	 * data for current leg day was imported
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
		allWaypointsCsv = null;
		if (event.getTargetPage() instanceof ImportWpSelectWizardPage){
			if (event.getCurrentPage() instanceof ImportGpxWizardPage){
				//read all waypoints from gpx file
				final List<String> filenames = ((ImportGpxWizardPage)event.getCurrentPage()).getFiles();
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						if (type == GPSDataImport.ImportType.WAYPOINT){
							allWaypoints = GPSDataImport.getWaypointsGpx(filenames, monitor);
						}else if (type == GPSDataImport.ImportType.TRACK){
							allWaypoints = GPSDataImport.getTrackPoints(filenames, monitor);
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
						File f = null;
						try{
							monitor.setTaskName(Messages.ImportGpsDataWizard_Progress_ImportingGPS);
							f = GPSBabel.getData(deviceType, Collections.singleton(type));
							if (type == ImportType.WAYPOINT){
								allWaypoints = GPSDataImport.getWaypointsGpx(Collections.singletonList(f.getAbsolutePath()), monitor);
							}else if (type == ImportType.TRACK){
								allWaypoints = GPSDataImport.getTrackPoints(Collections.singletonList(f.getAbsolutePath()), monitor);
							}
						}catch (final Exception ex){
							Display.getDefault().syncExec(new Runnable(){
								@Override
								public void run() {
									SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
								}});
							
						}finally{
							try{
								if (f != null){
									f.delete();
								}
							}catch (final Exception ex){
								Display.getDefault().syncExec(new Runnable(){
									@Override
									public void run() {
										Display.getDefault().syncExec(new Runnable(){
											@Override
											public void run() {
												SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
											}});
									}});
							}
						}
					}
				});
				}catch (Exception ex){
					SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
					event.doit = false;
				}
				
	
			}else if(event.getCurrentPage() instanceof ImportCsvDetailsWizardPage){
				final ImportCsvDetailsWizardPage currentPage = (ImportCsvDetailsWizardPage)event.getCurrentPage();
				type = GPSDataImport.ImportType.WAYPOINTCSV;
				setCsvConfigs((ImportCsvDetailsWizardPage)event.getCurrentPage());

				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
					pmd.run(true, false, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							allWaypointsCsv = csvConfig.getWaypoints(monitor, null);
						}
					});
				}catch (Exception ex){
					SmartPatrolPlugIn.displayLog("Could not import data from CSV file", ex);
					event.doit = false;
				}
			}
			if (allWaypoints == null && allWaypointsCsv == null){
				event.doit = false;
			}else if ((allWaypoints == null || allWaypoints.size() == 0) && (allWaypointsCsv == null || allWaypointsCsv.size() == 0)){
				MessageDialog.openInformation(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_File_WarningNoneFound, new Object[]{ type.guiName }));
				event.doit = false;
			}else{
				if(allWaypointsCsv == null){
					((ImportWpSelectWizardPage)event.getTargetPage()).setWaypoints(allWaypoints);
				}else{
					((ImportWpSelectWizardPage)event.getTargetPage()).setWaypointsFromObjects(allWaypointsCsv);
				}
			}
		}
		
		
		if (event.doit){
			lastPage = (WizardPage)event.getTargetPage();
		}
	}
	private WizardPage lastPage = null;

	//pass in:
	//1- all data
	//2- only current day
	//3- select which ones
	//I went with this option because I couldn't think of a worse way to do it...
	public void setCsvDataOption(int option){
		if(option ==1){
			allCsvData = true;
			onlyTodays = false;
			selectPoints = false;
			allData = true;
			
		}else if(option ==2){
			allCsvData = false;
			onlyTodays = true;
			selectPoints = false;
			allData = false;
			
		}else if(option==3){
			allCsvData = false;
			onlyTodays = false;
			selectPoints = true;
			allData = false;
		}
	}
	

	public boolean showCsvSelection(){
		return selectPoints;
	}
  
	public boolean loadAllCsv(){
		return allCsvData;
	}
	
	public void setCsvFile(String file){
		csvConfig = new CSVImportConfiguration();
		csvConfig.setFileName(file);
	}

	private void setCsvConfigs(ImportCsvDetailsWizardPage currentPage){
		csvConfig.setDateFormat(currentPage.getDateFormat() );
		
		csvConfig.setXColumn(currentPage.getXColumnNumber());
		csvConfig.setYColumn(currentPage.getYColumnNumber());
		csvConfig.setDateColumn(currentPage.getDateColumnNumber());
		csvConfig.setTimeColumn(currentPage.getTimeColumnNumber());
		
		csvConfig.setIdColumn(currentPage.getIdColumnNumber());
		csvConfig.setCommentsColumn(currentPage.getCommentsColumnNumber());
		
		csvConfig.setXColumn(currentPage.getXColumnNumber());
		csvConfig.setXColumn(currentPage.getXColumnNumber());
		
		csvConfig.setProjection(currentPage.getProjection());
		csvConfig.setSkipHeaders(currentPage.skipHeaders() );

		List<Waypoint> list = currentDay.getWaypoints();
		int x= 0;
		int max = 0;
		while(x < list.size()){
			int id = list.get(x).getId();
			if( id > max){
				max = id; 
			}
			x++;
		}
		csvConfig.setMaxId(max);
		
	}
}
