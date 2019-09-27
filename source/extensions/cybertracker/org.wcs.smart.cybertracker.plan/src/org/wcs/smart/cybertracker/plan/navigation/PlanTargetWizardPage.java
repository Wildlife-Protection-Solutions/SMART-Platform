/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.plan.navigation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.plan.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Wizard page for selecting sampling units from a survey design.
 * 
 * @author Emily
 *
 */
public class PlanTargetWizardPage extends WizardPage {

	public static final String PAGE_NAME ="PatrolTrack"; //$NON-NLS-1$
	
	private static final String SELECTPLAN = Messages.PlanTargetWizardPage_SelectaPlan;
	
	private CheckboxTableViewer tblViewer;
	private ComboViewer cmbViewer;
	
	
	private AtomicBoolean isLoaded = new AtomicBoolean(false);
	
	private List<SpatialPlanTarget> selectedItems = new ArrayList<>();
	
	protected PlanTargetWizardPage() {
		super(PAGE_NAME);
	}

	@Override
	public void setVisible(boolean visible) {
		if (!isLoaded.getAndSet(true)) {
			loadValues.schedule();
		}
		super.setVisible(visible);
	}
	
	public List<SpatialPlanTarget> getPlanTargets(){
		return selectedItems;
	}
	
	@Override
	public boolean isPageComplete() {
		if (!selectedItems.isEmpty()) return true;
		return false;
	}
	
	@Override
	public IWizardPage getNextPage() {
		return null;
	}

	@Override
	public IWizardPage getPreviousPage() {
		return getWizard().getPages()[0];
	}
	
	@Override
	public void createControl(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main,SWT.NONE);
		l.setText(Messages.PlanTargetWizardPage_PlanLAbel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		cmbViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plan) return MessageFormat.format("{0} [{1}]",((Plan)element).getName(),((Plan)element).getId()); //$NON-NLS-1$
				return super.getText(element);
			}
		});
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cmbViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		l = new Label(main,SWT.NONE);
		l.setText(Messages.PlanTargetWizardPage_TargetsLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite wrapper = new Composite(main, SWT.NONE);
		wrapper.setLayout(new TableColumnLayout());
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblViewer = CheckboxTableViewer.newCheckList(wrapper, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblViewer));
		
		TableViewerColumn tc = new TableViewerColumn(tblViewer, SWT.CHECK);
		((TableColumnLayout)wrapper.getLayout()).setColumnData(tc.getColumn(), new ColumnWeightData(100));
		tc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SpatialPlanTarget) {
					SpatialPlanTarget pt = (SpatialPlanTarget)element;
					return pt.getName();
				}
				return super.getText(element);
			}
		});
		
		tblViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				selectedItems.clear();
				for (Object o : tblViewer.getCheckedElements()) {
					if (o instanceof SpatialPlanTarget) selectedItems.add((SpatialPlanTarget)o);
				}
				getWizard().getContainer().updateButtons();
			}
		});
		cmbViewer.addSelectionChangedListener(e->{
			selectedItems.clear();
			
			List<SpatialPlanTarget> tgs = new ArrayList<>();
			Object p = e.getStructuredSelection().getFirstElement();
			if ( p != null && p instanceof Plan) {
				Plan plan = (Plan)p;
				for (PlanTarget t : plan.getTargets()) {
					if (t instanceof SpatialPlanTarget) tgs.add((SpatialPlanTarget) t);
				}
			}
			
			tblViewer.setInput(tgs);
			tblViewer.setAllChecked(true);
			selectedItems.addAll(tgs);
			getWizard().getContainer().updateButtons();
		});
		
		setControl(main);
		
		setTitle(Messages.PlanTargetWizardPage_Title);
		setMessage(Messages.PlanTargetWizardPage_Message);
	}


	private Job loadValues = new Job("loading plans") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Plan> items;
			try(Session session = HibernateManager.openSession()){
				items = QueryFactory.buildQuery(session, Plan.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				
				for (Iterator<Plan> iterator = items.iterator(); iterator.hasNext();) {
					Plan plan = iterator.next();
					boolean ok = false;
					for(PlanTarget pt : plan.getTargets()) {
						if (pt instanceof SpatialPlanTarget) {
							ok = true;
							break;
						}
					}
					if (!ok) iterator.remove();
					
				}
				items.sort((a,b)->-1*a.getStartDate().compareTo(b.getStartDate()));
			}
			Display.getDefault().syncExec(()->{
				
				List<Object> all = new ArrayList<>();
				all.add(SELECTPLAN);
				all.addAll(items);
				cmbViewer.setInput(all);
				cmbViewer.refresh();
				cmbViewer.setSelection(new StructuredSelection(SELECTPLAN));
			});
			return Status.OK_STATUS;
		}
		
	};
}
