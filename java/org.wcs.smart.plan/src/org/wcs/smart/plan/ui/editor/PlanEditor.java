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
package org.wcs.smart.plan.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.internal.PlanLabelProvider;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;


/**
 * The Plan Editor
 * 
 * @author jeffloun
 * @author Emily 
 * @since 1.0.0
 */
public class PlanEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.plan.ui.editor.PlanEditor"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Plan plan;
	
	private SummaryPlanEditorPage summaryPage;
	private MapPlanEditorPage mapPage;
	
	
	/**
	 * listener for plan change events.
	 */
	private IPlanEventListener planListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			if (source.getUuid().equals(getPlan().getUuid())){
				if (type != PlanEventManager.PATROL_PLAN_ATTRIBUTE){
					//force reload plan to get latest changes
					plan = loadPlan();
					initEditor();
					mapPage.refreshPlanTargets();
					mapPage.refreshPatrols();
				}	
			}
			
			if (type == PlanEventManager.PATROL_PLAN_ATTRIBUTE){
				summaryPage.refreshPatrolLinks();
				mapPage.refreshPatrols();
			}
		}
	};

	/**
	 * listener for plan delete events.
	 */
	private IPlanEventListener deleteListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			if (source.equals(PlanEditor.this.plan)) {
				//close this editor
				PlanEditor.this.getEditorSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						PlanEditor.this.getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(PlanEditor.this, false);					
					}
				});
			}
		}
	};
	
	private IPatrolEventListener patrolListener = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			if (attributeChanged < 0 || 
					attributeChanged == PatrolEventManager.PATROL_TRACKS){
				summaryPage.refreshPatrolLinks();
				mapPage.refreshPatrols();
			}
		}
	};
	
	/*
	 * refreshes the children targets.  This is done in a separate job
	 * as it may take time and requires an active hibernate session
	 */
	private Job refreshSubPlanTargets = new Job(Messages.PlanEditor_RefreshJob_Title){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<PlanTarget> childTargets = new ArrayList<PlanTarget>();
			Plan thisPlan = null;
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				thisPlan = (Plan) session.get(Plan.class, plan.getUuid());	//load a copy so we don't have problems with trying to have plan open in multiple sessions
				getChildTargets(thisPlan, childTargets);
			
				for(PlanTarget pt : childTargets){
					pt.refreshStatus(Locale.getDefault(), session);
				}
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			
			mapPage.updateSubplanTargetLayer(thisPlan);
			
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					summaryPage.refreshChildTargets(childTargets);		
				}});
			
			
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * recomputes target status
	 */
	private Job computeStatus = new Job(Messages.TargetProgressViewer_ComputeTargetStatus_JobTitle){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				final List<PlanTarget> targets = (List<PlanTarget>)plan.getTargets();
				for (PlanTarget pt : targets){
					pt.refreshStatus(Locale.getDefault(), s);
				}
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					summaryPage.refreshPlanTargetList();
				}
			});
			mapPage.refreshPlanTargets();
			
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}};
	
	
	/**
	 * Default constructor
	 */
	public PlanEditor() {
		super();
		
		PlanEventManager.getInstance().addListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().addListener(EventType.PLAN_DELETED, deleteListener);
		PatrolEventManager.getInstance().addListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().addListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().addListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_MODIFIED, patrolListener);
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		
		PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_DELETED, deleteListener);
		PatrolEventManager.getInstance().removeListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().removeListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().removeListener(org.wcs.smart.patrol.PatrolEventManager.EventType.PATROL_MODIFIED, patrolListener);
		
		super.dispose();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof PlanEditorInput)) {
			throw new IllegalArgumentException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
	}

	
	
	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * Runs a job and reloads the sub plan targets
	 * from the database, recomputes the 
	 * status, updates the ui and map
	 */
	public void refreshSubPlanTargets(){
		refreshSubPlanTargets.schedule();
	}
	
	/**
	 * Refreshes the plan targets map layer
	 */
	public void refreshPlanTargetsMapLayer(){
		mapPage.refreshPlanTargets();
	}
	
	/**
	 * Runs a job and computes the status
	 * of each target in the current plan, updates
	 * the ui and the map
	 */
	public void computePlanTargetStatus(){
		computeStatus.schedule();
	}
	
	public void openPatrol(PatrolEditorInput p){
		(new OpenPatrolHandler()).openPatrol(p, ((IEclipseContext)getSite().getService(IEclipseContext.class)).get(MWindow.class));
	}

	/**
	 * 
	 * @return the current plan associated with the editor
	 */
	public synchronized Plan getPlan(){
		if (plan == null){
			plan = loadPlan();
		}
		return plan;
	}

	public boolean canEdit() {
		//analyst users can never edit
		return SmartDB.getCurrentEmployee().getSmartUserLevel() != Employee.SmartUserLevel.ANALYST;
	}
	
	/**
	 * loads the plan from the database populating all 
	 * lazy fields from the database.
	 * 
	 * Will always get a new object.
	 */
	public Plan loadPlan(){
		UUID puuid = ((PlanEditorInput) getEditorInput()).getUuid();
		Session session = HibernateManager.openSession();
		//load parent plan so don't have lazy loading issues later.
		session.beginTransaction();
		Plan p = (Plan) session.load(Plan.class, puuid);
		if (p.getParent() != null) {
			p.getParent().getId();
		}
		if(p.getTargets() != null){
			p.getTargets().size();
		}
		Station st = p.getStation();
		if(st != null){
			st.getName();
		}
		p.getTeam();
		Team t = p.getTeam();
		if(t != null){
			t.getName();
		}
		for (org.wcs.smart.ca.Label name : p.getNames()) {
			name.getLanguage().getCode();
		}
		session.getTransaction().rollback();
		session.close();
		return p;
	}
	
	

	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		summaryPage.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createPages() {
		try{
			summaryPage = new SummaryPlanEditorPage(this);
			int index = addPage(summaryPage, getEditorInput());
			super.setPageText(index, Messages.PlanEditor_PlanSummaryEditorTabName);
		
			mapPage = new MapPlanEditorPage(this);
			index = addPage(mapPage, getEditorInput());
			super.setPageText(index, Messages.PlanEditor_PlanMapTabName);
			
		}catch (Exception ex){
			SmartPlanPlugIn.displayLog(Messages.PlanEditor_ErrorOpeningPlanEditor + ex.getMessage(), ex);
		}
		initEditor();
	}
	
	private void initEditor(){
		summaryPage.initValues();	//loads the plan
		setPartName(plan.getLabel());
		setTitleImage(PlanLabelProvider.getImage(plan.getType()).createImage());
		computePlanTargetStatus();
	}
	
	private void getChildTargets(Plan parent, List<PlanTarget> targets){
		for(Plan p : parent.getChildren()){
			targets.addAll(p.getTargets());
			getChildTargets(p, targets);
		}
	}

	@Override
	public Map getMap() {
		return mapPage.getMap();
	}

	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
	}

	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
	}

	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

	/**
	 * 
	 * @return the current plan associated with the editor
	 */
	protected void getChildPlanPatrols(Plan plan, Set<PatrolEditorInput> kids, Session session){
		if(plan.getChildren() == null){
			return;
		}
		for (Plan p : plan.getChildren()){
			kids.addAll(PlanHibernateManager.getPatrols(p, session));
			getChildPlanPatrols(p, kids, session);
		}
	}
}
