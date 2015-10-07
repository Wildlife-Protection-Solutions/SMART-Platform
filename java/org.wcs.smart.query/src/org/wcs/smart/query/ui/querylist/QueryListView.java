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
package org.wcs.smart.query.ui.querylist;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.menus.IMenuService;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.util.E3Utils;

/**
 * View that displays saved queries to the user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListView {

	public static final String ID = "org.wcs.smart.query.parts.itemlist"; //$NON-NLS-1$

	private TreeViewer queryList;
	
	private @Inject MPart part;
	private @Inject IMenuService mService;
	private @Inject ESelectionService selService;
	@Inject private UISynchronize ui;
	
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
			
			if (eventType == IQueryListener.FOLDER_ADDED){
				editElement(object);
			}
		}
	};
	
	private Job loadQueriesJob = new Job(Messages.QueryListView_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(QueryListContentProvider.QUERY_KEY, SavedQueryTree.getInstance().getQueries());
			data.put(QueryListContentProvider.FOLDER_KEY, SavedQueryTree.getInstance().getFolders());
			
			ui.asyncExec(new Runnable(){

				@Override
				public void run() {
					queryList.setInput(data);
					focusCellManager.getFocusCell();
					queryList.expandToLevel(2);
				}});
			
			return Status.OK_STATUS;
		}
	};

	private TreeViewerFocusCellManager focusCellManager;
		
	public QueryListView(){
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		SavedQueryTree.getInstance().clearData();
		loadQueriesJob.schedule();
	}
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
		if (partEvent == null) return;
		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
		if (activePart.equals(part)){
			//this is necessary to get the menus to show up correctly if you right click on the current selection
			//when the focus is not on the current view (run a report, click on the report view, then right click on the report
			//item in the report list.  Without this, the menu will not display correctly.
			queryList.setSelection(queryList.getSelection());
			return;
		}
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof IQueryEditor){
			queryList.setSelection(new StructuredSelection(((IQueryEditor)lpart).getInputInternal()));
			focusCellManager.getFocusCell();
		}
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth  = 0;
		
		Composite main = new Composite(parent, SWT.NONE);
		
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		main.setLayout(gl);
		
		queryList = new TreeViewer(main, SWT.MULTI );
		queryList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.setContentProvider(new QueryListContentProvider(true));
		queryList.setLabelProvider(new QueryListLabelProvider());
		queryList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = ((IStructuredSelection)queryList.getSelection()).getFirstElement();
				if (x != null && x instanceof QueryEditorInput){
					(new OpenQueryHandler()).openQuery((QueryEditorInput)x);
				}
			}
		});
		queryList.addSelectionChangedListener(new ViewerSelectionListener(selService));
		queryList.setCellEditors(new CellEditor[] { new TextCellEditor(queryList.getTree()) });
		queryList.setColumnProperties(new String[] { "col1" }); //$NON-NLS-1$
		queryList.setCellModifier(new NameCellEditor(queryList));
		focusCellManager = new TreeViewerFocusCellManager(queryList, new MultiFocusCellOwnerDrawHighlighter(queryList));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
				queryList) {
			
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
						return event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};		
		TreeViewerEditor.create(queryList, actSupport, ColumnViewerEditor.DEFAULT);

		queryList.setInput(Messages.QueryListView_LoadingLabel);

		/* dnd support */
		int operations = DND.DROP_COPY | DND.DROP_MOVE ;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		queryList.addDragSupport(operations, transferTypes , new QueryListDragListener(queryList));
		queryList.addDropSupport(operations, transferTypes, new QueryListDropListener(queryList){
			@Override
			public boolean performDrop(Object data) {
				boolean ok = super.performDrop(data);
//				if (ok){
//					setChangesMade(true);
//				}
				return ok;
			}
		});
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		mService.populateContributionManager(menuManager, "popup:" + ID ); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(queryList.getControl());
		queryList.getControl().setMenu(menu);
		
		
		SavedQueryTree.getInstance().addListener(listener);
		
		loadQueriesJob.schedule();
		
		QueryEventManager.getInstance().addListener(new QueryListenerAdapter() {
			@Override
			public void queryModified(int eventType, Object object) {
				if (eventType == QueryListenerAdapter.QUERY_NAME_MODIFIED){
					queryList.refresh();
				}
			}
		});
		
	}
	
	public void editElement(Object obj){
		queryList.editElement(obj, 0);
	}

	@PreDestroy
	public void dispose(){
		SavedQueryTree.getInstance().removeListener(listener);
		listener = null;
	}
	
	@Focus
	public void setFocus() {
		queryList.getControl().setFocus();
	}

	public static class QueryListViewWrapper extends DIViewPart<QueryListView>{
		public QueryListViewWrapper(){
			super(QueryListView.class);
		}
	}
}
