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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * View that displays saved queries to the user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListView extends ViewPart {

	public static final String ID = "org.wcs.smart.query.QueryListView"; //$NON-NLS-1$

	
	private TreeViewer queryList;
	
	/* listener to update query definition when window changes */
	private IPartListener2 editorListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if ( IQueryEditor.class.isAssignableFrom( part.getClass() ) ){
				IStructuredSelection selection = new StructuredSelection(((EditorPart)part).getEditorInput());
				queryList.setSelection(selection);
				focusCellManager.getFocusCell();
			}
		}
	};
	
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
			
			getSite().getShell().getDisplay().asyncExec(new Runnable(){

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
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(editorListener);
	}
	
	@Override
	public void createPartControl(Composite parent) {
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
					OpenQueryHandler.openQuery((QueryEditorInput)x);
				}
			}
		});
		
		queryList.setCellEditors(new CellEditor[] { new TextCellEditor(queryList.getTree()) });
		queryList.setColumnProperties(new String[] { "col1" }); //$NON-NLS-1$
		queryList.setCellModifier(new NameCellEditor(queryList));
		focusCellManager = new TreeViewerFocusCellManager(queryList, new MultiFocusCellOwnerDrawHighlighter(queryList));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
				queryList) {
			
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
//				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
//						return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
//						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						return event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};		
		TreeViewerEditor.create(queryList, actSupport, ColumnViewerEditor.DEFAULT);

		queryList.setInput(Messages.QueryListView_LoadingLabel);

		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(queryList.getControl());
		queryList.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  queryList);
		getSite().setSelectionProvider(queryList);
		
		
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

	@Override
	public void dispose(){
		super.dispose();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(editorListener);
		SavedQueryTree.getInstance().removeListener(listener);
		listener = null;
	}
	
	@Override
	public void setFocus() {
		queryList.getControl().setFocus();
	}

}
