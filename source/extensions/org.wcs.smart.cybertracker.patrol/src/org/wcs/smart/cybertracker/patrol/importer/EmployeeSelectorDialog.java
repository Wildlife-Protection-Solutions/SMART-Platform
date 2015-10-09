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
package org.wcs.smart.cybertracker.patrol.importer;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Dialog for selecting an employee for leader or pilot
 * for a given patrol leg.
 * 
 * @author Emily
 *
 */
public class EmployeeSelectorDialog extends TitleAreaDialog{
	
	public enum Type {
		LEADER(Messages.EmployeeSelectorDialog_LeaderType),
		PILOT(Messages.EmployeeSelectorDialog_PilotType);
	
		String name;
		private Type(String name){
			this.name = name;
		}
		
	
	};

	private String message;
	private String title;
	private Type type;
	private ComboViewer employeeViewer;
	private PatrolLeg patrol;
	
	public EmployeeSelectorDialog(Shell parentShell, String title, String message, Type type, PatrolLeg patrol) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.type = type;
		this.patrol = patrol;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		setTitle(title);
		getShell().setText(Messages.EmployeeSelectorDialog_DialogTitle);
		setMessage(message);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
        Label patrolLabel = new Label(main, SWT.NONE);
        patrolLabel.setText(type.name + ":"); //$NON-NLS-1$
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        employeeViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
        employeeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        employeeViewer.setContentProvider(ArrayContentProvider.getInstance());
        employeeViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof PatrolLegMember){
					return SmartLabelProvider.getShortLabel( ((PatrolLegMember) element).getMember() );
				}
				return super.getText(element);
			}
		});
        
        
        employeeViewer.setInput(patrol.getMembers());
        employeeViewer.setSelection(new StructuredSelection(patrol.getMembers().get(0)));
        
		return composite;
	}
	
	/**
	 * Notifies that the ok button of this dialog has been pressed.
	 * <p>
	 * The <code>Dialog</code> implementation of this framework method sets
	 * this dialog's return code to <code>Window.OK</code> and closes the
	 * dialog. Subclasses may override.
	 * </p>
	 */
	protected void okPressed() {
		for(PatrolLegMember member: patrol.getMembers()){
			if (type == Type.LEADER){
				member.setIsLeader(false);
			}else if (type == Type.PILOT){
				member.setIsPilot(false);
			}
		}
		if (type == Type.LEADER){
			((PatrolLegMember)((IStructuredSelection)employeeViewer.getSelection()).getFirstElement()).setIsLeader(true);
		}else if (type == Type.PILOT){
			((PatrolLegMember)((IStructuredSelection)employeeViewer.getSelection()).getFirstElement()).setIsPilot(true);
		}
		
		super.okPressed();
	}
}
