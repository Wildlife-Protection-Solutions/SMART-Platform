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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.dialog.composite.CmListItemLabelProvider;
import org.wcs.smart.dataentry.dialog.composite.DisplayModeComboViewer;
import org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl;
import org.wcs.smart.dataentry.dialog.composite.ImageSelectionControl.IImageContentProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Rename dialog for providing aliases for configurable model list attribute items.
 * 
 * @author Emily
 * @author Evgeniy
 *
 */
public class EditListDialog extends TitleAreaDialog{

	protected CmAttribute attribute;
	protected ConfigurableModel editModel;
	
	private Viewer itemViewer;
	private TableViewer nameTable ;
	
	private NamedItem dmNode;
	private CmAttributeListItem cmNode;
	
	private Button btnEnable;
	private ImageSelectionControl imageControl;
	
	public EditListDialog(Shell parentShell, CmAttribute attribute, ConfigurableModel editModel) {
		super(parentShell);
		this.attribute = attribute;
		this.editModel = editModel;
	}

	protected Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		
		setTitle(attribute.getName());
		setMessage(Messages.RenameListDialog_DialogMessage);
		getShell().setText(Messages.ConfigurableModelEditDialog_Title);
		
		Composite container = new Composite(main, SWT.NONE);
		GridLayout cgd = new GridLayout(1, false);
		cgd.marginBottom=0;
		cgd.marginHeight = 0;
		cgd.marginLeft = 0;
		cgd.marginRight = 0;
		cgd.marginTop = 0;
		cgd.marginWidth = 0;
		container.setLayout(cgd);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite upperConlrolsCmp = new Composite(container, SWT.NONE);
		upperConlrolsCmp.setLayout(new GridLayout(2, false));
		upperConlrolsCmp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));

		org.eclipse.swt.widgets.Label label = new org.eclipse.swt.widgets.Label(upperConlrolsCmp, SWT.NONE);
		label.setText(Messages.EditListDialog_DisplayMode);
		final DisplayModeComboViewer modeViewer = new DisplayModeComboViewer(upperConlrolsCmp);
		modeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		modeViewer.setSelection(new StructuredSelection(attribute.getCurrentDisplayMode() != null ? attribute.getCurrentDisplayMode() : DisplayMode.DEFAULT_DISPLAY_MODE));
		modeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				attribute.setCurrentDisplayMode(modeViewer.getSelectedDisplayMode());
				//we need to save either configurable model global setting (for default configuration)
				//or attribute option (for custom configuration), this is way we try to save both below
				attribute.getCmAttributeOptions().get(CmAttributeOption.ID_DISPLAY_MODE);
				editModel.getAttributeSettings().get(attribute.getAttribute());
			}
		});
		
		SashForm comp = new SashForm(container, SWT.HORIZONTAL);
		comp.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		Composite left = new Composite(comp, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = gl.marginWidth = 0;
		left.setLayout(gl);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		itemViewer = createItemViewer(left);
		
		Composite btnPanel = new Composite(left, SWT.NONE);
		GridLayout gla = new GridLayout();
		gla.marginHeight = gla.marginWidth = 0;
		btnPanel.setLayout(gla);
		btnPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,false, false));
		
		btnEnable = new Button(btnPanel, SWT.PUSH);
		GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd.verticalIndent = 2;
		gd.horizontalIndent = 2;
		btnEnable.setLayoutData(gd);
		btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnEnable.setEnabled(false);
		btnEnable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enable = true;
				if (btnEnable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
					enable = false;
				}
				IStructuredSelection selection = (IStructuredSelection) itemViewer.getSelection();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object type = iterator.next();
					if (type instanceof NamedItem){
						enableItem((NamedItem)type, enable);
					}
					
				}				
				itemViewer.refresh();
				updateEnableButtonText();
			}
		});
		super.setButtonLayoutData(btnEnable);

		Composite right = new Composite(comp, SWT.NONE);
		GridLayout gr = new GridLayout();
		gr.marginHeight = gr.marginWidth = 0;
		right.setLayout(gr);
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createNameTable(right);
		createListItemConfigControls(right);
		
		comp.setWeights(new int[]{35,65});
		itemViewer.refresh();
		
		
		return main;
	}
	
	/**
	 * Creates a table with one row for each language.
	 * 
	 * @param parent
	 */
	private void createNameTable(Composite parent) {
		nameTable = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		nameTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		nameTable.setContentProvider(ArrayContentProvider.getInstance());
		nameTable.getTable().setHeaderVisible(true);
		nameTable.getTable().setLinesVisible(true);
		
		TableViewerColumn colLang = new TableViewerColumn(nameTable, SWT.NONE);
		colLang.getColumn().setWidth(100);
		colLang.getColumn().setText(Messages.AbstractRenameDialog_LanguageColumnName);
		colLang.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					return ((Language) element).getDisplayName();
				}
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colName = new TableViewerColumn(nameTable, SWT.NONE);
		colName.getColumn().setWidth(150);
		colName.getColumn().setText(Messages.AbstractRenameDialog_ConfiguredName);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (cmNode != null){
						String label = cmNode.findNameNull((Language) element);
						if (label != null){
							return label;
						}
					}
					if (dmNode != null){
						return dmNode.findName((Language) element);
					}
					return ""; //$NON-NLS-1$
				}
			  	return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (cmNode == null || cmNode.findNameNull((Language)element) == null){
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
				return null;
			}
		});
		
		colName.setEditingSupport(new EditingSupport(nameTable) {
			private TextCellEditor editor =  new TextCellEditor(nameTable.getTable());
		
			@Override
			protected void setValue(Object element, Object value) {
				Language lang = (Language)element;
				String newValue = (String)value;
				
				if (newValue.trim().length() == 0){
					
					if (cmNode != null){
						for (Iterator<Label> iterator = cmNode.getNames().iterator(); iterator.hasNext();) {
							Label l = iterator.next();
							if (l.getLanguage().equals(lang)){
								iterator.remove();
							}
						}
					}
				}else if(!dmNode.findName(lang).equals(newValue)){
					if (cmNode != null){
						cmNode.updateName(((Language)element), (String)value);
					}
				}				
				nameTable.refresh();
				itemViewer.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				if (cmNode != null){
					String label = cmNode.findNameNull(((Language)element));
					if (label != null){
						return label;
					}
				}
				if (dmNode != null){
					return dmNode.findName(((Language)element));
				}
				return ""; //$NON-NLS-1$
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn dmName = new TableViewerColumn(nameTable, SWT.NONE);
		dmName.getColumn().setWidth(150);
		dmName.getColumn().setText(Messages.AbstractRenameDialog_DataModelColumnName);
		dmName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (dmNode != null){
						return dmNode.findName((Language) element);
					}else{
						return ""; //$NON-NLS-1$
					}
				}
			  	return super.getText(element);
			}
		});
		
		nameTable.setInput(SmartDB.getCurrentConservationArea().getLanguages());
		nameTable.getTable().setEnabled(false);
	}
	
	private void createListItemConfigControls(Composite parent) {
		Composite imgCmp = new Composite(parent, SWT.NONE);
		GridLayout imgLayout = new GridLayout(2, false);
		imgLayout.marginHeight = imgLayout.marginWidth = 0;
		imgCmp.setLayout(imgLayout);
		imgCmp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		org.eclipse.swt.widgets.Label imgLbl = new org.eclipse.swt.widgets.Label(imgCmp, SWT.NONE);
		imgLbl.setText(Messages.EditListDialog_Image);
		imageControl = new ImageSelectionControl(imgCmp, new IImageContentProvider() {
			@Override
			public File getImageFile() {
				return cmNode != null ? cmNode.getImageFile() : null;
			}

			@Override
			public void setImageFile(File file) {
				if (cmNode != null) {
					cmNode.setImageFile(file);
					imageControl.redrawCanvas();
				}
			}
		});
	}
	
	

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		p.y = Math.max(p.y, 450);
		return p;
	}
	
	/**
	 * only have a ok button here; cannot cancel
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	/**
	 * Sets the current selection from the item viewer.  If null
	 * the name table will be disabled.
	 * 
	 * @param dmNode
	 * @param cmNode
	 */
	public void setCurrentSelection(NamedItem dmNode, CmAttributeListItem cmNode){
		this.dmNode = dmNode;
		this.cmNode = cmNode;
		nameTable.refresh();
		
		nameTable.getTable().setEnabled(dmNode != null);
		btnEnable.setEnabled(dmNode != null);
		updateEnableButtonText();
		imageControl.redrawCanvas();
	}
	
	private void updateEnableButtonText(){

		if (this.cmNode == null || this.cmNode.getIsActive()){
			btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}else{
			btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}
	}
	
	/**
	 * Creates the viewer for the list.
	 * @param parent
	 * @return
	 */
	protected Viewer createItemViewer(Composite parent) {
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tableComp.getLayoutData()).heightHint = 300;
		
		final TableViewer listViewer = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(new CmListItemLabelProvider(editModel));
		listViewer.setInput(attribute.getCurrentList());
		
		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection) listViewer.getSelection()).getFirstElement();
				AttributeListItem currentNode = null;
				CmAttributeListItem currentCmNode = null;
				if (x instanceof AttributeListItem) {
					currentNode = (AttributeListItem) x;
					currentCmNode = getConfiguredNode(x);
				}
				if (x instanceof CmAttributeListItem) {
					currentCmNode = (CmAttributeListItem) x;
					currentNode = currentCmNode.getListItem();
				}
				EditListDialog.this.setCurrentSelection(currentNode, currentCmNode);
			}
		});
		
		TableColumnLayout ll = new TableColumnLayout();
		ll.setColumnData(new TableColumn(listViewer.getTable(),SWT.NONE), new ColumnWeightData(100));
		tableComp.setLayout(ll);
		
		return listViewer;
	}

	private CmAttributeListItem getConfiguredNode(Object x){
		if (x instanceof CmAttributeListItem) {
			return (CmAttributeListItem) x;
		}
		
		if (x instanceof AttributeListItem) {
			Session s = HibernateManager.openSession();
			try{
				AttributeListItem tmp = (AttributeListItem) x;
				List<?> items = s.createCriteria(CmAttributeListItem.class)
					.add(Restrictions.eq("listItem", tmp))  //$NON-NLS-1$
					.add(Restrictions.eq("configurableModel", editModel)).list(); //$NON-NLS-1$
				if (items.size() > 0) {
					return (CmAttributeListItem) items.get(0);
				}
			}finally{
				s.close();
			}
		}
		return null;
	}
	
	/**
	 * Enable or disable the configured node associated with the given data model node.
	 * 
	 * @param dmNode
	 * @param enable
	 */
	protected void enableItem(NamedItem dmNode, boolean enable){
		CmAttributeListItem item = getConfiguredNode(dmNode);
		if (item != null){
			item.setIsActive(enable);
		}
	}
	
}
