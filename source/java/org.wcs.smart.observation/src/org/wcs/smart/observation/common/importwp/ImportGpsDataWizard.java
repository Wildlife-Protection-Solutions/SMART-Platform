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
package org.wcs.smart.observation.common.importwp;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Wizard for importing data from GPS Device
 * @author Emily
 * @since 1.0.0
 */
public abstract class ImportGpsDataWizard extends Wizard implements IPageChangingListener {
	
	private ImportType type;
	
	private ImportOption currentOption = ImportOption.ALL;
	private IImportEngine engine;
	
	private IImportWizardPage lastPage = null;
	
	private List<Waypoint> importedData = null;
	
	private Date date;
	
	private boolean canFinish = false;
	/**
	 * Creates a new wizard.
	 */
	public ImportGpsDataWizard(GPSDataImport.ImportType type) {
		setWindowTitle(MessageFormat.format(Messages.ImportGpsDataWizard_ImportOp, new Object[]{type.guiName}));
		this.type = type;
		super.setForcePreviousAndNextButtons(true);
		super.setNeedsProgressMonitor(true);
	}
	public void setImportEngine(IImportEngine engine){
		this.engine = engine;
	}
	
	
	public List<Waypoint> getImportedData() {
		return importedData;
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
	
	public ImportOption getImportOption() {
		return currentOption;
	}
	
	/**
	 * Sets if the wizard can finish
	 * @param canFinish if the wizard can finish
	 */
	public void setCanFinish(boolean canFinish){		
		this.canFinish = canFinish;
	}
	
	@Override
	public boolean canFinish(){
		return this.canFinish;
	}

	public ImportType getType(){
		return this.type;
	}
	
	public Date getDateOption() {
		return date;
	}
	public void setDateOption(Date date) {
		this.date = date;
	}
	
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		IImportEngine[] engines = getEngines(); 
		List<IImportEngine> ops = new ArrayList<IImportEngine>();
		for (IImportEngine engine : engines){
			if (engine.supportsType(type)){
				ops.add(engine);
			}
		}
		
		super.addPage(new ImportWpTypeWizardPage(ops));
		
		setCanFinish(false);
	}

	public abstract IImportEngine[] getEngines(); 

	@Override
	public boolean performFinish() {
		if (lastPage == null){
			return false;
		}
		if (!lastPage.beforeMoveNext(null)){
			return false;
		}
		return processFinish();
	}
	
	public abstract boolean processFinish();
	
//	/**
//	 * 
//	 */
//	@Override
//	public boolean performFinish() {
//		if (lastPage == null){
//			return false;
//		}
//		if (!lastPage.beforeMoveNext(null)){
//			return false;
//		}
//	
//		//valid overwrite if importing tracks
//		if (type == ImportType.TRACK){
//			if (currentOption == ImportOption.DATE || currentOption == ImportOption.SELECT){
//				if (currentDay.getTrack() != null){
//					//warn user
//					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.PatrolLegDayInputComposite_SetTrackDialog_Message)){
//						return false;
//					}
//				}
//			}
//			if (currentOption == ImportOption.ALL){
//				boolean warn = false;
//				for(PatrolLeg l : currentDay.getPatrolLeg().getPatrol().getLegs()){
//					for(PatrolLegDay d : l.getPatrolLegDays()){
//						if (d.getTrack() != null){
//							warn = true;
//							break;
//						}
//					}
//					if (warn) break;
//				}
//				
//				if (warn){
//					//warn user
//					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_TrackWarningOverwriteNew)){
//						return false;
//					}
//				}
//			}
//		}
//		
//		final String[] successMessage =  new String[]{null};
//		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//		
//		try {
//			pmd.run(true, false, new IRunnableWithProgress() {
//				@Override
//				public void run(IProgressMonitor monitor) throws InvocationTargetException,
//						InterruptedException {
//					try {
//						if (getImportOption() == ImportOption.DATE || getImportOption() == ImportOption.ALL){
//							importedData = engine.getWaypoints(getImportOption(), type, getCurrentDate().getDate(), monitor);
//						}
//						
//						if (importedData != null && importedData.size() == 0){
//							//nothing found
//							Display.getDefault().syncExec(new Runnable(){
//
//								@Override
//								public void run() {
//									MessageDialog.openWarning(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new  Object[]{getType().guiName, getType().guiName}));
//								}});
//							return;
//						}
//						
//						String message = engine.updatePatrol(getImportOption(), getType(), getCurrentDate().getPatrolLeg().getPatrol(), getCurrentDate(), importedData, monitor);
//						successMessage[0] = message;
//					} catch (final Exception e) {
//						Display.getDefault().syncExec(new Runnable(){
//							@Override
//							public void run() {
//								SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), e.getMessage(), e);
//							}
//							
//						});
//						successMessage[0] = null;
//					}
//				}
//			});
//		} catch (Exception ex) {
//			ObservationPlugIn.displayLog(GPX_FILE_ERROR + ex.getLocalizedMessage(), ex);
//			return false;
//		}
//		if(successMessage[0] != null){
//			MessageDialog.openInformation(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_ImportCompleteMessage + " " + successMessage[0]); //$NON-NLS-1$
//			return true;
//		}else{
//			return false;
//		}
//	}


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

}
