/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.incident.ui;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.celleditor.ComboBoxViewerCellEditor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.IncidentPropertyManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.incident.patrol.IncidentToPatrolProcessorJob;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Property page for managing patrol teams.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class IncidentTypePropertyPage extends AbstractPropertyJHeaderDialog {

	
	private CTabFolder tabs;
	private LanguageViewer languageViewer;
	private TableViewer tableViewer;
	private TableViewer tblFallbackTypes;
	private Button btnDisable, btnDelete, btnEdit, btnAdd;
	private Text txtDistance;
	private ControlDecoration cdDistance;
	private Text txtMaxTime;
	private ControlDecoration cdMaxTime;
	
	private IconCache images ;
	
	private List<IncidentType> types = null;
//	private Map<IncidentType, IncidentType> fallbackTypes = null;
	private HashSet<IncidentType> toDelete = new HashSet<>();
	
	
	private ConservationArea currentCa = null;
	
	private int editIndex = -1;
	
	/*
	 * columns in the station table
	 */
	private enum Column {
		ICON(DialogConstants.ICON_TEXT, 1),
		NAME("Incident Type", 2),
		ACTIVE("Active", 1),
		LINK_TO_PATROL("Link To Patrol", 1, "These incidents will be linked to a patrol based on date/time and location of patrol tracks"),
		MOVE_TO_PATROL("Move To Patrol", 1, "These incidents will be converted to patrol waypoints based on date/time and location of patrol tracks"),
		KEY("Key", 1),
		SYSTEM("System", 1, "System types are required and cannot be removed.");
		
		String name;
		String tooltip;
		int weight;
		
		Column(String name, int weight){
			this(name, weight, null);
		}
		Column(String name, int weight, String tooltip) {
			this.name = name;
			this.weight = weight;
			this.tooltip = tooltip;
		}
	};
	
	/**
	 * @param parent
	 * @param title
	 */
	public IncidentTypePropertyPage(Shell parent) {
		super(parent, "Configure Independent Incident Types");
		this.currentCa = SmartDB.getCurrentConservationArea();
	}	
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.RIGHT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x < 800) p.x = 800;
		return p;		
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {	
		images = new IconCache();
		parent.addListener(SWT.Dispose, e->images.dispose());
//		
//		Composite container = new Composite(parent, SWT.NONE);
//		container.setLayout(new GridLayout());
//		
		tabs = new CTabFolder(parent,  SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		CTabItem typesItem = new CTabItem(tabs, SWT.NONE);
		typesItem.setText("Incident Types");
		Composite typesPanel = createTypesPanel(tabs);
		typesItem.setControl(typesPanel);

		CTabItem settingsItem = new CTabItem(tabs, SWT.NONE);
		settingsItem.setText("Move && Link Settings");
		Composite settings = createMoveAndLinkSettings(tabs);
		settingsItem.setControl(settings);
		
		tabs.setSelection(0);

		loadTypes();
		
		setTitle("Independent Incident Types");
		setMessage("Configure incident types");
		
		return tabs;
	}
	
	private Composite createMoveAndLinkSettings(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		Label l = new Label(container, SWT.WRAP);
		l.setText(
				"These settings control the matching of incidents to patrols. Incidents of types that are flagged as 'link to patrol' or 'move to patrol' can be created in SMART before the patrol data is loaded. Once the patrol data is loaded these incidents are linked or moved to the appropriate patrol by matching track position/time data to the incident position/time."
				);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		Button btnRunNow = new Button(container, SWT.NONE);
		btnRunNow.setText(Messages.IncidentOptionsPropertyPage_RunNowButton);
		btnRunNow.setBackground(btnRunNow.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRunNow.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		btnRunNow.setToolTipText(Messages.IncidentOptionsPropertyPage_RunNowButtonTooltip);
		btnRunNow.addListener(SWT.Selection, e->IncidentToPatrolProcessorJob.getInstance().schedule());
		
		SmartUiUtils.createSubHeaderLabel(container, Messages.IncidentOptionsPropertyPage_MaxDistanceOp);

		//The incident must be within the maximum distance of the track points found based on the incident time. An option also exists to convert incidents to simple {1} if not patrol is found after X days.
		Composite inner = new Composite(container, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		l = new Label(inner, SWT.WRAP);
		l.setText(Messages.IncidentOptionsPropertyPage_MaxDistanceInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		
		txtDistance = new Text(inner, SWT.BORDER);
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDistance.getLayoutData()).widthHint = 80;
		cdDistance = createDecoration(txtDistance);
		txtDistance.addListener(SWT.Modify, e->setChangesMade(true));
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentOptionsPropertyPage_Units);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)l.getLayoutData()).horizontalIndent = 5;
		
		SmartUiUtils.createSubHeaderLabel(container, "Fallback Settings");
		inner = new Composite(container, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l = new Label(inner, SWT.WRAP);
//		l.setText(MessageFormat.format(
//				Messages.IncidentOptionsPropertyPage_ConvertToIncidentTypeMessage, 				 
//				IncidentManager.getInstance().getIncidentProvider(IntegrateIncidentSource.KEY).getName()));
		l.setText("Convert incidents with a type of 'link to patrol' to the fallback type after they are matched to a patrol or if no matching patrol is found after X days. Convert incidents with a type of 'move to patrol' to the fallback type if no matching patrol is found after X days. Set to -1 to never convert unmatched incidents.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 350;
		
		txtMaxTime = new Text(inner, SWT.BORDER);
		txtMaxTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMaxTime.getLayoutData()).widthHint = 80;
		cdMaxTime = createDecoration(txtMaxTime);
		txtMaxTime.addListener(SWT.Modify, e->setChangesMade(true));
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.IncidentOptionsPropertyPage_days);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)l.getLayoutData()).horizontalIndent = 5;
		
		tblFallbackTypes = new TableViewer(inner, SWT.BORDER | SWT.FULL_SELECTION);
		tblFallbackTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblFallbackTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tblFallbackTypes.getTable().setHeaderVisible(true);
		tblFallbackTypes.getTable().setLinesVisible(true);
		
		TableViewerColumn type = new TableViewerColumn(tblFallbackTypes, SWT.NONE);
		type.getColumn().setText("Link/Move Type");
		type.getColumn().setWidth(250);
		type.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IncidentType it) return it.getName();
				return super.getText(element);
			}
		});
		
		TableViewerColumn totype = new TableViewerColumn(tblFallbackTypes, SWT.NONE);
		totype.getColumn().setText("Fallback Type");
		totype.getColumn().setWidth(250);
		totype.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IncidentType it) {
					if (it.getFallbackType() != null) return it.getFallbackType().getName();
					return "";
				}
				return super.getText(element);
			}
		});
		
		ComboBoxViewerCellEditor editor = new ComboBoxViewerCellEditor(totype.getColumn().getParent());
		editor.setContentProvider(ArrayContentProvider.getInstance());
		editor.setLabelProvider(new NamedItemLabelProvider());
		
		totype.setEditingSupport(new EditingSupport(totype.getViewer()) {

			@Override
			protected CellEditor getCellEditor(Object element) {
				List<IncidentType> inputs = new ArrayList<>();
				for (IncidentType t : types) {
					if (t.equals(element)) continue;
					if (!t.doLinkPatrol() && !t.doMovePatrol()) {
						inputs.add(t);
					}
				}
				editor.setInput(inputs);
				return editor;
			}

			@Override
			protected boolean canEdit(Object element) {				
				return (element instanceof IncidentType);				
			}

			@Override
			protected Object getValue(Object element) {
				if (element instanceof IncidentType it) {
					return it.getFallbackType();
				}
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof IncidentType it) {
					if (value instanceof IncidentType nw) {
						it.setFallbackType(nw);
					}else {
						it.setFallbackType(null);						
					}
						
					setChangesMade(true);
					tblFallbackTypes.refresh();
				}
			}
			
		});
		
		return container;
		
	}
	private Composite createTypesPanel(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText("Language:");

		languageViewer = new LanguageViewer(container, SWT.NONE, currentCa);
		languageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tableViewer.refresh();
			}
		});
	
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)composite2.getLayoutData()).heightHint = 200;
		
		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		

		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
//		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellHighlighter(tableViewer){});
//		
//		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
//			protected boolean isEditorActivationEvent(
//					ColumnViewerEditorActivationEvent event) {
//				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
//						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
//						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
//						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
//			}
//		};
//		
//		TableViewerEditor.create(tableViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.setComparator(new TypeSorter());
		
		
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateDisabledButtons();
			}
		});

		tableViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tableViewer.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == Column.KEY.ordinal()){
					editKey();
				}else if (cell.getColumnIndex() == Column.ICON.ordinal()){
					editIcon();
				}else if (cell.getColumnIndex() == Column.ACTIVE.ordinal()) {
					toggleActive();
				}else if (cell.getColumnIndex() == Column.LINK_TO_PATROL.ordinal()) {
					toggleLinkToPatrol();
				}else if (cell.getColumnIndex() == Column.MOVE_TO_PATROL.ordinal()) {
					toggleMoveToPatrol();
				}
			}
		});
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		btnAdd = new Button(composite, SWT.NONE);
		btnAdd.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addListener(SWT.Selection, e->addType());
		
		btnEdit = new Button(composite, SWT.NONE);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEdit.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEdit.setEnabled(false);
		btnEdit.addListener(SWT.Selection, e->editKey());
		
		btnDisable = new Button(composite, SWT.NONE);
		btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisable.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisable.setEnabled(false);
		btnDisable.addListener(SWT.Selection, e->toggleActive());
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addListener(SWT.Selection, e->deleteType());
		
		Menu menu = new Menu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);
			
		MenuItem miAdd = new MenuItem(menu, SWT.NONE);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addType());
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem miEdit = new MenuItem(menu, SWT.PUSH);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.addListener(SWT.Selection, evt->{
			if (editIndex == -1) return;
			if (editIndex == Column.ICON.ordinal()) {
				editIcon();
			}else if (editIndex == Column.KEY.ordinal()) {
				editKey();
			}else {
				tableViewer.editElement(tableViewer.getStructuredSelection().getFirstElement(), editIndex);
			}
		});
		
				
		MenuItem miClear = new MenuItem(menu, SWT.PUSH);
		miClear.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miClear.setText(DialogConstants.CLEAR_IMAGE_TEXT);
		miClear.addListener(SWT.Selection, et->updateIcon(getSelection(), null));
			
		MenuItem miDisable = new MenuItem(menu, SWT.NONE);
		miDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		miDisable.setEnabled(!tableViewer.getStructuredSelection().isEmpty());
		miDisable.addListener(SWT.Selection, e->toggleActive());
			
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem miDelete = new MenuItem(menu, SWT.NONE);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->deleteType());

		tableViewer.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = tableViewer.getCell(tableViewer.getControl().toControl(evt.x,  evt.y));
			editIndex = -1;
			if (cell != null) editIndex = cell.getColumnIndex();	
		});
		
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				
				IncidentType x = getSelection();
				
				miEdit.setEnabled(x != null && !x.isSystem());
				miClear.setEnabled(x != null);
				miDelete.setEnabled(x != null && !x.isSystem());
				
				if (x != null) {
					miDisable.setEnabled(true);
					miDisable.setText( x.getIsActive() ? DialogConstants.DISABLE_BUTTON_TEXT : DialogConstants.ENABLE_BUTTON_TEXT );
					miDisable.setImage( x.getIsActive() ? SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON) : SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON) );
				}else {
					miDisable.setEnabled(false);
				}
		}});
		return container;
	}
	
	@Override
	protected void setChangesMade(boolean ischanged) {
		super.setChangesMade(ischanged);
		validate();
	}
	
	private void refreshFallbackTypes() {
		List<IncidentType> fallbacks = new ArrayList<>();
		for (IncidentType type : types) {
			if (type.doLinkPatrol() || type.doMovePatrol()) fallbacks.add(type);
		}
		tblFallbackTypes.setInput(fallbacks);
	}
	
	protected void validate() {
		boolean ok = true;
		setErrorMessage(null);
		
		try{
			Integer x = Integer.parseInt(txtDistance.getText());
			if (x < 0) throw new Exception();
			cdDistance.hide();
		}catch (Exception ex) {
			ok = false;
			cdDistance.setDescriptionText(Messages.IncidentOptionsPropertyPage_distanceValidationMessage);
			setErrorMessage(Messages.IncidentOptionsPropertyPage_DistanceError);
			cdDistance.show();
		}
		
		try{
			Integer x = Integer.parseInt(txtMaxTime.getText());
			if (x < 0 && x != -1) throw new Exception();
			cdMaxTime.hide();
		}catch (Exception ex) {
			ok = false;
			cdMaxTime.setDescriptionText(Messages.IncidentOptionsPropertyPage_timeValidationMessage);
			setErrorMessage(Messages.IncidentOptionsPropertyPage_TimeError);
			cdMaxTime.show();
		}
		
		for (IncidentType t : types) {
			if (t.doLinkPatrol() || t.doMovePatrol()) {
				if (t.getFallbackType() == null) {
					setErrorMessage(MessageFormat.format("A fallback type is required for incident type {0}.", t.getName()));
					ok = false;
					break;
				}
				if (t.getFallbackType() != null && !types.contains(t.getFallbackType())) {
					setErrorMessage(MessageFormat.format("A fallback type is required for incident type {0}.", t.getName()));
					ok = false;
					break;
				}
				if (t.getFallbackType() != null && (t.getFallbackType().doLinkPatrol() || t.getFallbackType().doMovePatrol())) {
					setErrorMessage(MessageFormat.format("The fallback type for incident type {0} cannot be set to a type that is set to 'link' or 'move' to a patrol.", t.getName()));
					ok = false;
					break;
				}
			}
		}
		
		Button btnok = getButton(IDialogConstants.OK_ID);
		if (btnok != null) btnok.setEnabled(super.changesMade && ok);
	}
	
	private IncidentType getSelection() {
		return (IncidentType)tableViewer.getStructuredSelection().getFirstElement();
	}
	
	private void editKey(){
		IncidentType type = getSelection();
		if (type == null) return;
		if (type.isSystem()) {
			MessageDialog.openError(getShell(),DialogConstants.ERROR_STRING, "Cannot change the key of system types.");
			return;
		}
		String currentKey = type.getKeyId();
		
		List<IncidentType> kids = new ArrayList<>(this.types);
		kids.remove(type);
		
		InputDialog id = new KeyInputDialog(getShell(), currentKey, kids);
		int ret = id.open();
		if (ret != Window.CANCEL) {
//			updateValue(Column.KEY, type, id.getValue());
			String error = DataModelManager.INSTANCE.validateKey(id.getValue(), kids);
			if (error != null) {
				type.setKeyId(id.getValue());
			}
			tableViewer.refresh();
		}
	}
	
	private void deleteType(){
		IncidentType type = getSelection();
		if (type == null) return;
		
		if (type.isSystem()) {
			MessageDialog.openError(getShell(), "Delete", "System types cannot be removed.");
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), "Delete", 
				MessageFormat.format("Are you sure you want to delete the incident type {0}? This action cannot be undone.", type.getName()))){
			return;
		}

		if (type.getUuid() != null) {
			try(Session session = HibernateManager.openSession()){
				DeleteManager.canDelete(type, session);									
			}
			
			catch (Exception ex){
				IncidentPlugIn.displayLog(MessageFormat.format("Cannot delete incident type {0}: {1}", type.getName(), ex.getMessage()), ex);
				return;
			}
		}
		types.remove(type);
		for (IncidentType t : types) {
			if (t.getFallbackType() != null && t.getFallbackType().equals(type)) {
				t.setFallbackType(null);
			}
		}
		toDelete.add(type);
		setChangesMade(true);
		tableViewer.refresh();
		refreshFallbackTypes();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		validate();
		if (getErrorMessage() != null) return false;
		
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.beginTransaction();
			try {
				
				IncidentPropertyManager.INSTANCE.updateSetting(s, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_DISTANCE, Integer.valueOf(txtDistance.getText()));
				IncidentPropertyManager.INSTANCE.updateSetting(s, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_EXPIRE, Integer.valueOf(txtMaxTime.getText()));
				
				for (IncidentType t : toDelete){
					if(t.isSystem()) throw new Exception("Cannot delete system types.");
					s.remove(t);
				}
				s.flush();
				
				List<IncidentType> siblings = new ArrayList<>(types);
				
				for (IncidentType type : types) {
					
					if (!type.doLinkPatrol() && !type.doMovePatrol()) {
						type.setFallbackType(null);
					}else if ( (type.getFallbackType() == null) || (type.getFallbackType() != null && !types.contains(type.getFallbackType()))) {
						throw new Exception(MessageFormat.format("fallback type required for type {0}", type.getName()));
					}
					
					HibernateManager.saveOrMerge(s,  type.getIcon());
					
					siblings.remove(type);
					String error = DataModelManager.INSTANCE.validateKey(type.getKeyId(), siblings);
					siblings.add(type);
					if (error != null){
						throw new Exception(error);
					}					
					HibernateManager.saveOrMerge(s,  type);					
				}
				s.getTransaction().commit();
				toDelete.clear();
				setChangesMade(false);
				return true;
				
			} catch (Exception ex) {
				s.getTransaction().rollback();
				IncidentPlugIn.displayLog(MessageFormat.format("Error saving updates. Close the dialog and try again.\n\n{0}", ex.getMessage()), ex);						
			}
		}
		return false;
	}
	
	private void addType(){
		IncidentType type = new IncidentType();
		type.setConservationArea(currentCa);
		type.setIsActive(true);
		type.updateName(currentCa.getDefaultLanguage(), "Incident Type");
		type.setName(type.findName(currentCa.getDefaultLanguage()));	
		types.add(type);
		setChangesMade(true);
		tableViewer.refresh();
		tableViewer.editElement(type, Column.NAME.ordinal());		
		refreshFallbackTypes();
	}
	
	private void toggleLinkToPatrol(){
		IncidentType type = getSelection();
		if (type == null);
		
		if (type.isSystem()) {
			MessageDialog.openError(getShell(),DialogConstants.ERROR_STRING, "Cannot change the link to patrol setting of system types.");
			return;
		}
		
		if (type.doLinkPatrol()) {
			type.setLinkPatrol(false);
		}else {
			type.setLinkPatrol(true);
		}
		setChangesMade(true);
		tableViewer.refresh();
		refreshFallbackTypes();
	}
	
	private void toggleMoveToPatrol(){
		IncidentType type = getSelection();
		if (type == null);
		if (type.isSystem()) {
			MessageDialog.openError(getShell(),DialogConstants.ERROR_STRING, "Cannot change the move to patrol setting of system types.");
			return;
		}
		
		if (type.doMovePatrol()) {
			type.setMovePatrol(false);
		}else {
			type.setMovePatrol(true);
		}
		setChangesMade(true);
		tableViewer.refresh();
		refreshFallbackTypes();
	}
	
	private void toggleActive(){
		IncidentType type = getSelection();
		if (type == null);
		
		type.setIsActive(!type.getIsActive());
		setChangesMade(true);
		tableViewer.refresh();
		updateDisabledButtons();
	}
	
	private void updateDisabledButtons() {
		IncidentType type = getSelection();
		if (type == null) {
			btnDisable.setEnabled(false);	
			btnEdit.setEnabled(false);
			btnDelete.setEnabled(false);
			return;
		}
		
		btnEdit.setEnabled(!type.isSystem());
		btnDelete.setEnabled(!type.isSystem());
		btnDisable.setEnabled(true);
		if (!type.getIsActive()){
			btnDisable.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));			
		}else{
			btnDisable.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		}
	}
	
	
	private void editIcon() {
		IncidentType type = getSelection();
		if (type == null) return;
		
		IconSelectionDialog dialog = new IconSelectionDialog(tableViewer.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		updateIcon(type, dialog.getSelectedIcon());
	}
	
	private void updateIcon(IncidentType type, Icon icon) {
		if (type == null) return;

		images.clearCache(type);
		type.setIcon(icon);
		tableViewer.refresh();
		setChangesMade(true);
	}
	
	/**
	 * Finds the property for the given column from the station.
	 * 
	 * @param type the property required
	 * @param stn station 
	 * @return string value of the requested property
	 */
	private String findValue(Column column, IncidentType type) {
		switch(column) {
		case ACTIVE:
			if (type.getIsActive()) {
				return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			}else {
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		case SYSTEM:
			if (type.isSystem()) {
				return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			}else {
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		case ICON: return ""; //$NON-NLS-1$
		case KEY:
			return type.getKeyId();
		case LINK_TO_PATROL:
			if (type.doLinkPatrol()) {
				return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			}else {
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		case MOVE_TO_PATROL:
			if (type.doMovePatrol()) {
				return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
			}else {
				return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
			}
		case NAME:
			String x =  type.findNameNull(languageViewer.getCurrentSelection());
			if (x == null) return ""; //$NON-NLS-1$
			return x;
		};
		return ""; //$NON-NLS-1$
		
	}

	private void createColumns(TableViewer viewer) {
		for (int i = 0; i < Column.values().length; i++) {
			final Column colum = Column.values()[i];
		
			final TableViewerColumn col = createTableViewerColumn(viewer, colum);
			
			if (colum == Column.ICON) col.getColumn().setWidth( 32 * 3 + 20);

			col.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof IncidentType it) return findValue(colum, it);
					return super.getText(element);
				}
				@Override
				public Image getImage(Object element) {
					if (colum == Column.ICON && element instanceof IncidentType it) {
						return images.getImage(it);
					}
					return null;
				}
				@Override
				public Color getForeground(Object element){
					 if (element instanceof IncidentType it) {
						 if (it.getIsActive()) {
							 return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
						 }else{
							 return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
						 }
					 }
					 return super.getForeground(element);
				}
			});
		}
	}
	
	private TableViewerColumn createTableViewerColumn(TableViewer viewer,
			Column col) {
		
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(col.name);
		if (col.tooltip != null) column.setToolTipText(col.tooltip);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(col.weight,ColumnWeightData.MINIMUM_WIDTH, true));
		
		if (col == Column.NAME) {
			viewerColumn.setEditingSupport(new EditingSupport(viewer){
				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor(viewer.getTable());
				}

				@Override
				protected boolean canEdit(Object element) {
					return true;
				}
				@Override
				protected Object getValue(Object element) {
					return IncidentTypePropertyPage.this.findValue(col, (IncidentType)element);
				}

				@Override
				protected void setValue(Object element, Object value) {
					IncidentType toUpdate = (IncidentType)element;
					String newValue = (String)value;
					
					if (!IncidentTypePropertyPage.this.findValue(col, toUpdate).equals(newValue)){						
						if(!SmartUtils.isSimpleString(newValue.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, 
								org.wcs.smart.ca.Label.MAX_LENGTH)){							
							//invalid value, show error
							MessageDialog.openError(getShell(), DialogConstants.ERROR_STRING, MessageFormat.format("Name must only contain {0} and be less than {1} characters in length.", new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, org.wcs.smart.ca.Label.MAX_LENGTH}));
						}
						toUpdate.updateName(languageViewer.getCurrentSelection(), newValue);
						if (toUpdate.getUuid() == null) {
							List<IncidentType> kids = new ArrayList<>(types);
							kids.remove(toUpdate);
							toUpdate.setKeyId(DataModelManager.INSTANCE.generateKey(newValue, kids));
						}
						setChangesMade(true);
						tableViewer.refresh();
					}		
					
					
				}
					
			});
		}
		
		return viewerColumn;
	}
	
	
		
	class TypeSorter extends ViewerComparator{
		private Column column = null;
		private int direction = SWT.DOWN;
		
		public void setSortColumn(Column sort, TableColumn tcolumn){
			
			if (column != null &&column == sort){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = sort;
			tableViewer.getTable().setSortDirection(direction);
			tableViewer.getTable().setSortColumn(tcolumn);
			tableViewer.refresh();
		}
			
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null) return 0;
			if (object1 instanceof IncidentType it1 && object2 instanceof IncidentType it2) {
				if (direction == SWT.UP){
					return -compareValue(it1, it2);
				}else{
					return compareValue(it1, it2);
				}
			}
			return 0;
		}
		
		private int compareValue(IncidentType s1, IncidentType s2){
			if (s1 == null && s2 == null) return 0;
			if (s1== null && s2 != null) return 1;
			if (s1 != null && s2 == null) return -1;						
			String a = findValue(column, s1);
			String b = findValue(column, s2);
			return Collator.getInstance().compare(a, b);
		}
	};
	
	private void loadTypes() {
		tableViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		loadTypes.schedule();
	}
	
	
	private Job loadTypes = new Job("load types") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IncidentType> itypes = null;
			String distance;
			String maxtime;
			try(Session session = HibernateManager.openSession()){
				itypes = IncidentManager.getInstance().getIncidentTypes(session, currentCa, false);
				for (IncidentType t : itypes) HibernateManager.loadIcon(t, session);
				distance = String.valueOf(IncidentPropertyManager.INSTANCE.getSetting(session, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_DISTANCE));
				maxtime = String.valueOf(IncidentPropertyManager.INSTANCE.getSetting(session, SmartDB.getCurrentConservationArea(), IncidentPropertyManager.IncidentProperty.INTEGRATE_TO_PATROL_EXPIRE));
			}
			
			types = itypes;
			Display.getDefault().asyncExec(()->{
				if (tableViewer.getControl().isDisposed()) return;
				tableViewer.setInput(types);				
				tableViewer.refresh();
				
				txtDistance.setText(distance);
				txtMaxTime.setText(maxtime);
				
				refreshFallbackTypes();
				setChangesMade(false);
			});
			return Status.OK_STATUS;
		}};
}
