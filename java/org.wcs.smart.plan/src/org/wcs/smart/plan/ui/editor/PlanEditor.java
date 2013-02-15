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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.patrol.ui.PatrolPerspective;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory.PanelType;
import org.wcs.smart.plan.ui.targets.TargetProgressViewer;


/**
 * The Plan Editor
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.plan.ui.editor.PlanEditor"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private Plan plan;
	
	private boolean isDirty;
	private Form form;
	private Text txtStation;
	private Text txtTeam;
	private Text txtType;
	private Text txtUnavailableEmployees;
	private Text txtParentPlanId;
	private Text txtPlanID;
	private Text txtName;
	private Text txtDescription;
	private Text txtStartDate;
	private Text txtEndDate;

	private TargetProgressViewer targetList;
	private TargetProgressViewer targetList2; //the child plan's targets

	private Composite content;
	private Composite patrolLinks;
	
	private ScrolledComposite summaryScroll;
	
	/*
	 * refreshes the children targets.  This is done in a separate job
	 * as it may take time and requires an active hibernate session
	 */
	Job refreshPlanTargets = new Job(Messages.PlanEditor_RefreshJob_Title){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<PlanTarget> childTargets = new ArrayList<PlanTarget>();
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				Plan thisPlan = (Plan) session.get(Plan.class, plan.getUuid());	//load a copy so we don't have problems with trying to have plan open in multiple sessions
				getChildTargets(thisPlan, childTargets);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			Display.getDefault().asyncExec(new Runnable(){

				@Override
				public void run() {
					targetList2.initValues(childTargets);
					
				}});
			
			
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * job to load the patrol links; done inside a job
	 * as it may take time and requies an active hibernate session
	 */
	private Job loadPatrolsLinksJob = new Job(Messages.PlanEditor_LoadPatrolsJob_Title){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					for (Control kid : patrolLinks.getChildren()){
						kid.dispose();
					}
					toolkit.createLabel(patrolLinks, Messages.PlanEditor_Loading_Label);
					form.layout();
				}});
			
			final List<PatrolEditorInput> myPatrols = PlanHibernateManager.getPatrols(plan);
			final Set<PatrolEditorInput> childPatrols = new HashSet<PatrolEditorInput>();
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				Plan thisPlan = (Plan) s.get(Plan.class, plan.getUuid());	//load a copy so we don't have problems with trying to have plan open in multiple sessions
				getChildPlanPatrols(thisPlan, childPatrols);
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					for (Control kid : patrolLinks.getChildren()){
						kid.dispose();
					}
					
					
					if (myPatrols.size() == 0 && childPatrols.size() == 0) {
						toolkit.createLabel(patrolLinks,
								Messages.PlanEditor_None_Text);
						patrolLinks.layout();
					} else {
						for (final PatrolEditorInput x : myPatrols){
							Hyperlink lnk = toolkit.createHyperlink(patrolLinks, x.getPatrolId(), SWT.WRAP);
							lnk.addHyperlinkListener(new HyperlinkAdapter() {
								@Override
								public void linkActivated(HyperlinkEvent e) {
									openPatrol(x);
								}
							});
						}
						for (final PatrolEditorInput x : childPatrols){
							Hyperlink lnk = toolkit.createHyperlink(patrolLinks, x.getPatrolId() + "*", //$NON-NLS-1$
									SWT.WRAP);
							lnk.addHyperlinkListener(new HyperlinkAdapter() {
								@Override
								public void linkActivated(HyperlinkEvent e) {
									openPatrol(x);
								}
							});
						}
					}					
			        form.layout();
	
				}});
			return Status.OK_STATUS;

		}};
	
	/**
	 * listener for plan change events.
	 */
	private IPlanEventListener planListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			if (Arrays.equals(source.getUuid(), getPlan().getUuid())){
				if (type == PlanEventManager.PATROL_PLAN_ATTRIBUTE){
					loadPatrolsLinksJob.schedule();
				}else{
					//if this is our plan we want to update our object
					//with the new one.
					plan = source;
					initValues();
				}
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
	
	/**
	 * Default constructor
	 */
	public PlanEditor() {
		super();
		PlanEventManager.getInstance().addListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().addListener(EventType.PLAN_DELETED, deleteListener);
	}

	@Override
	public void dispose() {
		PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_DELETED, deleteListener);
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

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public void createPartControl(Composite parent) {
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		form.getBody().setLayout(new GridLayout(1, true));

		final Section summary = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE );
		summary.setLayout(new GridLayout(2, false));
		summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		summary.setText(Messages.PlanEditor_Summary_Label);
		summary.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (summary.isExpanded()){
					summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));			
				}else{
					summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				summary.getParent().layout(true, true);
			}
		});
		
		summaryScroll = new ScrolledComposite(summary, SWT.V_SCROLL | SWT.H_SCROLL );
		
		summaryScroll.setExpandHorizontal(true);
		summaryScroll.setExpandVertical(true);
		toolkit.adapt(summaryScroll);
		
		content = toolkit.createComposite(summaryScroll, SWT.NONE);
		summaryScroll.setContent(content);
		summary.setClient(summaryScroll);

		GridLayout layout1 = new GridLayout(2, false);
		layout1.marginHeight = 0;
		content.setLayout(layout1);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Composite topContent = toolkit.createComposite(content, SWT.NONE);
		GridLayout layout2 = new GridLayout(2, false);
		layout2.marginHeight = 0;
		topContent.setLayout(layout1);
		topContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite leftContent = toolkit.createComposite(topContent, SWT.NONE);
		leftContent.setLayout(new GridLayout(3, false));
		leftContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite rightContent = toolkit.createComposite(topContent, SWT.NONE);
		rightContent.setLayout(new GridLayout(3, false));
		rightContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)rightContent.getLayout()).marginLeft = 20;
		
		//left
		toolkit.createLabel(leftContent, Messages.PlanEditor_PlanId_Label);
		txtPlanID= toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtPlanID.setEditable(false);
		txtPlanID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(leftContent, "");  //$NON-NLS-1$
		
		toolkit.createLabel(leftContent, Messages.PlanEditor_Name_Label);
		txtName = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(leftContent, ""); //$NON-NLS-1$
		
		toolkit.createLabel(leftContent, Messages.PlanEditor_Description_Label);
		txtDescription = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtDescription.setEditable(false);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtDescription.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, leftContent, PanelType.PLANID);
		
		// - right
		toolkit.createLabel(rightContent, Messages.PlanEditor_Type_Label);
		txtType = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtType.setEditable(false);
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, rightContent, PanelType.TYPE);

		toolkit.createLabel(rightContent, Messages.PlanEditor_UnavailableEmployees_Label);
		txtUnavailableEmployees = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtUnavailableEmployees.setEditable(false);
		txtUnavailableEmployees.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, rightContent, PanelType.TYPE);
		
		toolkit.createLabel(rightContent, Messages.PlanEditor_ParentPlan_Label);
		txtParentPlanId = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtParentPlanId.setEditable(false);
		txtParentPlanId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, rightContent, PanelType.PLANPARENTID);

		// left
		Label lbl = toolkit.createLabel(leftContent, ""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		
		toolkit.createLabel(leftContent, Messages.PlanEditor_StartDate_Label);
		txtStartDate = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(leftContent, ""); //$NON-NLS-1$

		toolkit.createLabel(leftContent, Messages.PlanEditor_EndDate_Label);
		txtEndDate = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtEndDate.setEditable(false);
		txtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		createEditLink(toolkit, leftContent, PanelType.STARTDATE);
		
		//right
		Label lbl1 = toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		
		toolkit.createLabel(rightContent, Messages.PlanEditor_Station_Label);
		txtStation = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtStation.setEditable(false);
		txtStation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(rightContent, "");  //$NON-NLS-1$

		toolkit.createLabel(rightContent, Messages.PlanEditor_Team_Label);
		txtTeam = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtTeam.setEditable(false);
		txtTeam.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, rightContent, PanelType.STATION);
		
		//bottom
		final Composite bottomContent = toolkit.createComposite(content, SWT.NONE);
		bottomContent.setLayout(new GridLayout(2, false));
		bottomContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
		Label l = toolkit.createLabel(bottomContent, Messages.PlanEditor_PatrolLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData)l.getLayoutData()).verticalIndent = 4;
		patrolLinks  = toolkit.createComposite(bottomContent);
		patrolLinks.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.spacing = 5;
		
		patrolLinks.setLayout(layout);
		
		final Label lc = toolkit.createLabel(bottomContent, "*" + Messages.PlanEditor_ChildPatrolLabel); //$NON-NLS-1$
		lc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		summaryScroll.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		summaryScroll.addControlListener(new ControlAdapter() {
		      public void controlResized(ControlEvent e) {    	 
		        Rectangle r = summaryScroll.getClientArea();
		        Point p2 = topContent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		        
		        int newHeight = content.computeSize(r.width, SWT.DEFAULT).y;
		        int newWidth = p2.x;
		        summaryScroll.setMinSize(newWidth, newHeight);
		      }
		    });
		
		/* --- target section --- */
		final Section targetSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE);
		targetSection.setText(Messages.PlanEditor_Targets_Label);
		targetSection.setLayout(new GridLayout(3, false));
		targetSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		targetSection.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (targetSection.isExpanded()){
					targetSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
				}else{
					targetSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				targetSection.getParent().layout();
			}
		});
	
		ScrolledComposite scroll = new ScrolledComposite(targetSection, SWT.V_SCROLL | SWT.H_SCROLL );
		
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		toolkit.adapt(scroll);
		
		Composite targetContent = toolkit.createComposite(scroll);
		targetSection.setClient(scroll);
		scroll.setContent(targetContent);
		
		targetContent.setLayout(new GridLayout(2, false));
		targetContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label ll2 = toolkit.createLabel(targetContent, Messages.PlanEditor_PlanTarges_Label);
		ll2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		FontData fd = ll2.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		final Font boldFont = new Font(Display.getDefault(),fd);
		form.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				boldFont.dispose();
				
			}
		});
		ll2.setFont(boldFont);
		
		
		targetList  = new TargetProgressViewer(targetContent);
		targetList.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (canEdit()) {
					showEditDialog(PanelType.TARGETS);
				}
			}
		});

		Composite targetButtons = toolkit.createComposite(targetContent, SWT.NONE);
		targetButtons.setLayout(new GridLayout(1, false));
		targetButtons.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
	
		Hyperlink lnkRefresh = toolkit.createHyperlink(targetButtons, Messages.PlanEditor_Refresh_Link_Label, SWT.NONE);
		lnkRefresh.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				targetList.refreshStatus();
			}
		});
		createEditLink(toolkit, targetButtons, PanelType.TARGETS);

		
		Label ll = toolkit.createLabel(targetContent, Messages.PlanEditor_ChildPlanTarges_Label);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		ll.setFont(boldFont);
		
		targetList2  = new TargetProgressViewer(targetContent, true);
		

		Composite childTargetButtons = toolkit.createComposite(targetContent, SWT.NONE);
		childTargetButtons.setLayout(new GridLayout(1, false));
		childTargetButtons.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
	
		Hyperlink lnkRefreshChild = toolkit.createHyperlink(childTargetButtons, Messages.PlanEditor_Refresh_Link_Label, SWT.NONE);
		lnkRefreshChild.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshPlanTargets.schedule();
			}
		});
		scroll.setMinSize(targetContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		initValues();
	}
	
	/**
	 * Updates the widgets with the value from the plan.
	 */
	private void initValues() {
		Plan plan = getPlan();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			session.update(plan);
			setPartName(plan.getLabel());

			setTitleImage(SmartPlanPlugIn.getDefault().getImageRegistry()
					.get(plan.getType().getIconKey()));
			form.setText(plan.getLabel());
			String none = Messages.PlanEditor_None_Label;

			if (plan.getStation() != null) {
				txtStation.setText(plan.getStation().getName());
			} else {
				txtStation.setText(none);
			}
			if (plan.getTeam() != null) {
				txtTeam.setText(plan.getTeam().getName());
			} else {
				txtTeam.setText(none);
			}
			txtType.setText(plan.getType().getName());
			txtUnavailableEmployees.setText(plan.getUnavailableEmployees()
					.toString());
			if (plan.getParent() != null) {
				txtParentPlanId.setText(plan.getParent().getId());
			} else {
				txtParentPlanId.setText(none);
			}
			txtPlanID.setText(plan.getId());
			if (plan.getName() != null) {
				txtName.setText(plan.getName());
			}
			if (plan.getDescription() != null) {
				txtDescription.setText(plan.getDescription());
			}
			txtStartDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
					.format(plan.getStartDate()));
			txtEndDate.setText(DateFormat.getDateInstance(DateFormat.LONG)
					.format(plan.getEndDate()));

			for (Control kid : patrolLinks.getChildren()){
				kid.dispose();
			}
			
			targetList.initValues(plan.getTargets());
			
			loadPatrolsLinksJob.schedule();
			refreshPlanTargets.schedule();
			
			
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
		
	}
	
	private void getChildTargets(Plan parent, List<PlanTarget> targets){
		for(Plan p : parent.getChildren()){
			targets.addAll(p.getTargets());
			getChildTargets(p, targets);
		}
	}
	private void openPatrol(PatrolEditorInput p){
		try {
			PlatformUI.getWorkbench().showPerspective(PatrolPerspective.ID, 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().openEditor(p, PatrolEditor.ID);
		} catch (Exception e1) {
			SmartPlanPlugIn.displayLog(
					Messages.PlanEditor_CannotOpenFile_Error
							+ e1.getLocalizedMessage(), e1);
		}
	}

	
	/**
	 * 
	 * @return the current plan associated with the editor
	 */
	private void getChildPlanPatrols(Plan plan, Set<PatrolEditorInput> kids){
		if(plan.getChildren() == null){
			return;
		}
		for (Plan p : plan.getChildren()){
			kids.addAll(PlanHibernateManager.getPatrols(p));
		}
	}

	/**
	 * 
	 * @return the current plan associated with the editor
	 */
	private Plan getPlan(){
		if (plan == null){
			plan = loadPlan();
		}
		return plan;
	}

	/*
	 * loads the plan and all lazy fields from the database.
	 * Will always get a new object.
	 */
	private Plan loadPlan(){
		byte[] puuid = ((PlanEditorInput) getEditorInput()).getUuid();
		Session session = HibernateManager.openSession();
		//load parent plan so don't have lazy loading issues later.
		session.beginTransaction();
		plan = (Plan) session.load(Plan.class, puuid);
		if (plan.getParent() != null) {
			plan.getParent().getId();
		}
		if(plan.getTargets() != null){
			plan.getTargets().size();
		}
		Station st = plan.getStation();
		if(st != null){
			st.getName();
		}
		plan.getTeam();
		Team t = plan.getTeam();
		if(t != null){
			t.getName();
		}
		session.getTransaction().rollback();
		session.close();
		return plan;
	}
	/**
	 * Creates an edit hyperlink button
	 * @param tolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit tolkit, Composite parent, final PanelType panelType) {
		Hyperlink editLink = toolkit.createHyperlink(parent, Messages.PlanEditor_Edit_Link_Label, SWT.WRAP);
		
		if (!canEdit()) {
			editLink.setEnabled(false);
			editLink.setVisible(false);
		}
		
		if (panelType != null){
			editLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					showEditDialog(panelType);
				}
			});
		}
		return editLink;
	}
	
	private boolean canEdit() {
		//analyst users can never edit
		return SmartDB.getCurrentEmployee().getSmartUserLevel() != Employee.SmartUserLevel.ANALYST;
	}

	/**
	 * Displays and edit dialog for editing a particular item in
	 * plan object.
	 * 
	 * @param panelType type of inner panel to be created
	 * @return  true if changes made, false otherwise
	 */
	private boolean showEditDialog(PanelType panelType){
		
		int ret = -1;
		try {
			//get a copy to edit incase something goes wrong
			Plan copy = loadPlan();
			final EditPlanItemDialog editDialog = new EditPlanItemDialog(
					getEditorSite().getShell(), 
					panelType, copy);
			
			ret = editDialog.open();
		} finally {
			
		}
		
		if (ret == IDialogConstants.OK_ID){
			return true;
		}
		return false;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		//TODO: fix me
		txtPlanID.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
