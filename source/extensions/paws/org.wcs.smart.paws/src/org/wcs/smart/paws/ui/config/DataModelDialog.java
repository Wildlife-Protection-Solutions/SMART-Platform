/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.config;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for selecting data model item for simple PAWS 
 * Classification
 * 
 * @author Emily
 *
 */
public class DataModelDialog extends SmartStyledDialog {

	private TreeViewer dmTree;
	private List<ClassificationData> selectedItems;
	
	protected DataModelDialog(Shell parent) {
		super(parent);
	}
	
	@Override
	public Point getInitialSize() {
		Point pnt = new Point(400,500);
		return pnt;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		dmTree = new TreeViewer(c, SWT.BORDER | SWT.MULTI);
		dmTree.setContentProvider(new DataModelContentProvider());
		dmTree.setLabelProvider(new DataModelLabelProvider() {
			private HashMap<Object, Image> images = new HashMap<>();
			
			@Override
			public void dispose() {
				super.dispose();
				for (Image img : images.values()) img.dispose();
				images.clear();
			}
			
			@Override
			public String getText(Object element) {
				if (element instanceof String) return (String)element;
				if (element instanceof DataModelContentProvider.CategoryItemWrapper) {
					DataModelContentProvider.CategoryItemWrapper w = (DataModelContentProvider.CategoryItemWrapper)element;
					if (w.li != null) return super.getText(w.li);
					if (w.node != null) return super.getText(w.node);
				}
				return super.getText(element);
			}
		});
		dmTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dmTree.setInput(DialogConstants.LOADING_TEXT);
		dmTree.addDoubleClickListener(e->{
			okPressed();
		});
		
		loadDm.schedule();
		
		getShell().setText("SMART Data Model");
		return parent;
	}
	
	public void okPressed() {
		selectedItems = new ArrayList<>();
		for (Iterator<?> iterator = dmTree.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object dmObject = (Object) iterator.next();
			PawsSimpleClass item = null;

			String lbl = "";
			
			if (dmObject instanceof Category) {
				Category c = (Category)dmObject;
				
				item = new PawsSimpleClass();
				item.setCategoryHkey(c.getHkey());
				item.setClassification(c.getName().toLowerCase());
				
				lbl = c.getFullCategoryName();
				
			}else if (dmObject instanceof DataModelContentProvider.CategoryItemWrapper) {
				Category c = (( DataModelContentProvider.CategoryItemWrapper)dmObject).c;
				AttributeListItem li = (( DataModelContentProvider.CategoryItemWrapper)dmObject).li;
				AttributeTreeNode node = (( DataModelContentProvider.CategoryItemWrapper)dmObject).node;
				
				item = new PawsSimpleClass();
				item.setCategoryHkey(c.getHkey());
				if (li != null) {
					item.setAttributeKey(li.getAttribute().getKeyId());
					item.setAttributeListItemKey(li.getKeyId());
					item.setClassification(li.getName().toLowerCase() + "_" + li.getAttribute().getName().toLowerCase() + "_" + c.getName().toLowerCase());
					lbl = ClassificationData.createLabel(c,  li.getAttribute(), li);
				}else if (node != null) {
					item.setAttributeKey(node.getAttribute().getKeyId());
					item.setAttributeTreeNodeHkey(node.getHkey());
					item.setClassification(node.getName().toLowerCase() + "_" + node.getAttribute().getName().toLowerCase() + "_" + c.getName().toLowerCase());
					lbl = ClassificationData.createLabel(c,  node.getAttribute(), node);
				}

			}else if (dmObject instanceof CategoryAttribute) {
				MessageDialog.openWarning(getShell(), "Invalid Selection", MessageFormat.format("Attribute cannot be added a classifications.  Please select a category, list item or tree node.", ((CategoryAttribute)dmObject).getAttribute().getName()));
				return;
			}else {
				MessageDialog.openWarning(getShell(), "Invalid Selection", MessageFormat.format("Selected item {0} cannot be added.  Please select a category, list item or tree node.", dmObject.toString()));
				return;
			}
			
			if (item != null) selectedItems.add(new ClassificationData(item, lbl));
		}
		super.okPressed();
	}
	
	public List<ClassificationData> getSelectedItems(){
		return this.selectedItems;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private Job loadDm = new Job("loading datamodel") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			DataModel fdm = QueryDataModelManager.getInstance().getDataModel();
			Display.getDefault().asyncExec(()->{
				dmTree.setInput(fdm);
				dmTree.expandToLevel(2);
			});
			return Status.OK_STATUS;
		}
		
	};
}
