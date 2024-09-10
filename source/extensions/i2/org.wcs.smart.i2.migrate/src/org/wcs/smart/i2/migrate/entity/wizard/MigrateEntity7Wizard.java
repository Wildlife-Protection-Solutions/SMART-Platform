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

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.entity.Entity7Database;
import org.wcs.smart.i2.migrate.entity.EntityMigrationJob;
import org.wcs.smart.i2.migrate.entity.EntityTypeItem;
import org.wcs.smart.i2.migrate.entity.EntityTypeMappingRecord;
import org.wcs.smart.i2.migrate.internal.Messages;

/**
 * Wizard for migrating SMART7 entity data to SMART7 profile records
 * 
 * Only imports data from the current Conservation Area
 * 
 * @author Emily
 *
 */
public class MigrateEntity7Wizard extends Wizard {

	private EntityTypeMappingPage page3;
	private Entity7Database db;
	
	public MigrateEntity7Wizard() {
		super();
		setNeedsProgressMonitor(true);
		this.db = new Entity7Database();
	}
	
	@Override
	public void dispose(){
		super.dispose();
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
		
		IEventBroker eventBroker = EclipseContextFactory.getServiceContext(MigratePlugin.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
		
		Map<ConservationArea, Employee> userMapping = new HashMap<>();
		userMapping.put(SmartDB.getCurrentConservationArea(), SmartDB.getCurrentEmployee());
		
		//ensure the profiles are in the active profile
		//this is necessary for the BIRT templates to get created
		for (EntityTypeMappingRecord r : page3.getMappings()) {
			ProfilesManager.INSTANCE.addActiveProfile(r.getProfile(), eventBroker);
		}
		
		EntityMigrationJob job = new EntityMigrationJob(db, page3.getMappings(), userMapping);
		
		job.setLabels(Messages.MigrateEntity7Wizard_Title, Messages.MigrateEntity7Wizard_Message, 
				Messages.MigrateEntity7Wizard_AttributeError);

		try {
			getContainer().run(true, true, job);
		}catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			processException(e);
			return false;
		}
		
		try {
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
    	
    	setWindowTitle(Messages.MigrateEntity7Wizard_WindowTitle);
    	
    	page3 = new EntityTypeMappingPage();
    	super.addPage(page3);
    	
    	setDefaultPageImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
    	
    	Job getMappings = new Job("loading entity types") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<EntityTypeMappingRecord> mappings = new ArrayList<>();
				try {
					for (EntityTypeItem type : db.getTypes(SmartDB.getCurrentConservationArea())) {
						EntityTypeMappingRecord record = new EntityTypeMappingRecord(SmartDB.getCurrentConservationArea());
						record.setEntitytype(type);
						mappings.add(record);
					}
				}catch (SQLException ex) {
					MigratePlugin.displayLog(ex.getMessage(), ex);
				}
				Display.getDefault().asyncExec(()->page3.setMappings(mappings));
				return Status.OK_STATUS;
			}
    	};
    	getMappings.schedule();
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
