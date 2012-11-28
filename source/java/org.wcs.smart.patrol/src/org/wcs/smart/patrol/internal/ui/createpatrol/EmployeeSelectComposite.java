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



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Composite to select employees.  This composite
 * consists of two list: a list of employees to select from
 * and a list of employees selected.  Users
 * move items from one list to the other.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeSelectComposite extends Composite{

	
	private EmployeeLabelProvider employeeLabelProvider = new EmployeeLabelProvider();
	private WritableList selectedEmployees = new WritableList();
	private WritableList allEmployees = new WritableList();

	private TableViewer employeeListViewer;
	private TableViewer selectedEmployeeListViewer;
	
	private List<IListChanged> changeListeners = new ArrayList<IListChanged>();
	
	/**
	 * Creates new compliste 
	 */
	public EmployeeSelectComposite(Composite parent, int style) {
		super(parent, style);
		
		createControls();
	}
	
	/**
	 * Adds a listener that is fired when the list of selected
	 * employees changes.
	 * 
	 * @param listener listener to add
	 */
	public void addSelectionChangedListener(IListChanged listener){
		changeListeners.add(listener);
	}
	
	/**
	 * Removes listener added
	 * @param listener
	 */
	public void removeSelectionChangedListener(IListChanged listener){
		changeListeners.remove(listener);
	}
	
	/*
	 * Fires change listeners
	 */
	private void fireChangeListeners(){
		for (IListChanged listener : changeListeners){
			listener.listChanged(selectedEmployees);
		}
	}
	
	/*
	 * Creates the employee select composite
	 */
	private void createControls(){
		setLayout(new GridLayout(3, false));
		
		Label lbl = new Label(this, SWT.NONE);
		lbl.setText(Messages.EmployeeSelectComposite_AddEmployee_Label);
		
		new Label(this, SWT.NONE);
		
		lbl = new Label(this, SWT.NONE);
		lbl.setText(Messages.EmployeeSelectComposite_SelectedEmployees_Label);
		
		employeeListViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER);
		employeeListViewer.setContentProvider(new ObservableListContentProvider());
		employeeListViewer.setLabelProvider(employeeLabelProvider);
		employeeListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		employeeListViewer.setInput(allEmployees);
		
		Composite btnComposite = new Composite(this, SWT.NONE);
		btnComposite.setLayout(new GridLayout(1, false));
		btnComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Button btnAdd = new Button(btnComposite, SWT.PUSH);
		btnAdd.setText(Messages.EmployeeSelectComposite_Add_Button);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addEmployees();
			}
			
		});
		Button btnRemove = new Button(btnComposite, SWT.PUSH);
		btnRemove.setText(Messages.EmployeeSelectComposite_Remove_Button);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeEmployees();
			}
			
		});
		
		selectedEmployeeListViewer = new TableViewer(this, SWT.MULTI | SWT.BORDER);
		selectedEmployeeListViewer.setContentProvider(new ObservableListContentProvider());
		selectedEmployeeListViewer.setLabelProvider(employeeLabelProvider);
		selectedEmployeeListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectedEmployeeListViewer.setInput(selectedEmployees);
	}
	
	/**
	 * Moves employees from the list of all employees to the list of
	 * selected employees.
	 */
	private void addEmployees(){
		Iterator<?> items = ((IStructuredSelection)employeeListViewer.getSelection()).iterator();
		while(items.hasNext()){
			Employee next = (Employee)items.next();
			allEmployees.remove(next);
			selectedEmployees.add(next);
		}
		employeeListViewer.refresh();
		selectedEmployeeListViewer.refresh();
		
		fireChangeListeners();
	}
	/**
	 * Moves employees from the list of selected employees to the
	 * list of all employees.
	 */
	private void removeEmployees(){
		Iterator<?> items = ((IStructuredSelection)selectedEmployeeListViewer.getSelection()).iterator();
		while(items.hasNext()){
			Employee next = (Employee)items.next();
			allEmployees.add(next);
			selectedEmployees.remove(next);
		}
		employeeListViewer.refresh();
		selectedEmployeeListViewer.refresh();
		
		fireChangeListeners();
	}
	/**
	 * Initialized the employee lists
	 * @param allEmployees the list of employees to select from
	 * @param current the list employees selected by defaul
	 */
	public void setEmployeeData(List<Employee> allEmployees, List<Employee> current){
		selectedEmployees.clear();
		this.allEmployees.clear();
		
		this.allEmployees.addAll(allEmployees);
		this.allEmployees.removeAll(current);
		selectedEmployees.addAll(current);
		
		employeeListViewer.refresh();
		selectedEmployeeListViewer.refresh();
		
		fireChangeListeners();
	}

	public WritableList getSelectedEmployees(){
		return this.selectedEmployees;
	}
	
	/**
	 * Change listener fired when the list of
	 * selected employees changes
	 * 
	 */
	public interface IListChanged{
		/**
		 * Fired when the list of selected employees is changes
		 * @param newEmployees the new list of selected employees
		 */
		public void listChanged(List<Employee> newEmployees);
	}
}
