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

import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.composite.CmTreeLabelProvider;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;

/**
 * Rename dialog for cm tree attribute
 * @author Emily
 *
 */
public class RenameTreeDialog extends TitleAreaDialog{

	private Attribute attribute;
	private Session currentSession;
	private TreeViewer tree;
	private TableViewer names;
	
	private AttributeTreeNode currentNode;
	private CmAttributeTreeNode currentCmNode;
	private ConfigurableModel editModel;
	
	public RenameTreeDialog(Shell parentShell, Attribute attribute, ConfigurableModel editModel, Session currentSession) {
		super(parentShell);
		this.attribute = attribute;
		this.currentSession = currentSession;
		this.editModel = editModel;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		setTitle(attribute.getName());
		setMessage("Rename tree nodes.  This affects all places the attribute is used in the configurable model.");
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, true));
		comp.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		tree = new TreeViewer(comp);
		tree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setContentProvider(new AttributeTreeContentProvider(true, false));
		tree.setLabelProvider(new CmTreeLabelProvider(currentSession));
		tree.setInput(attribute.getTree());
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((StructuredSelection)tree.getSelection()).getFirstElement();
				currentNode = null;
				currentCmNode = null;
				if (x instanceof AttributeTreeNode){
					currentNode = (AttributeTreeNode) x;
					List items = currentSession.createCriteria(CmAttributeTreeNode.class).add(Restrictions.eq("dmTreeNode", currentNode)).list();
					if (items.size() > 0){
						currentCmNode = (CmAttributeTreeNode) items.get(0);
					}
				}
				names.refresh();
				
			}
		});
		
		names = new TableViewer(comp, SWT.FULL_SELECTION | SWT.BORDER);
		names.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		names.setContentProvider(ArrayContentProvider.getInstance());
		names.getTable().setHeaderVisible(true);
		names.getTable().setLinesVisible(true);
		
		TableViewerColumn colLang = new TableViewerColumn(names, SWT.NONE);
		colLang.getColumn().setWidth(100);
		colLang.getColumn().setText("Language");
		colLang.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					return ((Language) element).getDisplayName();
				}
			  	return super.getText(element);
			}

		});
		
		TableViewerColumn colName = new TableViewerColumn(names, SWT.NONE);
		colName.getColumn().setWidth(150);
		colName.getColumn().setText("Configured Name");
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (currentCmNode != null){
						String label = currentCmNode.findNameNull((Language) element);
						if (label != null){
							return label;
						}
					}
					if (currentNode != null){
						return currentNode.findName((Language) element);
					}
					return "";
				}
			  	return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (currentCmNode == null || currentCmNode.findNameNull((Language)element) == null){
					return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				}
				return null;
			}
		});
		
		colName.setEditingSupport(new EditingSupport(names) {
			private TextCellEditor editor =  new TextCellEditor(names.getTable());
		
			@Override
			protected void setValue(Object element, Object value) {
				Language lang = (Language)element;
				String newValue = (String)value;
				
				if (newValue.trim().length() == 0){
					
					if (currentCmNode != null){
						for (Label l : currentCmNode.getNames()){
							if (l.getLanguage().equals(lang)){
								currentCmNode.getNames().remove(l);
								l.setElement(null);
								l.setLanguage(null);
								currentSession.delete(l);
							}
						}
					}
				}else if(!currentNode.findName(lang).equals(newValue)){
					
					if (currentCmNode == null){
						currentCmNode = new CmAttributeTreeNode();
						currentCmNode.setConfigurableModel(editModel);
						currentCmNode.setDmTreeNode(currentNode);
						currentSession.save(currentCmNode);
					}
					currentCmNode.updateName(((Language)element), (String)value);
					for (Label l : currentCmNode.getNames()){
						currentSession.saveOrUpdate(l);
					}
					currentSession.saveOrUpdate(currentCmNode);
					currentSession.flush();
					
					
				}
				names.refresh();
				tree.refresh();
			}
			
			@Override
			protected Object getValue(Object element) {
				if (currentCmNode != null){
					String label = currentCmNode.findNameNull(((Language)element));
					if (label != null){
						return label;
					}
				}
				if (currentNode != null){
					return currentNode.findName(((Language)element));
				}
				return "";
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
		
		TableViewerColumn dmName = new TableViewerColumn(names, SWT.NONE);
		dmName.getColumn().setWidth(150);
		dmName.getColumn().setText("Data Model Name");
		dmName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Language){
					if (currentNode != null){
						return currentNode.findName((Language) element);
					}else{
						return "";
					}
				}
			  	return super.getText(element);
			}
		});
		
		
		
		names.setInput(SmartDB.getCurrentConservationArea().getLanguages());
		
		return parent;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
