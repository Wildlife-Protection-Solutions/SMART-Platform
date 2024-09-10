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
package org.wcs.smart.i2.migrate.entity.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.migrate.ExtractDbJob;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.entity.Entity6Database;
import org.wcs.smart.i2.migrate.entity.EntityMigrationJob;
import org.wcs.smart.i2.migrate.intelligence.wizard.CaListWizardPage;
import org.wcs.smart.i2.migrate.intelligence.wizard.Smart6WizardPage;
import org.wcs.smart.i2.migrate.internal.Messages;

/**
 * Wizard for migrating SMART6 entity records to SMART7+ profile records
 * 
 * @author Emily
 *
 */
public class MigrateEntityWizard extends Wizard  implements IPageChangingListener {

	private Smart6WizardPage page1;
	private CaListWizardPage page2;
	private EntityTypeMappingPage page3;
	
	private Entity6Database smart6;
	private List<ConservationArea> toProcess;
	private Map<ConservationArea, Employee> userMapping;
	
	public MigrateEntityWizard() {
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
		EntityMigrationJob job = new EntityMigrationJob(smart6, page3.getMappings(), userMapping);
		
		try {
			getContainer().run(true, true, job);
		}catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			processException(e);
			return false;
		}
		
		try {
			IEventBroker eventBroker = EclipseContextFactory.getServiceContext(MigratePlugin.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
			eventBroker.send(IntelEvents.ENTITY_TYPE_NEW, job.getSavedTypes());
		}catch (Exception ex) {
			MigratePlugin.log(ex.getMessage(), ex);
		}
		
		String message = MessageFormat.format(Messages.MigrateEntityWizard_CompleteMessage, job.getSavedTypes().size(), job.getSavedEntities());
		MessageDialog.openInformation(getShell(), Messages.MigrateEntityWizard_CompleteTitle, message);
		return true;
	}

	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	
    	setWindowTitle(Messages.MigrateEntityWizard_Smart6WindowsTitle);
    	
    	page1 = new Smart6WizardPage();
    	page2 = new CaListWizardPage();
    	page3 = new EntityTypeMappingPage();
    	
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
				ExtractDbJob<Entity6Database> job = new ExtractDbJob<Entity6Database>(fname) {

					@Override
					protected List<ConservationArea> getConservationAreasWithData(Entity6Database database) throws SQLException {
						return database.getConservationAreasWithEntity();
					}

					@Override
					protected Entity6Database createDatabase(Path path) throws SQLException {
						return new Entity6Database(path);
					}

					@Override
					protected boolean validateVersion(Entity6Database database) throws SQLException {
						return database.validateEntityVersion();
					}};
				getContainer().run(true, true, job);
				
				this.toProcess = job.getConservationAreas();
				this.smart6 = (Entity6Database)job.getDatabase();
				
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
				userMapping = job.getEmployeeMapping();
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
		if (msg.isEmpty()) msg = MessageFormat.format(Messages.MigrateEntityWizard_ConversionError,  ex.getStackTrace()[0].toString());
		MigratePlugin.displayLog(msg, ex);
	}

}
