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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DataModelMergeAndUpdater;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelSmartToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlSmartDataModelManager;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog1;
import org.wcs.smart.ui.ca.properties.AddAttributeDialog2;
import org.wcs.smart.ui.ca.properties.AttributeInfoPanel;
import org.wcs.smart.ui.internal.ca.CategoryDialogPage;
import org.wcs.smart.ui.internal.ca.properties.handlers.ShowDataModelPropertyPageHandler;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Property page for modifying data model
 * @author Emily
 * @since 1.0.0
 */
public class DataModelPropertyPage  extends AbstractPropertyJHeaderDialog{

	private static final String DELETE_DIALOG_TITLE = Messages.DataModelPropertyPage_Delete_DialogTitle;

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
	private LanguageViewer cmbLanguage = null;
	
	private ConservationArea currentCa = SmartDB.getCurrentConservationArea();
	/**
	 * Creates new data model property page
	 */
	public DataModelPropertyPage(Shell parent){
		super(parent, Messages.DataModelPropertyPage_Dialog_Title);		
		//attach aggregations to current session
		for (Aggregation agg : DataModel.getAggregations()){
			getSession().update(agg);
		}
	}
	
	public boolean close(){
		
		//evict all items from cache to ensure they are correctly loaded next time
		getSession().getSessionFactory().getCache().evictEntityRegion(Category.class);
		getSession().getSessionFactory().getCache().evictEntityRegion(Attribute.class);
		getSession().getSessionFactory().getCache().evictEntityRegion(AttributeTreeNode.class);
		getSession().getSessionFactory().getCache().evictEntityRegion(AttributeListItem.class);
		getSession().getSessionFactory().getCache().evictEntityRegion(CategoryAttribute.class);
		
		getSession().getSessionFactory().getCache().evictCollectionRegion("org.wcs.smart.ca.datamodel.Attribute.attributeList"); //$NON-NLS-1$
		getSession().getSessionFactory().getCache().evictCollectionRegion("org.wcs.smart.ca.datamodel.Attribute.activeTreeNodes"); //$NON-NLS-1$
		getSession().getSessionFactory().getCache().evictCollectionRegion("org.wcs.smart.ca.datamodel.Attribute.tree"); //$NON-NLS-1$
		getSession().getSessionFactory().getCache().evictCollectionRegion("org.wcs.smart.ca.datamodel.AttributeTreeNode.children"); //$NON-NLS-1$
		getSession().getSessionFactory().getCache().evictCollectionRegion("org.wcs.smart.ca.datamodel.AttributeTreeNode.activeChildren"); //$NON-NLS-1$
		
		return super.close();
	}
	
	/**
	 * Sets the data model
	 * @param dm
	 */
	public void setDataModel(DataModel dm){
		this.dataModel = dm;
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
			if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_Close_DialogTitle, Messages.DataModelPropertyPage_Close_DialogMessage)){
				return false;
			}
		}else{
			MessageDialog md = new MessageDialog(getShell(), Messages.DataModelPropertyPage_SaveChanges_DialogTitle, null, Messages.DataModelPropertyPage_SaveChanges_DialogMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
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
		return cmbLanguage.getCurrentSelection();
	}
	
	/**
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		parent.setLayout(new GridLayout(1, false));		
	
		Composite thisparent = new Composite(parent, SWT.NONE);
		thisparent.setLayout(new GridLayout(1, false));
		thisparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm comp = new SashForm(thisparent, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPanel = new Composite(comp, SWT.NONE);
		leftPanel.setLayout(new GridLayout(1, false));
		
		cmbLanguage = new LanguageViewer(leftPanel, SWT.NONE, currentCa);
		cmbLanguage.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbLanguage.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				((DataModelLabelProvider)viewer.getLabelProvider()).setLanguage(getLanguage());
				viewer.refresh();
				//refresh selection to refresh panels
				viewer.setSelection(viewer.getSelection());
			}
		});
		
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
		FilteredTree fTree = new FilteredTree(leftPanel, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, patternFilter, true);
		viewer = fTree.getViewer();
		viewer.setContentProvider(new DataModelContentProvider());
		viewer.setLabelProvider(new DataModelLabelProvider(getLanguage()));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true,true);
		gd.heightHint = 200;
		viewer.getTree().setLayoutData(gd);
		viewer.setAutoExpandLevel(3);
		
		//schedule this for later
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				viewer.setInput(dataModel);
			}});
		
		
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
		btnAddCategory.setText(Messages.DataModelPropertyPage_AddCategory_Button);
		btnAddCategory.setToolTipText(Messages.DataModelPropertyPage_AddCategory_Tooltip);
		btnAddCategory.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addCategory();
			}
		});
		
		btnAddAttribute = new Button(buttonPanel, SWT.PUSH);
		btnAddAttribute.setEnabled(false);
		btnAddAttribute.setText(Messages.DataModelPropertyPage_AddAttribute_Button);
		btnAddAttribute.setToolTipText(Messages.DataModelPropertyPage_AddAttribute_Tooltip);
		btnAddAttribute.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				addAttribute();
			}
		});
		
		btnDisableElement = new Button(buttonPanel, SWT.NONE);
		btnDisableElement.setEnabled(false);
		btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		btnDisableElement.setToolTipText(Messages.DataModelPropertyPage_Disable_Tooltip);
		btnDisableElement.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				disableElement();
			}
		});
		setButtonLayoutData(btnDisableElement);
		
		btnDeleteElement = new Button(buttonPanel, SWT.NONE);
		btnDeleteElement.setEnabled(false);
		btnDeleteElement.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteElement.setToolTipText(Messages.DataModelPropertyPage_Delete_Tooltip);
		btnDeleteElement.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteElement();
			}
		});
		setButtonLayoutData(btnDeleteElement);
		
		
		Group infoPanel = new Group(rightPanel, SWT.SHADOW_ETCHED_IN);
		((Group)infoPanel).setText(Messages.DataModelPropertyPage_PropertiesGroup_Label);
		
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoPanel.setLayout(new GridLayout(1, false));
		
		
		infoInnerPanel = new Composite(infoPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		catInfoPanel = new CategoryInfoPanel(infoInnerPanel, SWT.NONE, false, false);
		
		attInfoPanel = new AttributeInfoPanel(infoInnerPanel, SWT.NONE, false, false, getSession());
		attInfoPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		Composite infoButtonPanel = new Composite(infoPanel , SWT.NONE);
		infoButtonPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		infoButtonPanel.setLayout(new GridLayout(1, false));
		
		btnModifyElement = new Button(infoButtonPanel, SWT.NONE);
		btnModifyElement.setEnabled(false);
		btnModifyElement.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnModifyElement.setToolTipText(Messages.DataModelPropertyPage_Edit_Tooltip);
		btnModifyElement.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editElement();
			}
		});
		setButtonLayoutData(btnModifyElement);		
		
		Composite bottomComp = new Composite(thisparent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		bottomComp.setLayout(gl);
		
		//merge
		Button mergeButton = new Button(bottomComp, SWT.PUSH);
		mergeButton.setText(Messages.DataModelPropertyPage_MergeButton);
		mergeButton.setToolTipText(Messages.DataModelPropertyPage_MergeTooltip);
		mergeButton.addSelectionListener(new SelectionAdapter() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				mergeDataModel();
				
			}
		});
		
		//export 
		Button exportButton = new Button(bottomComp, SWT.PUSH);
		exportButton.setText(Messages.DataModelPropertyPage_ExportXml_Button);
		exportButton.setToolTipText(Messages.DataModelPropertyPage_ExportTooltip);
		exportButton.addSelectionListener(new SelectionAdapter() {		
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportXml();
			}
		});
			
		viewer.refresh();
		setTitle(Messages.DataModelPropertyPage_PageTitle);
		setMessage(Messages.DataModelPropertyPage_Dialog_Message);
		
		return thisparent;
	}

	private void mergeDataModel(){
		if (changesMade){
			if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_SaveDialogTitle, Messages.DataModelPropertyPage_SaveDialogMessage)){
				return;
			}
			if (!performSave()) return;
			if (changesMade) return;	//something went wrong 
		}
		FileDialog fd = new FileDialog(this.getShell(), SWT.OPEN);
		fd.setFilterNames(new String[]{Messages.DataModelPropertyPage_XmlFile_FilterName});
		fd.setFilterExtensions(new String[]{"*.xml"});; //$NON-NLS-1$
		
		String file = fd.open();
		if (file == null){
			//nothing selected
			return;
		}
		final File f = new File(file);
		if (!f.exists()){
			MessageDialog.openError(getShell(), Messages.DataModelPropertyPage_ErrorDialogTitle, MessageFormat.format(Messages.DataModelPropertyPage_FileNotFound, f.toString()));
			return;
		}
		
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try{
						monitor.beginTask(Messages.DataModelPropertyPage_TaskName, 2);
						DataModel sourceDm = ((DataModel) viewer.getInput());
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress1);
						DataModelXmlToSmartConverter converter = new DataModelXmlToSmartConverter();
						DataModel targetDm = converter.convert(f, currentCa, false);
						monitor.worked(1);
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress2);
						DataModelMergeAndUpdater updater = new DataModelMergeAndUpdater(sourceDm, targetDm, currentCa);
						
						final List<String> warnings = updater.merge(new SubProgressMonitor(monitor, 1));
						
						//add any new objects that are not saved via relationships
						for (Attribute a : sourceDm.getAttributes()){
							if (a.getUuid() == null){
								session.save(a);
							}
						}
						for (Category c : sourceDm.getCategories()){
							if (c.getUuid() == null){
								session.save(c);
							}
						}
						
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								setChangesMade(true);
								if (warnings.size() > 0){
									WarningDialog wd = new WarningDialog(getShell(), 
											Messages.DataModelPropertyPage_WarningDialogTitle, Messages.DataModelPropertyPage_WarningsMessage, warnings);
									wd.open();
								}
								viewer.refresh();
								//update selection to refresh panel
								viewer.setSelection(viewer.getSelection());
							}	
						});
						
						monitor.done();
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.DataModelPropertyPage_MergerError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
								closeAndReopen();
							}	
						});
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.DataModelPropertyPage_MergerError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			closeAndReopen();
		}
	}
	
	private void closeAndReopen(){
		Shell parent = getParentShell();
		DataModelPropertyPage.this.close();
		(new ShowDataModelPropertyPageHandler()).execute(parent);
	}
	
	/**
	 * Exports the current model as defined (not necessary saved) to an xml file.
	 */
	private void exportXml(){
		FileDialog fd = new FileDialog(this.getShell(), SWT.SAVE);
		fd.setFilterNames(new String[]{Messages.DataModelPropertyPage_XmlFile_FilterName});
		fd.setFilterExtensions(new String[]{"*.xml"});; //$NON-NLS-1$
		
		String file = fd.open();
		if (file == null){
			//nothing selected
			return;
		}
		final File f = new File(file);
		if (f.exists()){
			if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_OverwriteFile_DialogTitle, 
					MessageFormat.format(Messages.DataModelPropertyPage_OverwriteFile_DialogMessage, new Object[]{ f.getName()}))){
				return;
			}
		}
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try{
						monitor.beginTask(Messages.DataModelPropertyPage_Progress_ExportingXml, 2);
						DataModel dm = ((DataModel) viewer.getInput());
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress_ConvertingXml);
						org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = DataModelSmartToXmlConverter.convert(dm, monitor);
						monitor.worked(1);
						
						monitor.subTask(Messages.DataModelPropertyPage_Progress_WritingXml);
						try(FileOutputStream fout = new FileOutputStream(f)){
							XmlSmartDataModelManager.writeDataModel(xml,fout);
						}
						monitor.done();
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(pmd.getShell(), Messages.DataModelPropertyPage_ExportSuccess_DialogTitle, Messages.DataModelPropertyPage_ExportSuccess_DialogMessage);
							}});
						
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_XmlExport, ex);
							}});
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_XmlExport, ex);
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
		final Session s = getSession();
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						currentTransaction.commit();
						DataModelManager.INSTANCE.fireChangeListeners();
						
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
			SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_SavingDataModel + ex.getLocalizedMessage(), ex);
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
			DataModelManager.INSTANCE.fireEnabledStateListener(getSession(), o);
		}else if (o instanceof CategoryAttribute){
			((DataModel)viewer.getInput()).disableAttribute((CategoryAttribute)o, !((CategoryAttribute)o).getIsActive());
			DataModelManager.INSTANCE.fireEnabledStateListener(getSession(), o);
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
			dialog.run(true, true, runnable);		
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_Unknown + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			try{
				if (currentTransaction != null && currentTransaction.isActive()){
					currentTransaction.rollback();
					currentTransaction = null;
				}
			}catch (Exception ex2){
				SmartPlugIn.log("Error in data model dialog", ex2); //$NON-NLS-1$
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
			boolean ret = MessageDialog.openConfirm(getShell(), DELETE_DIALOG_TITLE, Messages.DataModelPropertyPage_ConfirmDeleteCategory_DialogMessage + cat.getFullCategoryName(getLanguage()));
			if (!ret){
				return;
			}
			
			runInProgressDialog(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
					boolean delete = false;
					try{
						delete = DataModelManager.INSTANCE.validateDelete(cat, monitor, getSession());
					
						DataModelManager.INSTANCE.fireDeleteListener(getSession(), cat);
						
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
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable() {
								
							@Override
							public void run() {
								MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.DataModelPropertyPage_DeleteErrorDialogTitle, ex.getLocalizedMessage());
							}
						});
					}	
				}
			});
			
		}else if (o instanceof CategoryAttribute){
			final CategoryAttribute catAtt  = (CategoryAttribute)o;
			String attributeName = catAtt.getAttribute().findNameNull(getLanguage());
			if (attributeName == null){
				attributeName = catAtt.getAttribute().findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			boolean ret = MessageDialog.openConfirm(getShell(), DELETE_DIALOG_TITLE, MessageFormat.format(Messages.DataModelPropertyPage_ConfirmDeleteCatAtt_DialogMessage, new Object[]{catAtt.getCategory().getFullCategoryName(getLanguage()), attributeName}));
			if (!ret){
				return;
			}
			runInProgressDialog(new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try{
						boolean delete = DataModelManager.INSTANCE.validateDelete(catAtt, monitor, getSession());
						if (delete){
							DataModelManager.INSTANCE.fireDeleteListener(getSession(), catAtt);
							
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
							boolean canDeleteAttribute = false;
							try{
								if (DeleteManager.canDelete(catAtt.getAttribute(), getSession())){
									canDeleteAttribute = true;
								}
							}catch (Exception ex){
								//something is using this attribute therefore
								//it cannot be deleted
							}
							
							if (canDeleteAttribute){
								final int[] ret = {-1};
								Display.getDefault().syncExec(new Runnable(){

									@Override
									public void run() {
										MessageDialog dialog = new MessageDialog(getShell(), DELETE_DIALOG_TITLE, null,
												MessageFormat.format(Messages.DataModelPropertyPage_Confirm_DeleteAttribute, new Object[]{ catAtt.getAttribute().getName() } ), 
												MessageDialog.CONFIRM, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 1);
										ret[0] = dialog.open();
									}});
								
								
								
								
								if (ret[0] == 0){  //YES
									delete = DataModelManager.INSTANCE.validateDelete(catAtt.getAttribute(), monitor, getSession());
									if (delete){
										DataModelManager.INSTANCE.fireDeleteListener(getSession(), catAtt.getAttribute());
										
										dataModel.getAttributes().remove(catAtt.getAttribute());
										if (catAtt.getAttribute().getUuid() != null){
											getSession().delete(catAtt.getAttribute());
										}
									}
								}
							}
						}
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable() {
								
							@Override
							public void run() {
								MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.DataModelPropertyPage_DeleteErrorDialogTitle, ex.getLocalizedMessage());
							}
						});
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
		newCat.setConservationArea(currentCa);
		newCat.setIsActive(true);
		newCat.setChildren(new ArrayList<Category>());
		newCat.setIsMultiple(true);
		
		CategoryDialogPage dd = new CategoryDialogPage(getShell(), newCat, siblings, currentCa.getDefaultLanguage());
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
		}
		newCat.updateHkey();
		DataModelManager.INSTANCE.fireAddListener(getSession(), newCat);
		session.flush();
		
		if (newCat.getParent() != null){
			viewer.setExpandedState(newCat.getParent(), true);
		}
		refreshTree();
		setChangesMade(true);
	}

	/*
	 * modifies category or attribute
	 */
	private void editElement(){
		Object o = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (o instanceof Category){
			try{
				String canEdit = DataModelManager.INSTANCE.canEdit((Category)o, session);
				if (canEdit != null){
					if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_EditWarningTitle, Messages.DataModelPropertyPage_CategoryWarningMessage + "\n\n" + canEdit + "\n\n" + Messages.DataModelPropertyPage_ContinueLabel)){  //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.DataModelPropertyPage_CannotEditoCategory + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				return;
			}
			
			List<Category> siblings = null;
			if (((Category) o).getParent() != null){
				siblings = ((Category) o).getParent().getChildren();
			}else{
				siblings = dataModel.getCategories();
			}
			CategoryDialogPage dd = new CategoryDialogPage(getShell(), (Category)o, siblings, getLanguage());
			int ret = dd.open();
			
			if (ret == Window.CANCEL){
				return;
			}
			refreshTree();
		}else if (o instanceof CategoryAttribute){
			try{
				String canEdit = DataModelManager.INSTANCE.canEdit(((CategoryAttribute)o).getAttribute(), session);
				if (canEdit != null){
					if (!MessageDialog.openQuestion(getShell(), Messages.DataModelPropertyPage_EditWarningTitle, Messages.DataModelPropertyPage_AttributeWarningMessage + "\n\n" + canEdit + "\n\n" + Messages.DataModelPropertyPage_ContinueLabel)){  //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.DataModelPropertyPage_CannotEditAttribute + "\n\n" + ex.getMessage(), ex);  //$NON-NLS-1$
				return;
			}

			
			//warn that this affects everywhere this attribute is used.
			
			Set<CategoryAttribute> usages = ((DataModel)viewer.getInput()).findAttribute(((CategoryAttribute)o).getAttribute());
			if (usages.size() > 1){
				MessageDialog.openWarning(getShell(), Messages.DataModelPropertyPage_ModifyWarning_DialogTitle, Messages.DataModelPropertyPage_ModifyWarning_DialogMessage);
			}
			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(),
					((CategoryAttribute) o).getAttribute(),
					((DataModel) viewer.getInput()).getAttributes(),
					getLanguage(), getSession());			
			//show new attribute dialog
			int ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			refreshTree();
			try{
				new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress(){

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.DataModelPropertyPage_ApplyingUpdates, 0);
					getSession().flush();
				}});
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.DataModelPropertyPage_Error_Edit + ex.getLocalizedMessage(), ex);
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
			MessageDialog.openInformation(getShell(), Messages.DataModelPropertyPage_AddAttribute_DialogTitle, Messages.DataModelPropertyPage_AddAttribute_DialogMessage);
		}
		
		//show dialog
		List<CategoryAttribute> newAttributes = new ArrayList<CategoryAttribute>();
		AddAttributeDialog1 d1 = new AddAttributeDialog1(getShell(), parent, (DataModel)viewer.getInput(), getLanguage(), getSession());
		int ret = d1.open();
		if (ret == AddAttributeDialog1.CANCEL){
			return;
		}else if (ret == AddAttributeDialog1.FINISH){
			newAttributes.addAll(d1.getAddedAttributes());
			refreshTree();
		}else if (ret == AddAttributeDialog1.NEXT){
			Attribute att = new Attribute();
			att.setConservationArea(currentCa);
			
			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(), att,
					((DataModel) viewer.getInput()).getAttributes(),
					currentCa.getDefaultLanguage(), getSession());
			
			//show new attribute dialog
			ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			
			DataModel dm = (DataModel)viewer.getInput();
			try{
				newAttributes.add( dm.addNewAttribute(att, parent) );
				//added a new attribute
				DataModelManager.INSTANCE.fireAddListener(session, att);
				session.saveOrUpdate(att);
			}catch (Exception ex){
				SmartPlugIn.displayLog(ex.getMessage(), ex);
			}
			viewer.setExpandedState(parent, true);
			refreshTree();
		}
		
		for (CategoryAttribute newAttribute: newAttributes){
			//added category/attribute links
			DataModelManager.INSTANCE.fireAddListener(session, newAttribute);
		}
		session.flush();
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
			//just a viewer we don't care about siblings
			catInfoPanel.setCategory((Category)o, null, getLanguage());

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
			attInfoPanel.setAttribute( ((CategoryAttribute)o).getAttribute(), null, getLanguage() ); //readonly, don't care about siblings
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
