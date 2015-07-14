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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.menus.IMenuService;
import org.osgi.service.event.Event;
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
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.util.E3Utils;

/**
 * View that displays saved queries to the user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ReportListView {

	public static final String ID = "org.wcs.smart.report.ReportListView"; //$NON-NLS-1$

	private TreeViewer reportList;
	
	@Inject private MPart part;
	@Inject private IMenuService menuService;
	@Inject private ESelectionService selService;
	
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
	}

	/*
	 * Updates the tree when an object is modified
	 */
	private void refreshTree(final Object o, final EventType type){
		Display d = part.getContext().get(Display.class);
		if (type == EventType.REPORT_UPDATED || type == EventType.FOLDER_UPDATED){
			d.syncExec(new Runnable(){
				@Override
				public void run() {
					reportList.update(o, null);
					if (o instanceof Report){
						reportList.refresh(((Report)o).getFolder());
					}else if (o instanceof ReportFolder){
						reportList.refresh(((ReportFolder)o).getParentFolder());
					}
				}
			});
		}else if (type == EventType.REPORT_ADDED || type == EventType.REPORT_DELETED){
			d.syncExec(new Runnable(){
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
			d.syncExec(new Runnable(){
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
			d.syncExec(new Runnable(){
				@Override
				public void run() {
					reportList.refresh();	
				}
			});
		}
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
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
					ReportManager.viewReport((Report) x, part.getContext());
				}
				
			}
		});
		reportList.addSelectionChangedListener(new ViewerSelectionListener(selService));
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.report.ReportListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(reportList.getControl());
		reportList.getControl().setMenu(menu);	
		
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

	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
		if (partEvent == null) return;
		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
		if (activePart.equals(part)){
			//this is necessary to get the menus to show up correctly if you right click on the current selection
			//when the focus is not on the current view (run a report, click on the report view, then right click on the report
			//item in the report list.  Without this, the menu will not display correctly.
			reportList.setSelection(reportList.getSelection());
			return;
		}
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof ReportView && ((ReportView)lpart).getReport() != null){
			reportList.setSelection(new StructuredSelection(((ReportView)lpart).getReport()));
		}
	}
	
	@PreDestroy
	public void dispose(){
		ReportEventManager.getInstance().removeReportListener(listener);
	}
	
	@Focus
	public void setFocus() {
		reportList.getControl().setFocus();
	}

	public static class ReportListViewWrapper extends DIViewPart<ReportListView>{
		public ReportListViewWrapper(){
			super(ReportListView.class);
		}
	}
}
