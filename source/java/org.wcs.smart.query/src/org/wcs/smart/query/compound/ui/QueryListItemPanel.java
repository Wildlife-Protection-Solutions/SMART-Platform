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
package org.wcs.smart.query.compound.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.SavedQueryTree;

/**
 * Item panels that lists all mappable queries.  Mappable queries types
 * must implement IMappableQueryType
 * @author Emily
 *
 */
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
	
	private QueryListenerAdapter queryListener = new QueryListenerAdapter() {
		@Override
		public void queryModified(int eventType, Object object) {
			if (eventType == QueryListenerAdapter.QUERY_NAME_MODIFIED){
				queryList.refresh();
			}
		}
	};
	
	private Job loadQueriesJob = new Job(Messages.QueryListView_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			//load queries and filter out mappable query objects
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			HashMap<UUID, List<QueryEditorInput>> queries = SavedQueryTree.getInstance().getQueries();
			HashMap<UUID, List<QueryEditorInput>> queriescopy = new HashMap<UUID, List<QueryEditorInput>>(); 
			for(Entry<UUID, List<QueryEditorInput>> qs : queries.entrySet()){
				List<QueryEditorInput> newInput = new ArrayList<QueryEditorInput>();
				for(QueryEditorInput in: qs.getValue()){
					if (in.getType() instanceof IMappableQueryType){
						newInput.add(in);
					}
				}
				if (!newInput.isEmpty()){
					queriescopy.put(qs.getKey(), newInput);
				}	
			}
			
			//set input and expand
			data.put(QueryListContentProvider.QUERY_KEY, queriescopy);
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
		
		
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText(Messages.QueryListItemPanel_AddToQuery);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addQueryItem((IStructuredSelection)queryList.getSelection());
			}
		});
		
		loadQueriesJob.schedule();
	
		//these are never disposed
		QueryEventManager.getInstance().addListener(queryListener);
		SavedQueryTree.getInstance().addListener(listener);
		
		return main;
	}

	
	@Override
	public void refreshPanel() {
		queryList.refresh();
	}

}
