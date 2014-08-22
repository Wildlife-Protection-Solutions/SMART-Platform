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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyListTreeNode.Type;
import org.wcs.smart.er.ui.mision.editor.MissionEditor;
import org.wcs.smart.er.ui.mision.editor.MissionEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * View for listing survey designs and associated surveys and missions. 
 * @author Emily
 *
 */
public class SurveyDesignListView extends ViewPart implements IDoubleClickListener{

	public static final String ID = "org.wcs.smart.er.surveyDesignListView"; //$NON-NLS-1$

	private SurveyDesignListFilter filter;
	
	private TreeViewer lstViewer;
	private TableViewer designViewer;
	
	private ISurveyEventListener listener = new ISurveyEventListener(){
		@Override
		public void event(Object o) {
			refreshJob.schedule();
		}};
		
		
	public SurveyDesignListView(){
		filter = new SurveyDesignListFilter();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		TabFolder bar = new TabFolder(parent, SWT.NONE);

		lstViewer = new TreeViewer(bar, SWT.NONE);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		lstViewer.setContentProvider(new LazySurveyDesignTreeContentProvider());
		lstViewer.setInput(null);
		getSite().setSelectionProvider(lstViewer);
		lstViewer.addDoubleClickListener(this);
		
		TabItem titem = new TabItem(bar, SWT.NONE);
		titem.setText("Surveys");
		titem.setControl(lstViewer.getControl());
		
		designViewer = new TableViewer(bar, SWT.MULTI);
		designViewer.setContentProvider(ArrayContentProvider.getInstance());
		designViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		designViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		designViewer.addDoubleClickListener(this);
		
		TabItem item = new TabItem(bar, SWT.NONE);
		item.setText("Designs");
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
	
	Job refreshJob = new Job("Loading Survey Designs"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<SurveyListTreeNode> ins = new ArrayList<SurveyListTreeNode>();
			final List<SurveyDesign> designs = new ArrayList<SurveyDesign>();
			
			Session s = HibernateManager.openSession();
			try{
				//surveys
				List<Survey> items = filter.buildQuery(s).list();
				Collections.sort(items, new Comparator<Survey>() {

					@Override
					public int compare(Survey o1, Survey o2) {
						if (o1.getSurveyDesign().equals(o2.getSurveyDesign())){
							return -o1.getStartDate().compareTo(o2.getStartDate());
						}else{
							return Collator.getInstance().compare(o1.getSurveyDesign().getName().toUpperCase(), o2.getSurveyDesign().getName().toUpperCase());
						}
					}
				});
				for (Survey sv : items){
					ins.add(new SurveyListTreeNode(sv.getId() + " [" + sv.getSurveyDesign().getName() + "]", sv.getUuid(), Type.SURVEY));
				}
				
				//designs
				designs.addAll(s.createQuery("FROM SurveyDesign WHERE conservationArea = :ca").setParameter("ca", SmartDB.getCurrentConservationArea()).list());
				
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog("Error loading survey designs." + "\n\n" + ex.getMessage(), ex);
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
			try {
				IWorkbenchPage page = getSite().getPage();
				if (selection instanceof SurveyListTreeNode){
					SurveyListTreeNode node = (SurveyListTreeNode)selection;
					switch (node.getType()) {
						case SURVEY_DESIGN:
							page.openEditor(new SurveyDesignEditorInput(node.getLabel(), node.getUuid()), SurveyDesignEditor.ID);						
							break;
						case SURVEY:
							//TODO: implement SURVEY
							break;
						case MISSION:
							page.openEditor(new MissionEditorInput(node.getLabel(), node.getUuid()), MissionEditor.ID);
							break;
					}
				}else if (selection instanceof SurveyDesign){
					page.openEditor(new SurveyDesignEditorInput(((SurveyDesign) selection).getName(), ((SurveyDesign) selection).getUuid()), SurveyDesignEditor.ID);
				}
			} catch (Throwable t) {
				EcologicalRecordsPlugIn.displayLog(t.getLocalizedMessage(), t);
			}
		}
	}
}
