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
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for editing Configurable Models.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConfigurableModelEditDialog extends AbstractPropertyJHeaderDialog {

	private ConfigurableModel model;
	
	private TreeViewer modelTreeViewer;
	private Button btnAddGroup;
	private Button btnAddCategory;
	private Button btnDelete;
	
	public ConfigurableModelEditDialog(ConfigurableModel model) {
		super(Display.getDefault().getActiveShell(), "Configured Data Model");
		this.model = model;
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		modelTreeViewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelTreeViewer.setLabelProvider(new NamedItemLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider());
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelTreeViewer.setInput(model);
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanelState();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));

		btnAddGroup = new Button(rightPanel, SWT.PUSH);
		btnAddGroup.setText("Add SubGroup");
		btnAddGroup.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSubGroup();
			}
		});

		btnAddCategory = new Button(rightPanel, SWT.PUSH);
		btnAddCategory.setText("Add Datamodel Category");
		btnAddCategory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addDatamodelCategory();
			}
		});
		
		btnDelete = new Button(rightPanel, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDeleteNode();
			}
		});

		setTitle("Configured Data Model");
		setMessage("Editing data model configuration");
		
		return container;
		
	}

	@Override
	protected boolean performSave() {
		DataentryHibernateManager.saveConfigurableModel(model);
		setChangesMade(false);
		return true;
	}

	private void addSubGroup() {
		CmNode parentNode = getCurrentNode();
		CmNode node = new CmNode();
		node.setModel(model);
		node.setParent(parentNode);
		node.setName("New SubGroup");
		node.updateName(SmartDB.getCurrentLanguage(), node.getName());
		node.setNodeOrder(parentNode.getChildren().size());
		parentNode.getChildren().add(node);
		setChangesMade(true);
		modelTreeViewer.refresh();
	}
	
	private void addDatamodelCategory() {
		Session s = getSession();
		s.beginTransaction();
		try {
			DataModel dm = getDataModel(s);
			DatamodelCatecorySelectorDialog dialog = new DatamodelCatecorySelectorDialog(dm);
			if (dialog.open() == IDialogConstants.OK_ID) {
				CmNode parentNode = getCurrentNode();
				if (parentNode != null) {
					Category category = dialog.getCategory();
					CmNode node = new CmNode();
					node.setModel(model);
					node.setParent(parentNode);
					node.setCategory(category);
					node.setName(category.getName());
					for (Label label : category.getNames()) { //we need a copy, not the same instance of set
						node.updateName(label.getLanguage(), label.getValue());
					}
					node.setNodeOrder(parentNode.getChildren().size());
					parentNode.getChildren().add(node);
					List<Attribute> attrList = new ArrayList<Attribute>();
					category.getAllAttribute(attrList, true);
					for (Attribute a : attrList) {
						CmAttribute cma = new CmAttribute();
						cma.setNode(node);
						cma.setAttribute(a);
						cma.setName(a.getName());
						for (Label label : a.getNames()) { //we need a copy, not the same instance of set
							cma.updateName(label.getLanguage(), label.getValue());
						}
						//TODO: add CmAttribute default options
						node.getCmAttributes().add(cma);
					}
				}
				setChangesMade(true);
				modelTreeViewer.refresh();
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	private void handleDeleteNode() {
		CmNode node = getCurrentNode();
		CmNode parentNode = node.getParent();
		if (parentNode == null) {
			//this is the root node
			model.getNodes().remove(node);
		} else {
			//not a root node
			parentNode.getChildren().remove(node);
		}
		node.setParent(null);
		node.setModel(null);

		setChangesMade(true);
		modelTreeViewer.refresh();
	}
	
	private CmNode getCurrentNode() {
		IStructuredSelection selection = (IStructuredSelection) modelTreeViewer.getSelection();
		Object obj = selection.getFirstElement();
		if (obj instanceof CmNode) {
			return (CmNode) obj;
		}
		return null;
	}

	private void updateRightPanelState() {
		btnAddGroup.setEnabled(false);
		btnAddCategory.setEnabled(false);
		btnDelete.setEnabled(false);
		
		IStructuredSelection selection = (IStructuredSelection) modelTreeViewer.getSelection();
		Object obj = selection.getFirstElement();
		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			boolean isGroup = node.isGroup();
			btnAddGroup.setEnabled(isGroup);
			btnAddCategory.setEnabled(isGroup);
			btnDelete.setEnabled(true);
		}
	}
	
	private DataModel getDataModel(Session session) {
		DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		//load into memory; no-lazy loading here.
		for (Category cat: dataModel.getCategories()){
			visitCategory(cat);
		}
		for (Attribute att: dataModel.getAttributes()){
			att.getAggregations().size();
		}
		return dataModel;
	}
	
	private void visitCategory(Category cat){
		for (Category child : cat.getActiveChildren()){
			visitCategory(child);
			child.getName();
		}
//		for (CategoryAttribute ca: cat.getAttributes()){
//			ca.getAttribute().getName();
//		}	
	}
	
}
