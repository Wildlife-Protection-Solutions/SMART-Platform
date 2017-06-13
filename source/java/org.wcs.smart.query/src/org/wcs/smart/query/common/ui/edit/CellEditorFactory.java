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
package org.wcs.smart.query.common.ui.edit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.celleditor.DateCellEditor;
import org.wcs.smart.common.celleditor.DoubleCellEditor;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.common.celleditor.TimeCellEditor;
import org.wcs.smart.common.celleditor.TreeViewerCellEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Factory for generating cell editors.
 * 
 * @author Emily
 *
 */
public class CellEditorFactory {
	
	public static DoubleCellEditor newDoubleCellEditor(Composite parent) {
		return new DoubleCellEditor(parent);
	}

	public static IntegerCellEditor newIntegerCellEditor(Composite parent) {
		return new IntegerCellEditor(parent);
	}
	
	public static TextCellEditor newTextCellEditor(Composite parent) {
		return new TextCellEditor(parent){
			@Override
			public LayoutData getLayoutData() {
				LayoutData layoutData = super.getLayoutData();
				layoutData.verticalAlignment = SWT.CENTER;
				layoutData.minimumHeight = getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT,true).y;
				return layoutData;
			}
		};
	}

	public static DateCellEditor newDateCellEditor(Composite parent) {
		return new DateCellEditor(parent);
	}
	
	public static TimeCellEditor newTimeCellEditor(Composite parent) {
		return new TimeCellEditor(parent);
	}
	
	/**
	 * Creates a drop down cell editor that opens a dialog for editing observations
	 * 
	 * @param parent
	 * @return
	 */
	public static ObservationDialogCellEditor newObservationEditor(Composite parent){
		return new ObservationDialogCellEditor(parent);
	}
	
	/**
	 * Creates a drop down cell list editor that includes true and false values
	 * @param parent
	 * @return
	 */
	public static ComboBoxViewerCellEditor newBooleanCellEditor(Composite parent){
		
		ComboBoxViewerCellEditor editor = new ComboBoxViewerCellEditor(parent, SWT.READ_ONLY | SWT.DROP_DOWN){
			@Override
			protected void doSetFocus() {
				super.doSetFocus();
				
				//automatically open the bbox on focus
				//https://www.eclipse.org/forums/index.php/t/104515/
				try{
					CCombo comboBox = (CCombo) getControl();				
					Method method = CCombo.class.getDeclaredMethod("dropDown", boolean.class); //$NON-NLS-1$
					method.setAccessible(true);
					method.invoke(comboBox, true);
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}
		};
		editor.setContentProvider(ArrayContentProvider.getInstance());
		editor.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Boolean){
					return element.toString();
				}
				return super.getText(element);
			}
		});
		editor.setInput(new Object[]{Boolean.TRUE, Boolean.FALSE});
		return editor;
	}
	
	
	/**
	 * Creates a list drop down editor for attributes of list types
	 * 
	 * @param parent
	 * @param attributeKey
	 * @return
	 */
	public static ComboBoxViewerCellEditor newAttributeListCellEditor(Composite parent, String attributeKey){
		
		ComboBoxViewerCellEditor listCellEditor = new ComboBoxViewerCellEditor(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		listCellEditor.setContentProvider(ArrayContentProvider.getInstance());
		listCellEditor.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof AttributeListItem){
					return ((AttributeListItem)element).getName();
				}
				return super.getText(element);
			}
		});
		listCellEditor.setInput(new Object[]{DialogConstants.LOADING_TEXT});
			
		Job j = new Job("load list items"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<AttributeListItem> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					final Attribute dmAttribute = (Attribute)s.createCriteria(Attribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.add(Restrictions.eq("keyId", attributeKey)).uniqueResult(); //$NON-NLS-1$
					if (dmAttribute == null) return Status.OK_STATUS;
					if (dmAttribute.getType() != AttributeType.LIST) return Status.OK_STATUS;
					
					items.addAll(dmAttribute.getActiveListItems());
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!listCellEditor.getControl().isDisposed()){
						listCellEditor.setInput(items);
						listCellEditor.recomputeSize();
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		
		return listCellEditor;
	}

	
	/**
	 * Creates a tree drop down editor for attributes of tree types
	 * 
	 * @param parent
	 * @param attributeKey
	 * @return
	 */
	public static TreeViewerCellEditor newAttributeTreeCellEditor(Composite parent, String attributeKey){
		
		TreeViewerCellEditor treeCellEditor = new TreeViewerCellEditor(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		treeCellEditor.setContentProvider(new AttributeTreeContentProvider(true, false));
		
		treeCellEditor.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof AttributeTreeNode){
					return ((AttributeTreeNode)element).getName();
				}
				return super.getText(element);
			}
		});
		treeCellEditor.setInput(DialogConstants.LOADING_TEXT);
			
		Job j = new Job("load tree nodes"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<AttributeTreeNode> items = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					final Attribute dmAttribute = (Attribute)s.createCriteria(Attribute.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
							.add(Restrictions.eq("keyId", attributeKey)).uniqueResult(); //$NON-NLS-1$
					if (dmAttribute == null) return Status.OK_STATUS;
					if (dmAttribute.getType() != AttributeType.TREE) return Status.OK_STATUS;
					
					List<AttributeTreeNode> nodes = new ArrayList<>(dmAttribute.getActiveTreeNodes());
					while(!nodes.isEmpty()){
						AttributeTreeNode toVisit = nodes.remove(0);
						toVisit.getName();
						nodes.addAll(toVisit.getActiveChildren());
					}
					items.addAll(dmAttribute.getActiveTreeNodes());
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					if (!treeCellEditor.getControl().isDisposed()){
						treeCellEditor.setInput(items);
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		
		return treeCellEditor;
	}
}
