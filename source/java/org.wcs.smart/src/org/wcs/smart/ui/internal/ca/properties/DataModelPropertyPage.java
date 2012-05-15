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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
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
	
	private TreeViewer viewer;

	private Button btnAddCategory;
	private Button btnAddAttribute;
	
	private CategoryInfoPanel catInfoPanel;
	private AttributeInfoPanel attInfoPanel;
	private Composite infoInnerPanel;
	private Composite emptyComposite;

	private Button btnDisableElement;

	private Button btnModifyElement;
	
	
	private DataModel dataModel = null;
	
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
	
	
	public void setDataModel(DataModel dm){
		this.dataModel = dm;
		if (viewer != null){
			viewer.setInput(this.dataModel);
			viewer.refresh();
		}
	}
	
	@Override
	public boolean  close(){
		boolean canClose = super.close();

		return canClose;
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		
		parent.setLayout(new GridLayout(1, false));		
	
		Composite thisparent = new Composite(parent, SWT.BORDER);
		thisparent.setLayout(new GridLayout(1, false));
		thisparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		
		SashForm comp = new SashForm(thisparent, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/* left data tree */

		viewer = new TreeViewer(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new DataModelContentProvider());
		//TODO: implement language support
		viewer.setLabelProvider(new DataModelLabelProvider(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,true));
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
		buttonPanel.setLayout(new GridLayout(3, false));
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
		
		Group infoPanel = new Group(rightPanel, SWT.SHADOW_ETCHED_IN);
		((Group)infoPanel).setText("Properties");
		
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		infoPanel.setLayout(new GridLayout(1, false));
		
		
		infoInnerPanel = new Composite(infoPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		catInfoPanel = new CategoryInfoPanel(infoInnerPanel, SWT.NONE, false, false, SmartDB.getCurrentConservationArea().getDefaultLanguage()) {
			@Override
			protected List<Category> getSiblings() {
				return null;
			}
		};
		
		attInfoPanel = new AttributeInfoPanel(infoInnerPanel, SWT.NONE, false, false, SmartDB.getCurrentConservationArea().getDefaultLanguage()) {			
			@Override
			public Collection<Attribute> getSiblings() {
				return null;
			}
		};
		
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
		
//		
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
		//exportButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2,1));
//				
		viewer.refresh();
		setMessage("Manage conservation area data model.");
		return thisparent;
	}

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
	
	public Session getSession(){
		if (session == null || !session.isOpen()){
			session = HibernateManager.openSession();
			session.refresh(ca);
		}
		return session;
	}
	
//	private void importXml(){
//		FileDialog fd = new FileDialog(this.getShell(), SWT.OPEN);
//		String file = fd.open();
//		if (file == null){
//			//nothing selected
//			return;
//		}
//		File f = new File(file);
//		
//		try{
//			DataModel newDataModel = DataModelXMLConverter.convert(f);
//			MessageDialog.openInformation(getShell(), "Great!", "Imported Successfully!");
//			viewer.setAutoExpandLevel(3);
//			viewer.setInput(newDataModel);
//			viewer.refresh();
//			viewer.expandToLevel(3);
//		}catch (Exception ex){
//			SmartPlugIn.displayLog(ex.getMessage(), ex);
//		}
//		setChangesMade(true);
//	}
	
	/* (non-Javadoc)
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
					s.beginTransaction();
					try {
						((DataModel)viewer.getInput()).save(s);
						s.getTransaction().commit();
						setChangesMade(false);
					}catch (Exception ex){
						SmartPlugIn.log(null, ex);
						throw new IllegalStateException(ex);
					}
				}
			});
		} catch (Throwable ex) {
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			s.close();
			SmartPlugIn.displayLog(errorShell,"Error saving data model changes.", ex);
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
		refreshTree();
		setChangesMade(true);
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
//				StringBuilder categories = new StringBuilder();
//				for (CategoryAttribute it : usages){
//					categories.append(it.getCategory().findName())
//				}
				MessageDialog.openWarning(getShell(), "Modify Warning", "This attribute is referenced by  multiple categories.  Modifying it will affect all categories that reference this attribute.");
			}
			

			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(), ((CategoryAttribute)o).getAttribute(), ((DataModel)viewer.getInput()).getAttributes(),ca.getDefaultLanguage());			
			//show new attribute dialog
			int ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			refreshTree();
			
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
			
			AddAttributeDialog2 d2 = new AddAttributeDialog2(getShell(), att, ((DataModel)viewer.getInput()).getAttributes(),ca.getDefaultLanguage());
			
			//show new attribute dialog
			ret = d2.open();
			if (ret == Window.CANCEL){
				return;
			}
			att.setConservationArea(ca);
			DataModel dm = (DataModel)viewer.getInput();
			dm.addAttribute(att, parent);
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

			if (((Category)o).getIsActive()){
				btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			}else{
				btnDisableElement.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			}
			btnDisableElement.setEnabled(true);
		}
		if (o instanceof CategoryAttribute){
			attInfoPanel.setAttribute( ((CategoryAttribute)o).getAttribute() );
			((StackLayout)infoInnerPanel.getLayout()).topControl = attInfoPanel;
			btnAddCategory.setEnabled(false);
			btnAddAttribute.setEnabled(false);
			btnModifyElement.setEnabled( ((CategoryAttribute)o).getIsActive());
			
			if (((CategoryAttribute)o).getIsActive()){
				btnDisableElement.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			}else{
				btnDisableElement.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			}
			btnDisableElement.setEnabled(true);
		}
		if (o instanceof DataModelContentProvider.RootNode){
			btnAddCategory.setEnabled(true);
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			btnAddAttribute.setEnabled(false);
			
			btnModifyElement.setEnabled(false);
			btnDisableElement.setEnabled(false);
		}
		infoInnerPanel.layout();
	}
}
