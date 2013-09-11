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
package org.wcs.smart.dataentry.dialog.composite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.DatamodelCatecorySelectorDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.hibernate.SmartHibernateManager;

/**
 * Info composite containing some common logic and building blocks for {@link CmRootNode},  {@link CmNode},  {@link CmAttribute}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public abstract class AbstractInfoComposite extends Composite {

	private ConfigurableModel model;
	
	private List<IModelChangedListener> listeners = new ArrayList<IModelChangedListener>();

	public AbstractInfoComposite(Composite parent, ConfigurableModel model) {
		super(parent, SWT.NONE);
		this.model = model;
	}

	public abstract Object getSourceObject();

	protected void createAddButtons() {
		Button btnAddGroup = new Button(this, SWT.PUSH);
		btnAddGroup.setText("Add SubGroup");
		btnAddGroup.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSubGroup();
			}
		});

		Button btnAddCategory = new Button(this, SWT.PUSH);
		btnAddCategory.setText("Add Datamodel Category");
		btnAddCategory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addDatamodelCategory();
			}
		});
	}
	
	private void addToParent(CmNode node) {
		Object obj = getSourceObject();
		if (obj instanceof CmNode) {
			CmNode parentNode = (CmNode) obj;
			node.setParent(parentNode);
			node.setNodeOrder(parentNode.getChildren().size());
			parentNode.getChildren().add(node);
		} else if (obj instanceof CmRootNode) {
			node.setParent(null);
			node.setNodeOrder(model.getNodes().size());
			model.getNodes().add(node);
		}
		fireModelChanged();
	}
	
	protected void addSubGroup() {
		CmNode node = new CmNode();
		node.setModel(model);
		node.setName("New SubGroup");
		node.updateName(SmartDB.getCurrentLanguage(), node.getName());
		addToParent(node);
	}
	
	protected void addDatamodelCategory() {
		Session s = SmartHibernateManager.openSession();
		s.beginTransaction();
		try {
			DataModel dm = getDataModel(s);
			DatamodelCatecorySelectorDialog dialog = new DatamodelCatecorySelectorDialog(dm);
			if (dialog.open() == IDialogConstants.OK_ID) {
				Category category = dialog.getCategory();
				CmNode node = new CmNode();
				node.setModel(model);
				node.setCategory(category);
				node.setName(category.getName());
				for (Label label : category.getNames()) { //we need a copy, not the same instance of set
					node.updateName(label.getLanguage(), label.getValue());
				}
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
				addToParent(node);
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
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

	public ConfigurableModel getModel() {
		return model;
	}
	
	protected void fireModelChanged() {
		for (IModelChangedListener listener : listeners) {
			listener.modelChanged();
		}
	}
	
	public void addModelChangedListener(IModelChangedListener listener) {
		listeners.add(listener);
	}

	public void removeModelChangedListener(IModelChangedListener listener) {
		listeners.remove(listener);
	}
	
	public static interface IModelChangedListener {
		public void modelChanged();
	}
}
