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
import java.util.Comparator;

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;

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
	}

}
