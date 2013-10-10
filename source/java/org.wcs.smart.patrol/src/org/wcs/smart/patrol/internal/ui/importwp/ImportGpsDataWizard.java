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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.GPSDataImport.ImportType;
import org.wcs.smart.patrol.internal.ui.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.patrol.internal.ui.importwp.csv.CsvImportEngine;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Waypoint;

/**
 * Wizard for importing data from GPS Device
 * @author Emily
 * @since 1.0.0
 */
public class ImportGpsDataWizard extends Wizard implements IPageChangingListener{
	
	private static final String GPX_FILE_ERROR = Messages.ImportGpsDataWizard_GPXFileImportError;
	private static final String IMPORT_DIALOG_TITLE = Messages.ImportGpsDataWizard_ImportDialog_Title;
	
	private PatrolLegDay currentDay;
	private ImportType type;
	
	private ImportOption currentOption = ImportOption.ALL;
	private IImportEngine engine;
	
	private IImportWizardPage lastPage = null;
	
	private List<Waypoint> importedData = null;
	
	private boolean canFinish = false;
	/**
	 * Creates a new wizard.
	 */
	public ImportGpsDataWizard(PatrolLegDay currentDay, GPSDataImport.ImportType type) {
		setWindowTitle(MessageFormat.format(Messages.ImportGpsDataWizard_DialogTitle, new Object[]{type.guiName}));
		this.currentDay = currentDay;
		this.type = type;
		super.setForcePreviousAndNextButtons(true);
		super.setNeedsProgressMonitor(true);
	}
	public void setImportEngine(IImportEngine engine){
		this.engine = engine;
	}
	
	
	
	public void setImportedData(List<Waypoint> waypoints){
		this.importedData = waypoints;
	}
	public IImportEngine getImportEngine(){
		return engine;
	}
	public void setImportOption(ImportOption op){
		this.currentOption = op;
	}
	
	public ImportOption getImportOption(){
		return currentOption;
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
		
		IImportEngine[] engines = {new GpsImportEngine(),  new GpxImportEngine(), new CsvImportEngine(), new TrackFromWaypointEngine()}; 
		List<IImportEngine> ops = new ArrayList<IImportEngine>();
		for (IImportEngine engine : engines){
			if (engine.supportsType(type)){
				ops.add(engine);
			}
		}
		
		super.addPage(new ImportWpTypeWizardPage(ops));
		
		setCanFinish(false);
	}
	
	
	/**
	 * 
	 */
	@Override
	public boolean performFinish() {
		if (lastPage == null){
			return false;
		}
		if (!lastPage.beforeMoveNext(null)){
			return false;
		}
	
		//valid overwrite if importing tracks
		if (type == ImportType.TRACK){
			if (currentOption == ImportOption.DATE || currentOption == ImportOption.SELECT){
				if (currentDay.getTrack() != null){
					//warn user
					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.PatrolLegDayInputComposite_SetTrackDialog_Message)){
						return false;
					}
				}
			}
			if (currentOption == ImportOption.ALL){
				boolean warn = false;
				for(PatrolLeg l : currentDay.getPatrolLeg().getPatrol().getLegs()){
					for(PatrolLegDay d : l.getPatrolLegDays()){
						if (d.getTrack() != null){
							warn = true;
							break;
						}
					}
					if (warn) break;
				}
				
				if (warn){
					//warn user
					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_TrackWarningOverwriteNew)){
						return false;
					}
				}
			}
		}
		
		final String[] successMessage =  new String[]{null};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						if (getImportOption() == ImportOption.DATE || getImportOption() == ImportOption.ALL){
							importedData = engine.getWaypoints(getImportOption(), type, getCurrentDate().getDate(), monitor);
						}
						
						if (importedData != null && importedData.size() == 0){
							//nothing found
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									MessageDialog.openWarning(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new  Object[]{getType().guiName, getType().guiName}));
								}});
							return;
						}
						
						String message = engine.updatePatrol(getImportOption(), getType(), getCurrentDate().getPatrolLeg().getPatrol(), getCurrentDate(), importedData, monitor);
						successMessage[0] = message;
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), e.getMessage(), e);
							}
							
						});
						successMessage[0] = null;
					}
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(GPX_FILE_ERROR + ex.getLocalizedMessage(), ex);
			return false;
		}
		if(successMessage[0] != null){
			MessageDialog.openInformation(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_ImportCompleteMessage + " " + successMessage[0]); //$NON-NLS-1$
			return true;
		}else{
			return false;
		}
	}


	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (!((IImportWizardPage)event.getCurrentPage()).beforeMoveNext((WizardPage)event.getTargetPage())){
			event.doit = false;
			return;
		}
		if (!((IImportWizardPage)event.getTargetPage()).init()){
			event.doit = false;
			return;
		}
		lastPage = (IImportWizardPage) event.getTargetPage();
		
	}
	/**
	 * 
	 */
//	@Override
//	public boolean performFinish() {
//		if (lastPage == null){
//			return false;
//		}
//		if (lastPage instanceof ImportGpxWizardPage){
//			allData = ((ImportGpxWizardPage)lastPage).getImportAll();
//			final List<String> filenames = ((ImportGpxWizardPage)lastPage).getFiles();
//			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//			
//			try {
//				pmd.run(true, false, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException,
//							InterruptedException {
//						if (allData){
//							importedData = (GPSDataImport.convertGpx(filenames, null, Collections.singleton(type), monitor)).get(type);
//						}else{
//							importedData = (GPSDataImport.convertGpx(filenames, currentDay.getDate(), Collections.singleton(type), monitor)).get(type);
//						}
//					}
//				});
//			} catch (Exception ex) {
//				SmartPatrolPlugIn.displayLog(GPX_FILE_ERROR + ex.getLocalizedMessage(), ex);
//				return false;
//			}
//		}else if (lastPage instanceof ImportGPSWizardPage ){
//			//import data from gps device
//			this.importedData = null;
//			final String deviceType = ((ImportGPSWizardPage)lastPage).getDeviceType();
//			allData  = ((ImportGPSWizardPage)lastPage).getImportAll();
//			
//			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//			
//			try {
//				pmd.run(true, false, new IRunnableWithProgress(){
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException{
//						try{
//							if (allData){
//								importedData = ( GPSDataImport.importGpsData(deviceType, null, Collections.singleton(type), monitor) ).get(type);
//							}else{
//								importedData = ( GPSDataImport.importGpsData(deviceType, currentDay.getDate(), Collections.singleton(type), monitor) ).get(type);
//							}
//						}catch(final Exception ex){
//							Display.getDefault().syncExec(new Runnable() {
//								@Override
//								public void run() {
//									SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
//								}
//							});
//							
//						}
//					}
//				});
//				
//			} catch (Exception ex) {
//				SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
//				return false;
//			}
//		
//		}else if (lastPage instanceof ImportWpSelectWizardPage ){
//			// ---- IMPORT SELECTED WAYPOINTS/TRACKS ----
//			allData = false;
//			//select data to import
//			if (type == GPSDataImport.ImportType.WAYPOINT){
//				importedData = GPSDataImport.convertWaypoints( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
//			}else if(type == GPSDataImport.ImportType.WAYPOINTCSV){
//				importedData = ((ImportWpSelectWizardPage)lastPage).getSelectedWaypointsObj();
//			}else if (type == GPSDataImport.ImportType.TRACK){
//				importedData = GPSDataImport.convertPointsToTrack( ((ImportWpSelectWizardPage)lastPage).getSelectedWaypoints() );
//			}
//		}else if (lastPage instanceof ImportFromWaypointWizardPage){
//			// ---- GENERATE TRACKS FROM WAYPOINTS ----
//			allData = ((ImportFromWaypointWizardPage)lastPage).getImportAll();
//			importedData = CREATE_FROM_WAYPOINTS;
//		}else if (lastPage instanceof ImportCsvDetailsWizardPage ){
//			type = GPSDataImport.ImportType.WAYPOINTCSV;
//			((ImportCsvDetailsWizardPage)lastPage).updateConfiguration(getCsvImportConfiguration());
//			
//			final ImportOption importOp = ((ImportCsvWizardPage)getPage(ImportCsvWizardPage.PAGE_NAME)).getImportOption();
//			if (importOp == ImportOption.ALL){
//				allData = true;
//			}else{
//				allData = false;
//			}
//			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//			
//			try {
//				pmd.run(true, false, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException,
//							InterruptedException {
//						if(importOp==ImportOption.DATE){
//							importedData = getCsvImportConfiguration().getWaypoints(monitor, currentDay.getDate());
//						}else{
//							importedData = getCsvImportConfiguration().getWaypoints(monitor, null);
//						}
//					}
//				});
//			} catch (Exception ex) {
//				SmartPatrolPlugIn.displayLog(Messages.ImportGpsDataWizard_0 , ex);
//				return false;
//			}
//			if (importedData == null){
//				return false;
//			}
//		}
//		
//		//validation
//		if (type == ImportType.TRACK){
//			if (allData){
//				boolean hasTracks = false;
//				for(PatrolLeg pl : getCurrentDate().getPatrolLeg().getPatrol().getLegs()){
//					for (PatrolLegDay pld : pl.getPatrolLegDays()){
//						if (pld.getTrack() != null){
//							hasTracks = true;
//							break;
//						}
//					}
//				}
//				if (hasTracks){
//					String warn = ""; //$NON-NLS-1$
//					if  (lastPage instanceof ImportWpSelectWizardPage ){
//						warn = Messages.ImportGpsDataWizard_TrackWarningOverwriteAll;
//					}else{
//						warn = Messages.ImportGpsDataWizard_TrackWarningOverwriteNew;
//					}
//					boolean cont= MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, warn);
//					if (!cont){
//						return false;
//					}
//				}
//			}else{
//				if (getCurrentDate().getTrack() != null){
//					boolean cont= MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.PatrolLegDayInputComposite_SetTrackDialog_Message);
//					if (!cont){
//						return false;
//					}
//				}
//			}
//		}
//		
//		if (importedData  == null || (importedData instanceof Collection &&  ((Collection<?>)importedData).size() == 0 )){
//			MessageDialog.openWarning(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new Object[]{this.type.guiName, this.type }));
//			return false;
//		}
//		return true;
//	}
//	
//	/**
//	 * 
//	 * @return the data imported by the wizard.  May be null if no data imported.
//	 */
//	public Object getImportedData(){
//		return this.importedData;
//	}
//	
//	/**
//	 * 
//	 * @return <code>true</code> if all data is imported and 
//	 * should be assigned to correct leg day; <code>false</code> if only
//	 * data for current leg day was imported
//	 */
//	public boolean getImportAll(){
//		return this.allData;
//	}
//	
//
//	/**
//	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
//	 */
//	@Override
//	public void handlePageChanging(PageChangingEvent event) {
//		allWaypoints = null;
//		allWaypointsCsv = null;
//		if (event.getTargetPage() instanceof ImportWpSelectWizardPage){
//			if (event.getCurrentPage() instanceof ImportGpxWizardPage){
//				//read all waypoints from gpx file
//				final List<String> filenames = ((ImportGpxWizardPage)event.getCurrentPage()).getFiles();
//				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//				try{
//				pmd.run(true, false, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException,
//							InterruptedException {
//						if (type == GPSDataImport.ImportType.WAYPOINT){
//							allWaypoints = GPSDataImport.getWaypointsGpx(filenames, monitor);
//						}else if (type == GPSDataImport.ImportType.TRACK){
//							allWaypoints = GPSDataImport.getTrackPoints(filenames, monitor);
//						}
//					}
//				});
//				}catch (Exception ex){
//					SmartPatrolPlugIn.displayLog(Messages.ImportGpsDataWizard_1, ex);
//					event.doit = false;
//				}
//				
//			}else if (event.getCurrentPage() instanceof ImportGPSWizardPage){
//				final String deviceType = ((ImportGPSWizardPage)event.getCurrentPage()).getDeviceType();
//				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//				try{
//				pmd.run(false, false, new IRunnableWithProgress() {
//					@Override
//					public void run(IProgressMonitor monitor) throws InvocationTargetException,
//							InterruptedException {
//						File f = null;
//						try{
//							monitor.setTaskName(Messages.ImportGpsDataWizard_Progress_ImportingGPS);
//							f = GPSBabel.getData(deviceType, Collections.singleton(type));
//							if (type == ImportType.WAYPOINT){
//								allWaypoints = GPSDataImport.getWaypointsGpx(Collections.singletonList(f.getAbsolutePath()), monitor);
//							}else if (type == ImportType.TRACK){
//								allWaypoints = GPSDataImport.getTrackPoints(Collections.singletonList(f.getAbsolutePath()), monitor);
//							}
//						}catch (final Exception ex){
//							Display.getDefault().syncExec(new Runnable(){
//								@Override
//								public void run() {
//									SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
//								}});
//							
//						}finally{
//							try{
//								if (f != null){
//									f.delete();
//								}
//							}catch (final Exception ex){
//								Display.getDefault().syncExec(new Runnable(){
//									@Override
//									public void run() {
//										Display.getDefault().syncExec(new Runnable(){
//											@Override
//											public void run() {
//												SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
//											}});
//									}});
//							}
//						}
//					}
//				});
//				}catch (Exception ex){
//					SmartPatrolPlugIn.displayLog(GPS_DEVICE_ERROR + ex.getLocalizedMessage(), ex);
//					event.doit = false;
//				}
//			}else if(event.getCurrentPage() instanceof ImportCsvDetailsWizardPage){
//				type = GPSDataImport.ImportType.WAYPOINTCSV;
//				((ImportCsvDetailsWizardPage)event.getCurrentPage()).updateConfiguration(getCsvImportConfiguration());
//				
//				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//				try{
//					pmd.run(true, false, new IRunnableWithProgress() {
//						@Override
//						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//							allWaypointsCsv = getCsvImportConfiguration().getWaypoints(monitor, null);
//						}
//					});
//				}catch (Exception ex){
//					SmartPatrolPlugIn.displayLog(Messages.ImportGpsDataWizard_2, ex);
//					event.doit = false;
//				}
//			}
//			
//			
//			if (allWaypoints == null && allWaypointsCsv == null){
//				event.doit = false;
//			}else if ((allWaypoints == null || allWaypoints.size() == 0) && (allWaypointsCsv == null || allWaypointsCsv.size() == 0)){
//				MessageDialog.openInformation(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_File_WarningNoneFound, new Object[]{ type.guiName }));
//				event.doit = false;
//			}else{
//				if(allWaypointsCsv == null){
//					((ImportWpSelectWizardPage)event.getTargetPage()).setWaypoints(allWaypoints);
//				}else{
//					((ImportWpSelectWizardPage)event.getTargetPage()).setWaypointsFromObjects(allWaypointsCsv);
//				}
//			}
//		}			
//		if (event.getCurrentPage() instanceof ImportCsvWizardPage){
//			if (!((ImportCsvWizardPage)event.getCurrentPage()).updateImportConfiguration(getCsvImportConfiguration())){
//				event.doit = false;
//			}
//		}
//		if (event.doit && event.getTargetPage() instanceof ImportCsvDetailsWizardPage){
//			((ImportCsvDetailsWizardPage)event.getTargetPage()).initConfiguration(getCsvImportConfiguration());
//		}
//		
//		
//		if (event.doit){
//			lastPage = (WizardPage)event.getTargetPage();
//		}
//	}
//	
//	
//	
//	public CSVImportConfiguration getCsvImportConfiguration(){
//		if (csvImportConfiguration == null){
//			csvImportConfiguration = new CSVImportConfiguration();
//		}
//		return csvImportConfiguration;
//	}

}
