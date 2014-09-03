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
package org.wcs.smart.er.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * View for listing survey designs and associated surveys and missions. 
 * @author Emily
 *
 */
public class SurveyDesignListView extends ViewPart implements IDoubleClickListener, IUpdatableView{

	public static final String ID = "org.wcs.smart.er.surveyDesignListView"; //$NON-NLS-1$

	private SurveyFilter filter;
	private SurveyDesignFilter designFilter;
	
	private TreeViewer lstViewer;
	private TableViewer designViewer;
	
	private TabFolder bar;
	
	private ISurveyEventListener listener = new ISurveyEventListener(){
		@Override
		public void event(Object o) {
			refreshJob.schedule();
		}};
		
		
	public SurveyDesignListView(){
		filter = new SurveyFilter();
		designFilter = new SurveyDesignFilter();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		bar = new TabFolder(parent, SWT.NONE);

		lstViewer = new TreeViewer(bar, SWT.NONE);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		lstViewer.setContentProvider(new LazySurveyDesignTreeContentProvider());
		lstViewer.setInput(null);
		lstViewer.addDoubleClickListener(this);
		
		TabItem titem = new TabItem(bar, SWT.NONE);
		titem.setText(Messages.SurveyDesignListView_SurveysTabName);
		titem.setControl(lstViewer.getControl());
		
		designViewer = new TableViewer(bar, SWT.MULTI);
		designViewer.setContentProvider(ArrayContentProvider.getInstance());
		designViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		designViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		designViewer.addDoubleClickListener(this);
		
		TabItem item = new TabItem(bar, SWT.NONE);
		item.setText(Messages.SurveyDesignListView_DesignsTabName);
		item.setControl(designViewer.getControl());

		refresh();
		
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_ADDED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_MODIFIED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_DELETED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_ADDED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_MODIFIED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DELETED, listener);
		
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_ADDED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_DELETED, listener);
		SurveyEventHandler.getInstance().addListener(EventType.MISSION_MODIFIED, listener);

		
		getSite().setSelectionProvider(new ISelectionProvider() {
			@Override
			public void setSelection(ISelection selection) {
				getActiveViewer().setSelection(selection);		
			}
			
			@Override
			public void removeSelectionChangedListener(
					ISelectionChangedListener listener) {
				lstViewer.removeSelectionChangedListener(listener);
				designViewer.removeSelectionChangedListener(listener);
			}
			
			@Override
			public ISelection getSelection() {
				return getActiveViewer().getSelection();
			}
			
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				lstViewer.addSelectionChangedListener(listener);
				designViewer.addSelectionChangedListener(listener);
			}
		});
		
		/* menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(bar);
		bar.setMenu(menu);
		lstViewer.getControl().setMenu(menu);
		designViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, lstViewer);
		getSite().registerContextMenu(menuManager, designViewer);
	}

	@Override
	public void setFocus() {
		lstViewer.getControl().setFocus();
	}
	
	public void refresh(){
		refreshJob.schedule();
	}
	
	private Job refreshJob = new Job(Messages.SurveyDesignListView_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<SurveyListTreeNode> ins = new ArrayList<SurveyListTreeNode>();
			final List<SurveyDesignEditorInput> designs = new ArrayList<SurveyDesignEditorInput>();
			
			Session s = HibernateManager.openSession();
			try{
				//surveys
				List<SurveyEditorInput> items = SurveyHibernateManager.getInstance().getSurveys(s, filter);
				
				for (SurveyEditorInput sv : items){
					ins.add(new SurveyListTreeNode(sv.getName(), sv.getUuid(), Type.SURVEY)); 
				}
				
				//designs
				designs.addAll(SurveyHibernateManager.getInstance().getSurveyDesigns(s, designFilter));
				
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog(Messages.SurveyDesignListView_LoadError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable(){

				@Override
				public void run() {
					Object[] path = lstViewer.getExpandedElements();
					lstViewer.setInput(ins);
					lstViewer.refresh();
					
					lstViewer.setExpandedElements(path);
					
					designViewer.setInput(designs);
				}});
			return Status.OK_STATUS;
		}
		
	};


	@Override
	public void doubleClick(DoubleClickEvent event) {
		
		Object selection = ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (selection != null) {
			if (selection instanceof SurveyListTreeNode){
				SurveyListTreeNode node = (SurveyListTreeNode)selection;
				switch (node.getType()) {
					case SURVEY:
						EditSurveyElementHandler.editSurvey(getSite().getShell(), node.getUuid());
						break;
					case MISSION:
						EditSurveyElementHandler.editMission(getSite().getShell(), node.getUuid(), node.getLabel());
						break;
				}
			}else if (selection instanceof SurveyDesignEditorInput){
				EditSurveyElementHandler.editSurveyDesign(getSite().getShell(), (SurveyDesignEditorInput) selection);
			}
		}
	}

	@Override
	public void updateContent() {
		refresh();
	}
	
	private Viewer getActiveViewer(){
		if(bar.getSelectionIndex() == 0){
			return lstViewer;
		}else{
			return designViewer;
		}
	}
	
	public void showFilterDialog(){
		if (bar.getSelectionIndex() == 0){
			SurveyFilterDialog dialog = new SurveyFilterDialog(getSite().getShell(), this, filter);
			dialog.open();
		}else{
			SurveyDesignFilterDialog dialog = new SurveyDesignFilterDialog(getSite().getShell(), this, designFilter);
			dialog.open();
		}
	}
}
