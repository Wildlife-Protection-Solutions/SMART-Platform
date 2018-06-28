package org.wcs.smart.r.ui.editor.script;

import java.io.OutputStream;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.model.RScript;

public class RScriptEditor extends MultiPageEditorPart implements IQueryEditor {

	public static final String ID = "org.wcs.smart.r.script.editor"; //$NON-NLS-1$
	
	private RunPage page1;
	private ResultsPage page2;
	private ScriptPage page3;
	
	private RScript script;
	
	public RScriptEditor() {
		
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}


	@Override
	public void setFocus() {
		page1.setFocus();
	}

	/**
	 * Executes the script
	 */
	void executeScript() {
		page1.executeScript();
	}
	
	/**
	 * Shows the results page
	 */
	void showResults() {
		setActiveEditor(page2);
	}
	
	OutputStream createPage2OutputStream() {
		return page2.createPage2OutputStream();
	}
	
	@Override
	protected void createPages() {
		
		try {
			page1 = new RunPage(this);
			int i = addPage(page1, getEditorInput());
			setPageText(i, "Run");
			
			page2 = new ResultsPage(this);
			i = addPage(page2, getEditorInput());
			setPageText(i, "Results");
			
			page3 = new ScriptPage(this);
			i = addPage(page3, getEditorInput());
			setPageText(i, "Script");
			
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		loadScriptJob.schedule();
	}

	
	public RScript getScript(){
		return this.script;
	}
	
	private Job loadScriptJob = new Job("load script") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			UUID suuid = ((RScriptEditorInput) getEditorInput()).getRScript();
			RScript temp = null;
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try{
					temp = (RScript) session.load(RScript.class, suuid);
					temp.getParameters().forEach(p->p.getValue());
				}finally {
					session.getTransaction().rollback();
				}
			}
			
			RScriptEditor.this.script = temp; 
			
			Display.getDefault().syncExec(()->{
				page1.update();
				page2.update();
				page3.update();
				
				setPartName(script.getName());
			});
			
			return Status.OK_STATUS;
		}
		
	};

	@Override
	public QueryProxy getQueryProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryEditorInput getInputInternal() {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void validate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reparseQuery() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDirty(boolean dirty) {
		// TODO Auto-generated method stub
		
	}
}
