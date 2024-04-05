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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.hibernate.SurveyDesignProxy;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.hibernate.SurveyMissionProxy;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.ui.handlers.EditSurveyElementHandler;
import org.wcs.smart.er.ui.mision.editor.MissionEditor;
import org.wcs.smart.er.ui.mision.editor.MissionEditorInput;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditor;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.util.E3Utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * View for listing survey designs and associated surveys and missions. 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class SurveyDesignListView implements IDoubleClickListener, IUpdatableView{

	public static final String ID = "org.wcs.smart.er.surveyDesignListView"; //$NON-NLS-1$

	private SurveyFilter filter;
	private SurveyDesignFilter designFilter;
	
	private TreeViewer lstViewer;
	private TableViewer designViewer;
	
	private CTabFolder bar;
	
	private boolean processevent = true;
	
	@Inject private MPart localPart;
	@Inject private IMenuService menuService;
	@Inject private ESelectionService selService; 
	@Inject private IEclipseContext context;
	
	private MenuItem moveMissionMenu = null;
	
	private ISurveyEventListener listener = new ISurveyEventListener(){
		@Override
		public void event(Object o) {
			refreshJob.schedule();
		}};
	
	@PreDestroy
	public void dispose() {
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_ADDED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_MODIFIED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_DELETED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_ADDED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_MODIFIED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DELETED, listener);
		
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_ADDED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_DELETED, listener);
		SurveyEventHandler.getInstance().removeListener(EventType.MISSION_MODIFIED, listener);
	}
	
	@Inject
	@Optional
	public void partActivation(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		if (event.getProperty(UIEvents.EventTags.ELEMENT) != localPart) return;
		ShowFieldDataPerspective.enableToolbarItem(ID, context);
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent();
	}
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent, EPartService pService){
		if (partEvent == null) return;
		if (!processevent) return;
		try {
			processevent = false;
			MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
			Object src = E3Utils.getSourceObject(activePart);
			if (src instanceof SurveyDesignEditor){
				designViewer.setSelection(new StructuredSelection( ((SurveyDesignEditor) E3Utils.getSourceObject(activePart)).getEditorInput() ));
				bar.setSelection(1);
				pService.bringToTop(localPart);
			}else if (src instanceof MissionEditor){
				UUID missionUuid = ((MissionEditorInput)((MissionEditor)src).getEditorInput()).getUuid();
				
				SurveyMissionProxy proxy = new SurveyMissionProxy(null, missionUuid, null, null);
				lstViewer.setSelection(new StructuredSelection(proxy));
				bar.setSelection(0);
				pService.bringToTop(localPart);
				pService.activate(activePart);
			}
		}finally {
			processevent = true;
		}
	}
	
	public SurveyFilter getSurveyMissionFilter() {
		return this.filter;
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		filter = SurveyFilter.newInstance();
		context.set(SurveyFilter.class, filter);
		
		designFilter = new SurveyDesignFilter(SmartDB.getCurrentConservationArea());
		
		bar = new CTabFolder(parent, SWT.NONE);

		lstViewer = new TreeViewer(bar, SWT.NONE);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		lstViewer.setContentProvider(new SurveyDesignTreeContentProvider());
		
		lstViewer.setInput(null);
		lstViewer.addDoubleClickListener(this);
		
		CTabItem titem = new CTabItem(bar, SWT.NONE);
		titem.setText(Messages.SurveyDesignListView_SurveyMissionTabName);
		titem.setControl(lstViewer.getControl());
		
		designViewer = new TableViewer(bar, SWT.MULTI);
		designViewer.setContentProvider(ArrayContentProvider.getInstance());
		designViewer.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		designViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		designViewer.addDoubleClickListener(this);
		
		CTabItem item = new CTabItem(bar, SWT.NONE);
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
				
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.er.surveyDesignListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(bar);
		bar.setMenu(menu);
		lstViewer.getControl().setMenu(menu);
		designViewer.getControl().setMenu(menu);
		
		menu.addListener(SWT.Show, e->{
			//if (e.widget != lstViewer.getControl()) return;
			if (moveMissionMenu != null) moveMissionMenu.dispose();
			if (!lstViewer.getControl().isVisible()) return;
			Object s = lstViewer.getStructuredSelection().getFirstElement();
			if (s == null) return;
			if (!(s instanceof SurveyMissionProxy)) return;
			if (((SurveyMissionProxy)s).getParent() == null) return;
			
			List<Survey> options = null;
			Mission mission = null;
			try(Session session = HibernateManager.openSession()){
				
				options = session.createQuery("FROM Survey WHERE surveyDesign.uuid = :sd", Survey.class) //$NON-NLS-1$
						.setParameter("sd", ((SurveyMissionProxy)s).getParent().getDesignUuid()) //$NON-NLS-1$
						.list();
				options.sort((a,b)->Collator.getInstance().compare(b.getId(), a.getId()));
				
				mission = session.get(Mission.class, ((SurveyMissionProxy)s).getUuid());
				options.remove(mission.getSurvey());
			}
			if (options == null || options.isEmpty()) return;
			
			moveMissionMenu = new MenuItem(menu, SWT.CASCADE, 1);
			moveMissionMenu.setText(Messages.SurveyDesignListView_MoveToSurvey);
			
			Menu surveyMenu = new Menu(moveMissionMenu);
			moveMissionMenu.setMenu(surveyMenu);
			final Mission fmission = mission;
			for (Survey op : options) {
				MenuItem ni = new MenuItem(surveyMenu, SWT.PUSH);
				ni.setText(op.getId());
				ni.addListener(SWT.Selection, evt->updateMission(fmission, op));
			}
		});
		
		/* selection */
		ViewerSelectionListener sel = new ViewerSelectionListener(selService);
		lstViewer.addSelectionChangedListener(sel);
		designViewer.addSelectionChangedListener(sel);
		bar.addSelectionListener(new SelectionAdapter() {
			//ensure correct selection is made when tab changed
			@Override
			public void widgetSelected(SelectionEvent e) {
				Viewer active = null;
				if (bar.getSelectionIndex() == 0){
					active = lstViewer;
					
				}else{
					active = designViewer;
				}
				active.setSelection(active.getSelection());
			}
		});
		bar.setSelection(0);
	}

	private void updateMission(Mission mission, Survey survey) {
		try(Session session = HibernateManager.openSession()){
			mission = session.get(Mission.class, mission.getUuid());
			if (mission == null) return;
			survey = session.get(Survey.class, survey.getUuid());
			if (survey == null) return;
			
			session.beginTransaction();
			try {
				mission.setSurvey(survey);
				session.getTransaction().commit();
			}catch (Exception ex) {
				EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.SurveyDesignListView_MoveError,  ex.getMessage() ), ex);
			}
		}
		SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
	}
	
	@Focus
	public void setFocus() {
		lstViewer.getControl().setFocus();
	}
	
	public void refresh(){
		refreshJob.schedule();
	}
	
	private Job refreshJob = new Job(Messages.SurveyDesignListView_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<SurveyMissionProxy> ins = new ArrayList<SurveyMissionProxy>();
			final List<SurveyDesignEditorInput> designs = new ArrayList<SurveyDesignEditorInput>();
			
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					//surveys
					ins.addAll( SurveyHibernateManager.getSurveys(s, filter) );
					
					
					//designs
					List<SurveyDesignProxy> objects =SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(s, designFilter);
					for (SurveyDesignProxy p : objects){
						designs.add(new SurveyDesignEditorInput(p));
					}
					Collections.sort(designs, new Comparator<SurveyDesignEditorInput>() {
						@Override
						public int compare(SurveyDesignEditorInput o1,
								SurveyDesignEditorInput o2) {
							return Collator.getInstance().compare(o1.getName(), o2.getName());
						}
					});
					
				}catch (Exception ex){
					EcologicalRecordsPlugIn.displayLog(Messages.SurveyDesignListView_LoadError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				}finally{
					s.getTransaction().rollback();
				}
			}
			
			getShell().getDisplay().asyncExec(new Runnable(){

				@Override
				public void run() {
//					Object[] path = lstViewer.getExpandedElements();
					lstViewer.setInput(ins);
					lstViewer.refresh();
//					if (path != null) {
//						lstViewer.setExpandedElements(path);
//					}else {
						lstViewer.expandToLevel(2);
//					}
					
					designViewer.setInput(designs);
				}});
			return Status.OK_STATUS;
		}
		
	};


	@Override
	public void doubleClick(DoubleClickEvent event) {
		Object selection = ((IStructuredSelection)event.getSelection()).getFirstElement();
		if (selection != null) {
			if (selection instanceof SurveyMissionProxy){
				SurveyMissionProxy node = (SurveyMissionProxy)selection;
				switch (node.getType()) {
					case SURVEY:
						EditSurveyElementHandler.editSurvey(getShell(), node.getUuid());
						break;
					case MISSION:
						EditSurveyElementHandler.editMission(getShell(), node.getUuid(), node.getId());
						break;
				}
			}else if (selection instanceof SurveyDesignEditorInput){
				(new EditSurveyElementHandler()).execute(new StructuredSelection(selection), getShell());
			}
		}
	}

	@Override
	public void updateContent() {
		refresh();
	}
		
	public void showFilterDialog(){
		if (bar.getSelectionIndex() == 0){
			SurveyFilterDialog dialog = new SurveyFilterDialog(getShell(), this, filter);
			dialog.open();
		}else{
			SurveyDesignFilterDialog dialog = new SurveyDesignFilterDialog(getShell(), this, designFilter);
			dialog.open();
		}
	}
	
	private Shell getShell(){
		return localPart.getContext().get(Shell.class);
	}
	
	
	
	public static class SurveyDesignListViewWrapper extends DIViewPart<SurveyDesignListView>{
		public SurveyDesignListViewWrapper(){
			super(SurveyDesignListView.class);
		}
		
	}
}
