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
package org.wcs.smart.cybertracker.incident;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Cybertracker package contribution for configuring an incident model
 * for the package.
 * 
 * @author Emily
 *
 */
public class IncidentPackageUiContribution implements IPackageUiContribution{

	//data model 
	private static final String DATAMODEL = "DATAMODEL"; //$NON-NLS-1$
	

	private Button btnCollect ;
	private ComboViewer cmbModel;
	
	private ICtPackage ctPackage;
	private boolean isInit = false;
	
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctPackage, Listener onValidate) {

		Group incidentComposite = new Group(parent, SWT.NONE);
		incidentComposite.setLayout(new GridLayout());
		incidentComposite.setText(Messages.IncidentPackageContribution_ConfigurationGroupLablel);
		
		btnCollect = new Button(incidentComposite, SWT.CHECK);
		btnCollect.setText(Messages.IncidentPackageContribution_CollectIncidentsOp);
		
		Composite modelComp = new Composite(incidentComposite, SWT.NONE);
		modelComp.setLayout(new GridLayout(2, false));
		((GridLayout)modelComp.getLayout()).marginWidth = 0;
		((GridLayout)modelComp.getLayout()).marginHeight = 0;
		modelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)modelComp.getLayoutData()).horizontalIndent = 20;

		Label l = new Label(modelComp, SWT.NONE);
		l.setText(Messages.IncidentPackageContribution_CmModelLabel);
		
		cmbModel = new ComboViewer(modelComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbModel.setContentProvider(ArrayContentProvider.getInstance());
		cmbModel.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof ConfigurableModel) return ((ConfigurableModel) element).getName();
				if (element == DATAMODEL) return Messages.IncidentPackageContribution_OriginalDmLabel;
				return super.getText(element);
			}
		});
		cmbModel.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbModel.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		
		l.setEnabled(false);
		cmbModel.getControl().setEnabled(false);
		
		btnCollect.addListener(SWT.Selection, e->{
			l.setEnabled(btnCollect.getSelection());
			cmbModel.getControl().setEnabled(btnCollect.getSelection());
		});
		
		boolean isSelected = false;
		btnCollect.setSelection(isSelected);
		l.setEnabled(btnCollect.getSelection());
		cmbModel.getControl().setEnabled(btnCollect.getSelection());
			
		this.ctPackage = ctPackage;
		if (ctPackage != null && ctPackage instanceof AbstractCtPackage) {
			if (((AbstractCtPackage)ctPackage).getHasIncident()){
				btnCollect.setSelection(true);
				cmbModel.getControl().setEnabled(true);
				l.setEnabled(true);
			}else {
				btnCollect.setSelection(false);
				cmbModel.getControl().setEnabled(false);
				l.setEnabled(false);
			}
		}
		loadCm.schedule();
		
		cmbModel.addSelectionChangedListener(e->{
			if (!isInit) onValidate.handleEvent(new Event());		
		});
		btnCollect.addListener(SWT.Selection, e->{if (!isInit) onValidate.handleEvent(e);});
		return incidentComposite;
	}

	@Override
	public String isValid() {
		if (this.btnCollect.getSelection()) {
			if (cmbModel.getSelection().isEmpty()) {
				return "A configurable model must be selected.";
			}else {
				Object x = cmbModel.getStructuredSelection().getFirstElement();
				if (!(x instanceof ConfigurableModel || x == DATAMODEL)) {
					return "A valid configurable model must be selected.";
					
				}
			}
		}
		return null;
	}
	
	@Override
	public void updatePackage(ICtPackage ctpackage) {
		if (ctpackage instanceof AbstractCtPackage) {
			if (!btnCollect.getSelection()) {
				((AbstractCtPackage) ctpackage).setHasIncident(false);
				((AbstractCtPackage) ctpackage).setIncidentModel(null);	
			}else {
				Object selection = cmbModel.getStructuredSelection().getFirstElement();
				if (selection instanceof ConfigurableModel) {
					((AbstractCtPackage) ctpackage).setHasIncident(true);
					((AbstractCtPackage) ctpackage).setIncidentModel((ConfigurableModel) selection);
				}else if (selection == DATAMODEL){
					//data model
					((AbstractCtPackage) ctpackage).setHasIncident(true);
					((AbstractCtPackage) ctpackage).setIncidentModel(null);
				}else {
					((AbstractCtPackage) ctpackage).setHasIncident(false);
					((AbstractCtPackage) ctpackage).setIncidentModel(null);
				}
			}
		}
	}
	

	
	private Job loadCm = new Job(Messages.IncidentPackageContribution_LoadingCmJobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				isInit = true;
				
				List<Object> items = new ArrayList<>();
				ConfigurableModel selection = null;
				UUID selectionUuid =  null;
				String cmUuid = "";
				if (ctPackage != null && ctPackage instanceof AbstractCtPackage) {
					AbstractCtPackage ct = (AbstractCtPackage)ctPackage;
					if (ct.getHasIncident()) {
						if (ct.getIncidentModel() != null) {
							selectionUuid = ct.getIncidentModel().getUuid();
						}else {
							cmUuid = DATAMODEL;
						}
					}
				}
				
				try(Session session = HibernateManager.openSession()){
					items.addAll(
							QueryFactory.buildQuery(session,  ConfigurableModel.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
					for (Object i : items) {
						if (((ConfigurableModel)i).getUuid().equals(selectionUuid)) {
							selection = (ConfigurableModel) i;
						}
					}
				}
				items.add(DATAMODEL);
				
				final ConfigurableModel fselection = selection;
				final String fcmUuid = cmUuid;
				Display.getDefault().syncExec(()->{
					cmbModel.setInput(items);
					
					if (fcmUuid.equals(DATAMODEL)) {
						cmbModel.setSelection(new StructuredSelection(DATAMODEL));
					}else if (fselection == null) {
						cmbModel.setSelection(new StructuredSelection(items.get(0)));
					}else {
						cmbModel.setSelection(new StructuredSelection(fselection));
					}
				});
				
				return Status.OK_STATUS;
			}finally {
				isInit = false;
			}
		} 
		
	};
	
}
