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
import org.wcs.smart.patrol.model.PatrolTransportType;

/**
 *  Patrol item composite for selecting patrol transport type. 
 *  
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTransportComposite extends PatrolLegItemComposite{

	private ComboViewer patrolTypeViewer = null;

	/**
	 * 
	 */
	public PatrolTransportComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolTransportComposite_TransportType_Lable);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		patrolTypeViewer = new ComboViewer(center, SWT.READ_ONLY);
		patrolTypeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolTypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					return ((PatrolTransportType)element).getName();
				}
				return super.getText(element);
			}
		});
		patrolTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
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
	public void setValues(PatrolLeg patrolLeg, Session session) {
		List<PatrolTransportType> types = PatrolHibernateManager.getActivePatrolTransporationTypes(patrolLeg.getPatrol().getConservationArea(), session);
		Collections.sort(types, new Comparator<PatrolTransportType>(){
			@Override
			public int compare(PatrolTransportType o1, PatrolTransportType o2) {
				return Collator.getInstance().compare(o1.getName(), o2.getName());
		}});
		patrolTypeViewer.setInput(types.toArray());
		if (types.size() > 0){
			patrolTypeViewer.setSelection(new StructuredSelection(types.get(0)));
		}

		if (patrolLeg.getType() != null){
			patrolTypeViewer.setSelection(new StructuredSelection(patrolLeg.getType()));	
		}

	}

	/**
	 * 
	 * @return selected transport type
	 */
	public PatrolTransportType getSelectedTransportType(){
		PatrolTransportType pm = (PatrolTransportType) ((IStructuredSelection)patrolTypeViewer.getSelection()).getFirstElement();
		return pm;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(PatrolLeg patrolLeg) {
		PatrolTransportType pm = getSelectedTransportType();
		if (pm != null){
			patrolLeg.setType(pm);
			patrolLeg.getPatrol().recalculateType();
			return true;
		}else{
			SmartPatrolPlugIn.displayLog(Messages.PatrolTransportComposite_Error_NoTransportType, null);
			return false;
		}
		
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.PatrolTransportComposite_Title;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}	
}