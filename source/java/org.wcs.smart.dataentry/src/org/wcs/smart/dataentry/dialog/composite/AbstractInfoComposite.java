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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog.ControlButton;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.DatamodelCategorySelectorDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Info composite containing some common logic and building blocks for {@link CmRootNode},  {@link CmNode},  {@link CmAttribute}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public abstract class AbstractInfoComposite extends Composite {

	private ConfigurableModel model;
	private Session session;
	
	private List<IModelChangedListener> modelListeners = new ArrayList<IModelChangedListener>();
	private List<ISourceObjectChangedListener> sourceListeners = new ArrayList<ISourceObjectChangedListener>();

	public AbstractInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, SWT.NONE);
		this.model = model;
		this.session = session;
	}

	public abstract Object getSourceObject();
	
	public abstract boolean isButtonValid(ConfigurableModelEditDialog.ControlButton button);
	
	public void processButton(ConfigurableModelEditDialog.ControlButton button){
		if (button == ControlButton.ADD_CATEGORY){
			addDatamodelCategory();
		}else if (button == ControlButton.ADD_GROUP){
			addSubGroup();
		}else if (button == ControlButton.DELETE){
			handleDeleteNode();
		}		
	}


	protected Composite createContentContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		container.setLayout(gd);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return container;
	}

	protected TranslatableNameComposite createDisplayNameControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.AbstractInfoComposite_DisplayName);
		TranslatableNameComposite tnc = new TranslatableNameComposite(parent);
		addSourceObjectChangedListener(tnc);
		return tnc;
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
			node.setNodeOrder(getModel().getNodes().size());
			getModel().getNodes().add(node);
		}
		fireModelChanged();
	}
	
	/**
	 * Handles delete button
	 */
	protected void handleDeleteNode(){
	}
	
	/**
	 * Handles the add subgroup button
	 */
	protected void addSubGroup() {
		CmNode node = new CmNode();
		node.setModel(getModel());
		node.setName(Messages.AbstractInfoComposite_NewGroupDefaultName);
		node.updateName(SmartDB.getCurrentLanguage(), node.getName());
		addToParent(node);
	}
	
	/**
	 * Handles the add datamodel category
	 */
	protected void addDatamodelCategory() {
		try {
			DataModel dm = getDataModel();
			DatamodelCategorySelectorDialog dialog = new DatamodelCategorySelectorDialog(dm);
			if (dialog.open() == IDialogConstants.OK_ID) {
				for (Category c : dialog.getCategories()){
					addCategory(c);
				}
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		}
	}
	
	private void addCategory(Category category){
		CmNode node = new CmNode();
		node.setModel(getModel());
		node.setCategory(category);
		node.setName(category.getName());
		for (org.wcs.smart.ca.Label label : category.getNames()) { //we need a copy, not the same instance of set
			node.updateName(label.getLanguage(), label.getValue());
		}
		List<Attribute> attrList = new ArrayList<Attribute>();
		category.getAllAttribute(attrList, true);
		for (Attribute a : attrList) {
			CmAttribute cma = new CmAttribute();
			cma.setNode(node);
			cma.setAttribute(a);
			cma.setName(a.getName());
			for (org.wcs.smart.ca.Label label : a.getNames()) { //we need a copy, not the same instance of set
				cma.updateName(label.getLanguage(), label.getValue());
			}
			cma.setOrder(node.getCmAttributes().size());
			cma.setCmAttributeOptions(CmAttributeOptionFactory.buildDefaultOptions(cma, a.getType()));
			node.getCmAttributes().add(cma);
		}
		addToParent(node);
	}

	private DataModel getDataModel() {
		DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), getSession());
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
	
	protected Session getSession() {
		return session;
	}
	
	protected void fireModelChanged() {
		for (IModelChangedListener listener : modelListeners) {
			listener.modelChanged();
		}
	}
	
	public void addModelChangedListener(IModelChangedListener listener) {
		modelListeners.add(listener);
	}

	public void removeModelChangedListener(IModelChangedListener listener) {
		modelListeners.remove(listener);
	}

	protected void fireSourceObjectChanged(Object newObject) {
		for (ISourceObjectChangedListener listener : sourceListeners) {
			listener.sourceObjectChanged(newObject);
		}
	}
	
	protected void addSourceObjectChangedListener(ISourceObjectChangedListener listener) {
		sourceListeners.add(listener);
	}

	protected void removeSourceObjectChangedListener(ISourceObjectChangedListener listener) {
		sourceListeners.remove(listener);
	}
	
	/**
	 * Represents listener for configurable model changes.
	 * Fired when some of controls changes some data in {@link ConfigurableModel}
	 * 
	 * @author elitvin
	 * @since 2.0.0
	 */
	public static interface IModelChangedListener {
		public void modelChanged();
	}

	/**
	 * Represents listener for source object changes.
	 * Fired when new source object is set for composite.
	 * 
	 * @author elitvin
	 * @since 2.0.0
	 */
	protected interface ISourceObjectChangedListener {
		public void sourceObjectChanged(Object newObject);
	}
	
	/**
	 * Composite with text field and "Translate" button.
	 * Contains all related functionality for translating {@link NamedItem}
	 * 
	 * @author elitvin
	 * @since 2.0.0
	 */
	protected class TranslatableNameComposite extends Composite implements ISourceObjectChangedListener {

		private Text text;
		private Button button;
		
		private NamedItem item;
		private boolean internalChange = false; //indicate if text was changed by user or by calling setter
		
		public TranslatableNameComposite(Composite parent) {
			super(parent, SWT.NONE);
			createControls();
		}
		
		private void createControls() {
			GridLayout gd = new GridLayout(2, false);
			gd.marginBottom=0;
			gd.marginHeight = 0;
			gd.marginLeft = 0;
			gd.marginRight = 0;
			gd.marginTop = 0;
			gd.marginWidth = 0;
			this.setLayout(gd);
			this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			text = new Text(this, SWT.BORDER);
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (internalChange && item != null) {
						item.setName(text.getText());
						item.updateName(SmartDB.getCurrentLanguage(), item.getName());
						fireModelChanged();
					}
				}
			});
			
			button = new Button(this, SWT.PUSH);
			button.setText(Messages.TranslatableNameComposite_Button_Translate);
		}
		
		public Text getText() {
			return text;
		}
		
		public Button getButton() {
			return button;
		}

		@Override
		public void sourceObjectChanged(Object newObject) {
			if (newObject instanceof NamedItem) {
				item = (NamedItem) newObject;
				internalChange = false;
				text.setText(item.getName());
				internalChange = true;
			} else if (newObject instanceof CmRootNode) {
				CmRootNode root = (CmRootNode) newObject;
				sourceObjectChanged(root.model);
			}
		}
	}
}
