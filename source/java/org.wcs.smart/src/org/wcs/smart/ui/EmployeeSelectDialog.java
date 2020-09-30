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
package org.wcs.smart.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Dialog for selecting employees
 * 
 * @author Emily
 *
 */
public class EmployeeSelectDialog extends SmartStyledDialog{

	private List<Employee> employees;
	private CheckboxTableViewer tblViewer;
	private List<Employee> initEmployees;
	
	/**
	 * Creates a new dialog where employee can be any
	 * active employee for the Conservation Area.
	 * @param parent
	 */
	public EmployeeSelectDialog(Shell parent) {
		super(parent);
	}
	
	/**
	 * Creates a new dialog where employees are selected
	 * from the list of employees.
	 * @param parent
	 * @param employees
	 */
	public EmployeeSelectDialog(Shell parent, List<Employee> employees) {
		super(parent);
		this.initEmployees = employees;
	}
	
	
	/**
	 * 
	 * @return the list of selected employees 
	 */
	public List<Employee> getSelectedEmployees(){
		return this.employees;
	}
	
	@Override
	public void okPressed() {
		employees = new ArrayList<>();
		for (Object o : tblViewer.getCheckedElements()) {
			if (o instanceof Employee) employees.add((Employee)o);
		}
		super.okPressed();
	}
	
	@Override
	public Point getInitialSize() {
		return new Point(350, 400);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		
		tblViewer = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		tblViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Employee) return SmartLabelProvider.getFullLabel((Employee)element);
				return super.getText(element);
			}
		});
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblViewer.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(tblViewer));
		
		if (initEmployees == null) {
			Job load = new Job(Messages.EmployeePropertyPage_loadingjob2) {
	
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<Employee> items = new ArrayList<>();
					try(Session session = HibernateManager.openSession()){
						items.addAll(HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), session));
					}
					items.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getFullLabel((Employee)a), SmartLabelProvider.getFullLabel((Employee)b)));
					Display.getDefault().syncExec(()->{
						tblViewer.setInput(items);
					});
					return Status.OK_STATUS;
				}
				
			};
			load.schedule();
		}else {
			tblViewer.setInput(initEmployees);
		}
		getShell().setText(Messages.EmployeePropertyPage_shelltext);
		return main;
	}
	
}
