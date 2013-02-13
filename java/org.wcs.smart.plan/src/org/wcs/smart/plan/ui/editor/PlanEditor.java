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
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory.PanelType;
import org.wcs.smart.plan.ui.targets.TargetListViewer;
import org.wcs.smart.plan.ui.targets.TargetProgressViewer;
import org.wcs.smart.plan.ui.targets.TargetPropertyPage;

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

	/**
	 * listener for plan change events.
	 */
	private IPlanEventListener planListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			if (Arrays.equals(source.getUuid(), getPlan().getUuid())){
				//if this is our plan we want to update our object
				//with the new one.
				plan = source;
				initValues();
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
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		form.getBody().setLayout(new GridLayout(1, true));

		Composite content = toolkit.createComposite(form.getBody(), SWT.NONE);
		GridLayout leftLayout = new GridLayout(3, false);
		leftLayout.verticalSpacing = 10;
		content.setLayout(leftLayout);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)content.getLayout()).marginRight = 10;
		
		
		toolkit.createLabel(content, "Plan ID:");
		txtPlanID= toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtPlanID.setEditable(false);
		txtPlanID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.PLANID);
		
		toolkit.createLabel(content, "Plan Name:");
		txtName = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtName.setEditable(false);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, "");
		
		toolkit.createLabel(content, "Description:");
		txtDescription = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtDescription.setEditable(false);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, ""); 

		//spacer row for grouping items
		toolkit.createLabel(content, "");
		Label lbl = toolkit.createSeparator(content, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		toolkit.createLabel(content, "Plan Type:");
		txtType = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtType.setEditable(false);
		txtType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.TYPE);

		toolkit.createLabel(content, "Unavailable Employees:");
		txtUnavailableEmployees = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtUnavailableEmployees.setEditable(false);
		txtUnavailableEmployees.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, "");

		//spacer row for grouping items
		toolkit.createLabel(content, "");

		Label lbl1 = toolkit.createSeparator(content, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		toolkit.createLabel(content, "Station:");
		txtStation = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStation.setEditable(false);
		txtStation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.STATION); 

		toolkit.createLabel(content, "Team:");
		txtTeam = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtTeam.setEditable(false);
		txtTeam.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, "");

		//spacer row for grouping items
		toolkit.createLabel(content, "");
		Label lbl2 = toolkit.createSeparator(content, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));

				
		toolkit.createLabel(content, "Parent ID:");
		txtParentPlanId = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtParentPlanId.setEditable(false);
		txtParentPlanId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.PLANPARENTID);

		//spacer row for grouping items
		toolkit.createLabel(content, "");
		Label lbl3 = toolkit.createSeparator(content, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		toolkit.createLabel(content, "Start Date:");
		txtStartDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtStartDate.setEditable(false);
		txtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		createEditLink(toolkit, content, PanelType.STARTDATE);

		toolkit.createLabel(content, "Unavailable Employees:");
		txtEndDate = toolkit.createText(content, "", SWT.NONE); //$NON-NLS-1$
		txtEndDate.setEditable(false);
		txtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		toolkit.createLabel(content, "");

		//spacer row for grouping items
		toolkit.createLabel(content, "");
		Label lbl4 = toolkit.createSeparator(content, SWT.SEPARATOR | SWT.HORIZONTAL);
		lbl4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		toolkit.createLabel(content, "Plan Targets:");
		targetList  = new TargetProgressViewer(content);

		
		Composite targetButtons = toolkit.createComposite(content, SWT.NONE);
		targetButtons.setLayout(new GridLayout(1, false));
		targetButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		
		createEditLink(toolkit, targetButtons, PanelType.TARGETS); 

		Button btnRefresh = new Button(targetButtons, SWT.NONE);
		btnRefresh.setText("Refresh");
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				initValues();
			}
			
		});
		
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
			String none = "<none>";

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

			targetList.initValues(plan.getTargets());
			session.getTransaction().rollback();
		} finally {
			session.close();
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
		plan.getTargets().size();
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
		Hyperlink editLink = toolkit.createHyperlink(parent, "Edit", SWT.WRAP);
		
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
