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
package org.wcs.smart.incident.ui.newwizard;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Incident wizard page.
 * 
 * @author Emily
 *
 */
public class ObserverWizardPage extends WizardPage {

	private NewIncidentWizard wizard;
	
	private ListViewer tblEmployees;
	private static final String NONE = ""; //$NON-NLS-1$

	public ObserverWizardPage(NewIncidentWizard wizard){
		super("OBSERVER_PAGE"); //$NON-NLS-1$
		this.wizard = wizard;
	}
	
	public boolean canFinish(){
		return false;
	}
	
	public boolean canFlipToNextPage(){
		return getErrorMessage() == null;
	}
	
	@Override
	public void createControl(Composite parent) {
		
		Composite item = new Composite(parent, SWT.NONE);
		item.setLayout(new GridLayout());
		item.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Label l = new Label(item, SWT.NONE);
		l.setText(Messages.ObserverWizardPage_ObserverField);
			
		tblEmployees = new ListViewer(item, SWT.BORDER | SWT.V_SCROLL);
		tblEmployees.setContentProvider(ArrayContentProvider.getInstance());
		tblEmployees.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblEmployees.getControl().getLayoutData()).heightHint = 200;
		tblEmployees.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element == NONE) return Messages.ObserverWizardPage_NoneUnknownOption;
				if (element instanceof Employee) return SmartLabelProvider.getFullLabel(((Employee)element));
				return super.getText(element);
			}
		});
			
		try(Session session = HibernateManager.openSession()){
			List<Employee> employees = QueryFactory.buildQuery(session, Employee.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"endEmploymentDate", null}).list(); //$NON-NLS-1$
			employees.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getFullLabel(((Employee)a)), SmartLabelProvider.getFullLabel(((Employee)b))));
			
			List<Object> all = new ArrayList<>(employees);
			all.add(0, NONE);
			tblEmployees.setInput(all);
			tblEmployees.setSelection(new StructuredSelection(NONE));			
		}
				
		setTitle(Messages.ObserverWizardPage_PageTitle);
		setMessage(Messages.ObserverWizardPage_PageMessage);
		
		super.setControl(item);
	}

	public String validate(){
		setErrorMessage(null);
		wizard.getContainer().updateButtons();
		return null;
	}
	
	public Employee getObserver() {
		Object x = tblEmployees.getStructuredSelection().getFirstElement();
		if (x != null && x instanceof Employee) return (Employee)x;
		return null;
	}
	
}
