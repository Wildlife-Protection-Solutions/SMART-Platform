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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelAttributeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for listing all attributes.
 * 
 * @author Emily
 *
 */
public class AttributeListDialog extends TitleAreaDialog {

	private TableViewer cmbTypes;
	private List<IntelAttribute> types = null;
	private NamedItemViewerFilter filter;
	
	private IStructuredSelection currentSelection = null;
	
	private Button btnEdit;
	private Button btnNew;
	private Button btnDelete;
	
	private MenuItem mnuEdit;
	private MenuItem mnuNew;
	private MenuItem mnuDelete;
	
	private Job loadTypes = new LoadAttributesJob(){
		@Override
		public void afterLoad() {
			types = attributes;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbTypes.setInput(types);
					if (currentSelection != null) cmbTypes.setSelection(currentSelection);
				}
			});
		}
		
	};
	
	public AttributeListDialog(Shell parentShell) {
		super(parentShell);
	}

	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				filter.setFilterString(typeFilter.getPatternFilter());
				cmbTypes.refresh();
			}
		});
		Label l = new Label(parent, SWT.NONE);
		l.setVisible(false);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		cmbTypes = new TableViewer(parent, SWT.MULTI | SWT.BORDER);
		cmbTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbTypes.setLabelProvider(AttributeLabelProvider.INSTANCE);
		cmbTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbTypes.getControl().setFocus();
		cmbTypes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		cmbTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDelete.setEnabled(!cmbTypes.getSelection().isEmpty());
				btnEdit.setEnabled(!cmbTypes.getSelection().isEmpty());
				mnuEdit.setEnabled(!cmbTypes.getSelection().isEmpty());
				mnuDelete.setEnabled(!cmbTypes.getSelection().isEmpty());
			}
		});
		filter = new NamedItemViewerFilter(cmbTypes);
		cmbTypes.setFilters(new ViewerFilter[]{filter});
		
		Menu typesMenu = new Menu(cmbTypes.getControl());
		cmbTypes.getControl().setMenu(typesMenu);
		
		mnuNew = new MenuItem(typesMenu, SWT.DEFAULT);
		mnuNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		
		mnuEdit = new MenuItem(typesMenu, SWT.DEFAULT);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		mnuEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		mnuDelete = new MenuItem(typesMenu, SWT.DEFAULT);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnNew = new Button(buttonPanel, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnNew.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				add();
			}
		});
		
		btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				edit();
			}
		});
		
		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				delete();
			}
		});
		
		
		btnEdit.setEnabled(false);
		btnDelete.setEnabled(false);
		btnNew.setEnabled(true);
		mnuEdit.setEnabled(false);
		mnuDelete.setEnabled(false);
		mnuNew.setEnabled(true);
		
		setTitle("Intelligence Attributes");
		getShell().setText("Intelligence Attributes");
		setMessage("Manage the attributes in the system.");
		
		loadTypes.setSystem(true);
		loadTypes.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,p.y*2);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void add(){
		IntelAttribute newAttribute = new IntelAttribute();
		newAttribute.setConservationArea(SmartDB.getCurrentConservationArea());
		newAttribute.setAttributeList(new ArrayList<IntelAttributeListItem>());
		
		AttributeDialog ad = new AttributeDialog(getShell(), newAttribute);
		ad.open();
		
		refresh();
	}
	
	private void edit(){
		IStructuredSelection items = (IStructuredSelection)cmbTypes.getSelection();
		Object first = items.getFirstElement();
		if (first instanceof IntelAttribute){
			IntelAttribute editAttribute = (IntelAttribute) first;
			AttributeDialog ad = new AttributeDialog(getShell(), editAttribute);
			ad.open();
			refresh();
		}
	}
	
	private void delete(){
		IStructuredSelection items = (IStructuredSelection)cmbTypes.getSelection();
		List<IntelAttribute> toDelete = new ArrayList<IntelAttribute>();
		
		StringBuilder sb = new StringBuilder();
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelAttribute){
				toDelete.add((IntelAttribute) x);
				sb.append(((IntelAttribute)x).getName());
				sb.append(", ");
			}
		}
		
		if (toDelete.isEmpty()) return;
		sb.deleteCharAt(sb.length()-1);
		sb.deleteCharAt(sb.length()-1);
		
		if (!MessageDialog.openConfirm(getShell(), "Delete Attributes", MessageFormat.format("Are you sure you want to delete the following {0} attributes? This action cannot be undone. \n {1}", toDelete.size(), sb.toString()))) return;
	
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Session s = HibernateManager.openSession();
					List<String> warnings = new ArrayList<String>();
					try{
						s.beginTransaction();
						for (IntelAttribute a : toDelete){
							try {
								IntelAttributeManager.INSTANCE.deleteAttribute(a, s);
							}catch(Exception ex){
								warnings.add(a.getName() + ": " + ex.getMessage());
							}
						}
						s.getTransaction().commit();
						if (!warnings.isEmpty()){
							Display.getDefault().syncExec(new Runnable(){
								@Override
								public void run() {
									WarningDialog wd = new WarningDialog(getShell(), "Delete Attributes", "The following attributes could not be removed: ", warnings);
									wd.open();		
								}
								
							});
							
						}
					}catch (Exception ex){
						s.getTransaction().rollback();
						Intelligence2PlugIn.displayLog("Could not delete attributes: " + ex.getMessage(), ex);
					}finally{
						s.close();
					}			
				}
			});
		}catch(Exception ex){
			Intelligence2PlugIn.displayLog("Could not delete attributes: " + ex.getMessage(), ex);
			
		}
		refresh();
	}
	
	private void refresh(){
		currentSelection = (IStructuredSelection) cmbTypes.getSelection();
		cmbTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadTypes.schedule(0);
	}
}