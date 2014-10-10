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
package org.wcs.smart.er.ui.mision.editor;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionDay;

/**
 * Dialog to select a day to move waypoints to.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MoveWaypointDialog extends TitleAreaDialog {

	private MissionDay missionDay;

	private MissionDay moveTo;
	
	public MoveWaypointDialog(Shell parentShell, MissionDay missionDay) {
		super(parentShell);
		this.missionDay = missionDay;
	}

	/**
	 * @return the day picked by the user
	 */
	public MissionDay getMoveToDate(){
		return this.moveTo;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	private List<MissionDay> createDates() {
		ArrayList<MissionDay> days = new ArrayList<MissionDay>();
		days.addAll(missionDay.getMission().getMissionDays());
		days.remove(missionDay);
		return days;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite legtype = new Composite(parent, SWT.NONE);
		legtype.setLayout(new GridLayout(2, false));
		legtype.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label lbl = new Label(legtype, SWT.NONE);
		lbl.setText(Messages.MoveWaypointDialog_MoveToLabel);
		
		final ComboViewer cmbViewer = new ComboViewer(legtype, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return DateFormat.getDateInstance().format(((MissionDay)element).getDate());
			}
		});
		List<MissionDay> days = createDates();
		cmbViewer.setInput(days);
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				moveTo = null;
				if (!cmbViewer.getSelection().isEmpty()){
					moveTo = (MissionDay) ((StructuredSelection)cmbViewer.getSelection()).getFirstElement();
				}
			}
		});
		cmbViewer.setSelection(new StructuredSelection(days.get(0)));
		
		setTitle(Messages.MoveWaypointDialog_Title);
		setMessage(Messages.MoveWaypointDialog_Description);
		
		getShell().setText(Messages.MoveWaypointDialog_Title);
		return parent;
	}
	
}