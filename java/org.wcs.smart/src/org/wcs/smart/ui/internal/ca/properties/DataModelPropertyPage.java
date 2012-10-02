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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelSmartToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.ui.internal.ca.CategoryDialogPage;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Property page for modifying data model
 * @author Emily
 * @since 1.0.0
 */
public class DataModelPropertyPage  extends AbstractPropertyJHeaderDialog{

	public static final String ID = "org.wcs.smart.ca.DataModelPropertyPage";
	
	/* ui components */
	private TreeViewer viewer;

	private Button btnAddCategory;
	private Button btnAddAttribute;
	private Button btnDisableElement;
	private Button btnDeleteElement;
	
	private CategoryInfoPanel catInfoPanel;
	private AttributeInfoPanel attInfoPanel;
	private Composite infoInnerPanel;
	private Composite emptyComposite;

	private Button btnModifyElement;
	
	/* data model */
	private DataModel dataModel = null;
	
	private Transaction currentTransaction = null;
	
	
	/**
	 * Creates new data model property page
	 */
	public DataModelPropertyPage(Shell parent){
		super(parent, "Data Model");		
		//attach aggregations to current session
		for (Aggregation agg : DataModel.getAggregations()){
			getSession().update(agg);
		}
	}
	
	
	/**
	 * Sets the data model
	 * @param dm
	 */
	public void setDataModel(DataModel dm){
		this.dataModel = dm;
		if (viewer != null){
			viewer.setInput(this.dataModel);
			viewer.refresh();
		}
	}
	
	/**
	 * Starts a new transaction and opens the dialog
	 */
	@Override
	public int open(){
		currentTransaction = getSession().beginTransaction();
		return super.open();
	}
	
	/** Validates the user wants to save changes.  If no the current transaction is rolled back,
	 * if yes the transaction is committed, if cancel <code>false</code>.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#validateSave()
	 */
	@Override
	protected boolean validateSave(){
		if (getErrorMessage() != null){
			if (!MessageDialog.openQuestion(getShell(), "Close", "Changes have been made that cannot be saved.  Are you sure you want to close?")){
				return false;
			}
		}else{
			MessageDialog md = new MessageDialog(getShell(), "Save Changes?", null, "There are unsaved changes.  Would you like to save your changes before closing?", MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!performSave()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}else if (ret == 1){
				if (currentTransaction != null){
					try{
						currentTransaction.rollback();
					}catch (Exception ex){}
				}
				currentTransaction = null;
			}
		}
		return true;
	}
	
	/**
	 * @return the current language the data model is dealing in
	 */
	private Language getLanguage(){
		//TODO: implement language for this dialog
		return SmartDB.getCurrentConservationArea().getDefaultLanguage();
	}
	
	/**
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		parent.setLayout(new GridLayout(1, false));		
	
		Composite thisparent = new Composite(parent, SWT.BORDER);
		thisparent.setLayout(new GridLayout(1, false));
		thisparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm comp = new SashForm(thisparent, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/* left data tree */
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((DataModelContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((DataModelLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		FilteredTree fTree = new FilteredTree(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, patternFilter, true);
		viewer = fTree.getViewer();
		viewer.setContentProvider(new DataModelContentProvider());
		viewer.setLabelProvider(new DataModelLabelProvider(getLanguage()));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.heightHint = 300;
		viewer.getTree().setLayoutData(gd);
		viewer.setAutoExpandLevel(3);
		viewer.setInput(this.dataModel);
		
		int operations = DND.DROP_MOVE;
		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		viewer.addDragSupport(operations, transferTypes , new DmTableDragListener(viewer));
		viewer.addDropSupport(operations, transferTypes, new DmTableDropListener(viewer){
			@Override
			public boolean performDrop(Object data) {
				boolean ok = super.performDrop(data);
				if (ok){
					setChangesMade(true);
				}
				return ok;
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateInfoPanel();
			}
		});
		
		Composite rightPanel = new Composite(comp, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite buttonPanel = new Composite(rightPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(4, false));
		buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		btnAddCategory = new Button(buttonPanel, SWT.PUSH);
		btnAddCategory.setEnabled(false);
		btnAddCategory.setText("Add Category");
		btnAddCategory.setToolTipText("Add a new sub-category to the selected category.");
		btnAddCategory.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addCategory();
			}
		});
		
		btnAddAttribute = new Button(buttonPanel, SWT.PUSH);
		btnAddAttribute.setEnabled(false);
		btnAddAttribute.setText("Add Attribute");
		btnAddAttribute.setToolTipText("Add a new attribute to the selected category.");
		btnAddAttribute.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addAttribute();
			}
		});
		
		btnDisableElement = new Button(buttonPanel, SWT.NONE);
		btnDisableElement.setEnabled(false);
		btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		btnDisableElement.setToolTipText("Disabled categories/attributes are not shown when recording data.");
		btnDisableElement.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				disableElement();
			}
		});
		setButtonLayoutData(btnDisableElement);
		
		btnDeleteElement = new Button(buttonPanel, SWT.NONE);
		btnDeleteElement.setEnabled(false);
		btnDeleteElement.setText("Delete");
		btnDeleteElement.setToolTipText("Deletes the selected category/attribute.");
		btnDeleteElement.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteElement();
			}
		});
		setButtonLayoutData(btnDeleteElement);
		
		
		Group infoPanel = new Group(rightPanel, SWT.SHADOW_ETCHED_IN);
		((Group)infoPanel).setText("Properties");
		
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoPanel.setLayout(new GridLayout(1, false));
		
		
		infoInnerPanel = new Composite(infoPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		catInfoPanel = new CategoryInfoPanel(infoInnerPanel, SWT.NONE, false, false, SmartDB.getCurrentConservationArea().getDefaultLanguage()) {
			@Override
			protected List<Category> getSiblings() {
				return null;
			}
		};
		
		attInfoPanel = new AttributeInfoPanel(infoInnerPanel, SWT.NONE, false,
				false, SmartDB.getCurrentConservationArea()
						.getDefaultLanguage(), getSession()) {
			@Override
			public Collection<Attribute> getSiblings() {
				return null;
			}
		};
		attInfoPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		Composite infoButtonPanel = new Composite(infoPanel , SWT.NONE);
		infoButtonPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		infoButtonPanel.setLayout(new GridLayout(1, false));
		
		btnModifyElement = new Button(infoButtonPanel, SWT.NONE);
		btnModifyElement.setEnabled(false);
		btnModifyElement.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnModifyElement.setToolTipText("Edit selected element.");
		btnModifyElement.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editElement();
			}
		});
		setButtonLayoutData(btnModifyElement);		
		
		Composite bottomComp = new Composite(thisparent, SWT.NONE);
		bottomComp.setLayout(new GridLayout(1, false));
		/* import button @ bottom */
		Button exportButton = new Button(bottomComp, SWT.PUSH);
		exportButton.setText("Export To XML ...");
		exportButton.addSelectionListener(new SelectionAdapter() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportXml();
			}
		});
			
		viewer.refresh();
		setMessage("Manage conservation area data model.");
		
		return thisparent;
	}

	/**
	 * Exports the current model as defined (not necessary saved) to an xml file.
	 */
	private void exportXml(){
		FileDialog fd = new FileDialog(this.getShell(), SWT.SAVE);
		fd.setFilterNames(new String[]{"Xml File (.xml)"});
		fd.setFilterExtensions(new String[]{"*.xml"});;
		
		String file = fd.open();
		if (file == null){
			//nothing selected
			return;
		}
		final File f = new File(file);
		if (f.exists()){
			if (!MessageDialog.openQuestion(getShell(), "Overwrite file", "The file " + f.getName() + " exists.  Do you want to overwrite it?")){
				return;
			}
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try{
					monitor.beginTask("Exporting data model to xml file...", 2);
					DataModel dm = ((DataModel) viewer.getInput());
					monitor.subTask("Converting ...");
					org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = DataModelSmartToXmlConverter.convert(dm);
					monitor.worked(1);
					monitor.subTask("Writing ...");
					FileOutputStream fout = new FileOutputStream(f);
					try{
						XmlSmartDataModelManager.writeDataModel(xml,fout);
					}finally{
						fout.close();
					}
					MessageDialog.openInformation(getShell(), "Success", "Data model exported successfully");
					monitor.done();
					}catch (Exception ex){
						SmartPlugIn.displayLog(getShell(),"Error exporting xml data model.", ex);			
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(),"Error exporting xml data model.", ex);
		}
	}
	
	/**
	 * Commits the current open transaction.
	 * 
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		final Shell errorShell = getShell();
		final Session s = getSession();
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						currentTransaction.commit();
						DataModelManager.getInstance().fireChangeListeners();
						currentTransaction = session.beginTransaction();
						setChangesMade(false);
					}catch (Exception ex){
						SmartPlugIn.log(null, ex);
						throw new IllegalStateException(ex);
					}
				}
			});
		} catch (Throwable ex) {
			try{
				if (currentTransaction != null && currentTransaction.isActive()){
					currentTransaction.rollback();
				}
			}catch (Exception ex2){}
			currentTransaction = null;
			s.close();
			SmartPlugIn.displayLog(errorShell,"Error saving data model changes.  Please close this dialog and re-open it before continuing. \n\n" + ex.getMessage(), ex);
			return false;
		}
		return true;
		
	}
	
	/*
	 * Disabled data model object
	 */
	private void disableElement(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (o instanceof Category){
			((DataModel)viewer.getInput()).disableCategory((Category)o, !((Category)o).getIsActive());			
		}else if (o instanceof CategoryAttribute){
			((DataModel)viewer.getInput()).disableAttribute((CategoryAttribute)o, !((CategoryAttribute)o).getIsActive());
		}
		updateInfoPanel();
		refreshTree();
		setChangesMade(true);
		
	}	
	
	/**
	 * Runs a task in a progress monitor dialog.
	 * @param runnable
	 */
	private void runInProgressDialog(IRunnableWithProgress runnable){
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(false, true, runnable);		
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(), "An error has occurred. " + ex.getMessage() + ".\n\nPlease close the data model window and re-open it.", ex);
			try{
				if (currentTransaction != null && currentTransaction.isActive()){
					currentTransaction.rollback();
					currentTransaction = null;
				}
			}catch (Exception ex2){
				SmartPlugIn.log("Error in data model dialog", ex2);
			}
			getSession().close();
		}
	}
	
	/*
	 * Deletes the currently selected item
	 */
	private void deleteElement(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (o instanceof Category){
			final Category cat  = (Category)o;
			boolean ret = MessageDialog.openConfirm(getShell(), "Delete", "Are you sure you want to delete the category : " + cat.getFullCategoryName(getLanguage()));
			if (!ret){
				return;
			}
			
			runInProgressDialog(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
					boolean delete = DataModelManager.getInstance().validateDelete(cat, monitor, getSession());
					if (delete){
						if (cat.getParent() != null){
							cat.getParent().getChildren().remove(cat);
							cat.setParent(null);
						}else{
							 ((DataModel) viewer.getInput()).getCategories().remove(cat);
							 if (cat.getUuid() != null){
								 getSession().delete(cat);
							}
							getSession().flush();
							getSession().evict(cat);
						}
					}
				}
			});
			
		}else if (o instanceof CategoryAttribute){
			final CategoryAttribute catAtt  = (CategoryAttribute)o;
			boolean ret = MessageDialog.openConfirm(getShell(), "Delete", "Are you sure you want to delete the category/attribute relationship:\nCategory: '" + catAtt.getCategory().getFullCategoryName(getLanguage()) + "'\nAttribute: '" + catAtt.getAttribute().findName(getLanguage()) + "'");
			if (!ret){
				return;
			}
			runInProgressDialog(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					boolean delete = DataModelManager.getInstance().validateDelete(catAtt, monitor, getSession());
					if (delete){
						catAtt.getCategory().getAttributes().remove(catAtt);
						if (catAtt.getCategory().getUuid() == null || catAtt.getAttribute().getUuid() == null){
							getSession().evict(catAtt);
							getSession().flush();
						}else{
							getSession().delete(catAtt);
							getSession().flush();
							getSession().evict(catAtt);
						}
						
						//at this point we should try to delete the attribute as well
						boolean contains = false;
						for (Category root :dataModel.getCategories()){
							if (containsAttribute(root, catAtt.getAttribute())){
								contains = true;
								break;
							}
						}
						if (!contains){
							MessageDialog dialog = new MessageDialog(getShell(), "Delete", null,
									"The attribute '" + catAtt.getAttribute().findName(getLanguage()) + "' is not longer associated with any categories.  Would you like to delete this attribute?", 
									MessageDialog.CONFIRM, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
							int ret = dialog.open();
							if (ret == 0){  //YES
								delete = DataModelManager.getInstance().validateDelete(catAtt.getAttribute(), monitor, getSession());
								if (delete){
									dataModel.getAttributes().remove(catAtt.getAttribute());
									if (catAtt.getAttribute().getUuid() != null){
										getSession().delete(catAtt.getAttribute());
									}
								}
							}
						}
					}
				}
			});
		}else{
			return;
		}
		
		refreshTree();
		setChangesMade(true);
		updateInfoPanel();
	}

	
	/**
	 * Determines if a particular category or parent category
	 * contains the given attribute 
	 * 
	 * @param cat category to test
	 * @param att attribute to look for
	 * @return
	 */
	private boolean containsAttribute(Category cat, Attribute att){
		if (cat.getAttributes() != null && cat.getAttributes().contains(new CategoryAttribute(cat,att))){
			return true;
		}else{
			if (cat.getChildren() != null){
				for (Category kid : cat.getChildren()){
					if (containsAttribute(kid, att)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/*
	 * adds a category
	 */
	private void addCategory(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		List<Category> siblings = null;
		if (o instanceof DataModelContentProvider.RootNode){
			siblings = ((DataModel) viewer.getInput()).getCategories();
		}else if (o instanceof Category){
			siblings = ((Category) o).getChildren();
		}
			
		Category newCat = new Category();
		newCat.setCategoryOrder(0);
		newCat.setAttributes(null);
		newCat.setChildren(null);		
		newCat.setConservationArea(ca);
		newCat.setIsActive(true);
		newCat.setChildren(new ArrayList<Category>());
		newCat.setIsMultiple(true);
		
		CategoryDialogPage dd = new CategoryDialogPage(getShell(), newCat, siblings, ca.getDefaultLanguage());
		int ret = dd.open();
		
		if (ret == Window.CANCEL){
			return;
		}
		
		if (o instanceof DataModelContentProvider.RootNode){
			DataModel dm = (DataModel)viewer.getInput();
			newCat.setParent(null);
			newCat.setCategoryOrder(dm.getCategories().size());
			dm.getCategories().add(newCat);
			getSession().saveOrUpdate(newCat);
		}else if (o instanceof Category){
			Category parentCat = (Category)o;
			if (parentCat.getChildren() == null){
				parentCat.setChildren(new ArrayList<Category>());
			}
			newCat.setParent(parentCat);
			newCat.setCategoryOrder(parentCat.getChildren().size());
			parentCat.getChildren().add(newCat);
			viewer.setExpandedState(parentCat, true);
		}
		newCat.updateHkey();
		
		refreshTree();
		setChangesMade(true);
	}

	/*
	 * modifies category or attribute
	 */
	private void editElement(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (o instanceof Category){
			List<Category> siblings = null;
			if (((Category) o).getParent() != null){
				siblings = ((Category) o).getParent().getChildren();
			}
			CategoryDialogPage dd = new CategoryDialogPage(getShell(), (Category)o, siblings, ca.getDefaultLanguage());
			int ret = dd.open();
			
			if (ret == Window.CANCEL){
				return;
			}
			refreshTree();
		}else if (o instanceof CategoryAttribute){
			//warn that this affects everywhere this attribute is used.
			
			Set<CategoryAttribute> usages = ((DataModel)viewer.getInput()).findAttribute(((CategoryAttribute)o).getAttribute());
			if (usages.size() > 1){
				MessageDialog.openWarning(getShell(), "Modify Warning", "This attribute is referenced by  multiple categories.  Modifying it will affect all categories that reference this attribute.");
			}

			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(),
					((CategoryAttribute) o).getAttribute(),
					((DataModel) viewer.getInput()).getAttributes(),
					ca.getDefaultLanguage(), getSession());			
			//show new attribute dialog
			int ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			refreshTree();
			try{
				getSession().flush();
			}catch (Exception ex){
				SmartPlugIn.displayLog(getShell(), "Error editing element.  Please close and re-start.\n\n" + ex.getMessage(), ex);
			}
			
		}
		updateInfoPanel();
		setChangesMade(true);
	}
	
	/*
	 * adds an attribute
	 */
	private void addAttribute(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (! (o instanceof Category)){
			return;
		}
		Category parent = (Category)o;
		if (parent.getChildren() != null && parent.getChildren().size() > 0){
			MessageDialog.openInformation(getShell(), "Add Attribute", "The attributes you add to this category will be inherted by all children categories.");
		}
		
		//show dialog
		AddAttributeDialog1 d1 = new AddAttributeDialog1(getShell(), parent, (DataModel)viewer.getInput(), ca.getDefaultLanguage());
		int ret = d1.open();
		if (ret == AddAttributeDialog1.FINISH){
			refreshTree();
		}else if (ret == AddAttributeDialog1.NEXT){
			Attribute att = new Attribute();
			
			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(), att,
					((DataModel) viewer.getInput()).getAttributes(),
					ca.getDefaultLanguage(), getSession());
			
			//show new attribute dialog
			ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			att.setConservationArea(ca);
			DataModel dm = (DataModel)viewer.getInput();
			dm.addNewAttribute(att, parent);
			session.saveOrUpdate(att);
			viewer.setExpandedState(parent, true);
			refreshTree();
		}
		setChangesMade(true);
		
	}
	
	/*
	 * refresh tree keeping expanded state the same
	 */
	private void refreshTree() {
		Object[] elements = viewer.getExpandedElements();
		TreePath[] paths = viewer.getExpandedTreePaths();

		viewer.refresh();
		viewer.setExpandedElements(elements);
		viewer.setExpandedTreePaths(paths);
	}

	/*
	 * updates the info panel with the current selected item
	 */
	private void updateInfoPanel() {
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		
		if (o instanceof Category){
			((StackLayout)infoInnerPanel.getLayout()).topControl = catInfoPanel;
			catInfoPanel.setCategory((Category)o);

			btnAddAttribute.setEnabled( ((Category)o).getIsActive() );
			btnAddCategory.setEnabled( ((Category)o).getIsActive() );
			btnModifyElement.setEnabled( ((Category)o).getIsActive() );
			btnDeleteElement.setEnabled( true );
			
			if (((Category)o).getIsActive()){
				btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			}else{
				btnDisableElement.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			}
			btnDisableElement.setEnabled(true);
		}else if (o instanceof CategoryAttribute){
			attInfoPanel.setAttribute( ((CategoryAttribute)o).getAttribute() );
			((StackLayout)infoInnerPanel.getLayout()).topControl = attInfoPanel;
			btnAddCategory.setEnabled(false);
			btnAddAttribute.setEnabled(false);
			btnModifyElement.setEnabled( ((CategoryAttribute)o).getIsActive());
			btnDeleteElement.setEnabled( true );
			
			if (((CategoryAttribute)o).getIsActive()){
				btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			}else{
				btnDisableElement.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			}
			btnDisableElement.setEnabled(true);
		} else	if (o instanceof DataModelContentProvider.RootNode){
			btnAddCategory.setEnabled(true);
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			btnAddAttribute.setEnabled(false);
			
			btnModifyElement.setEnabled(false);
			btnDisableElement.setEnabled(false);
			btnDeleteElement.setEnabled(false);
		} else{
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			
			btnAddCategory.setEnabled(false);
			btnAddAttribute.setEnabled(false);
			btnModifyElement.setEnabled(false);
			btnDisableElement.setEnabled(false);
			btnDeleteElement.setEnabled(false);			
		}
		infoInnerPanel.layout();
	}
}
