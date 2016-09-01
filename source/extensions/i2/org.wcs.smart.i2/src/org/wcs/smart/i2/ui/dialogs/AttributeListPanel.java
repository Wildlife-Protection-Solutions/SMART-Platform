package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ca.Label;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.ui.AttributeListItemLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

public class AttributeListPanel extends Composite {

	private ListViewer items;
	private IntelAttribute attribute;
	
	public AttributeListPanel(Composite parent, IntelAttribute attribute) {
		super(parent, SWT.NONE);
		this.attribute = attribute;
		createControls();
	}
	
	private void createControls(){
		setLayout(new GridLayout(2, false));
		
		items = new ListViewer(this, SWT.MULTI | SWT.BORDER);
		items.setContentProvider(ArrayContentProvider.getInstance());
		items.setLabelProvider(AttributeListItemLabelProvider.INSTANCE);
		items.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		items.setInput(attribute.getAttributeList());
		
		Composite btnPanel = new Composite(this, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btnPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();	
			}
		});
		
		Button btnEdit = new Button(btnPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();	
			}
		});
		
		Button btnRemove = new Button(btnPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}

	private void add(){
		IntelAttributeListItem it = new IntelAttributeListItem();
		it.setAttribute(attribute);
		if (attribute.getAttributeList() == null) attribute.setAttributeList(new ArrayList<IntelAttributeListItem>());
		attribute.getAttributeList().add(it);
		
		AttributeListItemDialog d = new AttributeListItemDialog(getShell(), it);
		if (d.open() == Window.CANCEL){
			attribute.getAttributeList().remove(it);
			it = null;
		}
		items.refresh();
	}
	
	private void edit(){
		Object x = ((IStructuredSelection)items.getSelection()).getFirstElement();
		if (! (x instanceof IntelAttributeListItem)) return;
		
		IntelAttributeListItem it = (IntelAttributeListItem) x;
		
		IntelAttributeListItem copy = new IntelAttributeListItem();
		copy.setAttribute(it.getAttribute());
		copy.setKeyId(it.getKeyId());
		copy.setName(it.getName());
		for (Label l : copy.getNames()){
			copy.updateName(l.getLanguage(), l.getValue());
		}
		AttributeListItemDialog d = new AttributeListItemDialog(getShell(), copy);
		if (d.open() == Window.OK){
			it.setKeyId(copy.getKeyId());
			it.setName(copy.getName());
			for (Label l : copy.getNames()){
				it.updateName(l.getLanguage(), l.getValue());
			}
		}
		items.refresh();
	}
	
	public void refresh(){
		items.refresh();
	}
}
