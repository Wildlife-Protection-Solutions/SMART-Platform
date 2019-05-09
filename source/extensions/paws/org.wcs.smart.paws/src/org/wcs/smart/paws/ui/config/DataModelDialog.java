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
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

import com.ibm.icu.text.MessageFormat;

public class DataModelDialog extends SmartStyledDialog {

	private TreeViewer dmTree;
	private IconSet iset;
	
	private List<PawsSimpleClass> selectedItems;
	
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
			
			@Override
			public Image getImage(Object element) {
				if (iset == null) return super.getImage(element);
				if (images.containsKey(element)) return images.get(element);
				
				if (element instanceof CategoryAttribute) {
					element = ((CategoryAttribute)element).getAttribute();
				}
				if (element instanceof DataModelContentProvider.CategoryItemWrapper) {
					DataModelContentProvider.CategoryItemWrapper w = (DataModelContentProvider.CategoryItemWrapper)element;
					if (w.li != null) element = w.li;
					if (w.node != null) element = w.node;
				}
				if (element instanceof DmObject) {
					Image img = null;
					DmObject d = (DmObject)element;
					if (d.getIcon() != null && d.getIcon().getIconFile(iset) != null) {
						img =  SmartUtils.getImage(d.getIcon().getIconFile(iset).getAttachmentFile().toPath(), 16);
						images.put(element, img);
					}
					if (img == null) {
						img = super.getImage(element);
					}
					
					return img;
				}
				return null;
			}
		});
		dmTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dmTree.setInput(DialogConstants.LOADING_TEXT);
		dmTree.addDoubleClickListener(e->{
			okPressed();
		});
		
		loadDm.schedule();
		return parent;
	}
	
	public void okPressed() {
		selectedItems = new ArrayList<>();
		for (Iterator<Object> iterator = dmTree.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object dmObject = (Object) iterator.next();
			PawsSimpleClass item = null;
			if (dmObject instanceof Category) {
				Category c = (Category)dmObject;
				
				item = new PawsSimpleClass();
				item.setCategory(c);
				item.setClassification(c.getName().toLowerCase());
			}else if (dmObject instanceof DataModelContentProvider.CategoryItemWrapper) {
				Category c = (( DataModelContentProvider.CategoryItemWrapper)dmObject).c;
				AttributeListItem li = (( DataModelContentProvider.CategoryItemWrapper)dmObject).li;
				AttributeTreeNode node = (( DataModelContentProvider.CategoryItemWrapper)dmObject).node;
				
				item = new PawsSimpleClass();
				item.setCategory(c);
				if (li != null) {
					item.setAttribute(li.getAttribute());
					item.setClassification(li.getName().toLowerCase() + "_" + li.getAttribute().getName().toLowerCase() + "_" + c.getName().toLowerCase());
				}else if (node != null) {
					item.setAttribute(node.getAttribute());
					item.setClassification(node.getName().toLowerCase() + "_" + node.getAttribute().getName().toLowerCase() + "_" + c.getName().toLowerCase());
				}
				
			}else if (dmObject instanceof CategoryAttribute) {
				MessageDialog.openWarning(getShell(), "Invalid Selection", MessageFormat.format("Attribute cannot be added a classifications.  Please select a category, list item or tree node.", ((CategoryAttribute)dmObject).getAttribute().getName()));
				return;
			}else {
				MessageDialog.openWarning(getShell(), "Invalid Selection", MessageFormat.format("Selected item {0} cannot be added.  Please select a category, list item or tree node.", dmObject.toString()));
				return;
			}
			
			if (item != null) selectedItems.add(item);
		}
		super.okPressed();
	}
	
	public List<PawsSimpleClass> getSelectedItems(){
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

		private void loadIcon(DmObject dm, Session session) {
			if (iset == null) return;
			if (dm.getIcon() != null && dm.getIcon().getIconFile(iset) != null) dm.getIcon().getIconFile(iset).computeFileLocation(session);

		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			DataModel dm = null;
			try(Session session = HibernateManager.openSession()){
				dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
				
				iset = QueryFactory.buildQuery(session, IconSet.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"isDefault", true}).uniqueResult();
				
				//lazy load
				List<Category> toVisit = new ArrayList<>();
				toVisit.addAll(dm.getActiveCategories());
				while(!toVisit.isEmpty()){
					Category c = toVisit.remove(0);
					toVisit.addAll(c.getActiveChildren());
					loadIcon(c, session);
					if (c.getAttributes() != null) {
						for(CategoryAttribute ca: c.getAttributes()) {
							ca.getAttribute().getName();
							loadIcon(ca.getAttribute(), session);
							if (ca.getAttribute().getAttributeList() != null) {
								for (AttributeListItem li : ca.getAttribute().getAttributeList()) {
									li.getName();
									loadIcon(li, session);
								}
							}
							if (ca.getAttribute().getTree() != null) {
								List<AttributeTreeNode> nodes = new ArrayList<>(ca.getAttribute().getActiveTreeNodes());
								while(!nodes.isEmpty()) {
									AttributeTreeNode node = nodes.remove(0);
									node.getName();
									loadIcon(node, session);
									if (node.getChildren() != null) nodes.addAll(node.getActiveChildren());
								}
							}
						}
					}
				}
			}
			
			DataModel fdm = dm;
			Display.getDefault().asyncExec(()->{
				dmTree.setInput(fdm);
				dmTree.expandToLevel(2);
			});
			return Status.OK_STATUS;
		}
		
	};
}
