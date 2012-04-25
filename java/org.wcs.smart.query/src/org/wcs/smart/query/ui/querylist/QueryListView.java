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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryHibernateManager;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.ui.QueryResultsEditor;

/**
 * View that displays saved queries to the user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListView extends ViewPart {

	public static final String ID = "org.wcs.smart.query.QueryListView";
	
	private HashMap<Integer, Object> data = new HashMap<Integer, Object>();
	
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
			if (partRef.getId().equals(QueryResultsEditor.ID) ){
				IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof QueryResultsEditor){
					IStructuredSelection selection = new StructuredSelection(((QueryResultsEditor)part).getEditorInput()); 
					QueryListView.this.getSite().getSelectionProvider().setSelection(selection);
					focusCellManager.getFocusCell();
				}	
			}
		}
	};
	
	
	private IQueryFolderListener listener = new IQueryFolderListener() {
		
		@Override
		public void folderChanged(int eventType, Object object) {
			
			if (eventType == IQueryFolderListener.FOLDER_DELETED){
				QueryFolder folder = (QueryFolder)object;
				if (folder.getParentFolder() == null){
					List<QueryFolder> folders = (List<QueryFolder>) data.get(QueryListViewContentProvider.FOLDER_KEY);
					for (QueryFolder f: folders){
						f.getChildren().remove(folder);
					}
							
				}
			}else if (eventType == IQueryFolderListener.QUERY_DELETED){
				QueryInput query = (QueryInput)object;
				HashMap<Integer, List<QueryInput>> queries = (HashMap<Integer, List<QueryInput>>) data.get(QueryListViewContentProvider.QUERY_KEY	);
				for (List<QueryInput> list : queries.values()){
					list.remove(query);
				}
			}else if (eventType == IQueryFolderListener.QUERY_SAVED){
				//update the name
				Query query = (Query)object;
				HashMap<Integer, List<QueryInput>> queries = (HashMap<Integer, List<QueryInput>>) data.get(QueryListViewContentProvider.QUERY_KEY	);				
				for (List<QueryInput> list : queries.values()){
					for (QueryInput input : list){
						if ( Arrays.equals(input.getUuid(), query.getUuid()) ){
							input.setQueryName(query.getName());
							break;
						}
					}
				}
			}else if (eventType == IQueryFolderListener.QUERY_ADDED){
				Query query = (Query)object;
				HashMap<Integer, List<QueryInput>> queries = (HashMap<Integer, List<QueryInput>>) data.get(QueryListViewContentProvider.QUERY_KEY	);
				
				byte[] key = null;
				if (query.getFolder() != null){
					key = query.getFolder().getUuid();
				}else if (query.getIsShared()){
					key = QueryHibernateManager.CA_QUERY_KEY;
				}else{
					key = QueryHibernateManager.USER_QUERY_KEY;
				}
				List<QueryInput> ins = queries.get(Arrays.hashCode(key));
				if (ins == null){
					ins = new ArrayList<QueryInput>();
					queries.put(Arrays.hashCode(key), ins);
				}
				object = new QueryInput(query);
				ins.add((QueryInput)object);
			}
			
			queryList.refresh();
			if (eventType == IQueryFolderListener.FOLDER_ADDED || 
					eventType == IQueryFolderListener.QUERY_ADDED){
				queryList.expandToLevel(object, 1);
			}
			if (eventType == IQueryFolderListener.FOLDER_ADDED){
				editElement(object);
			}
			
		}
	};
	
	private Job loadQueriesJob = new Job("Load Queries"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				List<QueryFolder> folders = QueryHibernateManager.getQueryFolders(s, true);
				HashMap<Integer, List<QueryInput>> queries = QueryHibernateManager.getQueryProxies(s);
			
				
				data.put(QueryListViewContentProvider.FOLDER_KEY, folders);
				data.put(QueryListViewContentProvider.QUERY_KEY, queries);
				
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						queryList.setInput(data);
						queryList.expandToLevel(2);
					}
				});
			
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			return Status.OK_STATUS;
		}
		
	};

	private TreeViewerFocusCellManager focusCellManager;
		
	public QueryListView(){
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(editorListener);
	}
	public QueryListViewContentProvider getQueryListContentProvider(){
		return (QueryListViewContentProvider) this.queryList.getContentProvider();
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
		
		queryList = new TreeViewer(main, SWT.NONE);
		queryList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.setContentProvider(new QueryListViewContentProvider(true));
		queryList.setLabelProvider(new QueryListLabelProvider());
		
		
		queryList.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = ((IStructuredSelection)queryList.getSelection()).getFirstElement();
				if (x != null && x instanceof QueryInput){
					try {
						getSite().getPage().openEditor((QueryInput)x, QueryResultsEditor.ID);
					} catch (Throwable t) {
						QueryPlugIn.displayLog(t.getMessage(), t);

					}
				}
				
			}
		});
		
		
//		
		queryList.setCellEditors(new CellEditor[] { new TextCellEditor(
				queryList.getTree()) });
		queryList.setColumnProperties(new String[] { "col1" });
		queryList.setCellModifier(new ICellModifier() {

			@Override
			public void modify(Object element, String property,
					final Object value) {
				element = ((Item) element).getData();
				if (element instanceof QueryFolder) {
					final QueryFolder folder = (QueryFolder) element;
					if (value.toString().equals(folder.getName())) {
						// nothing to update
						return;
					}
					folder.setName(value.toString());

					queryList.refresh();

					Job j = new Job("Update Name") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							Session session = HibernateManager.openSession();
							session.beginTransaction();
							try {
								session.saveOrUpdate((QueryFolder) folder);
								folder.updateName(value.toString(),
										SmartDB.getCurrentLanguage());
								session.getTransaction().commit();
							} catch (Exception ex) {
								session.getTransaction().rollback();
								QueryPlugIn.displayLog(
										"Could not save changes to folder name. "
												+ ex.getMessage(), ex);
							} finally {
								session.close();
							}

							return Status.OK_STATUS;
						}
					};
					j.schedule();
				}

			}

			@Override
			public Object getValue(Object element, String property) {
				return ((LabelProvider) queryList.getLabelProvider())
						.getText(element);
			}

			@Override
			public boolean canModify(Object element, String property) {
				if (element instanceof QueryFolder) {
					return !((QueryFolder) element).isRootFolder();
				}
				return false;
			}
		});
		

		focusCellManager = new TreeViewerFocusCellManager(queryList, new FocusCellOwnerDrawHighlighter(queryList));
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

		TreeViewerEditor.create(queryList, focusCellManager, actSupport,
				ColumnViewerEditor.TABBING_HORIZONTAL
						| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
						| ColumnViewerEditor.TABBING_VERTICAL
						| ColumnViewerEditor.KEYBOARD_ACTIVATION);

		

		loadQueriesJob.schedule();
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(queryList.getControl());
		queryList.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  queryList);
		getSite().setSelectionProvider(queryList);
		
		
		
		QueryEventManager.getInstance().addQueryFolderListener(listener);
	}
	
	
	public void editElement(Object obj){
		queryList.editElement(obj, 0);
	}

	@Override
	public void dispose(){
		super.dispose();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(editorListener);
		QueryEventManager.getInstance().removeQueryFolderListener(listener);
	}
	
	@Override
	public void setFocus() {
	}

}
