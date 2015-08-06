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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.internal.PlanLabelProvider;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory.PanelType;
import org.wcs.smart.plan.ui.targets.TargetProgressViewer;
import org.wcs.smart.plan.ui.targets.TargetPropertyDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;
/**
 * Plan editor summary page.
 * 
 * @author Emily
 *
 */
public class SummaryPlanEditorPage extends EditorPart {

	private static final int SECTION_SPACING = 8;
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

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
	private Label txtCreator;
	private Text txtComment;
	
	private TargetProgressViewer targetList;
	private TargetProgressViewer targetList2; //the child plan's targets

	private Composite content;
	private Composite patrolLinks;
	
	private ScrolledComposite summaryScroll;
	
	private PlanEditor parentEditor;
	
	
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
			

			final Set<PatrolEditorInput> childPatrols = new HashSet<PatrolEditorInput>(); 
			final List<PatrolEditorInput> myPatrols;
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				myPatrols = PlanHibernateManager.getPatrols(parentEditor.getPlan(), s);
				Plan thisPlan = (Plan) s.get(Plan.class, parentEditor.getPlan().getUuid());	//load a copy so we don't have problems with trying to have plan open in multiple sessions
				parentEditor.getChildPlanPatrols(thisPlan, childPatrols, s);
				s.getTransaction().rollback();
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
						
					} else {
						for (final PatrolEditorInput x : myPatrols){
							Hyperlink lnk = toolkit.createHyperlink(patrolLinks, x.getPatrolId(), SWT.WRAP);
							lnk.addHyperlinkListener(new HyperlinkAdapter() {
								@Override
								public void linkActivated(HyperlinkEvent e) {
									parentEditor.openPatrol(x);
								}
							});
						}
						for (final PatrolEditorInput x : childPatrols){
							Hyperlink lnk = toolkit.createHyperlink(patrolLinks, x.getPatrolId() + "*", //$NON-NLS-1$
									SWT.WRAP);
							lnk.addHyperlinkListener(new HyperlinkAdapter() {
								@Override
								public void linkActivated(HyperlinkEvent e) {
									parentEditor.openPatrol(x);
								}
							});
						}
					}	
					patrolLinks.layout();
			        form.layout();
	
				}});
			parentEditor.refreshSubPlanTargets();
			return Status.OK_STATUS;

		}};
		
	/**
	 * Creates a new plan editor page
	 * @param editor
	 */
	public SummaryPlanEditorPage(PlanEditor editor){
		this.parentEditor = editor;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		form = toolkit.createForm(parent);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		GridLayout glayout = new GridLayout();
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		form.getBody().setLayout(glayout);
		if (this.parentEditor.canEdit()) {
			Hyperlink translateLink = toolkit.createHyperlink(form.getBody(), Messages.PlanEditor_Translate_Link, SWT.WRAP);
			translateLink.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			translateLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(
							getEditorSite().getShell(), SummaryPlanEditorPage.this.parentEditor.getPlan());
					
					if (dialog.open() == IDialogConstants.OK_ID) {
						Session session = HibernateManager.openSession();
						try{
							PlanHibernateManager.savePlan(SummaryPlanEditorPage.this.parentEditor.getPlan(),session);
						}finally{
							session.close();
						}
						PlanEventManager.getInstance()
								.planChanged(0, SummaryPlanEditorPage.this.parentEditor.getPlan());
					}
				}
			});
		}

		final Section summary = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED | Section.TWISTIE );
		summary.setLayout(new GridLayout(2, false));
		summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summary.setText(Messages.PlanEditor_Summary_Label);
		summary.addExpansionListener(new ExpansionAdapter() {
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (summary.isExpanded()){
					summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
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
		topContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftContent = toolkit.createComposite(topContent, SWT.NONE);
		leftContent.setLayout(new GridLayout(3, false));
		leftContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite rightContent = toolkit.createComposite(topContent, SWT.NONE);
		rightContent.setLayout(new GridLayout(3, false));
		rightContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)rightContent.getLayout()).marginLeft = 20;
		
		//left
		toolkit.createLabel(leftContent, Messages.PlanEditor_PlanId_Label);
		txtPlanID= toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtPlanID.setEditable(false);
		txtPlanID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtPlanID.getLayoutData()).widthHint = 100;
		toolkit.createLabel(leftContent, "");  //$NON-NLS-1$
		
		toolkit.createLabel(leftContent, Messages.PlanEditor_Name_Label);
		txtName = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtName.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, leftContent, PanelType.PLANID);
		
		int height = txtName.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		Label clbl = toolkit.createLabel(leftContent, Messages.SummaryPlanEditorPage_CreatorLabel);
		clbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		((GridData)clbl.getLayoutData()).heightHint = height;
		
		txtCreator = toolkit.createLabel(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtCreator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		((GridData)txtCreator.getLayoutData()).heightHint = height;
		((GridData)txtCreator.getLayoutData()).widthHint = 100;
		
		// - right
		toolkit.createLabel(rightContent, Messages.PlanEditor_Type_Label);
		txtType = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtType.setEditable(false);
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtType.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, rightContent, PanelType.TYPE);
		
		toolkit.createLabel(rightContent, Messages.PlanEditor_UnavailableEmployees_Label);
		txtUnavailableEmployees = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtUnavailableEmployees.setEditable(false);
		txtUnavailableEmployees.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtUnavailableEmployees.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, rightContent, PanelType.TYPE);
		
		toolkit.createLabel(rightContent, Messages.PlanEditor_ParentPlan_Label);
		txtParentPlanId = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtParentPlanId.setEditable(false);
		txtParentPlanId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtParentPlanId.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, rightContent, PanelType.PLANPARENTID);

		
		//left and right spacer
		Label lbl = toolkit.createLabel(leftContent, ""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		((GridData)lbl.getLayoutData()).heightHint = SECTION_SPACING;
		
		lbl = toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		((GridData)lbl.getLayoutData()).heightHint = SECTION_SPACING;
		
		// left
		toolkit.createLabel(leftContent, Messages.PlanEditor_StartDate_Label);
		txtStartDate = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtStartDate.getLayoutData()).widthHint = 100;
		toolkit.createLabel(leftContent, ""); //$NON-NLS-1$

		toolkit.createLabel(leftContent, Messages.PlanEditor_EndDate_Label);
		txtEndDate = toolkit.createText(leftContent, "", SWT.NONE); //$NON-NLS-1$
		txtEndDate.setEditable(false);
		txtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtEndDate.getLayoutData()).widthHint = 100;
		
		createEditLink(toolkit, leftContent, PanelType.STARTDATE);
		
		//right
		toolkit.createLabel(rightContent, Messages.PlanEditor_Station_Label);
		txtStation = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtStation.setEditable(false);
		txtStation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtStation.getLayoutData()).widthHint = 100;
		toolkit.createLabel(rightContent, "");  //$NON-NLS-1$

		toolkit.createLabel(rightContent, Messages.PlanEditor_Team_Label);
		txtTeam = toolkit.createText(rightContent, "", SWT.NONE); //$NON-NLS-1$
		txtTeam.setEditable(false);
		txtTeam.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)txtTeam.getLayoutData()).widthHint = 100;
		createEditLink(toolkit, rightContent, PanelType.STATION);
		
		
		//left and right spacer
		lbl = toolkit.createLabel(leftContent, ""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		((GridData)lbl.getLayoutData()).heightHint = SECTION_SPACING;
				
		lbl = toolkit.createLabel(rightContent, ""); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,3,1));
		((GridData)lbl.getLayoutData()).heightHint = SECTION_SPACING;
				
		
		//left
		lbl = toolkit.createLabel(leftContent, Messages.PlanEditor_Description_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtDescription = toolkit.createText(leftContent, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtDescription.setEditable(false);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.heightHint = 40;
		gd.widthHint = 100;
		txtDescription.setLayoutData(gd);
		Hyperlink lnk = createEditLink(toolkit, leftContent, PanelType.PLANID);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		// right
		lbl = toolkit.createLabel(rightContent, Messages.SummaryPlanEditorPage_CommentLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtComment = toolkit.createText(rightContent, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		txtComment.setEditable(false);
		gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.heightHint = 40;
		gd.widthHint = 100;
		txtComment.setLayoutData(gd);
		lnk = createEditLink(toolkit, rightContent, PanelType.COMMENT);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		
		//bottom
		final Composite bottomContent = toolkit.createComposite(content, SWT.NONE);
		glayout = new GridLayout(2, false);
		glayout.verticalSpacing = 0;
		glayout.marginHeight = 0;
		glayout.marginBottom = 5;
		bottomContent.setLayout(glayout);
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
					summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}else{
					targetSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					if (summary.isExpanded()){
						summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					}else{
						summary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
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
		
		
		targetList  = new TargetProgressViewer(targetContent, toolkit);
		targetList.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editCurrentPlanTarget();
			}
		});

		Composite targetButtons = toolkit.createComposite(targetContent, SWT.NONE);
		targetButtons.setLayout(new GridLayout(1, false));
		targetButtons.setLayoutData(new GridData(SWT.TOP, SWT.BOTTOM, false, false));

		final Hyperlink editTargetLink = createEditLink(toolkit, targetButtons, null); //null is on purpose (not to add listener)
		editTargetLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				editCurrentPlanTarget();
			}
		});
		editTargetLink.setEnabled(false);
		targetList.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				editTargetLink.setEnabled(!targetList.getSelection().isEmpty());				
			}
		});
		Hyperlink manageLink = createEditLink(toolkit, targetButtons, PanelType.TARGETS);
		manageLink.setText(Messages.PlanEditor_Manage_Link_Label);
		
		Hyperlink lnkRefresh = toolkit.createHyperlink(targetButtons, Messages.PlanEditor_Refresh_Link_Label, SWT.NONE);
		lnkRefresh.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				targetList.clearStatus();
				parentEditor.computePlanTargetStatus();
			}
		});


		
		Label ll = toolkit.createLabel(targetContent, Messages.PlanEditor_ChildPlanTarges_Label);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		ll.setFont(boldFont);
		
		targetList2  = new TargetProgressViewer(targetContent, true, toolkit);
		

		Composite childTargetButtons = toolkit.createComposite(targetContent, SWT.NONE);
		childTargetButtons.setLayout(new GridLayout(1, false));
		childTargetButtons.setLayoutData(new GridData(SWT.TOP, SWT.BOTTOM, false, false));
	
		Hyperlink lnkRefreshChild = toolkit.createHyperlink(childTargetButtons, Messages.PlanEditor_Refresh_Link_Label, SWT.NONE);
		lnkRefreshChild.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				targetList2.clearStatus();
				parentEditor.refreshSubPlanTargets();
			}
		});
		scroll.setMinSize(targetContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	private void editCurrentPlanTarget() {
		if (parentEditor.canEdit()) {
			IStructuredSelection selection = (IStructuredSelection)targetList.getSelection();
	        if (selection.isEmpty()) {
	            return;
	        }
	        PlanTarget selected = (PlanTarget)selection.getFirstElement(); 
			TargetPropertyDialog dialog = new TargetPropertyDialog(getEditorSite().getShell(), parentEditor.getPlan().getTargets(), selected);
			dialog.open();
			ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
			if (dialog.isSavePerformed()) {
				boolean saved = false;
				Session session = HibernateManager.openSession();
				try{
					saved = PlanHibernateManager.savePlan(parentEditor.getPlan(), session);
				}finally{
					session.close();
				}
				if (saved) {
					PlanEventManager.getInstance().planChanged(0, parentEditor.getPlan());
				}
			}
		}
	}
	
	/**
	 * Updates the widgets with the value from the plan.
	 */
	public void initValues() {
		Plan plan = this.parentEditor.getPlan();

		setPartName(plan.getLabel());

		setTitleImage(PlanLabelProvider.getImage(plan.getType()).createImage());
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
		txtType.setText(plan.getType().getGuiName(Locale.getDefault()));
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
		txtStartDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(plan.getStartDate()));
		txtEndDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(plan.getEndDate()));

		if (plan.getCreator() != null){
			txtCreator.setText(LabelConstants.getFullLabel(plan.getCreator()));
		}else{
			txtCreator.setText(""); //$NON-NLS-1$
		}
		
		if (plan.getComment() != null){
			txtComment.setText(plan.getComment());
		}else{
			txtComment.setText(""); //$NON-NLS-1$
		}
		
		for (Control kid : patrolLinks.getChildren()) {
			kid.dispose();
		}
		
		targetList.initValues(plan.getTargets());
		
		refreshPatrolLinks();
		parentEditor.refreshSubPlanTargets();
	}
	
	public void refreshPlanTargetList(){
		targetList.refresh();		
	}
	
	public void refreshPatrolLinks(){
		loadPatrolsLinksJob.schedule();
	}
	/**
	 * Creates an edit hyperlink button
	 * @param tolkit toolkit
	 * @param parent parent composite
	 * @param partEditor editor to use
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit tolkit, Composite parent, final PanelType panelType) {
		Hyperlink editLink = toolkit.createHyperlink(parent, DialogConstants.EDIT_LINK_TEXT, SWT.WRAP);
		
		if (!this.parentEditor.canEdit()) {
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
			Plan copy = parentEditor.loadPlan();
			final EditPlanItemDialog editDialog = new EditPlanItemDialog(
					getEditorSite().getShell(), 
					panelType, copy);
			
			ret = editDialog.open();
		} finally {
			//this ensure the map tools work correctly
			ApplicationGIS.getToolManager().setCurrentEditor(parentEditor);
		}
		
		if (ret == IDialogConstants.OK_ID){
			return true;
		}
		return false;
	}
	
	
	public void refreshChildTargets(List<PlanTarget> childTargets){
		targetList2.initValues(childTargets);
	}

	@Override
	public void setFocus() {
		txtPlanID.setFocus();
	}

}
