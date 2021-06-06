package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.Smart6Database;

public class MigrateIntelligenceWizard extends Wizard  implements IPageChangingListener {

	private Smart6WizardPage page1;
	private CaListWizardPage page2;
	private RecordMappingPage page3;
	
	private Smart6Database smart6;
	private List<ConservationArea> toProcess;
	
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
		ConversionJob job = new ConversionJob(page3.getMappings(), smart6);
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
    	
    	setWindowTitle("Migrate SMART 6 Intelligence Data");
    	
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
				ExtractDbJob job = new ExtractDbJob(fname);
				getContainer().run(true, true, job);
				
				this.toProcess = job.getConservationAreas();
				this.smart6 = job.getDatabase();
			} catch (Exception e) {
				processException(e);
				event.doit = false;
				return;
			}
			
			if (toProcess == null || toProcess.isEmpty()) {
				MessageDialog.openError(getShell(), "No Conservation Areas", "No matching conservation areas found with intelligence data");
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
		String msg = "";
		Throwable t = ex;
		while(t != null) {
			if (t.getMessage() != null) msg += " " + t.getMessage();
			t = t.getCause();
		}
		MigratePlugin.displayLog(msg, ex);
	}

}
