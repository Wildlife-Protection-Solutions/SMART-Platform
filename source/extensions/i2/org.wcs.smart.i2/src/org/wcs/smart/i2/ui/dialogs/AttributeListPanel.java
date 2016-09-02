package org.wcs.smart.i2.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Label;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.ui.AttributeListItemLabelProvider;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;

public class AttributeListPanel extends Composite {

	private ListViewer items;
	private IntelAttribute attribute;
	private List<IChangeListener> listeners;
	private Button btnAdd;
	private Button btnRemove;
	private Button btnEdit;
	
	private MenuItem dItem;
	private MenuItem eItem;
	
	public AttributeListPanel(Composite parent, IntelAttribute attribute) {
		super(parent, SWT.NONE);
		this.attribute = attribute;
		createControls();
		listeners = new ArrayList<IChangeListener>();
	}
	
	public void addChangeListener(IChangeListener listener){
		listeners.add(listener);
	}
	
	private void modified(){
		for (IChangeListener l : listeners){
			l.itemModified();
		}
	}
	private void createControls(){
		setLayout(new GridLayout(2, false));
		
		items = new ListViewer(this, SWT.MULTI | SWT.BORDER);
		items.setContentProvider(ArrayContentProvider.getInstance());
		items.setLabelProvider(AttributeListItemLabelProvider.INSTANCE);
		items.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		items.setInput(attribute.getAttributeList());
		items.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		items.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!items.getSelection().isEmpty());
				btnRemove.setEnabled(!items.getSelection().isEmpty());
				
				dItem.setEnabled(!items.getSelection().isEmpty());
				eItem.setEnabled(!items.getSelection().isEmpty());
			}
		});
		
		Menu menu = new Menu(items.getControl());
		eItem = new MenuItem(menu, SWT.DEFAULT);
		eItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		eItem.setEnabled(false);
		eItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		dItem = new MenuItem(menu, SWT.DEFAULT);
		dItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		dItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		dItem.setEnabled(false);
		dItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		items.getControl().setMenu(menu);
		
		
		Composite btnPanel = new Composite(this, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnAdd = new Button(btnPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();	
			}
		});
		
		btnEdit = new Button(btnPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();	
			}
		});
		
		
		btnRemove = new Button(btnPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.setEnabled(false);
		btnRemove.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				delete();
			}
		});
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
		}else{
			modified();
		}
		items.refresh();
	}
	
	private void delete(){
		IStructuredSelection items = (IStructuredSelection)this.items.getSelection();
		List<IntelAttributeListItem> toDelete = new ArrayList<IntelAttributeListItem>();
		
		StringBuilder sb = new StringBuilder();
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelAttributeListItem){
				toDelete.add((IntelAttributeListItem) x);
				sb.append(((IntelAttributeListItem)x).getName());
				sb.append(", ");
			}
		}
		
		if (toDelete.isEmpty()) return;
		sb.deleteCharAt(sb.length()-1);
		sb.deleteCharAt(sb.length()-1);
		
		if (!MessageDialog.openConfirm(getShell(), "Delete List Items", MessageFormat.format("Are you sure you want to delete the following {0} list items? \n {1}", toDelete.size(), sb.toString()))) return;
	
		for (IntelAttributeListItem item : toDelete){
			item.setAttribute(null);
		}
		attribute.getAttributeList().removeAll(toDelete);
		this.items.refresh();
		modified();
	}
	
	
	private void edit(){
		Object x = ((IStructuredSelection)items.getSelection()).getFirstElement();
		if (! (x instanceof IntelAttributeListItem)) return;
		
		IntelAttributeListItem it = (IntelAttributeListItem) x;
		
		IntelAttributeListItem copy = new IntelAttributeListItem();
		copy.setAttribute(it.getAttribute());
		copy.setKeyId(it.getKeyId());
		copy.setName(it.getName());
		copy.setUuid(it.getUuid());
		for (Label l : it.getNames()){
			copy.updateName(l.getLanguage(), l.getValue());
		}
		AttributeListItemDialog d = new AttributeListItemDialog(getShell(), copy);
		if (d.open() == Window.OK){
			it.setKeyId(copy.getKeyId());
			it.setName(copy.getName());
			for (Label l : copy.getNames()){
				it.updateName(l.getLanguage(), l.getValue());
			}
			modified();
		}
		items.refresh();
	}
	
	public void refresh(){
		items.refresh();
	}
}
