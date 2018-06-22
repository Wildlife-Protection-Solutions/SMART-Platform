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
package org.wcs.smart.event.i2.entity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.event.i2.entity.EntityMapping.Type;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Create entity action parameter collector
 * @author Emily
 *
 */
public class CreateEntityParameterCollector implements IActionParameterCollector {

	private String initEntityTypeKey = null;

	private ComboViewer cmbEntityType;
	private TableViewer tblMappings;

	private List<Listener> modifyListeners;
	private List<EntityMapping> mappings = new ArrayList<>();
	private List<Attribute> dmAttributes;
	
	private IntelEntityType lastSelection = null;
	
	public CreateEntityParameterCollector() {
		modifyListeners = new ArrayList<>();
	}

	@Override
	public void initParameters(EAction action) {
		EActionParameterValue sourceParam = action.findParameter(EntityTypeParameter.INSTANCE.getKey());
		if (sourceParam != null) {
			initEntityTypeKey = sourceParam.getParameterValue();
			initSourceCombo(sourceParam.getParameterValue());
		}
		
		
		EActionParameterValue mappingParam = action.findParameter(MappingParameter.INSTANCE.getKey());
		if (mappingParam != null) {
			String jsonArray = mappingParam.getParameterValue();
			try(Session session = HibernateManager.openSession()){
				this.mappings = EntityMapping.parse(jsonArray, session, SmartDB.getCurrentConservationArea());
			}
			tblMappings.setInput(mappings);
			tblMappings.refresh();
			for(TableColumn c : tblMappings.getTable().getColumns()) c.pack();
		}
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void updateParameters(EAction action) {
		EActionParameterValue sourceParam = action.findParameter(EntityTypeParameter.INSTANCE.getKey());
		Object x = cmbEntityType.getStructuredSelection().getFirstElement();
		if (x instanceof IntelEntityType) {
			if (sourceParam == null) {
				sourceParam = new EActionParameterValue();
				sourceParam.getId().setAction(action);
				sourceParam.getId().setParameterKey(EntityTypeParameter.INSTANCE.getKey());
				action.getParameters().add(sourceParam);
			}
			sourceParam.setParameterValue(((IntelEntityType)x).getKeyId());
		}else {
			if (sourceParam != null) {
				action.getParameters().remove(sourceParam);
			}
		}
		
		
		EActionParameterValue datamodelParam = action.findParameter(MappingParameter.INSTANCE.getKey());
		
		if (mappings == null || mappings.isEmpty()) {
			if (datamodelParam != null) action.getParameters().remove(datamodelParam);
		}else {			
			if (datamodelParam == null) {
				datamodelParam = new EActionParameterValue();
				datamodelParam.getId().setAction(action);
				datamodelParam.getId().setParameterKey(MappingParameter.INSTANCE.getKey());
				action.getParameters().add(datamodelParam);
			}
			JSONArray jj = new JSONArray();
			for (EntityMapping m : mappings) {
				jj.add(m);
			}
			datamodelParam.setParameterValue(jj.toJSONString());
		}
		
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		Label l = new Label(top, SWT.NONE);
		l.setText("Entity Type:");
		
		cmbEntityType = new ComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelEntityType) {
					return ((IntelEntityType) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbEntityType.setInput(new String[] {DialogConstants.LOADING_TEXT});

		l = new Label(main, SWT.NONE);
		l.setText("Entity Attribute Mappings:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite bottom = new Composite(main, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		
		tblMappings = new TableViewer(bottom, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
		tblMappings.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblMappings.getTable().setHeaderVisible(true);
		tblMappings.getTable().setLinesVisible(true);
		tblMappings.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn entityColumn = new TableViewerColumn(tblMappings, SWT.NONE);
		entityColumn.getColumn().setText("Entity Attribute");
		entityColumn.getColumn().pack();
		entityColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EntityMapping) {
					StringBuilder sb = new StringBuilder();
					EntityMapping map = (EntityMapping) element;
					sb.append(map.getEntityAttribute().getName());
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn dmColumn = new TableViewerColumn(tblMappings, SWT.NONE);
		dmColumn.getColumn().setText("Data Model Attribute");
		dmColumn.getColumn().pack();
		dmColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EntityMapping) {
					StringBuilder sb = new StringBuilder();
					EntityMapping map = (EntityMapping) element;
					if (map.getDataModelAttribute() == null) return ""; //fixed
					sb.append(map.getDataModelAttribute().getName());
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn fixedColumn = new TableViewerColumn(tblMappings, SWT.NONE);
		fixedColumn.getColumn().setText("Fixed Value");
		fixedColumn.getColumn().pack();
		fixedColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EntityMapping) {
					EntityMapping map = (EntityMapping) element;
					if (map.getType() == Type.FIXED) {
						return map.getFixedValueAsString();
					}else if (map.getType() == Type.POSITION) {
						return "<Observation Position>";
					}
					return "";
				}
				return super.getText(element);
			}
		});
		
		tblMappings.setInput(mappings);
		
		Composite buttonArea = new Composite(bottom, SWT.NONE);
		buttonArea.setLayout(new GridLayout(1, false));
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)buttonArea.getLayout()).marginWidth = 0;
		((GridLayout)buttonArea.getLayout()).marginHeight = 0;
		
		Button btnAdd = new Button(buttonArea, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addListener(SWT.Selection, e->addMapping());
		btnAdd.setEnabled(false);
		
		Button btnRemove= new Button(buttonArea, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addListener(SWT.Selection, e->deleteMapping());
		btnRemove.setEnabled(false);
		
		cmbEntityType.addSelectionChangedListener(e->{
			btnAdd.setEnabled(cmbEntityType.getStructuredSelection().getFirstElement() instanceof IntelEntityType);
			if (lastSelection == cmbEntityType.getStructuredSelection().getFirstElement()) return;
			if (!mappings.isEmpty()) {
				if (!MessageDialog.openConfirm(main.getShell(), "Clear", "By changing the entity type you will loose all the configured mappings.  Are you sure you want to continue?")) {
					cmbEntityType.setSelection(new StructuredSelection(lastSelection));
					return;
				}
			}
			lastSelection = (IntelEntityType) cmbEntityType.getStructuredSelection().getFirstElement();
			mappings.clear();
			tblMappings.refresh();
		});
		
		
		tblMappings.addSelectionChangedListener(e->{
			btnRemove.setEnabled(!tblMappings.getStructuredSelection().isEmpty());
		});
		
		Menu mnu = new Menu(tblMappings.getControl());
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addMapping());
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->addMapping());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuDelete.setEnabled(!tblMappings.getSelection().isEmpty());
				mnuAdd.setEnabled(cmbEntityType.getStructuredSelection().getFirstElement() instanceof IntelEntityType);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		tblMappings.getControl().setMenu(mnu);
		
		loadEntities.schedule();
		return main;
	}
	
	
	private void deleteMapping() {
		for (Iterator<?> iterator = tblMappings.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			mappings.remove(x);
		}
		tblMappings.refresh();
		fireListeners();
	}
	
	private void addMapping() {
		Object x = cmbEntityType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IntelEntityType)) return;
		
		NewMappingDialog dialog = new NewMappingDialog(cmbEntityType.getControl().getShell(), (IntelEntityType)x, dmAttributes);
		if (dialog.open() != Window.OK) return;
		
		mappings.add(dialog.getMapping());
		tblMappings.refresh();
		fireListeners();
	}
		
	@Override
	public void addModifyListener(Listener listener) {
		modifyListeners.add(listener);
	}
	
	private void fireListeners() {
		modifyListeners.forEach(e->e.handleEvent(new Event()));
	}

	private void initSourceCombo(String key) {
		if (key == null) return;
		
		Object x = cmbEntityType.getInput();
		if (!(x instanceof List)) return;
		
		try {
			List<?> items = (List<?>)x;
			for (Object item : items) {
				if ((item instanceof IntelEntityType) && (((IntelEntityType)item).getKeyId()).equals(key)){
					lastSelection = (IntelEntityType) item;
					cmbEntityType.setSelection(new StructuredSelection(item));
					return;
				}
			}
		}finally {
			cmbEntityType.addSelectionChangedListener(evt->fireListeners());
		}
	}
	
	
	private Job loadEntities = new Job("loading entity types") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> srcs = new ArrayList<>();
			List<Attribute> dmAttributes = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				srcs.addAll(QueryFactory.buildQuery(session, IntelEntityType.class, 
						"conservationArea", SmartDB.getCurrentConservationArea()).list()); //$NON-NLS-1$
				for (IntelEntityType src : srcs) {
					src.getAttributes().forEach(a->{
						a.getAttribute().getName();
						if(a.getAttribute().getType() == AttributeType.LIST) {
							a.getAttribute().getAttributeList().forEach(e->e.getName());
						}
					});
				}
				
				dmAttributes.addAll(QueryFactory.buildQuery(session, Attribute.class,
						"conservationArea", SmartDB.getCurrentConservationArea()).list());
				for (Attribute a : dmAttributes) {
					a.getName();
				}
			}
			
			srcs.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			dmAttributes.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			CreateEntityParameterCollector.this.dmAttributes = dmAttributes;
			Display.getDefault().syncExec(()->{
				if (cmbEntityType.getControl().isDisposed()) return;
				cmbEntityType.setInput(srcs);
				initSourceCombo(initEntityTypeKey);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
		
}
