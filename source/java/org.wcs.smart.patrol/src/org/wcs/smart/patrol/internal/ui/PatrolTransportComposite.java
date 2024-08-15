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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
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
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.ui.EmployeeSelectorDialog;
import org.wcs.smart.ui.NamedIconItemLabelProvider;

/**
 *  Patrol item composite for selecting patrol transport type. 
 *  
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTransportComposite extends PatrolLegItemComposite{

	private TableViewer patrolTypeViewer = null;

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
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)center.getLayout()).marginWidth = 0;
		((GridLayout)center.getLayout()).marginHeight = 0;
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PatrolTransportComposite_TransportType_Lable);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		Composite table = new Composite(center, SWT.NONE);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setLayout(new TableColumnLayout());
		((GridData)table.getLayoutData()).heightHint = 100;
		
		patrolTypeViewer = new TableViewer(table, SWT.BORDER | SWT.SINGLE);
		patrolTypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeViewer.setLabelProvider(new NamedIconItemLabelProvider(IconManager.Size.SMALL));
		
		patrolTypeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeListeners();	
			}
		});
		((TableColumnLayout)table.getLayout()).setColumnData(
				new TableColumn(patrolTypeViewer.getTable(), SWT.NONE),
	            new ColumnWeightData(100));
		
		
		return center;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(PatrolLeg patrolLeg, Session session) {
		List<PatrolTransportType> types = PatrolHibernateManager.getActivePatrolTransporationTypes(patrolLeg.getPatrol().getConservationArea(), session);
		
		types.forEach(m->{
			if (m.getIcon() != null) {
				m.getIcon().getFiles().forEach(f->{
					f.computeFileLocation(session); 
					f.getIconSet().isDefault();
				});
			}
		});
		
		Collections.sort(types, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
		patrolTypeViewer.setInput(types);
		
		PatrolTransportType selection = null;
		if (types.size() > 0){
			selection = types.get(0);
		}

		if (patrolLeg.getType() != null){
			selection = patrolLeg.getType();
		}
		if (selection != null) {
			patrolTypeViewer.setSelection(new StructuredSelection(selection));
			patrolTypeViewer.reveal(selection);
		}
		patrolTypeViewer.getControl().getParent().layout(true, true);

	}

	/**
	 * 
	 * @return selected transport type
	 */
	public PatrolTransportType getSelectedTransportType(){
		PatrolTransportType pm = (PatrolTransportType) ((IStructuredSelection)patrolTypeViewer.getSelection()).getFirstElement();
		return pm;
	}
	
	@Override
	public boolean updatePatrol(Patrol p, Session session) {
		boolean ok = super.updatePatrol(p, session);
		p.recalculateType(session);
		return ok;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(PatrolLeg patrolLeg) {
		PatrolTransportType pm = getSelectedTransportType();
		if (pm != null){
			patrolLeg.setType(pm);
			
			
			//for edits only
			if (patrolLeg.getUuid() != null ){
				if (pm.getPatrolType().getRequiresPilot()){
					//prompt for pilot
					boolean hasPilot = false;
					for (PatrolLegMember member : patrolLeg.getMembers()){
						if (member.getIsPilot()){
							hasPilot = true;
							break;
						}
					}
					if (!hasPilot){
						//as for pilot
						EmployeeSelectorDialog dialog = new EmployeeSelectorDialog(patrolTypeViewer.getControl().getShell(), Messages.PatrolTransportComposite_PilotLabel, 
								MessageFormat.format(Messages.PatrolTransportComposite_PilotRequired, pm.getName()), EmployeeSelectorDialog.Type.PILOT,patrolLeg);
						if (dialog.open() != Window.OK) return false;  //not pilot selected
					}
				}
			}
			if (!pm.getPatrolType().getRequiresPilot() && patrolLeg.getMembers() != null){
				//remove all pilots
				for (PatrolLegMember member : patrolLeg.getMembers()){
					member.setIsPilot(false);
				}
			}
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