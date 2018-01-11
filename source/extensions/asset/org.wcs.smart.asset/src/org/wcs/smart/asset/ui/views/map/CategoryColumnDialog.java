/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.map.engine.IFilter;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.TreeEditorField;

/**
 * Dialog for configuring asset over map columns that count incidents based
 * on category/attribute filters.
 * 
 * @author Emily
 *
 */
public class CategoryColumnDialog extends TitleAreaDialog {

	private Text txtName;
	
	private Text txtAttributeFilters;
	private TreeEditorField<Category> field;
	private TreeViewer attributeOptions;
	
	private CategoryOverviewColumn newColumn = null;
	private CategoryOverviewColumn toUpdate = null;
	
	/**
	 * Creates a new dialog for creating a new columns
	 * @param parentShell
	 */
	public CategoryColumnDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Creates a new dialog for editing an existing column
	 * @param parentShell
	 * @param toUpdate
	 */
	public CategoryColumnDialog(Shell parentShell, CategoryOverviewColumn toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
	}
	
	/**
	 * 
	 * @return the new column created; this will return null if we are updating a column
	 */
	public CategoryOverviewColumn getNewColumn() {
		return newColumn;
	}
	
	private void enableOk(boolean enabled) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) ok.setEnabled(enabled);
	}
	
	private boolean validate() {
		if (txtName.getText().trim().isEmpty()) {
			setErrorMessage("A column name must be provided");
			enableOk(false);
			return false;
		}
		if (field.getValue() == null) {
			setErrorMessage("A category must be selected");
			enableOk(false);
			return false;
		}
		
		String text = txtAttributeFilters.getText().trim();
		if (!text.isEmpty()) {
			
			try(InputStream is = new ByteArrayInputStream(text.getBytes())){
				Parser parser = new Parser(is);
				IFilter f = parser.AttributeExpression();
				
				System.out.println("OK: " +f.toString());
			} catch (Exception e) {
				e.printStackTrace();
				setErrorMessage(e.getMessage());
				enableOk(false);
				return false;
			}
		}
		enableOk(true);
		setErrorMessage(null);
		return true;
	}
	
	@Override
	public void cancelPressed() {
		newColumn = null;
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		if (!validate()) return;
		
		if (toUpdate == null) {
			newColumn = new CategoryOverviewColumn(txtName.getText().trim(), field.getValue().getHkey(), txtAttributeFilters.getText());
		}else {
			toUpdate.updateValues(txtName.getText().trim(), field.getValue().getHkey(), txtAttributeFilters.getText());
		}
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	
		Label l = new Label(parent, SWT.NONE);
		l.setText("Name:");
		
		txtName = new Text(parent, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->validate());
		l = new Label(parent, SWT.NONE);
		l.setText("Category:");
		
		//simple content provider to show loading text
		IContentProvider provider = new ITreeContentProvider() {
			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return new String[] {DialogConstants.LOADING_TEXT};
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		};
		
		field = new TreeEditorField<Category>();
		field.createComposite(parent, provider, new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Category) return ((Category) element).getName();
				return super.getText(element);
			}
		});
		parent.addListener(SWT.Dispose, e->field.dispose());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Attribute Filters:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite attributeFilters = new Composite(parent, SWT.NONE);
		attributeFilters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		attributeFilters.setLayout(new GridLayout(2, true));
		
		txtAttributeFilters = new Text(attributeFilters, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		txtAttributeFilters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtAttributeFilters.addListener(SWT.Modify, e->validate());
		
		attributeOptions = new TreeViewer(attributeFilters, SWT.FULL_SELECTION  | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		attributeOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeOptions.setContentProvider(new ITreeContentProvider() {
			List<Attribute> attributes = null;
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				attributes = (List<Attribute>) newInput;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (attributes == null) return null;
				return attributes.toArray();
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Attribute) {
					if (((Attribute) parentElement).getType() == AttributeType.LIST) {
						return ((Attribute) parentElement).getActiveListItems().toArray(); 
					}else if (((Attribute) parentElement).getType() == AttributeType.TREE) {
						return ((Attribute) parentElement).getActiveTreeNodes().toArray();
					}
				}else if (parentElement instanceof AttributeTreeNode) {
					return ((AttributeTreeNode) parentElement).getActiveChildren().toArray();
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof Attribute) return null;
				if (element instanceof AttributeListItem) return ((AttributeListItem)element).getAttribute();
				if (element instanceof AttributeTreeNode) {
					if (((AttributeTreeNode) element).getParent() == null) return ((AttributeTreeNode) element).getAttribute();
					return ((AttributeTreeNode)element).getParent();
				}
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof AttributeTreeNode) return !((AttributeTreeNode) element).getActiveChildren().isEmpty();
				if (element instanceof Attribute) {
					Attribute a = (Attribute) element;
					return a.getType() == AttributeType.LIST || a.getType() == AttributeType.TREE;
				}
				return false;
			}
			
		});
		attributeOptions.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof NamedItem) return ((NamedItem) element).getName();
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element) {
				if (element instanceof Attribute) return DataModel.getAttributeImage(((Attribute) element).getType());
				return super.getImage(element);
			}
		});
		attributeOptions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelection();
			}
		});
		loadDataModel.schedule();
		
		field.addSelectionChangedListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				configureAttributePanel();
				validate();
			}
		});

		if (toUpdate != null) {
			txtName.setText(toUpdate.getName());
			txtAttributeFilters.setText(toUpdate.getAttributeFilter());
		}
		
		setTitle("New Summary Column");
		getShell().setText("Asset Overview Map");
		setMessage("Create a new summary column filtering on data model elements");
		return parent;
	}
	

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
		
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void addSelection() {
		Object option = attributeOptions.getStructuredSelection().getFirstElement();
		String part = null;
		
		if (option instanceof AttributeListItem) {
			AttributeListItem item = (AttributeListItem) option;
			part = "[" + item.getAttribute().getKeyId() + " = " + item.getKeyId() + "]";
		
		}else if (option instanceof AttributeTreeNode) {
			AttributeTreeNode item = (AttributeTreeNode) option;
			part = "[" + item.getAttribute().getKeyId() + " = " + item.getHkey() + "]";
		}else if (option instanceof Attribute) {
			Attribute a = (Attribute)option;
			if (a.getType() == AttributeType.LIST) return;
			if (a.getType() == AttributeType.TREE) return;
			
			if (a.getType() == AttributeType.BOOLEAN) {
				part = "[" + a.getKeyId() + " = <TRUE|FALSE>]";
			}else if (a.getType() == AttributeType.TEXT) {
				part = "[" + a.getKeyId() + " <equals|contains> \"<VALUE>\"]";
			}else if (a.getType() == AttributeType.NUMERIC) {
				part = "[" + a.getKeyId() + " < =|<>|<|>|<=|>= > <VALUE>]";
			}else if (a.getType() == AttributeType.DATE) {
				part = "[" + a.getKeyId() + " <equals|before|after> <YYYY-MM-DD>]";
			}
		}
		if (part != null) {
			if (txtAttributeFilters.getText().isEmpty()) {
				txtAttributeFilters.setText(part);
			}else {
				txtAttributeFilters.setText(txtAttributeFilters.getText() + " <AND|OR> " + part);
			}
		}
	}
	private void configureAttributePanel() {
		Object x = field.getValue();
		if (x == null) {
			txtAttributeFilters.setText("");
			attributeOptions.setInput(null);
			return;
		}
		List<Attribute> allAttributes = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			Category category = (Category)session.get(Category.class, ((Category)x).getUuid());
			
			
			category.getAllAttribute(allAttributes, true);
			
			for (Attribute a : allAttributes) {
				a.getName();
				a.getAttributeList().forEach(e->e.getName());
				
				List<AttributeTreeNode> toVisit = new ArrayList<>();
				toVisit.addAll(a.getActiveTreeNodes());
				while(!toVisit.isEmpty()) {
					AttributeTreeNode n = toVisit.remove(0);
					n.getName();
					toVisit.addAll(n.getActiveChildren());
				}
			}
		}
		
		attributeOptions.setInput(allAttributes);		
	}
	
	private Job loadDataModel = new Job("load data model") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			DataModel dm = null;
			Category toSelect = null;
			
			try(Session session = HibernateManager.openSession()){
				List<Category> rootCategories = QueryFactory.buildQuery(session, Category.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"parent", null}).list();
				List<Category> all = new ArrayList<>(rootCategories);
				while(all.size() > 0) {
					Category c = all.remove(0);
					c.getName();
					if (toUpdate != null && c.getHkey().equals(toUpdate.getCategoryKey())) {
						toSelect = c;
					}
					all.addAll(c.getChildren());
				}
				rootCategories.sort((a,b)->Integer.compare(a.getCategoryOrder(), b.getCategoryOrder()));
				dm = new DataModel(SmartDB.getCurrentConservationArea(), rootCategories, Collections.emptyList());
			}
			
			final DataModel fdm = dm;
			final Category ftoSelect = toSelect;
			Display.getDefault().syncExec(()->{
				
				if (field.getDropDown().getTreeViewer().getControl().isDisposed()) return;
				field.getDropDown().getTreeViewer().setContentProvider(new DataModelContentProvider(true, false) {
					@Override
					public Object[] getElements(Object inputElement) {
						return getChildren(root);
					}
				});
				field.getDropDown().getTreeViewer().setInput(fdm);
				if (ftoSelect != null) {
					field.setSelectedValue(ftoSelect);
					configureAttributePanel();
					validate();
				}
					
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
