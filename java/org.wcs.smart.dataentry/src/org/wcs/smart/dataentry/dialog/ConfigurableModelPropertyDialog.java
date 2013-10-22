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
package org.wcs.smart.dataentry.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for viewing Configurable Models.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConfigurableModelPropertyDialog extends AbstractPropertyJHeaderDialog {

	private static final int DIALOG_WIDTH = 500;
	private static final int DIALOG_HEIGHT = 550;
	
	private TableViewer modelListViewer;
	private TreeViewer modelTreeViewer;
	
	private Button btnNew;
	private Button btnEdit;

	public ConfigurableModelPropertyDialog(Shell parent) {
		super(parent, Messages.ConfigurableModelPropertyDialog_Title);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelListViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(getModelsList().toArray());
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateTreeViewer();
				btnEdit.setEnabled(!modelListViewer.getSelection().isEmpty());
			}
		});
		
		modelTreeViewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(false));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		btnNew = new Button(container, SWT.PUSH);
		btnNew.setText(Messages.ConfigurableModelPropertyDialog_Button_Create);
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Session session = getSession();
				try {
					ConfigurableModel cm = new ConfigurableModel();
					cm.setConservationArea(SmartDB.getCurrentConservationArea());
					cm.setName(Messages.ConfigurableModelPropertyDialog_ConfigurableModelDeafultName);
					cm.updateName(SmartDB.getCurrentLanguage(), cm.getName());
					Dialog dialog = new ConfigurableModelEditDialog(cm);
					dialog.open();
				} finally {
					//most likely session will be closed here as AbstractPropertyJHeaderDialog closes session when dialog is closed
					if (session.isOpen()){
						session.close();
					}
				}
				modelListViewer.setInput(getModelsList().toArray());
				updateTreeViewer();
			}
		});
		
		btnEdit = new Button(container, SWT.PUSH);
		btnEdit.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		((GridData)btnEdit.getLayoutData()).widthHint = 90;
		btnEdit.setEnabled(false);
		btnEdit.setText(Messages.ConfigurableModelPropertyDialog_Button_Edit);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Session session = getSession();
				try {
					ConfigurableModel cm = (ConfigurableModel) modelTreeViewer.getInput();
					cm = DataentryHibernateManager.getFullConfigurableModel(cm.getUuid(), session);
					Dialog dialog = new ConfigurableModelEditDialog(cm);
					dialog.open();
				} finally {
					//most likely session will be closed here as AbstractPropertyJHeaderDialog closes session when dialog is closed
					if (session.isOpen()){
						session.close();
					}
				}
				modelListViewer.setInput(getModelsList().toArray());
				updateTreeViewer();
			}
		});		
		
		setTitle(Messages.ConfigurableModelPropertyDialog_Title);
		setMessage(Messages.ConfigurableModelPropertyDialog_Message);
		
		return container;
	}

	@Override
	protected boolean performSave() {
		return true;
	}

	private List<ConfigurableModel> getModelsList() {
		List<ConfigurableModel> modelList = new ArrayList<ConfigurableModel>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			modelList = DataentryHibernateManager.getConfigurableModels(s);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		return modelList;
	}
	
	private void updateTreeViewer() {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		if (!selection.isEmpty()) {
			ConfigurableModel cm = (ConfigurableModel) selection.getFirstElement();
			cm = DataentryHibernateManager.getFullConfigurableModel(cm.getUuid());
			modelTreeViewer.setInput(cm);
		}
	}
	
}
