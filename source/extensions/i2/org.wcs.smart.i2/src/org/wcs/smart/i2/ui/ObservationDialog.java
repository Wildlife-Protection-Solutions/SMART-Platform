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
package org.wcs.smart.i2.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for editing observation details.
 * 
 * @author Emily
 *
 */
public class ObservationDialog extends Dialog {

	private IntelLocation location;
	
	private Composite attributeComposite;
	private TreeViewer dmTreeViewer;
	private TableViewer observationTable;
	private IntelObservation editObs = null;
	
	private List<IntelObservation> observations;
	
	private Button btnAdd;
	private boolean hasChanges = false;
	
	public ObservationDialog(Shell parentShell, IntelLocation location) {
		super(parentShell);
		this.location = location;
		if (location.getObservations() == null){
			location.setObservations(new ArrayList<IntelObservation>());
		}
		
		//clone observations for editing so we can cancel this dialog
		observations = new ArrayList<IntelObservation>();
		for (IntelObservation o : location.getObservations()){
			IntelObservation copy = new IntelObservation();
			copy.setCategory(o.getCategory());
			copy.setLocation(o.getLocation());
			copy.setUuid(o.getUuid());
			copy.setObservationAttributes(new ArrayList<IntelObservationAttribute>());
			if (o.getObservationAttributes() != null){
				for (IntelObservationAttribute a : o.getObservationAttributes()){
					IntelObservationAttribute acopy = new IntelObservationAttribute();
					acopy.setAttribute(a.getAttribute());
					acopy.setAttributeListItem(a.getAttributeListItem());
					acopy.setAttributeTreeNode(a.getAttributeTreeNode());
					acopy.setNumberValue(a.getNumberValue());
					acopy.setObservation(copy);
					acopy.setStringValue(a.getStringValue());
					copy.getObservationAttributes().add(acopy);
				}
			}
			observations.add(copy);
		}
	}

	private void loadDataModel(){
		Job j = new Job("loading data model") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				DataModel dm = null;
				Session s = HibernateManager.openSession();
				try{
					dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
					List<Category> toVisit = new ArrayList<>();
					toVisit.addAll(dm.getActiveCategories());
					while(!toVisit.isEmpty()){
						Category c = toVisit.remove(0);
						toVisit.addAll(c.getActiveChildren());
					}
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog("Error loading data model. " + ex.getMessage(), ex);
					return Status.OK_STATUS;
				}finally{
					s.close();
				}
				final DataModel fdm = dm;
				Display.getDefault().syncExec(()->{
					dmTreeViewer.setInput(fdm);
					dmTreeViewer.expandToLevel(3);
					observationTable.setInput(observations);
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	@Override
	public Point getInitialSize(){
		return new Point(800, 700);
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Location Observations");
		
		SashForm main = new SashForm(parent, SWT.VERTICAL);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite upperArea= new Composite(main, SWT.NONE);
		upperArea.setLayout(new GridLayout());
		
		SashForm upper = new SashForm(upperArea, SWT.HORIZONTAL);
		upper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftUpper = new Composite(upper, SWT.NONE);
		leftUpper.setLayout(new GridLayout());
		((GridLayout)leftUpper.getLayout()).marginWidth = 0;
		((GridLayout)leftUpper.getLayout()).marginHeight = 0;
		
		dmTreeViewer = new TreeViewer(leftUpper, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		dmTreeViewer.setLabelProvider(new DataModelLabelProvider());
		dmTreeViewer.setContentProvider(new DataModelContentProvider(true, true));
		dmTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dmTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selection = ((IStructuredSelection)dmTreeViewer.getSelection()).getFirstElement();
				if (selection instanceof Category){
					createAttributePanel((Category)selection);
					editObs = null;
				}
			}
		});
		attributeComposite = new Composite(upper, SWT.BORDER);
		attributeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeComposite.setLayout(new GridLayout());
		attributeComposite.setBackground(attributeComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		Label l = new Label(attributeComposite, SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		l.setText("To create a new observation select a category on the left.  To edit an exisitng observation select the observation then the edit button");
		((GridData)l.getLayoutData()).widthHint = 150;
		l.setBackground(attributeComposite.getBackground());
		
		Composite lower = new Composite(main, SWT.NONE);
		lower.setLayout(new GridLayout(2, false));
		((GridLayout)lower.getLayout()).marginHeight = 0;
		
		observationTable = new TableViewer(lower, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		observationTable.setContentProvider(ArrayContentProvider.getInstance());
		observationTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		observationTable.setInput("Observation here");
		observationTable.getTable().setHeaderVisible(true);
		observationTable.getTable().setLinesVisible(true);
		
		TableViewerColumn categoryColumn = new TableViewerColumn(observationTable, SWT.NONE);
		categoryColumn.getColumn().setText("Category");
		categoryColumn.getColumn().setWidth(250);
		categoryColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelObservation){
					return ((IntelObservation) element).getCategory().getName();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn attributeColumn = new TableViewerColumn(observationTable, SWT.NONE);
		attributeColumn.getColumn().setText("Attributes");
		attributeColumn.getColumn().setWidth(800);
		attributeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelObservation){
					StringBuilder sb = new StringBuilder();
					IntelObservation oo = (IntelObservation)element;
					for (IntelObservationAttribute aa : oo.getObservationAttributes()){
						sb.append(aa.getAttribute().getName());
						sb.append(": ");
						sb.append(aa.getAttributeValueAsString(Locale.getDefault()));
						sb.append(" | ");
					}
					if (sb.length() > 0){
						sb = sb.delete(sb.length() - 3, sb.length());
					}
					return sb.toString();
				}
				return super.getText(categoryColumn);
			}
		});
		
		Menu observationMnu = new Menu(observationTable.getControl());
		observationTable.getTable().setMenu(observationMnu);
		final MenuItem editItem = new MenuItem(observationMnu, SWT.PUSH);
		editItem.setText("Edit");
		editItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editObservation();
			}
		});
		
		final MenuItem deleteItem = new MenuItem(observationMnu, SWT.PUSH);
		deleteItem.setText("Delete");
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				 deleteObservation();
			}
		});
		
		observationMnu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				boolean isempty = observationTable.getSelection().isEmpty();
				editItem.setEnabled(!isempty);
				deleteItem.setEnabled(!isempty);
			}
		});
		
		
		Composite buttonPanel = new Composite(lower, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		Button btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setToolTipText("edit selected observation");
		btnEdit.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editObservation();
			}
		});
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setToolTipText("delete selected observation");
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteObservation();
			}
		});
		
		main.setWeights(new int[]{70,30});
		
		
		loadDataModel();
		return main;
	}
	
	private void editObservation(){
		 Object x = ((IStructuredSelection)observationTable.getSelection()).getFirstElement();
		 if (x != null && x instanceof IntelObservation){
			 editObs = (IntelObservation)x;
			 createAttributePanel(editObs.getCategory());
			 
			 List<IAttributeField<?>> fields = (List<IAttributeField<?>>) attributeComposite.getData(IAttributeField.class.getName());
			 for (IAttributeField<?> field: fields){
				 IntelObservationAttribute obs = null;
				 for (IntelObservationAttribute oba : editObs.getObservationAttributes()){
					 if (oba.getAttribute().equals(field.getAttribute())){
						 field.setValue(oba.getAttributeValue());
						 break;
					 }
				 }
			 }
			 
			 btnAdd.setText("Update Observation");
			 observationTable.refresh();
		 }
	}
		
	private void deleteObservation() {
		Object x = ((IStructuredSelection)observationTable.getSelection()).getFirstElement();
		 if (x != null && x instanceof IntelObservation){
			 if (x.equals(editObs)){
				 Category c = editObs.getCategory();
				 editObs = null;
				 createAttributePanel(c, false);
			 }
			 observations.remove(x);
			 observationTable.refresh();
			 hasChanges = true;
		 }
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void createAttributePanel(Category category){
		createAttributePanel(category, true);
	}
	private void createAttributePanel(Category category, boolean confirmChanges){
		if (confirmChanges){
			List<IAttributeField<?>> currentFields = (List<IAttributeField<?>>) attributeComposite.getData(IAttributeField.class.getName());
			if (currentFields != null){
				boolean ismodified = false;
				for (IAttributeField<?> field : currentFields){
					if(field.isModified()){
						ismodified = true;
						break;
					}
				}
				if (ismodified){
					if (MessageDialog.openQuestion(getShell(), "Save Changes", "Create / update observation?")){
						createObservation();
					}
				}
			}
		}
		for (Control c : attributeComposite.getChildren()){
			c.dispose();
		}
		
		Session s = HibernateManager.openSession();
		try{
			Category c = (Category) s.get(Category.class, category.getUuid());
			
			Label l = new Label(attributeComposite, SWT.WRAP);
			l.setText(c.getFullCategoryName());
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			final Font boldFont = new Font(l.getDisplay(), fd);
			l.setFont(boldFont);
			l.addDisposeListener(e -> boldFont.dispose());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 100;
			
			ScrolledComposite scattributes = new ScrolledComposite(attributeComposite, SWT.V_SCROLL);
			scattributes.setExpandHorizontal(true);
			scattributes.setExpandVertical(true);
			
			Composite attributes = new Composite(scattributes, SWT.NONE);
			scattributes.setContent(attributes);
			attributes.setLayout(new GridLayout(2, false));
			attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scattributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			ArrayList<Attribute> allAttributes = new ArrayList<>();
			c.getAllAttribute(allAttributes, true);
			
			
			List<IAttributeField> fields = new ArrayList<>();
			for (Attribute a :allAttributes){
				IAttributeField<?> field = AttributeFieldFactory.findAttributeField(a);
				field.createComposite(attributes);
				attributes.addListener(SWT.Dispose, e->field.dispose());
				
				if (a.getType() == AttributeType.TREE){
					List<AttributeTreeNode> nodes = new ArrayList<AttributeTreeNode>();
					nodes.addAll(a.getActiveTreeNodes());
					while(!nodes.isEmpty()){
						AttributeTreeNode n = nodes.remove(0);
						n.getName();
						nodes.addAll(n.getActiveChildren());
					}
				}
				fields.add(field);
			}
			attributeComposite.setData(Category.class.getName(), c);
			attributeComposite.setData(IAttributeField.class.getName(), fields);
			
			scattributes.setMinSize(attributes.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}finally{
			s.close();
		}
		List<Control> toColor = new ArrayList<Control>();
		toColor.add(attributeComposite);
		while(!toColor.isEmpty()){
			Control c = toColor.remove(0);
			c.setBackground(attributeComposite.getBackground());
			if (c instanceof Composite){
				for (Control add : ((Composite)c).getChildren()){
					toColor.add(add);
				}
			}
		}
		
		btnAdd = new Button(attributeComposite, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		btnAdd.setText("Create Observation");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: add observation
				createObservation();
			}
		});
		attributeComposite.layout(true);
	}
	
	private void createObservation(){
		IntelObservation oo = new IntelObservation();
		if (editObs != null){
			oo = editObs;
		}
		
		oo.setCategory((Category)attributeComposite.getData(Category.class.getName()));
		oo.setLocation(location);
		oo.setObservationAttributes(new ArrayList<IntelObservationAttribute>());
		List<IAttributeField<?>> fields = (List<IAttributeField<?>>) attributeComposite.getData(IAttributeField.class.getName());
		for (IAttributeField<?> f : fields){
			String err = f.validate();
			if (err != null){
				MessageDialog.openWarning(getShell(), "Error", MessageFormat.format("Cannot create observation.  Invalid value for attribute ''{0}''. {1}", f.getAttribute().getName(), err));
				return;
			}
		}
		for (IAttributeField<?> f : fields){	
			Object value = f.getValue();
			if (value != null){
				IntelObservationAttribute a = new IntelObservationAttribute();
				a.setAttribute(f.getAttribute());
				a.setObservation(oo);
				switch(a.getAttribute().getType()){
				case BOOLEAN:
					Boolean x = (Boolean)value;
					if (x){
						a.setNumberValue(1d);
					}else{
						a.setNumberValue(0d);
					}
					break;
				case NUMERIC:
					a.setNumberValue((Double)value);
					break;
				case DATE:
					a.setDateValue((Date)value);
					break;
				case LIST:
					a.setAttributeListItem((AttributeListItem)value);
					break;
				case TEXT:
					a.setStringValue((String)value);
					break;
				case TREE:
					a.setAttributeTreeNode((AttributeTreeNode)value);
					break;
				default:
					break;
				
				}
				oo.getObservationAttributes().add(a);
			}
			f.clear();
		}
		if (editObs == null){
			observations.add(oo);
		}
		editObs = null;
		observationTable.refresh(); 
		hasChanges = true;
	}
	
	@Override
	public void cancelPressed(){
		if (hasChanges){
			if (MessageDialog.openQuestion(getShell(), "Save", "Observations have been modified.  Save Changes?")){
				okPressed();
				return;
			}
		}
		super.cancelPressed();
	}
	
	
	@Override
	public void okPressed(){
		location.setObservations(observations);
		super.okPressed();
	}
}
