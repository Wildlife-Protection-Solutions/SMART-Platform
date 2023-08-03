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
package org.wcs.smart.asset.ui.config;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;

/**
 * Dialog for editing an attribute list item
 * @author Emily
 *
 */
public class AttributeListItemDialog extends SmartStyledTitleDialog {

	private AssetAttributeListItem item;
	private NameKeyComposite nameKeyInfo;
	private List<AssetAttributeListItem> siblings;
	
	public AttributeListItemDialog(Shell parentShell, AssetAttributeListItem item,
			List<AssetAttributeListItem> siblings) {
		super(parentShell);
		this.item = item;
		this.siblings = siblings;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		initFields();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
	}
	
	private void modified(){
		if (!nameKeyInfo.validate()){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		nameKeyInfo = new NameKeyComposite();
		nameKeyInfo.createControls(parent, true, item.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!nameKeyInfo.validate()){
					nameKeyInfo.updateFields(item);
				}
				modified();
			}
		});
		
		setTitle(Messages.AttributeListItemDialog_Title);
		getShell().setText(Messages.AttributeListItemDialog_Title);
		setMessage(Messages.AttributeListItemDialog_Message);
		
		return parent;
	}
	
	private void initFields(){
		nameKeyInfo.initFields(item, this.siblings, SmartDB.getCurrentConservationArea().getDefaultLanguage());
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}