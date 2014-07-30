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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * View for listing survey designs and associated surveys and missions. 
 * @author Emily
 *
 */
public class SurveyDesignListView extends ViewPart{

	public static final String ID = "org.wcs.smart.er.surveyDesignListView"; //$NON-NLS-1$

	private SurveyDesignListFilter filter;
	
	private TreeViewer lstViewer;
	
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
		Composite part = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		part.setLayout(gl);
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstViewer = new TreeViewer(part);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstViewer.setLabelProvider(new SurveyDesignLabelProvider());
		lstViewer.setContentProvider(new LazySurveyDesignTreeContentProvider());
		lstViewer.setInput(null);
		getSite().setSelectionProvider(lstViewer);
		lstViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				SurveyListTreeNode node = (SurveyListTreeNode)((IStructuredSelection)lstViewer.getSelection()).getFirstElement();
				if (node != null) {
					try {
						IWorkbenchPage page = getSite().getPage();
						switch (node.getType()) {
							case SURVEY_DESIGN:
								page.openEditor(new SurveyDesignEditorInput(node.getUuid()), SurveyDesignEditor.ID);						
								break;
							case SURVEY:
								//TODO: implement SURVEY
								break;
							case MISSION:
								//TODO: implement MISSION
								break;
						}
					} catch (Throwable t) {
						EcologicalRecordsPlugIn.displayLog(t.getLocalizedMessage(), t);
					}
				}
			}
		});

		
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
		Menu menu = menuManager.createContextMenu(lstViewer.getControl());
		lstViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, lstViewer);
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
			Session s = HibernateManager.openSession();
			try{
				//str.append("SELECT s.uuid, s.state, s.startDate, s.endDate, lbl.value "); //$NON-NLS-1$
				List<Object[]> data = filter.buildQuery(s).list();
				for (Object[] x : data){
					SurveyListTreeNode node = new SurveyListTreeNode((String)x[4], (byte[])x[0], SurveyListTreeNode.Type.SURVEY_DESIGN);
					
					ins.add(node);
				}
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
					
//					lstViewer.setExpandedTreePaths(path);
					lstViewer.setExpandedElements(path);
				}});
			return Status.OK_STATUS;
		}
		
	};
}
