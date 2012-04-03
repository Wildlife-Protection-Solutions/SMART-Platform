package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.query.QueryChangedListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.WaypointQuery;

public class QueryResultsEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.query.ui.QueryResults";
	
	private WaypointQuery thisQuery;

	private QueryChangedListener qListener = new QueryChangedListener() {
		
		@Override
		public void queryChanged(WaypointQuery query) {
			if (query.equals(thisQuery)){
				refreshQuery();
			}
			
		}
	};
	
	public WaypointQuery getQuery(){
		return thisQuery;
	}
	
	public QueryResultsEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		if (input instanceof QueryResultsInput){
			QueryResultsInput input2 = ((QueryResultsInput)input);
			if (input2.getUuid() == null){
				thisQuery = new WaypointQuery();
			}else{
				//load query from database.
			}
		}
		
		QueryEventManager.getInstance().addQueryChangedEvent(qListener);
	}

	@Override
	public void dispose(){
		super.dispose();
		QueryEventManager.getInstance().removeQueryChangedEvent(qListener);
	}
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private QueryEditorContent content ;
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		content = new QueryEditorContent(parent, this);	
	}
	
	
	
	public void refreshQuery(){
		
			content.showProgressArea();
			final IProgressMonitor mymonitor = content.getProgressMonitor();
	
			Job runQueryJob = new Job("Running query: " + thisQuery.getName()) {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try{
					List<QueryResultItem> results = thisQuery.getQueryResults(mymonitor);
					content.setTableData(results);
				}catch (Exception ex){
					QueryPlugIn.displayLog("Could not execute query.", ex);
					content.setTableData(new ArrayList<QueryResultItem>());
				}
					return Status.OK_STATUS;
				}
			};
			runQueryJob.schedule();
			
		
		
	}

	@Override
	public void setFocus() {
	}

}
