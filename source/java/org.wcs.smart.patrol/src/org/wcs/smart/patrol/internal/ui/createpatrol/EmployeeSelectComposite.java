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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite to select employees.  This composite
 * consists of two list: a list of employees to select from
 * and a list of employees selected.  Users
 * move items from one list to the other.
 * 
 * @author Emily
 * @author elitvin
 * @since 1.0.0
 */
public class EmployeeSelectComposite extends MultipleSelectComposite<Employee> {

	private ComboViewer cmbTeams;
	private List<Employee> nonFilteredItems = null;
	
	public EmployeeSelectComposite(Composite parent, int style) {
		super(parent, style);
		setLabelProvider(new EmployeeLabelProvider());
		setItemComparator(new Comparator<Employee>() {
			@Override
			public int compare(Employee e1, Employee e2) {
				return Collator.getInstance().compare(SmartLabelProvider.getFullLabel(e1), SmartLabelProvider.getFullLabel(e2));
			}
		});
		setLabelAllText(Messages.EmployeeSelectComposite_AddEmployee_Label);
		setLabelSelectedText(Messages.EmployeeSelectComposite_SelectedEmployees);
		
		((GridData)selectedItemsListViewer.getControl().getParent().getLayoutData()).minimumWidth = 200;
	}

	protected void contributeToFromLabelSection(Composite parent) {
		cmbTeams = new ComboViewer(new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER));
		cmbTeams.getControl().setToolTipText(Messages.EmployeeSelectComposite_filtertooltip);
		cmbTeams.setContentProvider(ArrayContentProvider.getInstance());
		cmbTeams.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
				return super.getText(element);
			}
		});
		cmbTeams.getControl().setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		((GridData)cmbTeams.getControl().getLayoutData()).widthHint = 150;
		cmbTeams.setInput(Collections.singletonList(DialogConstants.LOADING_TEXT));
		// nothing by default
		
		Job j = new Job(Messages.EmployeeSelectComposite_teamsjob) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> teams = new ArrayList<>();
				teams.add(Messages.EmployeeSelectComposite_AllEmployees);
				try(Session session = HibernateManager.openSession()){
					List<EmployeeTeam> items = QueryFactory.buildQuery(session, EmployeeTeam.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
					items.forEach(i->i.getMembers().forEach(m->m.getEmployee().getGivenName()));
					teams.addAll(items);
				}
				Display.getDefault().syncExec(()->{
					cmbTeams.setInput(teams);
					cmbTeams.setSelection(new StructuredSelection(teams.get(0)));
					cmbTeams.getControl().getParent().getParent().layout(true);
				});
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		
		cmbTeams.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (nonFilteredItems == null) nonFilteredItems = EmployeeSelectComposite.this.allItems;
				
				Object x = cmbTeams.getStructuredSelection().getFirstElement();
				if (x instanceof EmployeeTeam) {
					List<Employee> choices = ((EmployeeTeam)x).getMembers().stream().map(e->e.getEmployee()).collect(Collectors.toList());
					EmployeeSelectComposite.this.setItemsData(choices, getSelectedItemsAsList());
				}else {
					//all members
					if (nonFilteredItems != null) EmployeeSelectComposite.this.setItemsData(nonFilteredItems, getSelectedItemsAsList());
				}
			}
		});
	}

	protected void createButtonComposite(Composite btnComposite) {
		Button btnAddAll = new Button(btnComposite, SWT.PUSH);
		btnAddAll.setText(Messages.EmployeeSelectComposite_AddAllButton);
		btnAddAll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnAddAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				List<Employee> items = getSelectedItemsAsList();
				EmployeeSelectComposite.this.allItems.forEach(next->items.add(next));
				 EmployeeSelectComposite.this.setItemsData(EmployeeSelectComposite.this.allItems, items);
			}

		});
		
		super.createButtonComposite(btnComposite);
	}
}
