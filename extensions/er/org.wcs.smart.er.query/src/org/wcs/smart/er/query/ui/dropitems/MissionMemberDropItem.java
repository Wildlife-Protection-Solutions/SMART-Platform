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
package org.wcs.smart.er.query.ui.dropitems;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.ui.EmployeeLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Mission member/leader drop item
 * @author Emily
 *
 */
public class MissionMemberDropItem extends DropItem implements IFilterDropItem {

	private Label lblAttribute;
	private ComboViewer value;
	
	private Employee currentValue = null;
	private boolean isLeader = false;
	
	private boolean fireEvents = true;

	/*
	 * job to load all patrol ids
	 */
	private Job loadEmployees = new Job(Messages.MissionMemberDropItem_LoadingEmployeeJob) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (value.getControl().isDisposed()) {
				return Status.OK_STATUS;
			}
			
			final Employee lCurrentValue = currentValue;
			
			final List<Employee> data = new ArrayList<Employee>();
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try {
				List<Employee> employees = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), s);
				data.addAll(employees);
				Collections.sort(data, new Comparator<Employee>() {
					@Override
					public int compare(Employee e0, Employee e1) {
						return Collator.getInstance().compare(e0.getFullLabel().toUpperCase(), e1.getFullLabel().toUpperCase());
					}
				});
				s.getTransaction().rollback();
			} finally {
				s.close();
			}
			
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (value.getControl().isDisposed()) {
						return;
					}
					fireEvents = false;
					try {
						value.setInput(data);
						if (lCurrentValue != null) {
							value.setSelection(new StructuredSelection(lCurrentValue));
							currentValue = lCurrentValue;
						}
						getTargetPanel().redraw();
					} finally {
						fireEvents = true;
					}
				}
			});
			return Status.OK_STATUS;
		}
	};

	/**
	 * Creates a new mission member drop item
	 */
	public MissionMemberDropItem() {
		this(false);
	}
	
	/**
	 * Creates a new mission member drop item
	 */
	public MissionMemberDropItem(boolean isLeader) {
		this.isLeader = isLeader;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (isLeader){
			return Messages.MissionMemberDropItem_LeaderText;
		}else{
			return Messages.MissionMemberDropItem_MemberText;
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		Employee e = currentValue;
		if (currentValue == null){
			IStructuredSelection sel = (IStructuredSelection) value.getSelection();
			if (sel != null && !sel.isEmpty()){
				if (sel.getFirstElement() instanceof Employee){
					e = (Employee) sel.getFirstElement();
				}
			}
		}
		
		String key = ""; //$NON-NLS-1$
		if (isLeader){
			key = "s:missionleader:"; //$NON-NLS-1$
		}else{
			key = "s:missionmember:"; //$NON-NLS-1$
		}
		if (e != null){
			key += SmartUtils.encodeHex(e.getUuid());
		}
		return key;
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

		lblAttribute = new Label(main, SWT.NONE);
	
		value = new ComboViewer(main, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		value.setContentProvider(ArrayContentProvider.getInstance());
		value.setLabelProvider(EmployeeLabelProvider.getInstance());
		
		value.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fireEvents) return;
				
				Object x = ((StructuredSelection)value.getSelection()).getFirstElement();
				
				if (currentValue != null
						&& currentValue.equals(x)) {
					// ignore; not changed
					return;
				}
				queryChanged();
				currentValue = (Employee)x;
			}
		});
		
		initDrag(main);
		initDrag(lblAttribute);

		if (isLeader){
			lblAttribute.setText(formatStringForLabel(Messages.MissionMemberDropItem_LeaderLabel));
		}else{
			lblAttribute.setText(formatStringForLabel(Messages.MissionMemberDropItem_MemberLabel));
		}
		
		loadEmployees.schedule();
	}

	/**
	 * 
	 * @param data the employee
	 */
	@Override
	public void initializeData(Object data) {
		this.currentValue = (Employee)data;
	}

}
