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
package org.wcs.smart.i2.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.common.control.IconComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EditValidator;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceAttributeLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * Dialog for configuring record source attributes
 * @author Emily
 *
 */
public class RecordSourceDialog extends SmartStyledTitleDialog{

	@Inject
	protected IEclipseContext context;
	
	private TableViewer lstAttributes;
	private IconComposite iconComp;
	
	private Button[] attributeButtons;
	private MenuItem[] attributeMenu;
	
	private NameKeyComposite comp ;
	
	private List<IntelRecordSourceAttribute> attributesToDelete;
	private List<IntelRecordSourceAttribute> attributesMultiToSingle;	//tracks attribute which are changed from multi to single
		
	private IntelRecordSource currentSelection = null;
	private CheckboxTableViewer tblProfiles = null;
	
	private Job loadSource = new Job(Messages.RecordSourceDialog_jobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelProfile> profiles = new ArrayList<>();
			List<IntelRecordSource> srcs = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				profiles.addAll(ProfilesManager.INSTANCE.getProfiles(session, false));
				
				srcs.addAll(QueryFactory.buildQuery(session, IntelRecordSource.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				
				if (currentSelection.getUuid() != null) {
					IntelRecordSource src = session.get(IntelRecordSource.class, currentSelection.getUuid());
					srcs.remove(src);
				
					src.getNames().size();
				
					src.getProfiles().forEach(p->p.getRecordSource().getName());
				
					if (src.getAttributes() != null){
						src.getAttributes().forEach(a->{
							a.getNames().size();
							a.getAttribute();
							if (a.getEntityType() != null) a.getEntityType().getProfiles().forEach(p->p.getProfile().getUuid());
						});
					}
					currentSelection = src;
				}
			}
			
			Display.getDefault().asyncExec(()->{
				iconComp.setEnabled(true);
				iconComp.setImage(currentSelection.getIcon());
				lstAttributes.setInput(currentSelection.getAttributes());
				lstAttributes.getControl().setEnabled(true);
				tblProfiles.getControl().setEnabled(true);
				comp.initFields(currentSelection, srcs, SmartDB.getCurrentConservationArea().getDefaultLanguage());
				for (Button b : attributeButtons) b.setEnabled(true);
				for (MenuItem b : attributeMenu) b.setEnabled(true);
				tblProfiles.setInput(profiles);
				tblProfiles.setAllChecked(false);
				for (IntelProfileRecordSource p : currentSelection.getProfiles()) tblProfiles.setChecked(p.getProfile(), true);
				comp.validate();
				getButton(IDialogConstants.OK_ID).setEnabled(false);

				
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	public RecordSourceDialog(Shell parent, IntelRecordSource source){
		super(parent);		
		this.currentSelection = source;
		attributesToDelete = new ArrayList<>();	
		attributesMultiToSingle = new ArrayList<>();
	}
	
	@Override
	public Point getInitialSize(){
		return new Point(750, 550);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout(3, false));
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createDetailsPanel(body);
		
		//init
		lstAttributes.getTable().setEnabled(false);
		iconComp.setEnabled(false);
		for (Button b : attributeButtons) b.setEnabled(false);
		for (MenuItem b : attributeMenu) b.setEnabled(false);
		
		setTitle(currentSelection.getName());
		setMessage(Messages.RecordSourceAttributeDialog_Message);
		getShell().setText(currentSelection.getName());
		
		loadSource.schedule();
		
		return parent;
	}

	public void cancelPressed(){
		if (getButton(IDialogConstants.OK_ID).isEnabled()){
			if (MessageDialog.openQuestion(getShell(), Messages.RecordSourceAttributeDialog_ConfirmSaveTitle, Messages.RecordSourceAttributeDialog_ConfirmSaveMessage)){
				if (doSave()){
					super.cancelPressed();		
				}
			}else{
				super.cancelPressed();
			}
			
		}else{
			super.cancelPressed();
		}
	}
	
	@Override
	public void okPressed(){
		if (comp.validate()) {
			MessageDialog.openError(getShell(), Messages.RecordSourceDialog_ErrorTitle,  Messages.RecordSourceDialog_NameAndKeyRequired);
			return;
		}
		if (doSave()){
			Resources.INSTANCE.clearRecordSourceImageCache();
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			
		}
	}

	private void createDetailsPanel(Composite parent){
		Composite detailsPanel = new Composite(parent, SWT.NONE);
		detailsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailsPanel.setLayout(new GridLayout(3, false));
		
		comp = new NameKeyComposite();
		comp.createControls(detailsPanel, true, currentSelection.getUuid() == null, new IChangeListener() {
			@Override
			public void itemModified() {
				if (!comp.validate()){
					comp.updateFields(currentSelection);
				}
				modified();
			}
		});
		
		Label l = new Label(detailsPanel, SWT.NONE);
		l.setText(Messages.RecordSourceAttributeDialog_IconLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		iconComp = new IconComposite(detailsPanel);
		iconComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		iconComp.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentSelection != null){
					currentSelection.setIcon(iconComp.getImage());
					modified();
				}
			}
		});
	
		l = new Label(detailsPanel, SWT.NONE);
		l.setText(Messages.RecordSourceDialog_ProfilesLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		tblProfiles = CheckboxTableViewer.newCheckList(detailsPanel, SWT.BORDER);
		tblProfiles.setContentProvider(ArrayContentProvider.getInstance());
		tblProfiles.setLabelProvider(new ProfileLabelProvider());
		tblProfiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)tblProfiles.getControl().getLayoutData()).heightHint = 50;
		tblProfiles.addSelectionChangedListener(e->{
			modified();
		});
		new Label(detailsPanel, SWT.NONE);
		Label attributesLabel = new Label(detailsPanel, SWT.NONE);
		attributesLabel.setText(Messages.RecordSourceAttributeDialog_AttributeLabel);
		attributesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		lstAttributes = new TableViewer(detailsPanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new RecordSourceAttributeLabelProvider());
		lstAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		lstAttributes.getTable().setHeaderVisible(true);
		
		ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(lstAttributes) {
		    protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
		    }
		};
		
		TableViewerEditor.create(lstAttributes, activationSupport, ColumnViewerEditor.TABBING_HORIZONTAL);
		
		RecordSourceAttributeLabelProvider provider = new RecordSourceAttributeLabelProvider();
		TableViewerColumn colIcon = new TableViewerColumn(lstAttributes, SWT.NONE);
		colIcon.getColumn().setWidth(150);
		colIcon.setLabelProvider(provider);
		colIcon.getColumn().setText(Messages.RecordSourceAttributeDialog_AttributeTypeLabel);
		
		TableViewerColumn colName = new TableViewerColumn(lstAttributes, SWT.NONE);
		colName.getColumn().setWidth(150);
		colName.setLabelProvider(new RecordSourceAttributeLabelProvider(RecordSourceAttributeLabelProvider.Column.NAME));
		colName.getColumn().setText(Messages.RecordSourceAttributeDialog_FieldNameLabel);
		

		colName.setEditingSupport(new EditingSupport(colName.getViewer()) {
			TextCellEditor editor = new TextCellEditor(lstAttributes.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof IntelRecordSourceAttribute){
					String newName = (String) value;
					if (newName.isEmpty() || newName.equals(provider.getText(element))){
						
						for (org.wcs.smart.ca.Label l : ((IntelRecordSourceAttribute) element).getNames()){
							if (l.getLanguage().equals(SmartDB.getCurrentLanguage())){
								((IntelRecordSourceAttribute)element).getNames().remove(l);
								break;
							}
						}
						((IntelRecordSourceAttribute) element).setName(null);
					}else{
						IntelRecordSourceAttribute e = (IntelRecordSourceAttribute)element;
						e.updateName(SmartDB.getCurrentLanguage(), (String)value);
						e.setName((String)value);
						
						if (e.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage()) == null){
							e.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), (String)value);
						}
					}
					lstAttributes.refresh();
					modified();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof IntelRecordSourceAttribute){
					String name = ((IntelRecordSourceAttribute) element).getName();
					if (name == null){
						return provider.getText(element);
					}
					return name;
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof IntelRecordSourceAttribute) return true;
				return false;
			}
		});
		
		TableViewerColumn colKeyId = new TableViewerColumn(lstAttributes, SWT.NONE);
		colKeyId.getColumn().setWidth(100);
		colKeyId.setLabelProvider(new RecordSourceAttributeLabelProvider(RecordSourceAttributeLabelProvider.Column.KEY));
		colKeyId.getColumn().setText(Messages.RecordSourceDialog_KeyColumn);
		colKeyId.setEditingSupport(new EditingSupport(colName.getViewer()) {
			TextCellEditor editor = new TextCellEditor(lstAttributes.getTable());
			@SuppressWarnings("unchecked")
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof IntelRecordSourceAttribute){
					String newKey = ((String) value).toLowerCase(); //todo remove all other characters
					
					//make sure key is unique
					List<IntelRecordSourceAttribute> items = new ArrayList<>((List<IntelRecordSourceAttribute>) lstAttributes.getInput());
					items.remove(element);
					String msg = DataModelManager.INSTANCE.validateKey(newKey, items);
					if (msg != null) {
						MessageDialog.openInformation(getShell(), Messages.RecordSourceDialog_InvalidTitle, msg);
						return;
					}
					((IntelRecordSourceAttribute)element).setKeyId(newKey);
					lstAttributes.refresh();
					modified();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof IntelRecordSourceAttribute){
					return ((IntelRecordSourceAttribute)element).getKeyId();
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof IntelRecordSourceAttribute) return true;
				return false;
			}
		});
		
		TableViewerColumn colMulti = new TableViewerColumn(lstAttributes, SWT.NONE);
		colMulti.getColumn().setWidth(50);
		colMulti.setLabelProvider(new RecordSourceAttributeLabelProvider(RecordSourceAttributeLabelProvider.Column.MULTI));
		colMulti.getColumn().setText(Messages.RecordSourceAttributeDialog_IsMultiLabel);
		colMulti.setEditingSupport(new EditingSupport(colName.getViewer()) {
			CheckboxCellEditor editor = new CheckboxCellEditor(lstAttributes.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof IntelRecordSourceAttribute){
					IntelRecordSourceAttribute a = (IntelRecordSourceAttribute)element;
					Boolean newValue = (Boolean)value;
					
					if (a.getIsMultiple() != null && a.getIsMultiple() && !newValue){
						if (!MessageDialog.openQuestion(getShell(), Messages.RecordSourceAttributeDialog_WarningDialogTitle, Messages.RecordSourceAttributeDialog_MultiWarning)){
							//do not update
							return;
						}
						attributesMultiToSingle.add(a);
					}
					if (a.getIsMultiple() != null && newValue && attributesMultiToSingle.contains(a)){
						//change from single to multi back to multi so we do not want to delete
						attributesMultiToSingle.remove(a);
					}
					((IntelRecordSourceAttribute)element).setIsMultiple(newValue);
					lstAttributes.refresh();
					modified();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof IntelRecordSourceAttribute){
					Boolean obj = ((IntelRecordSourceAttribute) element).getIsMultiple();
					if (obj == null) return false;
					return obj;
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				if (element instanceof IntelRecordSourceAttribute){
					IntelRecordSourceAttribute a = (IntelRecordSourceAttribute) element;
					if (a.isListAttribute()) return true;
				}
				return false;
			}
		});
		Composite buttonPanel = new Composite(detailsPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addListener(SWT.Selection, e->{
			addAttribute();
		});
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addListener(SWT.Selection, e->{
			deleteAttribute();
		});
		
		l = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button btnMoveUp = new Button(buttonPanel, SWT.PUSH);
		btnMoveUp.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveUp.setText(Messages.RecordSourceAttributeDialog_moveUpLabel);
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveUp.addListener(SWT.Selection, e->{
			moveAttribute(-1);
		});
		
		Button btnMoveDown = new Button(buttonPanel, SWT.PUSH);
		btnMoveDown.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveDown.setText(Messages.RecordSourceAttributeDialog_moveDownLabel);
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveDown.addListener(SWT.Selection, e->{
			moveAttribute(1);
		});
		
		
		lstAttributes.addSelectionChangedListener(e->{
			btnDelete.setEnabled(!e.getSelection().isEmpty());
			btnMoveDown.setEnabled(!e.getSelection().isEmpty());
			btnMoveUp.setEnabled(!e.getSelection().isEmpty());
		});
		
		Menu attributeMenu = new Menu(lstAttributes.getControl());
		lstAttributes.getControl().setMenu(attributeMenu);
		
		MenuItem mnuAdd = new MenuItem(attributeMenu,SWT.PUSH);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.addListener(SWT.Selection, e->addAttribute());
		
		MenuItem mnuDelete = new MenuItem(attributeMenu,SWT.PUSH);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.addListener(SWT.Selection, e->deleteAttribute());
		
		MenuItem mnuEdit = new MenuItem(attributeMenu,SWT.PUSH);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.setText(Messages.RecordSourceAttributeDialog_TranslateLabel);
		mnuEdit.addListener(SWT.Selection, e->editAttribute());
		
		new MenuItem(attributeMenu,SWT.SEPARATOR);
		
		MenuItem mnuMoveUp = new MenuItem(attributeMenu,SWT.PUSH);
		mnuMoveUp.setText(Messages.RecordSourceAttributeDialog_moveUpLabel);
		mnuMoveUp.addListener(SWT.Selection, e->moveAttribute(-1));
		
		MenuItem mnuMoveDown = new MenuItem(attributeMenu,SWT.PUSH);
		mnuMoveDown.setText(Messages.RecordSourceAttributeDialog_moveDownLabel);
		mnuMoveDown.addListener(SWT.Selection, e->moveAttribute(1));
		
		
		attributeButtons = new Button[]{btnAdd, btnDelete, btnMoveUp, btnMoveDown};
		this.attributeMenu = new MenuItem[]{mnuAdd, mnuDelete};
	}
	
	private void editAttribute(){
		StructuredSelection s = (StructuredSelection) lstAttributes.getSelection();
		IntelRecordSourceAttribute toEdit = null;
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof IntelRecordSourceAttribute){
				toEdit = (IntelRecordSourceAttribute)x;
				break;
			}
		}
		if (toEdit == null) return;
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(getShell(), toEdit);
		if (dialog.open() == TranslateSimpleListItemDialog.OK){
			modified();
		}
		lstAttributes.refresh();
	}
	
	private void moveAttribute(int amount){
		if (amount == 0) return;
		StructuredSelection s = (StructuredSelection) lstAttributes.getSelection();
		List<IntelRecordSourceAttribute> toMove = new ArrayList<IntelRecordSourceAttribute>();
		
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof IntelRecordSourceAttribute){
				toMove.add((IntelRecordSourceAttribute)x);
			}
		}
		if (amount < 0){
			toMove.sort((a,b) -> Integer.compare(a.getOrder(), b.getOrder()));
			if (toMove.get(0).getOrder() == 0) return;
		}else if (amount > 0){
			toMove.sort((a,b) -> -1 * Integer.compare(a.getOrder(), b.getOrder()));
			if (toMove.get(0).getOrder() == currentSelection.getAttributes().size() - 1) return;
		}
		
		
		for (IntelRecordSourceAttribute x : toMove){
			int pos = x.getOrder();
			pos += amount;
			if (pos < 0) pos = 0;
			if (pos > currentSelection.getAttributes().size()-1) pos = currentSelection.getAttributes().size() - 1;
			
			currentSelection.getAttributes().remove(x);
			currentSelection.getAttributes().add(pos, x);
		}
		for (int i = 0; i < currentSelection.getAttributes().size(); i ++){
			currentSelection.getAttributes().get(i).setOrder(i);
		}
		
		lstAttributes.refresh();
		modified();
	}
	
	private void deleteAttribute(){
		StructuredSelection s = (StructuredSelection) lstAttributes.getSelection();
		List<IntelRecordSourceAttribute> delete = new ArrayList<IntelRecordSourceAttribute>();
		
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof IntelRecordSourceAttribute){
				delete.add((IntelRecordSourceAttribute)x);
			}
		}
		if (delete.size() == 0) return;
		if (!MessageDialog.openConfirm(getShell(), Messages.RecordSourceAttributeDialog_DeleteAttributeTitle, MessageFormat.format(Messages.RecordSourceAttributeDialog_DeleteAttributeMsg, delete.size()))){
			return;
		}
		currentSelection.getAttributes().removeAll(delete);
		attributesToDelete.addAll(delete);
		lstAttributes.refresh();
		modified();
	}
	
	
	@SuppressWarnings("unchecked")
	private void addAttribute(){
		
		if (currentSelection.getAttributes() == null){
			lstAttributes.setInput(currentSelection.getAttributes());
			currentSelection.setAttributes(new ArrayList<>());
		}
		SelectAttributeEntityTypeDialog dialog = new SelectAttributeEntityTypeDialog(getShell(), MessageFormat.format(Messages.RecordSourceAttributeDialog_AddDialogTitle, currentSelection.getName()));
		ContextInjectionFactory.inject(dialog, context);				
		if (dialog.open() == Window.OK){
			for (NamedItem ia : dialog.getSelectedAttributes()){
				IntelRecordSourceAttribute attribute = new IntelRecordSourceAttribute();
				attribute.setIsMultiple(null);
				String keyid = ""; //$NON-NLS-1$
				if (ia instanceof IntelAttribute){
					attribute.setAttribute((IntelAttribute) ia);
					keyid = ((IntelAttribute) ia).getKeyId();
					if (((IntelAttribute) ia).getType() == AttributeType.LIST){
						attribute.setIsMultiple(false);
					}
				}else if (ia instanceof IntelEntityType){
					attribute.setEntityType((IntelEntityType) ia);
					attribute.setIsMultiple(false);
					keyid = ((IntelEntityType) ia).getKeyId();
				}else{
					continue;
				}
				
				keyid = DataModelManager.INSTANCE.generateKey(keyid,( List<IntelRecordSourceAttribute>)lstAttributes.getInput());
				
				attribute.setKeyId(keyid);
				attribute.setSource(currentSelection);
				attribute.setOrder(currentSelection.getAttributes().size() + 1);
				currentSelection.getAttributes().add(attribute);
				lstAttributes.refresh();
				modified();
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private boolean doSave(){
		Set<IntelProfile> newProfiles = new HashSet<>();
		for (Object x : tblProfiles.getCheckedElements()) newProfiles.add((IntelProfile)x);
		
		try(Session session = HibernateManager.openSession()){
			//validate that all record sources are still valid
			//which means that the profile associated with the record
			//must match one of the profiles associated with that record source
			if (currentSelection.getUuid() != null) session.update(currentSelection);
			if (!newProfiles.isEmpty() && currentSelection.getUuid() != null) {
				String hsql = "SELECT count(*) FROM IntelRecord r WHERE r.recordSource = :source and profile not in (:profiles)"; //$NON-NLS-1$
				Long cnt = (Long)session.createQuery(hsql).setParameter("source", currentSelection).setParameter("profiles", newProfiles).uniqueResult(); //$NON-NLS-1$ //$NON-NLS-2$
				if (cnt > 0) {
					throw new Exception(Messages.RecordSourceDialog_ProfileError);
				}
			}
			
			List<IntelProfile> profiles = (List<IntelProfile>) tblProfiles.getInput();
			Set<IntelProfileRecordSource> currentprofiles = new HashSet<>(currentSelection.getProfiles());

			for (IntelProfile ip : profiles) {
				ip = session.get(IntelProfile.class, ip.getUuid());
				IntelProfileRecordSource mp = new IntelProfileRecordSource();
				mp.getId().setProfile(ip);
				mp.getId().setRecordSource(currentSelection);
				
				
				if (tblProfiles.getChecked(ip)) {
					if (!currentSelection.getProfiles().contains(mp)) {
						currentSelection.getProfiles().add(mp);
					}
					
				}else if (currentSelection.getUuid() != null){
					IntelProfileRecordSource temp = session.get(IntelProfileRecordSource.class, mp.getId());
					if (temp != null) {
						ip.getRecordSources().remove(temp);
						temp.getRecordSource().getProfiles().remove(temp);
						currentSelection.getProfiles().remove(temp);
					}
				}
			}
			
			//validate the profiles
			String v = ProfilesManager.INSTANCE.validateRecords(currentSelection);
			if (v != null) {
				currentSelection.setProfiles(currentprofiles);
				throw new Exception(v);		
			}
			
			//validate events or other extensions
			v = EditValidator.INSTANCE.isValid(currentSelection, session);
			if (v != null) {
				currentSelection.setProfiles(currentprofiles);
				throw new Exception(v);
			}
			
			session.beginTransaction();
			try{
				session.saveOrUpdate(currentSelection);
				session.flush();
				
				//delete all attribute values for attributes removed from given source
				for (IntelRecordSourceAttribute a : attributesToDelete){
					if (a.getUuid() == null) continue; //not saved skip
					
					//delete any values associated with this attribute 
					Query<?> q = session.createQuery("delete from IntelRecordAttributeValueList ii where ii.id.value in (SELECT v FROM IntelRecordAttributeValue v where v.attribute = :attribute)"); //$NON-NLS-1$
					q.setParameter("attribute", a); //$NON-NLS-1$
					q.executeUpdate();
					

					//delete any values associated with this attribute 
					q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
					q.setParameter("attribute", a); //$NON-NLS-1$
					q.executeUpdate();
					
					//delete the attributes
				//	session.delete(a);
				//	currentSelection.getAttributes().remove(a);
				}
				session.flush();
				
				//delete all attributes values for attributes that changes from multi to single
				for (IntelRecordSourceAttribute a : attributesMultiToSingle){
					if (a.getUuid() == null) continue; //not saved skip 
					
					//delete any values associated with this attribute 
					Query<?> q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
					q.setParameter("attribute", a); //$NON-NLS-1$
					q.executeUpdate();
				}
				
				session.getTransaction().commit();
				
				//clear all delete lists
				attributesToDelete.clear();
				attributesMultiToSingle.clear();
			}catch (Exception ex){
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.RecordSourceAttributeDialog_SaveError + ex.getMessage(), ex);
				return false;
			}
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.RecordSourceAttributeDialog_SaveError + ex.getMessage(), ex);
			return false;
		}
		
		context.get(IEventBroker.class).send(IntelEvents.RECORD_SOURCE_ALL, null);
		
		return true;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void modified(){
		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
}
