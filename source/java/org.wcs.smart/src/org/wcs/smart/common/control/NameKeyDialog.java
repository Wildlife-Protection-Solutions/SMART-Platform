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
package org.wcs.smart.common.control;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;

/**
 * Dialog for created a new named key object
 * @author Emily
 *
 */
public class NameKeyDialog<T extends NamedKeyItem> extends SmartStyledDialog{

	protected NameKeyComposite keyComp;
	
	protected T item;
	protected List<T> siblings;
	
	protected boolean canEdit = true;
	
	protected NameKeyDialog(Shell parentShell, T item, List<T> siblings) {
		this(parentShell, item, siblings, true);
	}

	protected NameKeyDialog(Shell parentShell, T item, List<T> siblings, boolean canEdit) {
		super(parentShell);
		this.item = item;
		this.siblings = siblings;
		this.canEdit = canEdit;
	}

	
	protected String getTitle(){
		return Messages.NameKeyDialog_DialogTitle;
	}
	
	public Point getInitialSize(){
		return new Point(400, 200);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public void okPressed(){
		keyComp.updateFields(item);
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite core = new Composite(composite, SWT.NONE);
		core.setLayout(new GridLayout(3, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		keyComp = new NameKeyComposite();
		keyComp.createControls(core, canEdit, item.getUuid() == null, new NameKeyComposite.IChangeListener() {
			@Override
			public void itemModified() {
				modified();
			}
		});
		keyComp.initFields(item, siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage() == null ? SmartDB.getCurrentLanguage() : SmartDB.getCurrentConservationArea().getDefaultLanguage());		
		
		getShell().setText(getTitle());
		
		return composite;
	}
	
	protected void modified(){
		if (!keyComp.validate()){
			if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(true);
		}else{
			if (getButton(IDialogConstants.OK_ID) != null) getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	public T getUpdatedItem(){
		return item;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}
