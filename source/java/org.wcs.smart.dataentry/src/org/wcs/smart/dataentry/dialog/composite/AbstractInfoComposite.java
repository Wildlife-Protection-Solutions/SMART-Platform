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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ControlButton;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.DatamodelCategorySelectorDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;

/**
 * Info composite containing some common logic and building blocks for {@link CmRootNode},  {@link CmNode},  {@link CmAttribute}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public abstract class AbstractInfoComposite extends Composite {

	private ConfigurableModel model;
	
	private List<IModelChangedListener> modelListeners = new ArrayList<IModelChangedListener>();
	private List<ISourceObjectChangedListener> sourceListeners = new ArrayList<ISourceObjectChangedListener>();

	public AbstractInfoComposite(Composite parent, ConfigurableModel model) {
		super(parent, SWT.NONE);
		this.model = model;
	}

	public abstract Object getSourceObject();
	
	public abstract boolean isButtonValid(ConfigurableModelEditorDefaultTab.ControlButton button);
	
	/**
	 * Process a button action.
	 * 
	 * @param button
	 * @return the resulting item to select from the model viewer or null if nothing
	 * to select
	 */
	public void processButton(ConfigurableModelEditorDefaultTab.ControlButton button){
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
	
	protected DisplayModeComboViewer createDisplayModeControls(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.AbstractInfoComposite_DisplayMode);
		final DisplayModeComboViewer modeViewer = new DisplayModeComboViewer(parent);
		modeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean fire = true;
				DisplayMode mode = modeViewer.getSelectedDisplayMode();
				Object obj = getSourceObject();
				if (obj instanceof CmRootNode) {
					CmRootNode r = (CmRootNode) obj;
					fire = mode != r.model.getDisplayMode();
					r.model.setDisplayMode(mode);
				} else if (obj instanceof CmNode) {
					CmNode m = (CmNode) obj;
					fire = mode != m.getDisplayMode();
					m.setDisplayMode(mode);
				} else {
					SmartPlugIn.log("Unexpected class in display mode combo viewer: " + obj.getClass(), null); //$NON-NLS-1$
				}
				if (fire) {
					fireModelChanged();
				}
			}
		});
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				DisplayMode mode = getSourceDisplayMode();
				modeViewer.setSelection(new StructuredSelection(mode != null ? mode : DisplayMode.DEFAULT_DISPLAY_MODE));
			}
			
			private DisplayMode getSourceDisplayMode() {
				Object obj = getSourceObject();
				if (obj instanceof CmRootNode) {
					CmRootNode r = (CmRootNode) obj;
					return r.model.getDisplayMode();
				} else if (obj instanceof CmNode) {
					CmNode m = (CmNode) obj;
					return m.getDisplayMode();
				} else if (obj instanceof CmAttribute) {
					CmAttribute a = (CmAttribute) obj;
					return a.getDisplayMode();
				}
				return null; //we should never be here
			}
		});

		return modeViewer;
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
			final DatamodelCategorySelectorDialog dialog = new DatamodelCategorySelectorDialog(dm);
			if (dialog.open() == IDialogConstants.OK_ID) {
		
				if (dialog.getCategories().isEmpty()) return;
				
				//this can be slow for large trees or many categories; put it in a pmd
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				pmd.run(true, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.AbstractInfoComposite_AddCategory, dialog.getCategories().size());
					
						Session s = HibernateManager.openSession();
						try{
							for (Category c : dialog.getCategories()){
								Category c2 = c;
								monitor.subTask(c.getName());
								s.saveOrUpdate(c);
								while(c2 != null){
									c2.getNames().size();
									for (CategoryAttribute ca : c2.getAttributes(true)){
										ca.getAttribute().getNames().size();
										if (ca.getAttribute().getAggregations()!= null) ca.getAttribute().getAggregations().size();
										if (ca.getAttribute().getActiveListItems() != null){
											for (AttributeListItem li : ca.getAttribute().getActiveListItems()){
												li.getNames().size();
											}
										}
										visitTreeNodes(ca.getAttribute().getActiveTreeNodes());
									}
									c2 = c2.getParent();
								}
								addCategory(c);
								monitor.worked(1);
							}
						}finally{
							s.close();
						}
						monitor.done();
						
					}
					private void visitTreeNodes(List<AttributeTreeNode> nodes){
						if (nodes == null) return;
						for (AttributeTreeNode n : nodes){
							n.getNames().size();
							visitTreeNodes(n.getActiveChildren());
						}
					}
				});
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		}
	}
	
	private void addCategory(Category category){
		
		final CmNode node = new CmNode();
		node.setModel(getModel());
		node.setCategory(category);
		node.setName(category.getName());
		for (org.wcs.smart.ca.Label label : category.getNames()) { // we need a copy, not the same instance of set
			node.updateName(label.getLanguage(), label.getValue());
		}
		List<Attribute> attrList = new ArrayList<Attribute>();
		category.getAllAttribute(attrList, true);
		for (Attribute a : attrList) {
			CmAttribute cma = new CmAttribute();
			cma.setNode(node);
			cma.setAttribute(a);
			cma.setName(a.getName());
			for (org.wcs.smart.ca.Label label : a.getNames()) { // we need a copy, not the same instance of set
				cma.updateName(label.getLanguage(), label.getValue());
			}
			cma.setOrder(node.getCmAttributes().size());
			cma.setCmAttributeOptions(CmAttributeOptionFactory
					.buildDefaultOptions(cma, a.getType()));
			node.getCmAttributes().add(cma);
			if (AttributeType.TREE.equals(a.getType())) {
				ensureDefaultTreeExists(a);
			}
			if (AttributeType.LIST.equals(a.getType())) {
				ensureDefaultListExists(a);
			}
		}
		//we need to call this part in the main ui thread
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				addToParent(node);
			}
		});
	}

	private void ensureDefaultTreeExists(Attribute a) {
		ConfigurableModel m = getModel();
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(m);
		if (!existingTrees.contains(a)) {
			List<CmAttributeTreeNode> defTree = CmDefaultTreesUtil.buildDefaultTree(m, a);
			m.getDefaultTrees().addAll(defTree);
		}
	}

	private void ensureDefaultListExists(Attribute a) {
		ConfigurableModel m = getModel();
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(m);
		if (!existingLists.contains(a)) {
			List<CmAttributeListItem> defList = CmDefaultListsUtil.buildDefaultList(m, a);
			m.getDefaultLists().addAll(defList);
		}
	}

	private DataModel cachedDataModel = null;
	private DataModel getDataModel() {
		if (cachedDataModel != null) return cachedDataModel;
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.AbstractInfoComposite_DmLoadingTaskName, 2);
					Session s = HibernateManager.openSession();
					try{
						DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
						monitor.worked(1);
						
						//load categories into memory; no-lazy loading here.
						//attributes are only loaded when needed (when added to the cm)
						SubProgressMonitor sub = new SubProgressMonitor(monitor, 1);
						sub.beginTask(Messages.AbstractInfoComposite_CategoriesSubTask, dataModel.getActiveCategories().size());
						for (Category cat: dataModel.getActiveCategories()){
							sub.subTask(cat.getName());
							visitCategory(cat);
							cat.getNames().size();
							sub.worked(1);
						}
						sub.done();
						
						cachedDataModel = dataModel;
						monitor.worked(1);
					}finally{
						monitor.done();
						s.close();
					}
				}
				
				private void visitCategory(Category cat){
					for (Category child : cat.getActiveChildren()){
						visitCategory(child);
						child.getName();
						child.getNames().size();
					}
	
				}
			});
		}catch (Exception ex){
			SmartPlugIn.displayError(Messages.AbstractInfoComposite_CategoryError + ex.getMessage(), ex);
		}
		return cachedDataModel;
		
	}
	
	public ConfigurableModel getModel() {
		return model;
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

	protected void fireSourceObjectChanged(Object newObject, Language language) {
		for (ISourceObjectChangedListener listener : sourceListeners) {
			listener.sourceObjectChanged(newObject, language);
		}
	}
	
	protected void addSourceObjectChangedListener(ISourceObjectChangedListener listener) {
		sourceListeners.add(listener);
	}

	protected void removeSourceObjectChangedListener(ISourceObjectChangedListener listener) {
		sourceListeners.remove(listener);
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
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
		public void sourceObjectChanged(Object newObject, Language language);
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
		private Language currentLanguage = null;
		
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
			
			final ControlDecoration cd = createControlDecoration(text);
			cd.hide();
			
			text.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					if (internalChange && item != null) {
						boolean changed = false;
						if (text.getText().length() > org.wcs.smart.ca.Label.MAX_LENGTH){
							MessageDialog.openError(getShell(), Messages.AbstractInfoComposite_ErrorDialogTitle, MessageFormat.format(Messages.AbstractInfoComposite_InvalidNameMessage, new Object[]{org.wcs.smart.ca.Label.MAX_LENGTH}));
							text.setText(item.getName());
						}else{
							changed = !item.getName().equals(text.getText());
							item.setName(text.getText());
							item.updateName(currentLanguage, item.getName());
							
						}
						if (changed){
							//only fire if name actually changed
							fireModelChanged();
						}
					}
					
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (internalChange && item != null) {
						if (text.getText().length() > org.wcs.smart.ca.Label.MAX_LENGTH){
							cd.setDescriptionText(MessageFormat.format(Messages.AbstractInfoComposite_InvalidNameMessage, new Object[]{org.wcs.smart.ca.Label.MAX_LENGTH}));
							cd.show();
						}else{
							cd.hide();
						}
						fireModelChanged();
						
					}
				}
			});
			
			button = new Button(this, SWT.PUSH);
			button.setText(Messages.TranslatableNameComposite_Button_Translate);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (item != null){
						TranslateSimpleListItemDialog translateDialog = new TranslateSimpleListItemDialog(getShell(), item);
						if (translateDialog.open() == Window.OK){
							updateText(item);
							fireModelChanged();
						}
						
					}
				}
			});
		}
		
		public Text getText() {
			return text;
		}
		
		public Button getButton() {
			return button;
		}

		private void updateText(NamedItem item){
			String l = item.findNameNull(currentLanguage);
			if (l == null){
				l = item.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			text.setText(l);
		}
		@Override
		public void sourceObjectChanged(Object newObject, Language language) {
			currentLanguage = language;
			if (newObject instanceof NamedItem) {
				item = (NamedItem) newObject;
				internalChange = false;
				updateText(item);
				internalChange = true;
			} else if (newObject instanceof CmRootNode) {
				CmRootNode root = (CmRootNode) newObject;
				sourceObjectChanged(root.model, language);
			}
		}
	}
}
