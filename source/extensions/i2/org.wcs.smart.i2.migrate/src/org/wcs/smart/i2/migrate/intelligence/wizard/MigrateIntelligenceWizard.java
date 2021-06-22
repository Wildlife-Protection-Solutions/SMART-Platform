/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.migrate.ExtractDbJob;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.Intel6Database;
import org.wcs.smart.i2.migrate.internal.Messages;

/**
 * Wizard for migrating SMART6 intelligence records to SMART7+ profile records
 * 
 * @author Emily
 *
 */
public class MigrateIntelligenceWizard extends Wizard  implements IPageChangingListener {

	private Smart6WizardPage page1;
	private CaListWizardPage page2;
	private RecordMappingPage page3;
	
	private Intel6Database smart6;
	private List<ConservationArea> toProcess;
	private Map<ConservationArea, Employee> userMappings;
	
	public MigrateIntelligenceWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (smart6 != null) {
			try {
				smart6.close();
			} catch (IOException e) {
				MigratePlugin.log(e.getMessage(), e);
			}
		}

	}
	
	@Override
	public boolean canFinish(){
		if (getContainer().getCurrentPage() == page3) {
			return super.canFinish();
		}
		return false;
	}
	
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

	}

	@Override
	public boolean performFinish() {
		ConversionJob job = new ConversionJob(page3.getMappings(), smart6, userMappings);
		try {
			getContainer().run(true, true, job);
		} catch (InvocationTargetException | InterruptedException e) {
			processException(e);
			return false;
		}
		
		return true;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	
    	setWindowTitle(Messages.MigrateIntelligenceWizard_Title);
    	
    	page1 = new Smart6WizardPage();
    	page2 = new CaListWizardPage();
    	page3 = new RecordMappingPage();
    	
    	super.addPage(page1);
    	super.addPage(page2);
    	super.addPage(page3);
    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	setDefaultPageImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
    }
	
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == page1) {
			if (this.smart6 != null) {
				try {
					this.smart6.close();
				}catch (Exception ex) {
					MigratePlugin.log(ex.getMessage(), ex);
				}
			}
			String fname = page1.getFile();
			try {
				ExtractDbJob<Intel6Database> job = new ExtractDbJob<Intel6Database>(fname) {
					protected List<ConservationArea> getConservationAreasWithData(Intel6Database db) throws SQLException{
						return db.getConservationAreasIntelWithData();	
					}

					@Override
					protected Intel6Database createDatabase(Path path) throws SQLException {
						return new Intel6Database(path);
					}

					@Override
					protected boolean validateVersion(Intel6Database database) throws SQLException {
						return database.validateIntelligenceVersion();
					}
				};
				getContainer().run(true, true, job);
				
				this.toProcess = job.getConservationAreas();
				this.smart6 = job.getDatabase();
			} catch (Exception e) {
				processException(e);
				event.doit = false;
				return;
			}
			
			if (toProcess == null || toProcess.isEmpty()) {
				MessageDialog.openError(getShell(), Messages.MigrateIntelligenceWizard_NoCas, Messages.MigrateIntelligenceWizard_NoMatchingCas);
				event.doit = false;
			}else {
				page2.setConservationArea(toProcess);
			}
		}else if (event.getCurrentPage() == page2 && event.getTargetPage() == page3) {
			try {
				ValidateUserJob job = new ValidateUserJob(smart6, page2.getConservationAreas(), getShell());
				getContainer().run(true, true, job);
				
				if (job.getMappingRecords() == null) {
					event.doit = false;
					return;
				}
				userMappings = job.getEmployeeMapping();
				page3.setMappings(job.getMappingRecords());
			} catch (Exception e) {
				processException(e);
				event.doit = false;
				return;
			}
		}
	}
	
	private void processException(Exception ex) {
		ex.printStackTrace();
		String msg = ""; //$NON-NLS-1$
		Throwable t = ex;
		while(t != null) {
			if (t.getMessage() != null) msg += " " + t.getMessage(); //$NON-NLS-1$
			t = t.getCause();
		}
		MigratePlugin.displayLog(msg, ex);
	}

}
