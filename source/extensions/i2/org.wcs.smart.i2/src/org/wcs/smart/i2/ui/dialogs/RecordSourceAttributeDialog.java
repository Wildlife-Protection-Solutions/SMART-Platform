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
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.ui.IconComposite;
import org.wcs.smart.i2.ui.RecordSourceAttributeLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.handler.EditRecordTemplateHandler;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * Dialog for configuring record source attributes
 * @author Emily
 *
 */
public class RecordSourceAttributeDialog extends TitleAreaDialog{

	@Inject
	protected IEclipseContext context;
	
	private TableViewer lstSources;
	private Label sourceLabel;
	private TableViewer lstAttributes;
	private IconComposite iconComp;
	
	private Button[] attributeButtons;
	private Button[] srcButtons;
	private MenuItem[] srcMenu;
	private MenuItem[] attributeMenu;
	
	private List<IntelRecordSource> sources;
	private List<IntelRecordSource> toDelete;
	private List<IntelRecordSourceAttribute> attributesToDelete;
	private List<IntelRecordSourceAttribute> attributesMultiToSingle;	//tracks attribute which are changed from multi to single
		
	private IntelRecordSource currentSelection = null;
	
	private Job loadSources = new Job(Messages.RecordSourceAttributeDialog_LoadSourceJobName){
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordSource> srcs = s.createCriteria(IntelRecordSource.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.list();
				srcs.forEach(e -> {
					e.getNames().size();
					if (e.getAttributes() != null){
						e.getAttributes().forEach(a->{
							a.getNames().size();
							a.getAttribute();
							a.getEntityType();
						});
					}
				});
				sources = srcs;
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(()->{
				lstSources.setInput(sources);
				for (Button b : srcButtons) b.setEnabled(true);
				for (MenuItem b : srcMenu) b.setEnabled(true);
			});
			return Status.OK_STATUS;
		}
	};
	
	
	public RecordSourceAttributeDialog(Shell parent){
		super(parent);		
		toDelete = new ArrayList<>();
		attributesToDelete = new ArrayList<>();	
		attributesMultiToSingle = new ArrayList<>();
	}
	
	@Override
	public Point getInitialSize(){
		return new Point(750, 440);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout(3, false));
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createSourceComposite(body);
		new Label(body, SWT.NONE);	//spacer
		createDetailsPanel(body);
		
		//init
		lstSources.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstAttributes.getTable().setEnabled(false);
		iconComp.setEnabled(false);
		loadSources.schedule();
		for (Button b : attributeButtons) b.setEnabled(false);
		for (MenuItem b : attributeMenu) b.setEnabled(false);
		for (Button b : srcButtons) b.setEnabled(false);
		for (MenuItem b : srcMenu) b.setEnabled(false);
		
		setTitle(Messages.RecordSourceAttributeDialog_Title);
		setMessage(Messages.RecordSourceAttributeDialog_Message);
		getShell().setText(Messages.RecordSourceAttributeDialog_Title);
		
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
		if (doSave()){
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	private void updateSourceSelection(){
		currentSelection = null;
		Object x = ((IStructuredSelection)lstSources.getSelection()).getFirstElement();
		if (x instanceof IntelRecordSource){
			currentSelection = (IntelRecordSource) x;
		}
		
		if (currentSelection == null){
			iconComp.setEnabled(false);
			iconComp.setImage(null);
			sourceLabel.setText(""); //$NON-NLS-1$
			lstAttributes.setInput(null);
			lstAttributes.getControl().setEnabled(false);
			for (Button b : attributeButtons) b.setEnabled(false);
			for (MenuItem b : attributeMenu) b.setEnabled(false);
		}else{
			iconComp.setEnabled(true);
			iconComp.setImage(currentSelection.getIcon());
			sourceLabel.setText(currentSelection.getName());
			lstAttributes.setInput(currentSelection.getAttributes());
			lstAttributes.getControl().setEnabled(true);
			for (Button b : attributeButtons) b.setEnabled(true);
			for (MenuItem b : attributeMenu) b.setEnabled(true);
		}
	}
	
	private void createDetailsPanel(Composite parent){
		Composite detailsPanel = new Composite(parent, SWT.NONE);
		detailsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailsPanel.setLayout(new GridLayout(2, false));
		
		sourceLabel = new Label(detailsPanel, SWT.NONE);
		sourceLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		FontData fd = sourceLabel.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font f = new Font(sourceLabel.getDisplay(), fd);
		sourceLabel.setFont(f);
		sourceLabel.addListener(SWT.Dispose, e->f.dispose());
		
		Label l = new Label(detailsPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite iconc = new Composite(detailsPanel, SWT.NONE);
		iconc.setLayout(new GridLayout(2, false));
		iconc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridLayout)iconc.getLayout()).marginWidth = 0;
		((GridLayout)iconc.getLayout()).marginHeight = 0;
		
		l = new Label(iconc, SWT.NONE);
		l.setText(Messages.RecordSourceAttributeDialog_IconLabel);
		
		iconComp = new IconComposite(iconc);
		iconComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		iconComp.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentSelection != null){
					currentSelection.setIcon(iconComp.getImage());
					((RecordSourceLabelProvider)lstSources.getLabelProvider()).disposeImages();
					lstSources.refresh();
					modified();
				}
			}
		});
		
		
		Label attributesLabel = new Label(detailsPanel, SWT.NONE);
		attributesLabel.setText(Messages.RecordSourceAttributeDialog_AttributeLabel);
		attributesLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lstAttributes = new TableViewer(detailsPanel, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		lstAttributes.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributes.setLabelProvider(new RecordSourceAttributeLabelProvider());
		lstAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
						//TODO: also add to default language;
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
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addListener(SWT.Selection, e->{
			addAttribute();
		});
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addListener(SWT.Selection, e->{
			deleteAttribute();
		});
		
		l = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Button btnMoveUp = new Button(buttonPanel, SWT.PUSH);
		btnMoveUp.setText(Messages.RecordSourceAttributeDialog_moveUpLabel);
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveUp.addListener(SWT.Selection, e->{
			moveAttribute(-1);
		});
		
		Button btnMoveDown = new Button(buttonPanel, SWT.PUSH);
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
		mnuEdit.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
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
	
	
	private void addAttribute(){
		Object x = ((IStructuredSelection)lstSources.getSelection()).getFirstElement();
		if (x == null || !(x instanceof IntelRecordSource)) return;
		IntelRecordSource source = (IntelRecordSource) x;
		if (source.getAttributes() == null){
			lstAttributes.setInput(source.getAttributes());
			source.setAttributes(new ArrayList<>());
		}
		SelectAttributeEntityTypeDialog dialog = new SelectAttributeEntityTypeDialog(getShell(), MessageFormat.format(Messages.RecordSourceAttributeDialog_AddDialogTitle, source.getName()));
		ContextInjectionFactory.inject(dialog, context);				
		if (dialog.open() == Window.OK){
			for (NamedItem ia : dialog.getSelectedAttributes()){
				IntelRecordSourceAttribute attribute = new IntelRecordSourceAttribute();
				attribute.setIsMultiple(null);
				if (ia instanceof IntelAttribute){
					attribute.setAttribute((IntelAttribute) ia);
					if (((IntelAttribute) ia).getType() == AttributeType.LIST){
						attribute.setIsMultiple(false);
					}
				}else if (ia instanceof IntelEntityType){
					attribute.setEntityType((IntelEntityType) ia);
					attribute.setIsMultiple(false);
				}else{
					continue;
				}
				
				attribute.setSource(source);
				attribute.setOrder(source.getAttributes().size() + 1);
				source.getAttributes().add(attribute);
				lstAttributes.refresh();
				modified();
			}
		}
	}
	private void createSourceComposite(Composite parent){
		Composite srcPanel = new Composite(parent, SWT.NONE);
		srcPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)srcPanel.getLayoutData()).widthHint = 250;
		srcPanel.setLayout(new GridLayout(2, false));
		
		Label l = new Label(srcPanel, SWT.NONE);
		l.setText(Messages.RecordSourceAttributeDialog_RecordSourceLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lstSources = new TableViewer(srcPanel, SWT.BORDER);
		lstSources.setContentProvider(ArrayContentProvider.getInstance());
		lstSources.setLabelProvider(new RecordSourceLabelProvider());
		lstSources.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstSources.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editSource();
			}
		});
		
		
		
		Composite buttonPanel = new Composite(srcPanel, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		buttonPanel.setLayoutData(new GridData(SWT.TOP, SWT.FILL, false, false));
		
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnAdd.addListener(SWT.Selection, e->{
			addSource();
		});
		
		Button btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addListener(SWT.Selection, e->{
			editSource();
		});
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addListener(SWT.Selection, e->{
			deleteSource();
		});
		
		
		Menu srcMenu = new Menu(lstSources.getControl());
		lstSources.getControl().setMenu(srcMenu);
		
		MenuItem mnuAdd = new MenuItem(srcMenu,SWT.PUSH);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.addListener(SWT.Selection, e->addSource());
		
		MenuItem mnuEdit = new MenuItem(srcMenu,SWT.PUSH);
		mnuEdit.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.addListener(SWT.Selection, e->editSource());
		
		MenuItem mnuDelete = new MenuItem(srcMenu,SWT.PUSH);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.addListener(SWT.Selection, e->deleteSource());
		
		
		lstSources.addSelectionChangedListener(e->{
			btnDelete.setEnabled(!e.getSelection().isEmpty());
			btnEdit.setEnabled(!e.getSelection().isEmpty());
			mnuDelete.setEnabled(!e.getSelection().isEmpty());
			mnuEdit.setEnabled(!e.getSelection().isEmpty());
			updateSourceSelection();
		});
		
		
		Hyperlink hk = new Hyperlink(srcPanel, SWT.NONE);
		hk.setText(Messages.RecordSourceAttributeDialog_EditTemplateLink);
		hk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		hk.setUnderlined(true);
		hk.setForeground(hk.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
		
		hk.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (doSave()){
					RecordSourceAttributeDialog.this.cancelPressed();
					(new EditRecordTemplateHandler()).execute();
				}
			}
		});
		
		srcButtons = new Button[]{btnAdd, btnEdit, btnDelete};
		this.srcMenu = new MenuItem[]{mnuAdd, mnuEdit, mnuDelete};
	}
	
	private void addSource(){
		IntelRecordSource newItem = new IntelRecordSource();
		newItem.setConservationArea(SmartDB.getCurrentConservationArea());
		NameKeyDialog<IntelRecordSource> dialog = new NameKeyDialog<IntelRecordSource>(getShell(), newItem, sources){
			@Override
			protected String getTitle(){
				return Messages.RecordSourceAttributeDialog_NewSourceDialogTitle;
			}
		};
		if (dialog.open() == Window.OK){
			IntelRecordSource updatedItem = dialog.getUpdatedItem();
			if (updatedItem.getAttributes() == null) updatedItem.setAttributes(new ArrayList<>());
			sources.add(updatedItem);
			lstSources.refresh();
			lstSources.setSelection(new StructuredSelection(updatedItem));
			modified();
		}
	}
	
	private void deleteSource(){
		StructuredSelection s = (StructuredSelection) lstSources.getSelection();
		List<IntelRecordSource> delete = new ArrayList<IntelRecordSource>();
		
		for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof IntelRecordSource){
				delete.add((IntelRecordSource)x);
			}
		}
		if (delete.isEmpty()) return;
		if (!MessageDialog.openConfirm(getShell(), Messages.RecordSourceAttributeDialog_DeleteSourceDialogTitle, MessageFormat.format(Messages.RecordSourceAttributeDialog_DeleteSourceDialogMsg, delete.size()))){
			return;
		}
		sources.removeAll(delete);
		toDelete.addAll(delete);
		lstSources.refresh();
		modified();
	}
	
	private void editSource(){
		Object selection = ((StructuredSelection) lstSources.getSelection()).getFirstElement();
		if (selection != null && selection instanceof IntelRecordSource){
			IntelRecordSource source = (IntelRecordSource) selection;
			List<IntelRecordSource> kids = new ArrayList<IntelRecordSource>();
			kids.addAll(sources);
			kids.remove(source);
			
			NameKeyDialog<IntelRecordSource> dialog = new NameKeyDialog<IntelRecordSource>(getShell(), source, kids){
				@Override
				protected String getTitle(){
					return Messages.RecordSourceAttributeDialog_EditoSourceDialogTitle;
				}
			};
			if (dialog.open() == Window.OK){
				lstSources.refresh();
				modified();
			}
		}
	}
	private boolean doSave(){
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			
			//delete all attribute values for attributes removed from given source
			for (IntelRecordSourceAttribute a : attributesToDelete){
				if (a.getUuid() == null) continue; //not saved skip
				
				//delete any values associated with this attribute 
				Query q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
				q.setParameter("attribute", a); //$NON-NLS-1$
				q.executeUpdate();
				
				//delete the attributes
				session.delete(a);
			}
			
			//delete all attributes values for attributes that changes from multi to single
			for (IntelRecordSourceAttribute a : attributesMultiToSingle){
				if (a.getUuid() == null) continue; //not saved skip 
				
				//delete any values associated with this attribute 
				Query q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
				q.setParameter("attribute", a); //$NON-NLS-1$
				q.executeUpdate();
			}
			
			//delete all records for removed sources
			for (IntelRecordSource source : toDelete){
				if (source.getUuid() == null) continue;
				
				//update intelligence records to have no source
				Query q = session.createQuery("UPDATE IntelRecord SET recordSource = null WHERE recordSource = :source"); //$NON-NLS-1$
				q.setParameter("source", source); //$NON-NLS-1$
				q.executeUpdate();
				
				//remove all attributes associated with source
				for (IntelRecordSourceAttribute a : source.getAttributes()){
					//delete any values associated with this attribute 
					q = session.createQuery("DELETE FROM IntelRecordAttributeValue a where a.attribute = :attribute "); //$NON-NLS-1$
					q.setParameter("attribute", a); //$NON-NLS-1$
					q.executeUpdate();
					//delete the attributes
					session.delete(a);
				}
				//delete source
				session.delete(source);
			}
			
			//save updates
			for (IntelRecordSource source : sources){
				session.saveOrUpdate(source);
			}
			session.getTransaction().commit();
			
			//clear all delete lists
			attributesToDelete.clear();
			attributesMultiToSingle.clear();
			toDelete.clear();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.RecordSourceAttributeDialog_SaveError + ex.getMessage(), ex);
			return false;
		}finally{
			session.close();
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
