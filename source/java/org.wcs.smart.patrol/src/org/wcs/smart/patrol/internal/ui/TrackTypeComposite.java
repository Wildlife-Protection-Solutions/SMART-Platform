/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.ui.NamedIconItemLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 *  Patrol item composite for selecting patrol transport type. 
 *  
 * @author Emily
 * @since 8.1.0
 */
public class TrackTypeComposite extends PatrolLegItemComposite{

	private TableViewer tblTrackType = null;

	/**
	 * 
	 */
	public TrackTypeComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

//		Composite center = new Composite(parent, SWT.NONE);
//		center.setLayout(new GridLayout());
//		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		((GridLayout)center.getLayout()).marginWidth = 0;
//		((GridLayout)center.getLayout()).marginHeight = 0;
//		
//		Label lbl = new Label(center, SWT.NONE);
//		lbl.setText(Messages.PatrolTransportComposite_TransportType_Lable);
//		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
//		
		Composite table = new Composite(parent, SWT.NONE);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLayout(new TableColumnLayout());
		((GridData)table.getLayoutData()).heightHint = 100;
		
		tblTrackType = new TableViewer(table,SWT.SINGLE);
		tblTrackType.setContentProvider(ArrayContentProvider.getInstance());
		tblTrackType.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.SMALL));
		
		tblTrackType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		((TableColumnLayout)table.getLayout()).setColumnData(
				new TableColumn(tblTrackType.getTable(), SWT.NONE),
	            new ColumnWeightData(100));
		
		
		return table;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(PatrolLeg patrolLeg, Session session) {
		
		List<PatrolType> types = PatrolHibernateManager.getActivePatrolTypes(patrolLeg.getPatrol().getConservationArea(), session);
		
		
		types.forEach(m->{
			if (m.getIcon() != null) {
				m.getIcon().getFiles().forEach(f->{
					f.computeFileLocation(session); 
					f.getIconSet().isDefault();
				});
			}
		});
		
		Collections.sort(types, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		tblTrackType.setInput(types);
		
		PatrolType selection = null;
		if (types.size() > 0){
			selection = types.get(0);
		}

		if (patrolLeg.getPatrol().getPatrolType() != null){
			selection = patrolLeg.getPatrol().getPatrolType();
		}
		if (selection != null) {
			tblTrackType.setSelection(new StructuredSelection(selection));
			tblTrackType.reveal(selection);
		}
		tblTrackType.getControl().getParent().layout(true, true);

	}

	/**
	 * 
	 * @return selected transport type
	 */
	public PatrolType getSelectedPatrolType(){
		return (PatrolType) ((IStructuredSelection)tblTrackType.getSelection()).getFirstElement();		
	}
	
	@Override
	public boolean updatePatrol(Patrol p, Session session) {		
		return super.updatePatrol(p, session);
	}
	
	@Override
	public boolean updatePatrol(PatrolLeg p) {
		p.getPatrol().setPatrolType(getSelectedPatrolType());
		for (PatrolLeg l : p.getPatrol().getLegs()) {
			
			
			if (l.getType() != null && !(l.getType().getPatrolType().equals(p.getPatrol().getPatrolType()))) {
			
				if (p.getPatrol().getUuid() != null) {
					MessageDialog.openError(tblTrackType.getControl().getShell(), 
							DialogConstants.ERROR_STRING,
							MessageFormat.format("{0} is not a valid transport type for track type {1}", l.getType().getName(), p.getPatrol().getPatrolType().getName()));
					return false;
				}else {
					l.setType(null);
				}
			}
		}
		return true;
	}	

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Track Type";
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}


}