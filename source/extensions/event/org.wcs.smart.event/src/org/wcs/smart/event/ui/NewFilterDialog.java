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
package org.wcs.smart.event.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.filter.Operator;
import org.wcs.smart.event.filter.ParsedFilter;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.ui.filter.DefinitionPanel;
import org.wcs.smart.event.ui.filter.DropItem;
import org.wcs.smart.event.ui.filter.DropItemFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for creating a new filter or updating an existing filter
 * 
 * @author Emily
 *
 */
public class NewFilterDialog extends TitleAreaDialog {
	
	private EFilter toUpdate;
	private Text txtId;
	private OptionCheckBoxDropDown chSourceFilter;
	private DefinitionPanel definitionPanel;
	
	private static IWaypointSource ALL_SOURCES = new IWaypointSource() {

		@Override
		public String getKey() {
			return "all";
		}

		@Override
		public String getName(Locale l) {
			return "All Sources";
		}

		@Override
		public String getDatastoreFileLocation(Object source, Session session) throws Exception {
			return null;
		}
		
	};
	
	public NewFilterDialog(Shell parentShell) {
		this(parentShell, null);
	}

	public NewFilterDialog(Shell parentShell, EFilter toUpdate) {
		super(parentShell);
		this.toUpdate = toUpdate;
	}
	
	private String getFilterString() {
		StringBuilder sb = new StringBuilder();
		
		Collection<?> items = chSourceFilter.getCheckObjects();
		for (Object x : items) {
			if (x == ALL_SOURCES) {
				sb = new StringBuilder();
				break;
			}else if (x instanceof IWaypointSource){
				IWaypointSource sss = (IWaypointSource) x;
				sb.append(sss.getKey());
				sb.append(ParsedFilter.SOURCE_SPACER);
			}
		}
		sb.append(ParsedFilter.SECTION_SPACER);
		sb.append(definitionPanel.getQueryPart());
		return sb.toString();
		
	}
	
	
	@Override
	protected void okPressed() {
		if (toUpdate == null) {
			toUpdate = new EFilter();
			toUpdate.setConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		toUpdate.setId(txtId.getText());
		toUpdate.setFilterString(getFilterString());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(toUpdate);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.log(ex.getMessage(), ex);
				return;
			}
		}
		super.okPressed();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button saveBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		saveBtn.setEnabled(false);
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
		
		Composite header = new Composite(main, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Filter ID:");
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtId.setTextLimit(EAction.MAX_ID_LENGTH);
		txtId.addModifyListener(e->validate());
		
		l = new Label(header, SWT.NONE);
		l.setText("Observation Source:");
		
		chSourceFilter = new OptionCheckBoxDropDown(header);
		chSourceFilter.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IWaypointSource) {
					return ((IWaypointSource) element).getName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		chSourceFilter.setContentProvider(ArrayContentProvider.getInstance());
		chSourceFilter.setInput(WaypointSourceEngine.INSTANCE.getSupportedSources());
		chSourceFilter.setOptionValue(ALL_SOURCES);
		chSourceFilter.addSelectionChangedListener(e->validate());
		
		l = new Label(main, SWT.NONE);
		l.setText("Filter:");
		
		createDataModelFilter(main);
		
		if (toUpdate != null) {
			txtId.setText(toUpdate.getId());
			
			ParsedFilter filter = null;
			try {
				filter = toUpdate.getParsedFilter();
			}catch (Exception ex) {
				EventPlugIn.displayLog("Unable to parse query filter:" + ex.getMessage(), ex);
			}
			
			if (filter != null) {
				if (filter.getSources() != null) {
					chSourceFilter.setValue(filter.getSources());
				}else {
					chSourceFilter.setValue(Collections.singletonList(ALL_SOURCES));
				}
				
				List<DropItem> dropItems = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					DropItemFactory.INSTANCE.createDropItem(filter.getFilter(), dropItems, session);
				}catch (Exception ex) {
					EventPlugIn.displayLog(MessageFormat.format("Unable to parse filter: {0}",ex.getMessage()), ex);
				}
				definitionPanel.addItems(dropItems);
			}
		}else {
			chSourceFilter.setValue(Collections.singletonList(ALL_SOURCES));
		}
		
		validate();
		
		setTitle("New Filter");
		getShell().setText("New Filter");
		setMessage("Create a new action filter from waypoint source and observation");
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private void validate() {
		String id = txtId.getText().trim();
		if (id.isEmpty()) {
			setError("An ID must be validated.");
			return;
		}
		
		if (chSourceFilter.getCheckObjects().isEmpty()) {
			setError("At least one waypoint source must be selected");
			return;
		}

		//TODO: validate query string
		
		setErrorMessage(null);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(true);
	}
	
	private void setError(String error) {
		setErrorMessage(error);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(false);
	}
	
	
	private void createDataModelFilter(Composite parent) {
		SashForm outer = new SashForm(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)outer.getLayoutData()).heightHint = 300;
		
		Composite leftPart = new Composite(outer, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		leftPart.setBackground(outer.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		definitionPanel = new DefinitionPanel() {
			public void fireQueryChangedListeners(){
				NewFilterDialog.this.validate();
			}
		};
		Composite cc = definitionPanel.createComposite(leftPart);
		
		
		cc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cc.setBackground(leftPart.getBackground());
		
		Composite rightPart = new Composite(outer, SWT.BORDER);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(leftPart.getBackground());
		((GridLayout)rightPart.getLayout()).marginWidth = 0;
		((GridLayout)rightPart.getLayout()).marginHeight = 0;
		
		TreeViewer dmTree = new TreeViewer(rightPart, SWT.V_SCROLL);
		dmTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dmTree.setContentProvider(new TreeContentProvider());
		dmTree.setLabelProvider(new DataModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == TreeContentProvider.ATTRIBUTE_NODE) return "Attributes";
				if (element == TreeContentProvider.CATEGORY_NODE) return "Categories";
				if (element == TreeContentProvider.OPERATOR_NODE) return "Operators";
				if (element instanceof Operator) return ((Operator)element).getGuiValue();
				return super.getText(element);
			}
			
			@Override
			public Image getImage(Object element) {
				if (element == TreeContentProvider.ATTRIBUTE_NODE) return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
				if (element == TreeContentProvider.CATEGORY_NODE) return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
				return super.getImage(element);
			}
		});
		dmTree.addDoubleClickListener(dc->{
			for (Iterator<?> iterator = dmTree.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object type = iterator.next();
				DropItem[] dropItems = DropItemFactory.INSTANCE.createDropItem(type);
				if (dropItems != null) {
					for (DropItem di : dropItems) definitionPanel.addItem(di);
				}
			}
		});
		try(Session session = HibernateManager.openSession()){
			DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
			List<Category> toVisit = new ArrayList<>();
			toVisit.addAll(dm.getCategories());
			while(!toVisit.isEmpty()){
				Category c = toVisit.remove(0);
				toVisit.addAll(c.getChildren());
				c.getAttributes().forEach(ca->ca.getAttribute().getName());
			}
			dmTree.setInput(dm);
		}
		outer.setWeights(new int[] {5,3});
	}
	
	private class TreeContentProvider implements ITreeContentProvider{
		public static final String CATEGORY_NODE = "Categories";
		public static final String ATTRIBUTE_NODE = "Attributes";
		public static final String OPERATOR_NODE = "Operators";
		
		private List<Attribute> dmAttributes = null;
		private DataModelContentProvider dmProvider = new DataModelContentProvider(false, false, true) {
			@Override
			public Object[] getElements(Object inputElement) {
				return super.model.getCategories().toArray();
			}
		};

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			dmProvider.inputChanged(viewer, oldInput, newInput);
			if (newInput instanceof DataModel) {
				dmAttributes = ((DataModel) newInput).getAttributes();
			}
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[] {CATEGORY_NODE,ATTRIBUTE_NODE,OPERATOR_NODE};
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement == CATEGORY_NODE) {
				return dmProvider.getElements(null);
			}else if (parentElement == OPERATOR_NODE) {
				return new Operator[] {Operator.BRACKETS, Operator.NOT};
			}else if (parentElement == ATTRIBUTE_NODE) {
				return dmAttributes.toArray();
			}
			return dmProvider.getChildren(parentElement);
		}

		@Override
		public Object getParent(Object element) {
			if (element == CATEGORY_NODE ||
					element == OPERATOR_NODE ||
					element == ATTRIBUTE_NODE) return null;
			if (element instanceof Attribute) return ATTRIBUTE_NODE;
			if (element instanceof Operator) return OPERATOR_NODE;
			if (element instanceof Category && ((Category) element).getParent() == null) return CATEGORY_NODE;
			return dmProvider.getParent(element);
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element == CATEGORY_NODE ||
					element == OPERATOR_NODE ||
					element == ATTRIBUTE_NODE) return true;
			if (element instanceof Category) return dmProvider.hasChildren(element);
			return false;
		}
		
	}
}