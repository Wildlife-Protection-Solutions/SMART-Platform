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
package org.wcs.smart.query.ui.summary;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryListenerAdapter;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFactory;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.ui.IQueryEditor;
import org.wcs.smart.query.ui.QueryAreaModifiedListener;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.QueryEditorUtils;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;
import org.wcs.smart.query.ui.definition.QueryDefView;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SummaryEditor extends EditorPart implements IQueryEditor {

	public static final String ID = "org.wcs.smart.query.ui.SummaryEditor"; //$NON-NLS-1$

	private SummaryQuery query;
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private boolean isDirty = false;
	private IAreaModifiedListener areaListener = null;
	
	private IQueryListener qListener = new QueryListenerAdapter() {
		@Override
		public void queryChanged(Query query) {
			if (query != null && query.equals(SummaryEditor.this.query)) {
				isDirty = true;
				firePropertyChange(PROP_DIRTY);
			}
		}

		@Override
		public void queryRun(Query query) {
			if (query != null && query.equals(SummaryEditor.this.query)) {
				refreshQuery();
			}
		}
		
		@Override
		public void queryNameUpdated(Query query) {
			if (query != null && query.equals(SummaryEditor.this.query)){
				boolean lIsDirty = isDirty;
				SummaryEditor.this.query.setName(query.getName());
				SummaryEditor.this.query.setNames(query.getNames());
				((QueryInput)getEditorInput()).setQueryName(query.getName());
				updatePartName();
				compQueryName.setText(query.getName(), query.getId());
				
				isDirty = lIsDirty;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}
	};

	private Job loadQueryLoad = new Job(Messages.SummaryEditor_LoadQueryJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			 QueryInput input = (QueryInput)SummaryEditor.this.getEditorInput();
			 Session session = HibernateManager.openSession();
			 session.beginTransaction();
			 try{
				 query = (SummaryQuery) session.load(SummaryQuery.class, input.getUuid());
				 query.generateDropItems(session);
				 
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						initQuery();
						updatePartName();
					}
				});
				 
			 }catch (Exception ex){
				 QueryPlugIn.displayLog(MessageFormat.format(Messages.SummaryEditor_ErrorLoadingQuery, new Object[]{input.getName(), ex.getMessage()}), ex);
			 }finally{
				 session.getTransaction().rollback();
				 session.close();
			 }

			return Status.OK_STATUS;
		}
	};

	/**
	 * Creates a new editor
	 */
	public SummaryEditor() {
		super();
		
		areaListener = new QueryAreaModifiedListener(this);
		ConservationAreaManager.getInstance().addAreaChangeListener(areaListener);
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
		QueryEventManager.getInstance().removeQueryChangedEvent(qListener);
		if (areaListener != null){
			ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
		}
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

		if (input instanceof QueryInput) {
			QueryInput input2 = ((QueryInput) input);
			if (input2.getUuid() == null) {
				// create a new query
				this.query = QueryFactory.createSummaryQuery();
				setDirty(false);
			} else {
				loadQueryLoad.schedule();
			}
		}
		QueryEventManager.getInstance().addQueryChangedEvent(qListener);
	}
	
	private void initQuery(){
		compQueryName.setText(getQuery().getName(), getQuery().getId());
	}

	/**
	 * 
	 * @return the query
	 */
	public SummaryQuery getQueryInternal() {
		return (SummaryQuery)getQuery();
	}
	/**
	 * @return the query
	 */
	public Query getQuery() {
		try {
			loadQueryLoad.join(); // wait for the query loading job if
									// applicable
		} catch (InterruptedException e) {
			QueryPlugIn.displayLog(Messages.SummaryEditor_ErrorParsingQuery + e.getLocalizedMessage(), e);
		}

		return this.query;
	}

	/**
	 * Updates the editor name with the query name
	 */
	public void updatePartName(){
		super.setPartName(getEditorInput().getName());
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		firePropertyChange(PROP_DIRTY);
	}


	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery() {
		// update date filter
		getQueryInternal().setDateFilter(dateFilterComposite.getDateFilter());

		if (!getQuery().isValid()) {
			MessageDialog
					.openError(getSite().getShell(), Messages.SummaryEditor_ErrorDialogTitle,
							Messages.SummaryEditor_QueryError);
			return;
		}

		// show progress area
		resultsArea.showProgressArea();

		// run query
		final IProgressMonitor mymonitor = resultsArea.createProgressMonitor();

		Job runQueryJob = new Job(Messages.SummaryEditor_RunQueryJobName + getQuery().getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// List<QueryResultItem> results =
					// query.getQueryResults(mymonitor);
					// resultsArea.updateAndShowTable(results);

					resultsArea.updateAndShowTable(query
							.getQueryResults(mymonitor));

				} catch (Exception ex) {
					QueryPlugIn.displayLog(Messages.SummaryEditor_ErrorRunningQuery, ex);
					// resultsArea.updateAndShowTable(new
					// ArrayList<QueryResultItem>());
				}
				return Status.OK_STATUS;
			}
		};
		runQueryJob.schedule();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public boolean isDirty() {
		return this.isDirty;
	}

	/**
	 * Saves the current query
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		Query savedQuery = QueryEditorUtils.doSave(this, monitor);
		if (savedQuery == null){
			//error 
			return;
		}
		if (savedQuery != query){
			//saved as new query
			this.query = (SummaryQuery) savedQuery;
			setInput(new QueryInput(savedQuery));
		}
		
		initQuery();
		updatePartName();
		setDirty(false);
	}
	
	/** 
	 * Saves a copy of the query as a new query
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		Query savedQuery = QueryEditorUtils.doSaveAs(this, true);
		if (savedQuery == null){
			return;
		}
		
		this.query = (SummaryQuery) savedQuery;
		setInput(new QueryInput(savedQuery));
		updatePartName();
		initQuery();
		
		setDirty(false);
		
		//TODO: this is a bit of a hack to get the querylistview to be updated
		//correctly
		//this cannot be called until setinput has bee called
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID));
		getSite().getWorkbenchWindow().getActivePage().activate(getSite().getPart());

	}


	@Override
	public void setFocus() {
		resultsArea.setFocus();
	}

		private QueryDateFilterComposite dateFilterComposite;
	private Form frmSummaryArea;
	private SummaryResultsArea resultsArea;

	private QueryHeaderComposite compQueryName;


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
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
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginRight = 5;
		layout.marginLeft = 5;
		queryProp.setLayout(layout);
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateFilterComposite = new QueryDateFilterComposite(queryProp);
		dateFilterComposite.adapt(toolkit);
		dateFilterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, Messages.SummaryEditor_PropertiesLabel,SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		editQueryProp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				QueryPropertiesDialog dialog = new QueryPropertiesDialog(
						getSite().getShell(), 
						getQuery());
				if (dialog.open() == Window.OK){
					initQuery();
					setDirty(true);
				}
			}
		});
		resultsArea = new SummaryResultsArea(main, this);
		resultsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		initQuery();
		updatePartName();
	}

	private void createNameHeader(Composite main, FormToolkit toolkit) {
		compQueryName = new QueryHeaderComposite(main, Messages.SummaryEditor_SummaryQueryLabel, 
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
	public QueryInput getInputInternal(){
		return (QueryInput) getEditorInput();
	}
	
	@Override
	public void reparseQuery() {
		//running it its own job so it has its own hibernate session
		//and does not interfere with other sessions.
		Job j = new Job("update drop items") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final Session session = HibernateManager.openSession();
				try{
				Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							try{
								getQuery().generateDropItems(session);
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}});
				}finally{
					session.close();
				}
						return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
				
		QueryEventManager.getInstance().fireQueryRefreshListeners(getQuery());
	}
}
