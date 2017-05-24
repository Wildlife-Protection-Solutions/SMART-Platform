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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolMandate;

/**
 * Patrol item composite for selecting patrol mandate. 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolMandateComposite extends PatrolLegItemComposite{

	private ComboViewer patrolMandateViewer = null;

	
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
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolMandateComposite_Mandate_Label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		patrolMandateViewer = new ComboViewer(center, SWT.READ_ONLY);
		patrolMandateViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolMandateViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolMandateViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolMandate){
					return ((PatrolMandate)element).getName();
				}
				return super.getText(element);
			}
		});
		patrolMandateViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		
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
		if (mandates.size() > 0){
			patrolMandateViewer.setSelection(new StructuredSelection(mandates.get(0)));
		}

		if (leg.getMandate() == null){
			if (leg.getPatrol().getTeam() != null && leg.getPatrol().getTeam().getMandate() != null){
	    		patrolMandateViewer.setSelection(new StructuredSelection(leg.getPatrol().getTeam().getMandate()));
	    	}	
		}else{
			patrolMandateViewer.setSelection(new StructuredSelection(leg.getMandate()));
		}

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