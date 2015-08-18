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
package org.wcs.smart.query.ui.model.impl;

import java.text.Collator;
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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Observer drop item.
 * 
 * @author Emily
 *
 */
public class ObserverDropItem extends DropItem implements IFilterDropItem{

	public static final String KEY_PART = "wpnobs:observer"; //$NON-NLS-1$
	
	private ComboViewer listViewer;
	
	private Font smallerFont;
	
	private Employee currentSelection = null;
	
	private Job loadEmployees = new Job(Messages.ObserverDropItem_loadingEmployeeJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			List<Employee> es = null;
			try{
				es = HibernateManager.getActiveEmployees(SmartDB.getCurrentConservationArea(), s);
				Collections.sort(es, new Comparator<Employee>(){
					@Override
					public int compare(Employee o1, Employee o2) {
						return Collator.getInstance().compare(getLabel(o1), getLabel(o2));
					}});
				for (Employee e: es){
					e.getUuid();
					getLabel(e);
				}
			}finally{
				s.close();
			}
			final List<Employee> fes = es; 
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.getControl().isDisposed()) return;
					listViewer.setInput(fes);
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
		}
		
	};
	
	/**
	 * Creates observer drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public ObserverDropItem() {
		super();
	}

	
	/**
	 * @param data - an array of the {Operator, IWaypointSource} 
	 */
	public void initializeData(Object data){
		currentSelection = (Employee)data;
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		listViewer = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return KEY_PART + " = " + listViewer.getCombo().getText(); //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder query = new StringBuilder(KEY_PART);
		query.append(" equals "); //$NON-NLS-1$
		
		Employee it = null;
		if (currentSelection != null){
			it = currentSelection;
		}else{
			IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
			if (sel != null && !sel.isEmpty()){
				it = (Employee) sel.getFirstElement();
			}
		}
		if (it != null){
			query.append("\"" + UuidUtils.uuidToString(it.getUuid()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return query.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ObserverDropItem_ObserverLabel + " " + Operator.EQUALS.getGuiValue());   //$NON-NLS-1$
		
		/* -- list viewer **/
		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Employee){
					return getLabel((Employee)element);
				}
				return super.getText(element);
			}
		});
		
		if (currentSelection != null){
			listViewer.setSelection(new StructuredSelection(currentSelection));
		}
		
		/* -- events --*/
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Employee newSelection = (Employee) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				if (! (currentSelection != null && currentSelection.equals(newSelection))){
					currentSelection = newSelection;
					queryChanged();	
				}			
				
			}
		});
		
		initDrag(main);
		initDrag(l);
		
		loadEmployees.schedule();
	}
	
	protected String getLabel(Employee e){
		return SmartLabelProvider.getFullLabel(e);
	}
}
