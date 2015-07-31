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
package org.wcs.smart.plan.internal.ui.patrol;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.EditPlanItemDialog;
import org.wcs.smart.plan.ui.editor.PlanEditor;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory.PanelType;
import org.wcs.smart.plan.ui.perspective.PlanPerspective;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Contribution item for the patrol editor.
 * 
 * @author Emily
 * 
 */
public class PatrolPlanContribution implements IPatrolEditorContribution {

	private Composite main;
	private FormToolkit toolkit;
	private Plan currentPlan = null;
	private Patrol currentPatrol = null;

	private IPlanEventListener planDeleteListener = new IPlanEventListener() {
		@Override
		public void eventFired(int attributeChanged, Plan source) {
			if (currentPlan == null){
				return;
			}
			if (source.getUuid().equals(currentPlan.getUuid())){
				//my plan was deleted
				currentPlan = null;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						initContent();
					}});
			}
		}
	};
	
	private IPlanEventListener planModifiedListener = new IPlanEventListener() {
		@Override
		public void eventFired(int attributeChanged, Plan source) {
			if (currentPlan == null){
				return;
			}
			if (source.getUuid().equals(currentPlan.getUuid())){
			
				Job j = new Job(Messages.PatrolPlanContribution_RefreshJob_Title){

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						//reload Plan
						Session session = HibernateManager.openSession();
						session.beginTransaction();
						try {
							currentPlan = (Plan) session.get(Plan.class, currentPlan.getUuid());
							Plan parent = currentPlan.getParent();
							// ensure parents are lazily loaded
							while (parent != null) {
								parent = parent.getParent();
							}
							session.getTransaction().rollback();
						} finally {
							session.close();
						}
						
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								initContent();
							}});
						return Status.OK_STATUS;
					}
					
				};
				j.schedule();
			}
		}
	};

	public PatrolPlanContribution() {
	}

	@Override
	public String getName() {
		return Messages.PatrolPlanContribution_Name;
	}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {
		Composite outer = toolkit.createComposite(parent);
		outer.setLayout(new GridLayout((canEdit ? 2 : 1),false));
		
		main = toolkit.createComposite(outer);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (canEdit){
			Hyperlink edit = toolkit.createHyperlink(outer, DialogConstants.EDIT_LINK_TEXT, SWT.NONE);
			edit.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			edit.addHyperlinkListener(new HyperlinkAdapter(){
				@Override
				public void linkActivated(HyperlinkEvent e) {
					editPlan();
				}
			});
		}
		
		
		this.toolkit = toolkit;
		
		PlanEventManager.getInstance().addListener(EventType.PLAN_MODIFIED, planModifiedListener);
		PlanEventManager.getInstance().addListener(EventType.PLAN_DELETED, planDeleteListener);
		main.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planModifiedListener);
				PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planDeleteListener);
			}
		});
		return outer;
	}

	private void editPlan(){
		EditPlanItemDialog dialog = new EditPlanItemDialog(main.getShell(), PanelType.PATROLPLAN, currentPlan){
			@Override
			protected boolean performSave() {
				Plan newPlan = null;
				Plan oldPlan = currentPlan;
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					String hql = "DELETE FROM PatrolPlan where id.patrol = :patrol"; //$NON-NLS-1$
					Query q = s.createQuery(hql).setParameter("patrol", currentPatrol); //$NON-NLS-1$
					q.executeUpdate();
					UUID planuuid = ((PatrolPlanComposite)content).getSelection();
					if (planuuid != null){
						PatrolPlan pp = new PatrolPlan();
						pp.setPatrol(currentPatrol);
						newPlan = (Plan)s.get(Plan.class, planuuid);
						Plan parent = newPlan.getParent();
						while(parent != null){
							parent = parent.getParent();
						}
						pp.setPlan(newPlan);
						s.save(pp);
					}
					s.getTransaction().commit();
					currentPlan = newPlan;
					setChangesMade(false);
					
					if (newPlan != null){
						PlanEventManager.getInstance().planChanged(PlanEventManager.PATROL_PLAN_ATTRIBUTE, newPlan);
					}
					if (oldPlan != null){
						PlanEventManager.getInstance().planChanged(PlanEventManager.PATROL_PLAN_ATTRIBUTE, oldPlan);
					}
					
					return true;
				}catch (Exception ex){
					s.getTransaction().rollback();
					SmartPlanPlugIn.displayLog(Messages.PatrolPlanContribution_Save_Error + ex.getLocalizedMessage(), ex);
					return false;
				}finally{
					s.close();
				}
			
			}
		};
		dialog.open();
		initContent();
	}
	
	@Override
	public void setPatrol(Patrol patrol) {
		this.currentPatrol = patrol;
		this.currentPlan = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			this.currentPlan = PlanHibernateManager.getPlanForPatrol(patrol, session);
			if (this.currentPlan != null) {
				Plan parent = currentPlan.getParent();
				// ensure parents are lazily loaded
				while (parent != null) {
					parent = parent.getParent();
				}
			}
			session.getTransaction().rollback();
		} finally {
			session.close();
		}
		initContent();

	}

	private void openPlan(){
		PlanEditorInput in = new PlanEditorInput(currentPlan.getUuid(), 
				currentPlan.getLabel(), currentPlan.getType());
		try {
			PlatformUI.getWorkbench().showPerspective(PlanPerspective.ID, 
					PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().openEditor(in, PlanEditor.ID);
		} catch (Exception e1) {
			SmartPlanPlugIn.displayLog(
					Messages.PatrolPlanContribution_Open_Error
							+ e1.getLocalizedMessage(), e1);
		}
	}
	
	private void initContent() {
		for (Control child : main.getChildren()) {
			child.dispose();
		}

		if (currentPlan == null) {
			toolkit.createLabel(main,
					Messages.PatrolPlanContribution_NoAssociatedPlan_Label);
		} else {
			Composite planc = toolkit.createComposite(main);
			planc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			planc.setLayout(new GridLayout(2, false));

			Label lbl = toolkit.createLabel(planc, Messages.PatrolPlanContribution_PlanId_Label);
			lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			Hyperlink lnk = toolkit.createHyperlink(planc, currentPlan.getId(),
					SWT.WRAP);
			lnk.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					openPlan();
				}
			});

			lbl = toolkit.createLabel(planc, Messages.PatrolPlanContribution_Name_Label);
			lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			toolkit.createLabel(planc, currentPlan.getName());

			lbl = toolkit.createLabel(planc, Messages.PatrolPlanContribution_Description_Label);
			lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			Text txt = toolkit.createText(planc, currentPlan.getDescription(),
					SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL | SWT.BORDER);
			// txt.setEditable(false);
			txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData) txt.getLayoutData()).heightHint = 80;
			((GridData) txt.getLayoutData()).widthHint = 100;

			lbl = toolkit.createLabel(planc, Messages.PatrolPlanContribution_Parents_Label);
			lbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			StringBuilder sb = new StringBuilder();
			Plan parent = currentPlan.getParent();
			while (parent != null) {
				sb.append(parent.getLabel());
				parent = parent.getParent();
				if (parent != null) {
					sb.append("\n"); //$NON-NLS-1$
				}
			}
			if (sb.length() == 0) {
				toolkit.createLabel(planc, Messages.PatrolPlanContribution_None_Label);
			} else {
				toolkit.createLabel(planc, sb.toString());
			}
		}
		main.getParent().getParent().getParent().layout(true, true);
	}
}
