package org.wcs.smart.query.compound.ui;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.SavedQueryTree;

public class QueryListItemPanel extends AbstractQueryItemPanel{

	public static final String ID = "org.wcs.smart.query.compound.querylist"; //$NON-NLS-1$
	
	private TreeViewer queryList;
	
	/*
	 * Listener for changes to the source data provided by
	 * the SavedQueryTree.
	 * <p>Note cannot just listener for query events and queryList.refresh() needs
	 * to be called AFTER the source data is updated.</p>
	 */
	private SavedQueryTree.ISourceChangedListener listener = new SavedQueryTree.ISourceChangedListener() {
		@Override
		public void sourceChanged(int eventType, Object object) {
			queryList.refresh();
			
			if (eventType == IQueryListener.FOLDER_ADDED ||
					eventType == IQueryListener.QUERY_ADDED){
				queryList.expandToLevel(object, 1);
			}
		}
	};
	
	private Job loadQueriesJob = new Job(Messages.QueryListView_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(QueryListContentProvider.QUERY_KEY, SavedQueryTree.getInstance().getQueries());
			data.put(QueryListContentProvider.FOLDER_KEY, SavedQueryTree.getInstance().getFolders());
			
			if (queryList != null && !queryList.getTree().isDisposed()){
				queryList.getTree().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						queryList.setInput(data);
						queryList.expandToLevel(2);
				}});
			}
			return Status.OK_STATUS;
		}
	};

	
	public QueryListItemPanel() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Composite getComposite(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		main.setLayout(gl);
	
		queryList = new TreeViewer(main, SWT.SINGLE);
		queryList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.setContentProvider(new QueryListContentProvider(true));
		queryList.setLabelProvider(new QueryListLabelProvider());
		queryList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addQueryItem((IStructuredSelection)queryList.getSelection());
			}
		});
		queryList.setInput(Messages.QueryListView_LoadingLabel);
		
	loadQueriesJob.schedule();
		
		//TODO dispose this listener
		QueryEventManager.getInstance().addListener(new QueryListenerAdapter() {
			@Override
			public void queryModified(int eventType, Object object) {
				if (eventType == QueryListenerAdapter.QUERY_NAME_MODIFIED){
					queryList.refresh();
				}
			}
		});
		//and this one:
		SavedQueryTree.getInstance().addListener(listener);
		
		
		return main;
	}

	@Override
	public void refreshPanel() {
		queryList.refresh();
	}

}
