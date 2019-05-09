package org.wcs.smart.paws.ui.run;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;

public class RunEditor extends MultiPageEditorPart {

	public static final String ID = "org.wcs.smart.paws.run.editor"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private RunSummaryPage summaryPage;
	private RunResultsPage resultsPage;
	
	
	/**
	 * Default constructor
	 */
	public RunEditor() {
		super();
		
	
	}

	@Override
	public void dispose() {
		toolkit.dispose();	
	}
	
	private RunEditorInput getInputInternal() {
		return (RunEditorInput)getEditorInput();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof RunEditorInput)) {
			throw new IllegalArgumentException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
	}

	
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		summaryPage.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createPages() {
		try{
			summaryPage = new RunSummaryPage(this);
			int index = addPage(summaryPage, getEditorInput());
			super.setPageText(index, "Summary");
		
			resultsPage = new RunResultsPage(this);
			index = addPage(resultsPage, getEditorInput());
			super.setPageText(index, "Results");
			
		}catch (Exception ex){
			PawsPlugIn.displayLog("Error opening PAWS Analysis" + "\n\n" + ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
		initEditor();
	}
	
	private void initEditor(){
		loadSettings.schedule();
	}
	

	private Job loadSettings = new Job("loading paws run settings") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			PawsRun pr = null;
			try(Session s = HibernateManager.openSession()){
				pr = s.get(PawsRun.class, getInputInternal().getUuid());
				
				//TODO: not found
				if (pr == null) return Status.OK_STATUS;
				
				pr.getConfiguration().getName();
			}
			
			PawsRun fpr = pr;
			Display.getDefault().syncExec(()->{
				summaryPage.init(fpr);
				
				RunEditor.this.setPartName(fpr.getId());
				
			});
			return Status.OK_STATUS;
		}
		
	};
}
