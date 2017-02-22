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
import org.wcs.smart.gpx.GPSDataImport.ImportType;
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
	public ImportGpsDataWizard(ObservationGPSDataImport.ImportType type) {
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

	/**
	 * Get supported import engines
	 * @return
	 */
	public abstract IImportEngine[] getEngines(); 

	/**
	 * Execute on finish
	 * @return
	 */
	public abstract boolean processFinish();
	
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
