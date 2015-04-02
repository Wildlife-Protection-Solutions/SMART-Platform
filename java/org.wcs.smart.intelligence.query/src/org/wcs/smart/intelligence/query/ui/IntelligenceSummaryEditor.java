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
package org.wcs.smart.intelligence.query.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.intelligence.query.IntelligenceQueryFactory;
import org.wcs.smart.intelligence.query.RecievedDateFilter;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.ui.ISummaryEditor;
import org.wcs.smart.query.common.ui.SummaryResultsArea;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Editor for intelligence summary queries.
 * 
 * @author Emily
 *
 */
public class IntelligenceSummaryEditor extends EditorPart implements IQueryEditor, ISummaryEditor{

	public static final String ID = "org.wcs.smart.intelligence.query.summary.editor"; //$NON-NLS-1$
	
	private QueryProxy query;

	private FormToolkit toolkit;
	
	private QueryDateFilterComposite dateFilterComposite;
	
	private Form frmSummaryArea;
	
	private SummaryResultsArea resultsArea;

	private QueryHeaderComposite compQueryName;
	
	private IQueryListener qListener = new QueryListenerAdapter() {

		@Override
		public void queryModified(int eventType, Object object) {
		}
		
		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(IntelligenceSummaryEditor.this.query.getQuery())) {
				refreshQuery();
			}
		}
		
	};

	
	Job runQueryJob = new Job(Messages.IntelligenceSummaryEditor_queryJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				IProgressMonitor mymonitor = resultsArea.createProgressMonitor();
				SummaryQueryResult results = (SummaryQueryResult) getQuery().executeQuery(mymonitor);
				
				if (monitor.isCanceled() || mymonitor.isCanceled()){
					resultsArea.updateAndShowTable(null);
					return Status.CANCEL_STATUS;
				}
				resultsArea.updateAndShowTable(results);
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.IntelligenceSummaryEditor_ErrorMsg, ex);
			}
			return Status.OK_STATUS;
		}
	};

	/**
	 * Creates a new editor
	 */
	public IntelligenceSummaryEditor() {
		super();
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
		QueryEventManager.getInstance().removeListener(qListener);
		query.dispose();
		runQueryJob.cancel();
	}

	@Override
	public void validate(){
		dateFilterComposite.validate();
	}
	
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);

		if (input instanceof QueryEditorInput) {
			QueryEditorInput input2 = ((QueryEditorInput) input);
			if (input2.getUuid() == null) {
				// create a new query
				this.query = new QueryProxy(IntelligenceQueryFactory.createIntelligenceSummaryQuery());
				setDirty(false);
			}
		}
		QueryEventManager.getInstance().addListener(qListener);
	}


	
	/**
	 * Get date filters
	 * @return
	 */
	protected  IDateFieldFilter[] getValidDateFilters(){
		return new IDateFieldFilter[]{RecievedDateFilter.INSTANCE};
	}
	
	private void initQuery(){
		compQueryName.setText(getQuery().getName(), getQuery().getId());
	}

	/**
	 * @return the query
	 */
	public Query getQuery() {
		return getQueryProxy().getQuery();
	}

	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
	}



	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery() {
		runQueryJob.cancel();
		// update date filter
		((IntelligenceSummaryQuery)getQuery()).setDateFilter(dateFilterComposite.getDateFilter());

		if (!getQueryProxy().isValid()) {
			MessageDialog.openError(getSite().getShell(), Messages.IntelligenceSummaryEditor_ErrorTitle, Messages.IntelligenceSummaryEditor_InvalidQueryMessage);
			return;
		}
		// show progress area
		resultsArea.showProgressArea();
		runQueryJob.schedule();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * Not saveable
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
	}
	
	/** 
	 * Not saveable
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
	}


	@Override
	public void setFocus() {
		resultsArea.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		frmSummaryArea = toolkit.createForm(container);
		frmSummaryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));

		layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		frmSummaryArea.getBody().setLayout(layout);
		//frmSummaryArea.setText("Summary");
		

		Composite main = toolkit.createComposite(frmSummaryArea.getBody());
		
		// Composite main = new Composite(frmSummaryArea.getBody(), SWT.BORDER);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = gl.verticalSpacing = gl.horizontalSpacing = 0;
		main.setLayout(gl);
		
		createNameHeader(main, toolkit);

		Composite queryProp = toolkit.createComposite(main, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginRight = 5;
		layout.marginLeft = 5;
		queryProp.setLayout(layout);
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateFilterComposite = new QueryDateFilterComposite(queryProp, getValidDateFilters(), IDateFilter.DATE_FILTERS);
		dateFilterComposite.adapt(toolkit);
		dateFilterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		resultsArea = new SummaryResultsArea(main, this);
		resultsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		initQuery();
		updatePartName();
		
		setTitleImage(getInputInternal().getType().getImage());
	}

	private void createNameHeader(Composite main, FormToolkit toolkit) {
		compQueryName = new QueryHeaderComposite(main, "",  //$NON-NLS-1$
				toolkit, frmSummaryArea.getFont(), 
				frmSummaryArea.getForeground());
		compQueryName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		compQueryName.addListener(SWT.Selection, new Listener(){

			@Override
			public void handleEvent(Event event) {
				getQuery().setName(event.text);
				setDirty(true);
				
			}});
	}
	
	/**
	 * @return the editor input as query input
	 */
	public QueryEditorInput getInputInternal(){
		return (QueryEditorInput) getEditorInput();
	}
	
	@Override
	public QueryProxy getQueryProxy(){
		return this.query;
	}
	
	/**
	 * no configuration; nothing to do
	 */
	@Override
	public void reparseQuery() {
	}

	/**
	 * not supported
	 * @param dirty
	 */
	@Override
	public void setDirty(boolean dirty) {		
	}
}