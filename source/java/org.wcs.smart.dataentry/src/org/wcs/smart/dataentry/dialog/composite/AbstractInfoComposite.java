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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.dataentry.CmDefaultListsUtil;
import org.wcs.smart.dataentry.CmDefaultTreesUtil;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ControlButton;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.DatamodelCategorySelectorDialog;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
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
	
	private List<IModelChangedListener> modelListeners = new ArrayList<IModelChangedListener>();
	private List<ISourceObjectChangedListener> sourceListeners = new ArrayList<ISourceObjectChangedListener>();

	protected boolean fireChanged = true;
	protected Session session;
	
	public AbstractInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, SWT.NONE);
		this.model = model;
		this.session = session;
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
		
		DisplayModeComboViewer modeViewer = new DisplayModeComboViewer(parent);
		modeViewer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modeViewer.addDisplayModeChangeListener(e->{
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
		});
		modeViewer.addCascadeListener(new CascadeDisplayModeListener(modeViewer, this));
		
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
					return a.getConfigDisplayMode();
				}
				return null; //we should never be here
			}
		});

		return modeViewer;
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
		node.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), node.getName());
		
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
	
	protected void loadFiles(Icon i, Session s) {
		if (i == null) return;
		for (IconFile file : i.getFiles()) {
			file.computeFileLocation(s);
			file.getIconSet().equals(null);
		}
	}
	
	/**
	 * Handles the add datamodel category
	 */
	protected void addDatamodelCategory() {
		try {
			DataModel dm = getDataModel();
			final DatamodelCategorySelectorDialog dialog = new DatamodelCategorySelectorDialog(dm) {
				@Override
				protected void createButtonsForButtonBar(Composite parent) {
					// create OK and Cancel buttons by default
					createButton(parent, IDialogConstants.OK_ID, "Add Selected", false);
					createButton(parent, IDialogConstants.FINISH_ID, "Add Selected With Children", true);
					createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CANCEL_LABEL, false);
					
					getButton(IDialogConstants.FINISH_ID).setEnabled(false);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					getButton(IDialogConstants.CLOSE_ID).setFocus();
					
					super.setReturnCode(IDialogConstants.CLOSE_ID);
				}
				protected void updateButtons() {
					super.updateButtons();
					getButton(IDialogConstants.FINISH_ID).setEnabled(getButton(IDialogConstants.OK_ID).getEnabled());
				}
			};
			
			int ret = dialog.open();
			if (ret != IDialogConstants.FINISH_ID && ret != IDialogConstants.OK_ID) return;
			if (dialog.getCategories().isEmpty()) return;
				
			List<Category> toAdd = dialog.getCategories();
			boolean includeKids = ret == IDialogConstants.FINISH_ID;
			
			//this can be slow for large trees or many categories; put it in a pmd
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					
					monitor.beginTask(Messages.AbstractInfoComposite_AddCategory, toAdd.size() + 2);
					if (includeKids)	{
						addCategoriesWithKids(toAdd, monitor);
					}else {
						addCategoriesWithoutKids(toAdd, monitor);
					}
					
					monitor.subTask("saving...");
					session.flush();
					monitor.worked(2);
					
					Display.getDefault().syncExec(()->fireModelChanged());
				}
			});
			
		} catch (Throwable ex) {
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		}
	}
	
	private CmNode createNodeFromCategory(Category category) {
		final CmNode node = new CmNode();
		node.setModel(getModel());
		node.setCategory(category);
		session.persist(node);
		
		List<Attribute> attrList = new ArrayList<Attribute>();
		category.getAllAttribute(attrList, true);
		for (Attribute a : attrList) {
			CmAttribute cma = new CmAttribute();
			cma.setNode(node);
			cma.setAttribute(a);

			cma.setOrder(node.getCmAttributes().size());
			cma.setCmAttributeOptions(CmAttributeOptionFactory.buildDefaultOptions(cma, a.getType()));
			node.getCmAttributes().add(cma);
			if (AttributeType.TREE.equals(a.getType())) {
				ensureDefaultTreeExists(a);
				cma.setConfig(getModel().getDefaultConfigs().get(a));
			}
			if (a.getType().isList()) {
				ensureDefaultListExists(a);
				cma.setConfig(getModel().getDefaultConfigs().get(a));
			}
			session.persist(cma);
		}
		return node;
	}
	

	private void ensureDefaultTreeExists(Attribute a) {
		ConfigurableModel m = getModel();
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(m);
		if (!existingTrees.contains(a) || m.getDefaultConfigs().get(a) == null) {
			CmAttributeConfig cfg = CmDefaultTreesUtil.buildDefaultTreeConfig(m, a);
			if (cfg.getUuid() == null) session.persist(cfg);
			m.getDefaultConfigs().put(a, cfg);
		}
	}

	private void ensureDefaultListExists(Attribute a) {
		ConfigurableModel m = getModel();
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(m);
		if (!existingLists.contains(a) || m.getDefaultConfigs().get(a) == null) {
			CmAttributeConfig cfg = CmDefaultListsUtil.buildDefaultListConfig(m, a);
			if (cfg.getUuid() == null) session.persist(cfg);
			m.getDefaultConfigs().put(a, cfg);
		}
	}
	
	private void addCategoriesWithKids(List<Category> category, IProgressMonitor monitor) {
		List<Category> allCategories = new ArrayList<>(category);
		allCategories.sort((a,b)->Integer.compare(Category.hkeyLength(a.getHkey()),Category.hkeyLength(b.getHkey())));
			
		List<Category> processed = new ArrayList<>();
		while(!allCategories.isEmpty()) {
			Category c = allCategories.removeFirst();
			monitor.subTask(c.getName());
			monitor.worked(1);
			
			if (processed.contains(c)) {
				monitor.worked(1);
				continue;
			}
			
			addCategoriesWithKids(Collections.singletonList(c), null, processed, category);
			monitor.worked(1);
			
		}		
	}
	
	private void addCategoriesWithKids(List<Category> category, CmNode parent, List<Category> processed, List<Category> originalSelection) {
		List<Category> allCategories = new ArrayList<>(category);
		allCategories.sort((a,b)->Integer.compare(Category.hkeyLength(a.getHkey()),Category.hkeyLength(b.getHkey())));
		
		while(!allCategories.isEmpty()) {
			Category c = allCategories.removeFirst();
			processed.add(c);			
			if (c.getChildren().isEmpty()) {
				//add this as a root
				CmNode node = createNodeFromCategory(c);
				
				Object xparent = parent;
				if (xparent == null) {
					xparent = getSourceObject();
				}
				
				if (xparent instanceof CmNode) {
					CmNode parentNode = (CmNode) xparent;
					node.setParent(parentNode);
					node.setNodeOrder(parentNode.getChildren().size());
					parentNode.getChildren().add(node);
				} else if (xparent instanceof CmRootNode) {
					node.setParent(null);
					node.setNodeOrder(getModel().getNodes().size());
					getModel().getNodes().add(node);
				}
			}else {
				//create a new SubGroup
				
				CmNode node = new CmNode();
				node.setModel(getModel());
				node.setName(c.getName());
				for (org.wcs.smart.ca.Label l : c.getNames()) {
					node.updateName(l.getLanguage(), l.getValue());
				}
				
				Object xparent = parent;
				if (xparent == null) {
					xparent = getSourceObject();
				}
				
				if (xparent instanceof CmNode) {
					CmNode parentNode = (CmNode) xparent;
					node.setParent(parentNode);
					node.setNodeOrder(parentNode.getChildren().size());
					parentNode.getChildren().add(node);
				} else if (xparent instanceof CmRootNode) {
					node.setParent(null);
					node.setNodeOrder(getModel().getNodes().size());
					getModel().getNodes().add(node);
				}
				
				//if no kids are in root selection then we add all kids
				//otherwise if one kid is in root selection we only
				//add the kids in the root selection
				List<Category> toAdd = new ArrayList<>(c.getActiveChildren());
				
				List<Category> existsInOriginal = new ArrayList<>();
				for (Category kid : toAdd) {
					if (originalSelection.contains(kid)) {
						existsInOriginal.add(kid);
					}
				}
				
				if (!existsInOriginal.isEmpty()) {
					addCategoriesWithKids(existsInOriginal, node, processed, originalSelection);
				}else {				
					addCategoriesWithKids(toAdd, node, processed, originalSelection);
				}
			}
			
		}
	}
	
	private void addCategoriesWithoutKids(List<Category> categories, IProgressMonitor monitor) {
		for (Category c : categories){
			monitor.subTask(c.getName());
			CmNode node = createNodeFromCategory(c);
			
			Object xparent = getSourceObject();
			
			if (xparent instanceof CmNode) {
				CmNode parentNode = (CmNode) xparent;
				node.setParent(parentNode);
				node.setNodeOrder(parentNode.getChildren().size());
				parentNode.getChildren().add(node);
			} else if (xparent instanceof CmRootNode) {
				node.setParent(null);
				node.setNodeOrder(getModel().getNodes().size());
				getModel().getNodes().add(node);
			}
			monitor.worked(1);
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
					SubMonitor progress = SubMonitor.convert(monitor, Messages.AbstractInfoComposite_DmLoadingTaskName, 3);
					
					DataModel dataModel = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
					progress.worked(1);
						
					//load categories into memory; no-lazy loading here.
					//attributes are only loaded when needed (when added to the cm)
					SubMonitor sub = progress.split(1);
					sub.setWorkRemaining(dataModel.getActiveCategories().size());
					sub.subTask(Messages.AbstractInfoComposite_CategoriesSubTask);
					for (Category cat: dataModel.getActiveCategories()){
						sub.subTask(cat.getName());
						visitCategory(cat);
						cat.getNames().size();
						sub.worked(1);
					}
						
					cachedDataModel = dataModel;
					progress.worked(1);
					monitor.done();
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
			SmartPlugIn.displayLog(Messages.AbstractInfoComposite_CategoryError + ex.getMessage(), ex);
		}
		return cachedDataModel;
		
	}
	
	public ConfigurableModel getModel() {
		return model;
	}
	
	protected void fireModelChanged() {
		if (!fireChanged) return;
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
	protected class TranslatableNameComposite extends CmTranslateNameComposite {

		public TranslatableNameComposite(Composite parent) {
			super(parent, null);
		}

		@Override
		protected void handleChanged() {
			fireModelChanged();
		}
		
	}
}
