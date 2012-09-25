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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.ui.IQueryEditor;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querylist.SaveQueryDialog;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SummaryEditor extends EditorPart implements IQueryEditor {

	public static final String ID = "org.wcs.smart.query.ui.SummaryEditor";

	private SummaryQuery query;

	private boolean isDirty = false;

	private IQueryListener qListener = new IQueryListener() {
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
				((QueryInput)getEditorInput()).setQueryName(query.getName());
				updatePartName();
				compQueryName.setText(query.getName(), query.getId());
				
				isDirty = lIsDirty;
				firePropertyChange(MultiPageEditorPart.PROP_DIRTY);
			}
		}
	};

	private Job loadQueryLoad = new Job("Load Query Job") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			 QueryInput input = (QueryInput)SummaryEditor.this.getEditorInput();
			 Session session = HibernateManager.openSession();
			 session.beginTransaction();
			 try{
				 query = (SummaryQuery) session.load(SummaryQuery.class, input.getUuid());
			
				 query.getFilterDropItems();
				 query.getValueDropItems();
				 query.getRowGroupByDropItems();
				 query.getColumnGroupByDropItems();
				 query.generateDropItems(session);
				 
				getSite().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						initQuery();
						updatePartName();
					}
				});
				 
			 }catch (Exception ex){
				 QueryPlugIn.log("Could not load query " + input.getName(), ex);
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
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		QueryEventManager.getInstance().removeQueryChangedEvent(qListener);
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
				this.query = new SummaryQuery();
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
			QueryPlugIn.displayLog("Could not load query." + e.getMessage(), e);
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

	private void updateQuery() {
		// update date filter
		getQueryInternal().setDateFilter(dateFilterComposite.getDateFilter());
		// getQuery().setDateFilter(page1.getDateFilter());
	}

	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery() {
		// update date filter
		updateQuery();

		if (!getQuery().isValid()) {
			MessageDialog
					.openError(getSite().getShell(), "Error",
							"Query invalid.  Please fix query definition and try again.");
			return;
		}

		// show progress area
		resultsArea.showProgressArea();

		// run query
		final IProgressMonitor mymonitor = resultsArea.createProgressMonitor();

		Job runQueryJob = new Job("Running query: " + getQuery().getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// List<QueryResultItem> results =
					// query.getQueryResults(mymonitor);
					// resultsArea.updateAndShowTable(results);

					resultsArea.updateAndShowTable(query
							.getQueryResults(mymonitor));

				} catch (Exception ex) {
					QueryPlugIn.displayLog("Could not execute query.", ex);
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
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// validate if user can save the current query
		if (query.getIsShared() && !QueryHibernateManager.canModifyCaQueries()){
			boolean ret = MessageDialog
					.openQuestion(
							getSite().getShell(),
							"Save",
							"You do not have permission to overwrite this query.  Would you like to save it as a new query?");
			if (ret) {
				doSaveAs();
			}
			return;
		}

		// ensure query is valid
		if (!getQuery().isValid()) {
			MessageDialog
					.openError(
							getSite().getShell(),
							"Save",
							"You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
			monitor.setCanceled(true);
			return;
		}else if (getQuery().getName().trim().length() == 0){
			MessageDialog.openError(getSite().getShell(), "Save", "Query name must not be blank.");
			monitor.setCanceled(true);
			return;
		}

		
		// update the query definition
		updateQuery();

		boolean newQuery = false;
		if (getQuery().getUuid() == null) {
			newQuery = true;
			// new query; we need to get folder location
			SaveQueryDialog dialog = new SaveQueryDialog(
					getSite().getShell(), query, false);
			if (dialog.open() != IDialogConstants.OK_ID) {
				monitor.setCanceled(true);
				return;
			}

			QueryFolder qf = dialog.getQueryFolder();
			if (qf == null) {
				QueryPlugIn.displayLog(
						"Query not saved.  Could not determine folder.", null);
				monitor.setCanceled(true);
				return;
			}

			if (!qf.isRootFolder()) {
				query.setFolder(qf);
				query.setIsShared(qf.getEmployee() == null);

			} else if (qf.getUuid().equals(QueryHibernateManager.CA_QUERY_KEY)) {
				query.setIsShared(true);
			}
			query.setOwner(SmartDB.getCurrentEmployee());
			query.setConservationArea(SmartDB.getCurrentConservationArea());

		}

		if (!QueryHibernateManager.saveQuery(query, false)){
			monitor.setCanceled(true);
			return ;
		}
		if (newQuery) {
			((QueryInput) super.getEditorInput()).setUuid(query.getUuid());
			((QueryInput) super.getEditorInput()).setId(query.getId());
		}
		
		((QueryInput)super.getEditorInput()).setQueryName(query.getName());
		updatePartName();
		initQuery();
		setDirty(false);
	}


	@Override
	public void doSaveAs() {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite()
				.getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					// ensure query is valid
					if (!getQuery().isValid()) {
						MessageDialog
								.openError(
										getSite().getShell(),
										"Save",
										"You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
						return;
					}

					monitor.beginTask("Save As...", 3);
					monitor.subTask("Cloning query...");
					updateQuery();
					SummaryQuery newQuery = getQueryInternal().clone();

					monitor.worked(1);

					monitor.subTask("Getting save location...");
					SaveQueryDialog dialog = new SaveQueryDialog(
							getSite().getShell(), query, true);
					if (dialog.open() != IDialogConstants.OK_ID) {
						return;
					}

					newQuery.setName(dialog.getQueryName());
					if (newQuery.getName().trim().length() == 0){
						MessageDialog.openError(getSite().getShell(), "Save", "Query name must not be blank.");
						monitor.setCanceled(true);
						return;
					}
					
					QueryFolder qf = dialog.getQueryFolder();
					if (!qf.isRootFolder()) {
						newQuery.setFolder(qf);
						newQuery.setIsShared(qf.getEmployee() == null);

					} else if (qf.getUuid().equals(
							QueryHibernateManager.CA_QUERY_KEY)) {
						newQuery.setIsShared(true);
					}
					newQuery.setOwner(SmartDB.getCurrentEmployee());
					newQuery.setConservationArea(SmartDB
							.getCurrentConservationArea());

					SummaryQuery oldQuery = SummaryEditor.this.query;
					SummaryEditor.this.query = newQuery;
					monitor.worked(1);

					monitor.subTask("Saving query...");
					
					if (!QueryHibernateManager.saveQuery(query, true)){
						SummaryEditor.this.query = oldQuery;
						return;
					}
					monitor.worked(1);
					SummaryEditor.this.setInput(new QueryInput(newQuery));
					
					updatePartName();
					initQuery();

					setDirty(false);
					monitor.worked(1);

					// TODO: update the Query Def View; see if there is a better
					// way to do this
					QueryDefView view = (QueryDefView) getSite()
							.getWorkbenchWindow().getActivePage()
							.findView(QueryDefView.ID);
					if (view != null) {
						if (!view.getQuery().equals(oldQuery)) {
							view.setQuery(newQuery);
						}
					}

					// TODO: this is a bit of a hack to get the querylistview to
					// be updated
					// correctly
					getSite().getWorkbenchWindow().getActivePage()
							.activate(view);
					getSite().getWorkbenchWindow().getActivePage()
							.activate(getSite().getPart());

				}
			});
		} catch (Exception ex) {
			QueryPlugIn
					.displayLog("Error saving query: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void setFocus() {
		resultsArea.setFocus();
	}

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

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
		
		createNameHeader(main);

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
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, "summary properties...",SWT.NONE);
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
		
		resultsArea = new SummaryResultsArea(main, toolkit, this);
		resultsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		initQuery();
		updatePartName();
	}

	private void createNameHeader(Composite main) {
		compQueryName = new QueryHeaderComposite(main, "Summary:", 
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
	
}
