/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelEntityRelationshipAttributeValue;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * Relationship attribute dialog.
 * 
 * @author Emily
 *
 */
public class RelationshipAttributeDialog  extends TitleAreaDialog {
	
	private IntelEntityRelationship relationship;
	
	private HashMap<AttributeFieldEditor, IntelEntityRelationshipAttributeValue> fields;
	private IntelRelationshipType type;
	
	private Composite core;
	private boolean modified = false;
	
	public RelationshipAttributeDialog(Shell parentShell, IntelEntityRelationship relationship) {
		super(parentShell);
		this.relationship = relationship;
		type = null;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(relationship.getRelationshipType().getName());
		setMessage(MessageFormat.format(Messages.RelationshipAttributeDialog_Message, relationship.getRelationshipType().getName(), relationship.getSourceEntity().getIdAttributeAsText(), relationship.getTargetEntity().getIdAttributeAsText()));
		getShell().setText(Messages.RelationshipAttributeDialog_Title);
		getShell().setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RELATIONSHIP));
		
		fields = new HashMap<>();
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		if (relationship.getRelationshipType().getAttributes().size() == 0){
			Label l = new Label(parent, SWT.NONE);
			l.setText(Messages.RelationshipAttributeDialog_NoAttributes);
		}else{
			ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			
			core = new Composite(scroll, SWT.NONE);
			core.setLayout(new GridLayout(2, false));
			scroll.setContent(core);
			
			for (int i = 0; i < relationship.getRelationshipType().getAttributes().size(); i ++){
				//so dialog is opened to correct size
				Label spacer = new Label(core, SWT.NONE);
				spacer.setText(DialogConstants.LOADING_TEXT);
				spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				((GridData)spacer.getLayoutData()).heightHint = 22;
			}
			
			scroll.setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			Job j = new Job(""){ //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						type = (IntelRelationshipType) s.get(IntelRelationshipType.class, relationship.getRelationshipType().getUuid());
						type.getAttributes().forEach(e -> {
							e.getAttribute().getName();
							if (e.getAttribute().getAttributeList() != null){
								e.getAttribute().getAttributeList().forEach(i -> i.getName());
							}
						});
					}finally{
						s.close();
					}
					Display.getDefault().syncExec(() -> initEditor());
					return Status.OK_STATUS;
				}
			};
			j.setSystem(true);
			j.schedule();
		}
		
		return parent;
	}
	
	private void modified(){
		modified = true;
		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	
	
	private void initEditor(){
		for (Control c : core.getChildren()) c.dispose();
		
		for (IntelRelationshipTypeAttribute x : type.getAttributes()){
			IntelAttribute attribute = x.getAttribute();
			AttributeFieldEditor editor = new AttributeFieldEditor(core, attribute);
			IntelEntityRelationshipAttributeValue item = null;
			if (relationship.getAttributes() != null){
				for (IntelEntityRelationshipAttributeValue value : relationship.getAttributes()){
					if (value.getAttribute().equals(attribute)){
						editor.initControl(value);
						item = value;
						break;
					}
				}
			}
			fields.put(editor, item);
			editor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					modified();
				}
			});
		}
		getShell().layout(true, true);
		((ScrolledComposite)core.getParent()).setMinSize(core.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	@Override
	protected void cancelPressed(){
		if (modified){
			if (MessageDialog.openQuestion(getShell(), Messages.RelationshipAttributeDialog_SaveConfirmTitle, Messages.RelationshipAttributeDialog_SaveConfirmMessage)){
				okPressed();
				return;
			}
		}
		super.cancelPressed();
	}
	@Override
	protected void okPressed(){
		if (relationship.getAttributes() == null) relationship.setAttributes(new ArrayList<IntelEntityRelationshipAttributeValue>());
		for (AttributeFieldEditor e : fields.keySet()){
			IntelEntityRelationshipAttributeValue v = fields.get(e);
			if (v == null){
				v = new IntelEntityRelationshipAttributeValue();
				v.setAttribute(e.getAttribute());
				v.setRelationship(relationship);
				relationship.getAttributes().add(v);
			}
			if (!e.updateValue((IntelEntityRelationshipAttributeValue)v)){
				relationship.getAttributes().remove(v);
			}
		}
		
		super.okPressed();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
//		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		int x = p.x;
		int y = p.y;
		if (x > 500) x = 500;
		if (y > 600) y = 600;
		return new Point(x,y);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}