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
package org.wcs.smart.asset.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.SmartStyledDialog;
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
public class ObservationDialog extends SmartStyledDialog {

	private Waypoint waypoint;
	
	private Composite attributeComposite;
	private TreeViewer dmTreeViewer;
	private TableViewer observationTable;
	private WaypointObservation editObs = null;
	
	private List<WaypointObservationGroup> groups;
	
	private Button btnAdd;
	private boolean hasChanges = false;
	
	private WaypointObservation toEdit = null;
	/**
	 * Edits the waypoint, selecting the specific observation for editing
	 * @param parentShell
	 * @param wo
	 */
	public ObservationDialog(Shell parentShell, WaypointObservation wo) {
		this(parentShell, wo.getWaypoint());
		toEdit = wo;
	}
	
	public ObservationDialog(Shell parentShell, Waypoint waypoint) {
		super(parentShell);
		this.waypoint = waypoint;
		if (waypoint.getObservationGroups() == null) waypoint.setObservationGroups(new ArrayList<>());
		
		try(Session session = HibernateManager.openSession()){
			//clone waypoint for editing so we can cancel this dialog
			Waypoint wptemp = session.get(Waypoint.class, waypoint.getUuid());
			groups = new ArrayList<>(wptemp.getObservationGroups());
			groups.forEach(g->g.getObservations().forEach(o->{
				o.getCategory().getName();
				o.getCategory().getFullCategoryName();
				o.getCategory().getAllAttribute(new ArrayList<>(), null);
				o.getAttributes().forEach(a->{
					a.getAttributeValueAsString(Locale.getDefault());
					a.getAttribute().getName();
					if (a.getAttributeListItems() != null) a.getAttributeListItems().forEach(ai->ai.getAttributeListItem().getName());
				});
			}));
		}
	}

	private void loadDataModel(){
		Job j = new Job(Messages.ObservationDialog_loadingdatamodeljob) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				DataModel dm = null;
				try(Session s = HibernateManager.openSession()){
					dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), s);
					List<Category> toVisit = new ArrayList<>();
					toVisit.addAll(dm.getActiveCategories());
					while(!toVisit.isEmpty()){
						Category c = toVisit.remove(0);
						toVisit.addAll(c.getActiveChildren());
					}
				}catch (Exception ex){
					AssetPlugIn.displayLog(Messages.ObservationDialog_DmLoadError + ex.getMessage(), ex);
					return Status.OK_STATUS;
				}
				final DataModel fdm = dm;
				Display.getDefault().syncExec(()->{
					dmTreeViewer.setInput(fdm);
					dmTreeViewer.expandToLevel(3);
					observationTable.setInput(groups);
					
					if (toEdit != null) {
						observationTable.setSelection(new StructuredSelection(toEdit));
						editObservation();
					}
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
	
	private void resize(ScrolledComposite scroll, Composite kid) {
		scroll.setMinSize(scroll.getSize().x-scroll.getVerticalBar().getSize().x, kid.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.ObservationDialog_Title);
		
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
					if (!confirmSave()) return;
					editObs = null;
					createAttributePanel((Category)selection);
				}
			}
		});
		attributeComposite = new Composite(upper, SWT.BORDER);
		attributeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeComposite.setLayout(new GridLayout());
		attributeComposite.setBackground(attributeComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		Label l = new Label(attributeComposite, SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		l.setText(Messages.ObservationDialog_NewMessage);
		((GridData)l.getLayoutData()).widthHint = 150;
		l.setBackground(attributeComposite.getBackground());
		
		Composite lower = new Composite(main, SWT.NONE);
		lower.setLayout(new GridLayout(2, false));
		((GridLayout)lower.getLayout()).marginHeight = 0;
		
		observationTable = new TableViewer(lower, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		observationTable.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					List<Object> all = new ArrayList<>();
					List<WaypointObservationGroup> items  = (List<WaypointObservationGroup>)inputElement;
					for (WaypointObservationGroup g : items) {
						all.add(g);
						for (WaypointObservation wo : g.getObservations()) {
							all.add(wo);
						}
					}
					return all.toArray();
				}else {
					return new Object[] {inputElement};
				}
			}
		});
		observationTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		observationTable.setInput(DialogConstants.LOADING_TEXT);
		observationTable.getTable().setHeaderVisible(true);
		observationTable.getTable().setLinesVisible(true);
		observationTable.addDoubleClickListener(event->editObservation());
		
		TableViewerColumn categoryColumn = new TableViewerColumn(observationTable, SWT.NONE);
		categoryColumn.getColumn().setText(Messages.ObservationDialog_CategoryColumn);
		categoryColumn.getColumn().setWidth(250);
		categoryColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof WaypointObservation){
					return ((WaypointObservation) element).getCategory().getName();
				}else if (element instanceof WaypointObservationGroup) {
					return Messages.ObservationDialog_ObservationGroupLabel;
				}
				return super.getText(element);
			}
			
			@Override
			public Color getBackground(Object element) {
				if (element instanceof WaypointObservationGroup) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				return null;
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof WaypointObservationGroup) return getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE);
				return null;
			}
		});
		
		TableViewerColumn attributeColumn = new TableViewerColumn(observationTable, SWT.NONE);
		attributeColumn.getColumn().setText(Messages.ObservationDialog_AttributesColumn);
		attributeColumn.getColumn().setWidth(800);
		attributeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof WaypointObservationGroup) return ""; //$NON-NLS-1$
				if (element instanceof WaypointObservation){
					StringBuilder sb = new StringBuilder();
					WaypointObservation oo = (WaypointObservation)element;
					for (WaypointObservationAttribute aa : oo.getAttributesSorted()){
						sb.append(aa.getAttribute().getName());
						sb.append(": "); //$NON-NLS-1$
						sb.append(aa.getAttributeValueAsString(Locale.getDefault()));
						sb.append(" | "); //$NON-NLS-1$
					}
					if (sb.length() > 0){
						sb = sb.delete(sb.length() - 3, sb.length());
					}
					return sb.toString();
				}
				return super.getText(categoryColumn);
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof WaypointObservationGroup) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				return null;
			}
		});
		
		Menu observationMnu = new Menu(observationTable.getControl());
		observationTable.getTable().setMenu(observationMnu);
		final MenuItem editItem = new MenuItem(observationMnu, SWT.PUSH);
		editItem.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editObservation();
			}
		});
		
		final MenuItem deleteItem = new MenuItem(observationMnu, SWT.PUSH);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				 deleteObservation();
			}
		});
		
		final MenuItem groupItem = new MenuItem(observationMnu, SWT.PUSH);
		groupItem.setText(Messages.ObservationDialog_NewObsGroupText);
		groupItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GROUP_ICON));
		groupItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				groupObservations();
			}
		});
		
		observationMnu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {			
				editItem.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation) ;
				deleteItem.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation);
				groupItem.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation);
			}
		});
		

		ToolBar tb = new ToolBar(lower, SWT.VERTICAL | SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		ToolItem btnEdit = new ToolItem(tb, SWT.PUSH);
		btnEdit.setToolTipText(Messages.ObservationDialog_edittooltip);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editObservation();
			}
		});
		
		ToolItem btnDelete = new ToolItem(tb, SWT.PUSH);
		btnDelete.setToolTipText(Messages.ObservationDialog_deletetooltip);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteObservation();
			}
		});
		
		ToolItem btnGroup = new ToolItem(tb, SWT.PUSH);
		btnGroup.setToolTipText(Messages.ObservationDialog_NewObsGroupTooltip);
		btnGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GROUP_ICON));
		btnGroup.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				groupObservations();
			}
		});
		
		observationTable.addSelectionChangedListener(e->{
			btnEdit.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation) ;
			btnDelete.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation);
			btnGroup.setEnabled (observationTable.getStructuredSelection().getFirstElement() instanceof WaypointObservation);
		});
		
		main.setWeights(new int[]{70,30});
		
		
		loadDataModel();
		
		return main;
	}
	
	@SuppressWarnings("unchecked")
	private boolean confirmSave() {
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
				int ret = MessageDialog.open(MessageDialog.QUESTION_WITH_CANCEL, getShell(), Messages.ObservationDialog_SaveTitle, Messages.ObservationDialog_SaveMessage, SWT.NONE, IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL);
				if (ret == 2) return false;
				if (ret == 0) createObservation();
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private void editObservation(){
		if (!confirmSave()) return;
		 Object x = ((IStructuredSelection)observationTable.getSelection()).getFirstElement();
		 if (x != null && x instanceof WaypointObservation){
			 editObs = (WaypointObservation)x;
			 createAttributePanel(editObs.getCategory());
			 
			 List<IAttributeField<?>> fields = (List<IAttributeField<?>>) attributeComposite.getData(IAttributeField.class.getName());
			 for (IAttributeField<?> field: fields){
				 for (WaypointObservationAttribute oba : editObs.getAttributes()){
					 if (oba.getAttribute().equals(field.getAttribute())){
						 field.setValue(oba.getAttributeValue());
						 break;
					 }
				 }
			 }

			 btnAdd.setText(Messages.ObservationDialog_UpdateBtn);
			 observationTable.refresh();
		 }
	}
		
	private void deleteObservation() {
		IStructuredSelection sel = observationTable.getStructuredSelection();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof WaypointObservation){
				 WaypointObservation wo = (WaypointObservation)x;
				 if (x.equals(editObs)){
					 Category c = editObs.getCategory();
					 editObs = null;
					 createAttributePanel(c);
				 }
				 wo.getObservationGroup().getObservations().remove(wo);	 
				 observationTable.refresh();
				 hasChanges = true;
			}
		}
	}
	
	private void groupObservations() {
		IStructuredSelection sel = observationTable.getStructuredSelection();
		List<WaypointObservation> toGroup = new ArrayList<>();
		
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof WaypointObservation) toGroup.add((WaypointObservation)x);
		}
		
		if (toGroup.isEmpty()) return;
		
		WaypointObservationGroup g = new WaypointObservationGroup();
		g.setObservations(new ArrayList<>());
		g.setWaypoint(this.waypoint);
		groups.add(0, g);
		for (WaypointObservation wo : toGroup) {
			wo.getObservationGroup().getObservations().remove(wo);
			wo.setObservationGroup(g);
			g.getObservations().add(wo);
		}
		
		List<WaypointObservationGroup> empty = new ArrayList<>();
		for (WaypointObservationGroup group : groups) if (group.getObservations().isEmpty()) empty.add(group);
		groups.removeAll(empty);
		
		observationTable.refresh();
		hasChanges = true;
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
		
		for (Control c : attributeComposite.getChildren()){
			c.dispose();
		}
		
		try(Session s = HibernateManager.openSession()){
			Category c = (Category) s.get(Category.class, category.getUuid());
			
			Composite top = new Composite(attributeComposite, SWT.NONE);
			top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			top.setLayout(new GridLayout(2, false));
			((GridLayout)top.getLayout()).marginWidth = 0;
			((GridLayout)top.getLayout()).marginHeight = 0;
			
			Label l = new Label(top, SWT.WRAP);
			l.setText(c.getFullCategoryName());
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			final Font boldFont = new Font(l.getDisplay(), fd);
			l.setFont(boldFont);
			l.addDisposeListener(e -> boldFont.dispose());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 100;
			
			l = new Label(top, SWT.WRAP);
			l.setText(editObs == null ? Messages.ObservationDialog_NewObsLabel : Messages.ObservationDialog_EditObsLabel);
			l.addDisposeListener(e -> boldFont.dispose());
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
			
			l = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
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
			
			top.addListener(SWT.Resize, e->resize(scattributes, attributes));
			
			List<IAttributeField<?>> fields = new ArrayList<>();
			for (Attribute a :allAttributes){
				
				IAttributeField<?> field = AttributeFieldFactory.findAttributeField(a);
				field.createComposite(attributes);
				field.addResizeListener(e->resize(scattributes, attributes));
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
		btnAdd.setBackground(attributeComposite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		btnAdd.setText(Messages.ObservationDialog_CreateButton);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createObservation();
			}
		});
		attributeComposite.layout(true);
	}
	
	@SuppressWarnings("unchecked")
	private void createObservation(){
		WaypointObservation oo = new WaypointObservation();
		if (editObs != null){
			oo = editObs;
		}
		
		oo.setCategory((Category)attributeComposite.getData(Category.class.getName()));
		if (groups.isEmpty()) {
			WaypointObservationGroup g = new WaypointObservationGroup();
			g.setObservations(new ArrayList<>());
			g.setWaypoint(waypoint);
			groups.add(g);
		}
		oo.setObservationGroup(groups.get(0));
		
		if (oo.getAttributes() == null) oo.setAttributes(new ArrayList<WaypointObservationAttribute>());
		
		List<IAttributeField<?>> fields = (List<IAttributeField<?>>) attributeComposite.getData(IAttributeField.class.getName());
		for (IAttributeField<?> f : fields){
			String err = f.validate();
			if (err != null){
				MessageDialog.openWarning(getShell(), Messages.ObservationDialog_ErrorTitle, MessageFormat.format(Messages.ObservationDialog_ErrorMessage, f.getAttribute().getName(), err));
				return;
			}
		}
		for (IAttributeField<?> f : fields){	
			Object value = f.getValue();
			
			WaypointObservationAttribute toUpdate = null;
			for (WaypointObservationAttribute a : oo.getAttributes()) {
				if (a.getAttribute().equals(f.getAttribute())) {
					toUpdate = a;
					break;
				}
			}
			
			if (value != null){
				//find the attribute to update
				if (toUpdate == null) {
					toUpdate = new WaypointObservationAttribute();
					toUpdate.setAttribute(f.getAttribute());
					toUpdate.setObservation(oo);
					oo.getAttributes().add(toUpdate);
				}
				toUpdate.setAttributeValue(f.getValue());
			}else {
				if (toUpdate != null) {
					oo.getAttributes().remove(toUpdate);
				}
			}
			f.clear();
		}
		if (editObs == null){
			groups.get(0).getObservations().add(oo);
		}
		editObs = null;
		observationTable.refresh(); 
		hasChanges = true;
	}
	
	@Override
	public void cancelPressed(){
		if (hasChanges){
			if (MessageDialog.openQuestion(getShell(), Messages.ObservationDialog_Save, Messages.ObservationDialog_ConfirmSave)){
				okPressed();
				return;
			}
		}
		super.cancelPressed();
	}
	
	
	@Override
	public void okPressed(){
		if (!confirmSave()) return;
		waypoint.setObservationGroups(groups);
		super.okPressed();
	}
}
