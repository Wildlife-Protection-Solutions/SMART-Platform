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

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ChangeTracker;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Rename dialog for providing aliases for configurable model tree and list attribute items 
 * @author Emily
 *
 */
public abstract class AbstractRenameDialog extends TitleAreaDialog{

	protected CmAttribute attribute;
	protected ConfigurableModel editModel;
	protected ChangeTracker tracker;
	
	private Viewer itemViewer;
	private TableViewer nameTable ;
	
	private NamedItem dmNode;
	private CmAttributeItem cmNode;
	
	private Button btnEnable;
	
	public AbstractRenameDialog(Shell parentShell, CmAttribute attribute, ConfigurableModel editModel, ChangeTracker tracker) {
		super(parentShell);
		this.attribute = attribute;
		this.tracker = tracker;
		this.editModel = editModel;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		setTitle(attribute.getName());
		setMessage(getDialogMessage());
		getShell().setText(Messages.ConfigurableModelEditDialog_Title);
		
		SashForm comp = new SashForm(parent, SWT.HORIZONTAL);
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
		
		createNameTable(comp);
		
		comp.setWeights(new int[]{35,65});
		itemViewer.refresh();
		
		
		return parent;
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
				if (cmNode != null){
					tracker.saveOrUpdate(cmNode);
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
	public void setCurrentSelection(NamedItem dmNode, CmAttributeItem cmNode){
		this.dmNode = dmNode;
		this.cmNode = cmNode;
		nameTable.refresh();
		
		nameTable.getTable().setEnabled(dmNode != null);
		btnEnable.setEnabled(dmNode != null);
		updateEnableButtonText();
	}
	
	private void updateEnableButtonText(){

		if (this.cmNode == null || this.cmNode.getIsActive()){
			btnEnable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}else{
			btnEnable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}
	}
	
	/**
	 * Creates the viewer for the tree or list.
	 * @param parent
	 * @return
	 */
	protected abstract Viewer createItemViewer(Composite parent);
	
	/**
	 * The dialog message
	 * @return
	 */
	protected abstract String getDialogMessage();
	
	/**
	 * Enable or disable the configured node associated with the given data model node.
	 * 
	 * @param dmNode
	 * @param enable
	 */
	protected abstract void enableItem(NamedItem dmNode, boolean enable);
	
}
