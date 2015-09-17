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
package org.wcs.smart.patrol.internal.ui.editor;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.util.SharedUtils;

/**
 * Dialog to select a patrol leg day.
 * <p>
 * This is used for moving waypoints from 
 * one leg day to another leg day.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MoveWaypointDialog extends TitleAreaDialog{

	private Patrol patrol;

	private List<PatrolLegDay> selectedPatrolLegDays;
	private PatrolLegDay moveTo;
	
	private ComboViewer cmbLegPart;
	
	/**
	 * @param parentShell
	 * @param p the patrol with associated leg days
	 */
	public MoveWaypointDialog(Shell parentShell, Patrol p) {
		super(parentShell);
		
		this.patrol = p;
	}

	/**
	 * @return the patrol leg day picked by the user
	 */
	public PatrolLegDay getMoveToPosition(){
		return this.moveTo;
	}
	
	@Override
	protected void okPressed() {
		if (selectedPatrolLegDays.size() == 1){
			moveTo = selectedPatrolLegDays.get(0);
		}else{
			moveTo = (PatrolLegDay) ((IStructuredSelection)cmbLegPart.getSelection()).getFirstElement();
		}
		super.okPressed();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));
		
		final Composite legtype = new Composite(parent, SWT.NONE);
		legtype.setLayout(new GridLayout(2, false));
		legtype.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(legtype, SWT.NONE);
		lbl.setText(Messages.MoveWaypointDialog_MoveTo_Label );
		
		final Combo dtCombo = new Combo(legtype, SWT.DROP_DOWN | SWT.READ_ONLY);

		final HashMap<Date, List<PatrolLegDay>> sets = new HashMap<Date, List<PatrolLegDay>>();
		for (PatrolLeg leg : this.patrol.getLegs()){
			for (PatrolLegDay day : leg.getPatrolLegDays()){
				Date tmp = SharedUtils.getDatePart(day.getDate(), false);
				List<PatrolLegDay> plds = sets.get(tmp);
				if (plds == null){
					plds = new ArrayList<PatrolLegDay>();
					sets.put(tmp, plds);
				}
				plds.add(day);
			}
		}
		ArrayList<Date> dates = new ArrayList<Date>();
		dates.addAll(sets.keySet());
		Collections.sort(dates,new Comparator<Date>() {

			@Override
			public int compare(Date o1, Date o2) {
				if (o1.before(o2)){
					return -1;
				}else if (o1.after(o2)){
					return 1;
				}
				return 0;
			}
		});
		for (Date dt : dates){
			dtCombo.add(DateFormat.getDateInstance(DateFormat.MEDIUM).format(dt));
		}
		dtCombo.select(0);
		selectedPatrolLegDays = sets.get(dates.get(0));
		
		final Composite legPart = new Composite(legtype, SWT.NONE);
		legPart.setLayout(new GridLayout(2, false));
		legPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		((GridLayout)legPart.getLayout()).marginWidth = 0;
		((GridLayout)legPart.getLayout()).marginHeight = 0;
		
		lbl = new Label(legPart, SWT.NONE);
		lbl.setText(Messages.MoveWaypointDialog_PatrolLeg_Label );
		
		cmbLegPart = new ComboViewer(legPart, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbLegPart.setContentProvider(ArrayContentProvider.getInstance());;
		cmbLegPart.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				PatrolLegDay pld = (PatrolLegDay)element;
				return pld.getPatrolLeg().getId();
			}
		});
		legPart.setVisible(false);
		
		dtCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String dt = dtCombo.getItem(dtCombo.getSelectionIndex());
				Date dd = null;
				try{
					dd=DateFormat.getDateInstance(DateFormat.MEDIUM).parse(dt);
				}catch(ParseException ex){}
				
				selectedPatrolLegDays = sets.get(dd);
				Object[] ll = selectedPatrolLegDays.toArray();
				cmbLegPart.setInput(ll);
				cmbLegPart.setSelection(new StructuredSelection(ll[0]));
				if (selectedPatrolLegDays.size() == 1){
					legPart.setVisible(false);
				}else{
					legPart.setVisible(true);
				}
				legtype.layout();
			}
		});
		
		Object[] ll = selectedPatrolLegDays.toArray();
		cmbLegPart.setInput(ll);
		cmbLegPart.setSelection(new StructuredSelection(ll[0]));
		if (selectedPatrolLegDays.size() == 1){
			legPart.setVisible(false);
		}else{
			legPart.setVisible(true);
		}
		
		setMessage(Messages.MoveWaypointDialog_DialogMessage);
		
		getShell().setText(Messages.MoveWaypointDialog_DialogTitle);
		return parent;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control ctr = super.createContents(parent);
		return ctr;
	}

	
}
