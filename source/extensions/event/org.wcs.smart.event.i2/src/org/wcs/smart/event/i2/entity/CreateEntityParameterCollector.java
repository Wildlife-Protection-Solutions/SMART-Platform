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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import org.wcs.smart.event.i2.ProfileParameter;
import org.wcs.smart.event.i2.entity.EntityMapping.Type;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.ui.model.IActionParameterCollector;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileEntityType;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Create entity action parameter collector
 * @author Emily
 *
 */
public class CreateEntityParameterCollector implements IActionParameterCollector {

	private String initEntityTypeKey = null;
	private String initProfileKey = null;

	private ComboViewer cmbEntityType;
	private ComboViewer cmbProfile;
	private TableViewer tblMappings;

	private List<Listener> modifyListeners;
	private List<EntityMapping> mappings = new ArrayList<>();
	private List<Attribute> dmAttributes;
	
	private IntelEntityType lastSelection = null;
	private List<IntelEntityType> entityTypes = null;
	
	private Composite warningSection = null;
	private Label warningLabel = null;
	
	public CreateEntityParameterCollector() {
		modifyListeners = new ArrayList<>();
	}

	@Override
	public void initParameters(EAction action) {
		EActionParameterValue profileParam = action.findParameter(ProfileParameter.INSTANCE.getKey());
		if (profileParam != null) {
			initProfileKey = profileParam.getParameterValue();
			initProfileCombo(profileParam.getParameterValue());
		}
		
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
		EActionParameterValue sourceParam = action.findParameter(ProfileParameter.INSTANCE.getKey());
		IntelProfile profile  = (IntelProfile) cmbProfile.getStructuredSelection().getFirstElement();
		if (sourceParam == null) {
			sourceParam = new EActionParameterValue();
			sourceParam.getId().setAction(action);
			sourceParam.getId().setParameterKey(ProfileParameter.INSTANCE.getKey());
			action.getParameters().add(sourceParam);
		}
		sourceParam.setParameterValue(profile.getKeyId());
		
		sourceParam = action.findParameter(EntityTypeParameter.INSTANCE.getKey());
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
			//order does matter in the case of duplicate mappings
			JSONArray jj = new JSONArray();
			for (EntityMapping m : mappings) {				
				jj.add(m.toJson());
			}
			datamodelParam.setParameterValue(jj.toJSONString());
		}
		
	}

	@Override
	public String validate() {
		Object profile = cmbProfile.getStructuredSelection().getFirstElement();
		if (profile == null || !(profile instanceof IntelProfile)) {
			return Messages.CreateEntityParameterCollector_ProfileRequired;
		}
		
		Object et = cmbEntityType.getStructuredSelection().getFirstElement();
		if ( et == null || !(et instanceof IntelEntityType)) {
			return Messages.CreateEntityParameterCollector_EntityTypeRequired;
		}
		
		Set<String> entityAttributeKeys = new HashSet<>();
		Set<String> duplicates = new HashSet<>();
		for (EntityMapping mm : mappings) {
			String keyId = mm.getEntityAttribute().getKeyId();
			if (entityAttributeKeys.contains(keyId)) {
				duplicates.add(mm.getEntityAttribute().getName());
			}
			entityAttributeKeys.add(keyId);
		}
		
		if (!duplicates.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.CreateEntityParameterCollector_MultipleMappings);
			for (String d : duplicates) {
				sb.append(d);
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length()-1);
			sb.deleteCharAt(sb.length()-1);
			
			warningLabel.setText(sb.toString());
			warningSection.setVisible(true);
		}else {
			warningLabel.setText(""); //$NON-NLS-1$
			warningSection.setVisible(false);
		}
		
		return null;
	}

	@Override
	public Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		top.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Label l = new Label(top, SWT.NONE);
		l.setText(Messages.CreateEntityParameterCollector_ProfileLabel);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbProfile = new ComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbProfile.setContentProvider(ArrayContentProvider.getInstance());
		cmbProfile.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IntelProfile) {
					return ((IntelProfile) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbProfile.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbProfile.getControl().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbProfile.addSelectionChangedListener(e->{
			initSourceCombo(initEntityTypeKey);
		});
		
		l = new Label(top, SWT.NONE);
		l.setText(Messages.CreateEntityParameterCollector_EntityTypeLabel);
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
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
		cmbEntityType.getControl().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));


		l = new Label(main, SWT.NONE);
		l.setText(Messages.CreateEntityParameterCollector_MappingsLable);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		
		Composite bottom = new Composite(main, SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		bottom.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		
		tblMappings = new TableViewer(bottom, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
		tblMappings.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblMappings.getTable().setHeaderVisible(true);
		tblMappings.getTable().setLinesVisible(true);
		tblMappings.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn entityColumn = new TableViewerColumn(tblMappings, SWT.NONE);
		entityColumn.getColumn().setText(Messages.CreateEntityParameterCollector_AttributeColumn);
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
		dmColumn.getColumn().setText(Messages.CreateEntityParameterCollector_DmAttributeColumn);
		dmColumn.getColumn().pack();
		dmColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EntityMapping) {
					StringBuilder sb = new StringBuilder();
					EntityMapping map = (EntityMapping) element;
					if (map.getDataModelAttribute() == null) return ""; //fixed //$NON-NLS-1$
					sb.append(map.getDataModelAttribute().getName());
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn fixedColumn = new TableViewerColumn(tblMappings, SWT.NONE);
		fixedColumn.getColumn().setText(Messages.CreateEntityParameterCollector_FixedOp);
		fixedColumn.getColumn().pack();
		fixedColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EntityMapping) {
					EntityMapping map = (EntityMapping) element;
					if (map.getType() == Type.FIXED) {
						return map.getFixedValueAsString(Locale.getDefault());
					}else if (map.getType() == Type.POSITION) {
						return Messages.CreateEntityParameterCollector_PositionLabel;
					}
					return ""; //$NON-NLS-1$
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
		buttonArea.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		
		Button btnAdd = new Button(buttonArea, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(buttonArea.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addListener(SWT.Selection, e->addMapping());
		btnAdd.setEnabled(false);
		
		Button btnEdit= new Button(buttonArea, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(buttonArea.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addListener(SWT.Selection, e->editMapping());
		btnEdit.setEnabled(false);
		
		Button btnRemove= new Button(buttonArea, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnRemove.setBackground(buttonArea.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnRemove.addListener(SWT.Selection, e->deleteMapping());
		btnRemove.setEnabled(false);
		
		l = new Label(buttonArea, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		Button btnMoveUp= new Button(buttonArea, SWT.PUSH);
		btnMoveUp.setText(Messages.CreateEntityParameterCollector_UpButton);
		btnMoveUp.setBackground(buttonArea.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveUp.addListener(SWT.Selection, e->move(-1));
		btnMoveUp.setEnabled(false);
		
		Button btnMoveDown= new Button(buttonArea, SWT.PUSH);
		btnMoveDown.setText(Messages.CreateEntityParameterCollector_DownButton);
		btnMoveDown.setBackground(buttonArea.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveDown.addListener(SWT.Selection, e->move(1));
		btnMoveDown.setEnabled(false);
		
		warningSection = new Composite(main, SWT.NONE);
		warningSection.setLayout(new GridLayout(2, false));
		warningSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)warningSection.getLayout()).marginWidth = 0;
		((GridLayout)warningSection.getLayout()).marginHeight = 0;
		warningSection.setVisible(false);
		warningSection.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		l = new Label(warningSection, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		warningLabel = new Label(warningSection, SWT.WRAP);
		warningLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)warningLabel.getLayoutData()).widthHint = 100;
		warningLabel.setText(""); //$NON-NLS-1$
		warningLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		cmbEntityType.addSelectionChangedListener(e->{
			btnAdd.setEnabled(
					cmbEntityType.getStructuredSelection().getFirstElement() instanceof IntelEntityType &&
					cmbProfile.getStructuredSelection().getFirstElement() instanceof IntelProfile);
			
			if (lastSelection == cmbEntityType.getStructuredSelection().getFirstElement()) return;
			if (!mappings.isEmpty()) {
				if (!MessageDialog.openConfirm(main.getShell(), Messages.CreateEntityParameterCollector_ClearTitle, Messages.CreateEntityParameterCollector_ClearMessage)) {
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
			btnEdit.setEnabled(!tblMappings.getStructuredSelection().isEmpty());
			btnMoveUp.setEnabled(!tblMappings.getStructuredSelection().isEmpty());
			btnMoveDown.setEnabled(!tblMappings.getStructuredSelection().isEmpty());
		});
		
		Menu mnu = new Menu(tblMappings.getControl());
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addMapping());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->editMapping());
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteMapping());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		 
		MenuItem mnuMoveUp = new MenuItem(mnu, SWT.PUSH);
		mnuMoveUp.setText(Messages.CreateEntityParameterCollector_UpButton);
		mnuMoveUp.addListener(SWT.Selection, e->move(-1));
		
		MenuItem mnuDownUp = new MenuItem(mnu, SWT.PUSH);
		mnuDownUp.setText(Messages.CreateEntityParameterCollector_DownButton);
		mnuDownUp.addListener(SWT.Selection, e->move(1));
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuDelete.setEnabled(!tblMappings.getSelection().isEmpty());
				mnuEdit.setEnabled(!tblMappings.getSelection().isEmpty());
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
	
	private void move(int dir) {
		Object x = tblMappings.getStructuredSelection().iterator().next();
		int index = mappings.indexOf(x);
		index += dir;
		
		if (index < 0) index = 0;
		if (index >= mappings.size()) index = mappings.size()-1;
		
		mappings.remove(x);
		mappings.add(index, (EntityMapping) x);
		tblMappings.refresh();
		fireListeners();
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
		EntityMapping mapping = dialog.getMapping();
		if (mapping != null) {
			mappings.add(dialog.getMapping());
			tblMappings.refresh();
			fireListeners();
		}
	}
	
	private void editMapping() {
		EntityMapping toEdit = (EntityMapping) tblMappings.getStructuredSelection().iterator().next();
		if (toEdit == null) return;
		Object x = cmbEntityType.getStructuredSelection().getFirstElement();
		if (!(x instanceof IntelEntityType)) return;
		
		NewMappingDialog dialog = new NewMappingDialog(cmbEntityType.getControl().getShell(), (IntelEntityType)x, dmAttributes, toEdit);
		if (dialog.open() != Window.OK) return;
		EntityMapping mapping = dialog.getMapping();
		if (mapping == null) {
			mappings.remove(toEdit);
		}else {
			int index = mappings.indexOf(toEdit);
			mappings.remove(toEdit);
			mappings.add(index, mapping);
		}
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
		if (entityTypes == null) return;
		
		IntelProfile ip = (IntelProfile) cmbProfile.getStructuredSelection().getFirstElement();

		Object selection = null;
		List<Object> items = new ArrayList<>();
		for (IntelEntityType rs : entityTypes) {
			for (IntelProfileEntityType ipr : rs.getProfiles()) {
				if (ipr.getProfile().equals(ip)) {
					items.add(rs);
					if (rs.getKeyId().equalsIgnoreCase(key)) {
						selection = rs;
					}else if (selection == null) {
						selection = rs;
					}
				}
			}
		}
		
		cmbEntityType.setInput(items);
		if (selection != null) cmbEntityType.setSelection(new StructuredSelection(selection));
		cmbEntityType.addSelectionChangedListener(evt->fireListeners());
	}
	
	private void initProfileCombo(String key) {
		if (key == null) return;
		
		Object x = cmbProfile.getInput();
		if (!(x instanceof List)) return;
		
		try {
			List<?> items = (List<?>)x;
			for (Object item : items) {
				if ((item instanceof IntelProfile) && (((IntelProfile)item).getKeyId()).equals(key)){
					cmbProfile.setSelection(new StructuredSelection(item));
					return;
				}
			}
		}finally {
			cmbProfile.addSelectionChangedListener(evt->fireListeners());
		}
	}
	private Job loadEntities = new Job(Messages.CreateEntityParameterCollector_loadingJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Attribute> dmAttributes = new ArrayList<>();
			List<IntelProfile> profiles = new ArrayList<>();

			try(Session session = HibernateManager.openSession()){
				
				profiles.addAll( QueryFactory.buildQuery(session, IntelProfile.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list() ); //$NON-NLS-1$
				
				entityTypes = QueryFactory.buildQuery(session, IntelEntityType.class, 
						"conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
				for (IntelEntityType src : entityTypes) {
					src.getProfiles().size();
					src.getAttributes().forEach(a->{
						a.getAttribute().getName();
						if(a.getAttribute().getType() == AttributeType.LIST) {
							a.getAttribute().getAttributeList().forEach(e->e.getName());
						}
					});
					if (src.getKeyId().equalsIgnoreCase(initEntityTypeKey)) {
						lastSelection = src;
					}
				}
				
				dmAttributes.addAll(QueryFactory.buildQuery(session, Attribute.class,
						"conservationArea", SmartDB.getCurrentConservationArea()).list()); //$NON-NLS-1$
				for (Attribute a : dmAttributes) {
					a.getName();
				}
			}
			
			profiles.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			entityTypes.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			dmAttributes.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			CreateEntityParameterCollector.this.dmAttributes = dmAttributes;
			Display.getDefault().syncExec(()->{
				if (cmbProfile.getControl().isDisposed()) return;
				cmbProfile.setInput(profiles);
				initProfileCombo(initProfileKey);
				initSourceCombo(initEntityTypeKey);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
		
}
