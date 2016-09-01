package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

public class AttributeListItemDialog extends TitleAreaDialog {

	private IntelAttributeListItem item;
	private NameKeyComposite nameKeyInfo;
	
	public AttributeListItemDialog(Shell parentShell, IntelAttributeListItem item) {
		super(parentShell);
		this.item = item;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		if (item.getUuid() != null){
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
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
		
		setTitle("Intelligence Attribute List Item");
		getShell().setText("Intelligence Attribute List Item");
		setMessage("Create or edit intelligence attribute list item.");
		
		initFields();
		
		return parent;
		
	}
	
	private void initFields(){
		List<IntelAttributeListItem> kids = new ArrayList<IntelAttributeListItem>();
		kids.addAll(item.getAttribute().getAttributeList());
		kids.remove(item);
		
		nameKeyInfo.initFields(item, kids, SmartDB.getCurrentConservationArea().getDefaultLanguage());
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}