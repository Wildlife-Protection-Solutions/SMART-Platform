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
package org.wcs.smart.patrol.internal.ui;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.ui.NamedIconItemLabelProvider;


/**
 * Patrol item composite for selecting patrol mandate. 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolMandateComposite extends PatrolLegItemComposite{

	private TableViewer patrolMandateViewer = null;

	
	/**
	 * 
	 */
	public PatrolMandateComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)center.getLayout()).marginWidth = 0;
		((GridLayout)center.getLayout()).marginHeight = 0;
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolMandateComposite_Mandate_Label);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite table = new Composite(center, SWT.NONE);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLayout(new TableColumnLayout());
		((GridData)table.getLayoutData()).heightHint = 100;

		patrolMandateViewer = new TableViewer(table, SWT.BORDER | SWT.SINGLE);
		patrolMandateViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolMandateViewer.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.SMALL));

		patrolMandateViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		((TableColumnLayout)table.getLayout()).setColumnData(
				new TableColumn(patrolMandateViewer.getTable(), SWT.NONE),
	            new ColumnWeightData(100));
		
		
		return center;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(PatrolLeg leg, Session session) {
		session.beginTransaction();
		List<PatrolMandate> mandates = null;
		try{
			mandates = PatrolHibernateManager.getActiveMandates(leg.getPatrol().getConservationArea(), session);
			mandates.forEach(m->{
				if (m.getIcon() != null) {
					m.getIcon().getFiles().forEach(f->{
						f.computeFileLocation(session); 
						f.getIconSet().isDefault();
					});
				}
			});
			session.getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolMandateComposite_Error_LoadingMandates, ex);
			session.getTransaction().rollback();
			session.close();
			return;
		}

		Collections.sort(mandates, new Comparator<PatrolMandate>(){
			@Override
			public int compare(PatrolMandate o1, PatrolMandate o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
		}});
		patrolMandateViewer.setInput(mandates.toArray());
		
		
		PatrolMandate selection = null;
		if (mandates.size() > 0){
			selection = mandates.get(0);
		}

		if (leg.getMandate() == null){
			if (leg.getPatrol().getTeam() != null && leg.getPatrol().getTeam().getMandate() != null){
				selection = leg.getPatrol().getTeam().getMandate();
	    	}	
		}else{
			selection = leg.getMandate();
		}
		if (selection != null) {
			patrolMandateViewer.setSelection(new StructuredSelection(selection));
			patrolMandateViewer.reveal(selection);
		}
		patrolMandateViewer.getControl().getParent().layout();
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(PatrolLeg leg) {
		PatrolMandate pm = (PatrolMandate) ((IStructuredSelection)patrolMandateViewer.getSelection()).getFirstElement();
		if (pm != null){
			leg.setMandate(pm);
		}else{
			leg.setMandate(null);
		}
		return true;
		
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.PatrolMandateComposite_Title;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_MANDATE;
	}
}