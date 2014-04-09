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
package org.wcs.smart.report.internal.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.query.ui.querylist.MultiFocusCellOwnerDrawHighlighter;
import org.wcs.smart.report.IReportListener;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportEventManager.EventType;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.LazyReportContentProvider;
import org.wcs.smart.report.ui.ReportLabelProvider;

/**
 * View that displays saved queries to the user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ReportListView extends ViewPart {

	public static final String ID = "org.wcs.smart.report.ReportListView"; //$NON-NLS-1$

	
	private TreeViewer reportList;
	
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
			if (partRef.getId().equals(ReportView.ID)){
				
				IWorkbenchPart part = partRef.getPart(false);
				if (part != null && part instanceof ReportView){
					if (((ReportView)part).getReport() != null){
						IStructuredSelection selection = new StructuredSelection(((ReportView)part).getReport());
						reportList.setSelection(selection);
					}
				}
				
				
			}
		}
	};
	
	/*
	 * listener for report chaning events
	 */
	private IReportListener listener = new IReportListener() {
		@Override
		public void reportEvent(Object o, EventType type) {
			refreshTree(o, type);
		}
	};
	
	/**
	 * Creates a new view
	 */
	public ReportListView(){
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(editorListener);
	}

	/*
	 * Updates the tree when an object is modified
	 */
	private void refreshTree(final Object o, final EventType type){
		if (type == EventType.REPORT_UPDATED || type == EventType.FOLDER_UPDATED){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					reportList.update(o, null);
					if (o instanceof Report){
						reportList.refresh(((Report)o).getFolder());
					}else if (o instanceof ReportFolder){
						reportList.refresh(((ReportFolder)o).getParentFolder());
					}
					//reportList.refresh();
				}
			});
		}else if (type == EventType.REPORT_ADDED || type == EventType.REPORT_DELETED){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					ReportFolder rf = ((Report)o).getFolder();
					if (rf != null){
						reportList.refresh(rf);
					}else if (((Report)o).getShared()){
						reportList.refresh(RootReportFolder.CA_ROOT_FOLDER);
					}else{
						reportList.refresh(RootReportFolder.USER_ROOT_FOLDER);
					}
					
				}
			});
		}else if (type == EventType.FOLDER_ADDED || type == EventType.FOLDER_DELETED){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					ReportFolder rf = (ReportFolder)o;
					if (rf.getParentFolder() != null){
						reportList.refresh(rf.getParentFolder());
					}else if (rf.getDeletedParent() !=null){
						reportList.refresh(rf.getDeletedParent());
					}else if (rf.getEmployee() == null){
						reportList.refresh(RootReportFolder.CA_ROOT_FOLDER);
					}else{
						reportList.refresh(RootReportFolder.USER_ROOT_FOLDER);
					}
					
					if (type == EventType.FOLDER_ADDED){
						IJobChangeListener listener = new JobChangeAdapter() {
							@Override
							public void done(IJobChangeEvent event) {
								((LazyReportContentProvider)reportList.getContentProvider()).removeUpdateCompleteListener(this);
								editElement(o);
								
							}
						};
						((LazyReportContentProvider)reportList.getContentProvider()).addUpdateCompleteListener(listener);
					}
				}
			});
		}else{
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					reportList.refresh();	
				}
			});
		}
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
		
		reportList = new TreeViewer(main, SWT.MULTI);
		reportList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		reportList.setContentProvider(new LazyReportContentProvider());
		reportList.setLabelProvider(new ReportLabelProvider());
		reportList.setInput(Messages.ReportListView_LoadingLabel);
		
		reportList.setCellEditors(new CellEditor[] { new TextCellEditor(reportList.getTree()) });
		reportList.setColumnProperties(new String[] { "col1" }); //$NON-NLS-1$
		reportList.setCellModifier(new ReportItemNameCellEditor());
		
		new TreeViewerFocusCellManager
				(reportList, new MultiFocusCellOwnerDrawHighlighter(reportList));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
				reportList) {
			
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
						return event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};		
		TreeViewerEditor.create(reportList, actSupport, ColumnViewerEditor.DEFAULT);
		
		//on double click open report
		reportList.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) reportList.getSelection();
				if (selection == null || selection.isEmpty()){
					return;
				}
				Object x = selection.getFirstElement();
				if (x instanceof Report){
					ReportManager.viewReport((Report) x);
				}
				
			}
		});
		
		
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(reportList.getControl());
		reportList.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  reportList);
		getSite().setSelectionProvider(reportList);
		
		reportList.expandToLevel(2);
		ReportEventManager.getInstance().addReportListener(listener);
	}
	
	/**
	 * Edits the given tree element
	 * @param obj
	 */
	public void editElement(Object obj){
		reportList.editElement(obj, 0);
	}

	@Override
	public void dispose(){
		super.dispose();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(editorListener);
		ReportEventManager.getInstance().removeReportListener(listener);
	}
	
	@Override
	public void setFocus() {
		reportList.getControl().setFocus();
	}

}
