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
package org.wcs.smart.patrol.internal.ui.properties;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvExportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImportConfig;
import org.wcs.smart.patrol.internal.export.PatrolTransportCsvImporter;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Property page for managaing patrol types and
 * transport types.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTypePropertyPage extends AbstractPropertyJHeaderDialog {

	//TODO: import/export???
	
	private static final String INVALID_TYPE_DIALOG_TITLE = Messages.PatrolTypePropertyPage_InvalidType_DialogTitle;
	private static final String DISABLED_LABEL = Messages.PatrolTypePropertyPage_DisabledLabel;
	private static final String ACTIVE_LABEL = Messages.PatrolTypePropertyPage_ActiveLabel;
	private static final String REQUIRES_PILOT =Messages.PatrolTypePropertyPage_RequiresPilotLbl;

	private static final String TITLE = Messages.PatrolTypePropertyPage_Title;
	
	private LanguageViewer languageViewer;
	private TableViewer patrolTypeTblViewer;
	private TableViewer transportTblViewer;
	private Button btnAddTransport, btnDisableType, btnDisableTransport, btnDeleteTransport, btnEditTransport;
	private Button btnEditType, btnAddType, btnDeleteType;
	private List<PatrolType> patrolTypes = null;
	private List<PatrolTransportType> transportTypes = null;
	private ConservationArea currentCa = null;
	
	private IconCache iconCache;
	private int editIndex = -1;
	private int typeEditIndex = -1;
	
	/**
	 * @param parent
	 * @param title
	 */
	public PatrolTypePropertyPage(Shell parent) {
		super(parent, TITLE);
		this.currentCa = SmartDB.getCurrentConservationArea();
	}

	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x < 650) p.x = 650;
		return p;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		iconCache = new IconCache(parent);
		
		try(Session s = HibernateManager.openSession()){
			s.get(ConservationArea.class, currentCa.getUuid()).getLanguages().size();
			transportTypes = new ArrayList<>();
			patrolTypes = new ArrayList<>(PatrolHibernateManager.getPatrolTypes(currentCa, s));		
			//ensure all types are lazily loaded
			for (PatrolType t : patrolTypes){
				if (t.getTransportTypes() != null){
					t.getTransportTypes().forEach(tt -> tt.getNames().size());
					transportTypes.addAll(t.getTransportTypes());
				}
			}
		}
		
		transportTypes.sort((a,b)-> {
			if (a.getPatrolType().equals(b.getPatrolType())){
				return Collator.getInstance().compare(getName(a), getName(b));
			}else{
				return Collator.getInstance().compare(getName(a.getPatrolType()), getName(b.getPatrolType()));
			}
		});
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lblNewLabel.setText(Messages.PatrolTypePropertyPage_LanguageLabel);

		languageViewer = new LanguageViewer(container, SWT.NONE, currentCa);
		languageViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				transportTblViewer.refresh();
				patrolTypeTblViewer.refresh();
			}
		});
		
		
		CTabFolder folder = new CTabFolder(container, SWT.TOP);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		/* --------- Patrol Type -------------- */
		CTabItem typeTab = new CTabItem(folder, SWT.NONE);
		Composite typeComp = new Composite(folder, SWT.NONE);
		typeTab.setControl(typeComp);
		typeComp.setLayout(new GridLayout(2, false));
		typeTab.setText(Messages.PatrolTypePropertyPage_TrackTypes);
		
		Composite tableTypeComp = new Composite(typeComp, SWT.NONE);
		tableTypeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableTypeComp.setLayout(tableLayout);
	
		patrolTypeTblViewer = new TableViewer( tableTypeComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		createTypeColumns(patrolTypeTblViewer);
		patrolTypeTblViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeTblViewer.setInput(patrolTypes);
		patrolTypeTblViewer.getTable().setHeaderVisible(true);
		patrolTypeTblViewer.getTable().setLinesVisible(true);
		
		patrolTypeTblViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = patrolTypeTblViewer.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == 3){
					disablePatrolType();
				}
				if (cell.getColumnIndex() == 2){
					toggleRequiresPilot();
				}
				if (cell.getColumnIndex() == 0) {
					editPatrolTypeIcon();
				}
			}
		});
		customizeTableViewerEditor(patrolTypeTblViewer);
		
		patrolTypeTblViewer.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = patrolTypeTblViewer.getCell(patrolTypeTblViewer.getControl().toControl(evt.x,  evt.y));
			typeEditIndex = -1;
			if (cell != null) typeEditIndex = cell.getColumnIndex();	
		});
		
		//menu
		Menu typeTableMenu = new Menu(patrolTypeTblViewer.getControl());
		patrolTypeTblViewer.getControl().setMenu(typeTableMenu);
				
		
		MenuItem miTypeAdd = new MenuItem(typeTableMenu, SWT.CASCADE);
		miTypeAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miTypeAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miTypeAdd.addListener(SWT.Selection, e->addPatrolType());
		
		new MenuItem(typeTableMenu, SWT.SEPARATOR);
		
		MenuItem miEditTypeKey = new MenuItem(typeTableMenu, SWT.PUSH);
		miEditTypeKey.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEditTypeKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEditTypeKey.addListener(SWT.Selection, evt->{
			if (typeEditIndex == -1) return;
			System.out.println(typeEditIndex);
			if (typeEditIndex == 0) {
				editPatrolTypeIcon();
			}else if (typeEditIndex == patrolTypeTblViewer.getTable().getColumnCount()-1) {
				editPatrolTypeKey();
			}else {
				patrolTypeTblViewer.editElement((PatrolType) patrolTypeTblViewer.getStructuredSelection().getFirstElement(), typeEditIndex);
			}
		});
		

		MenuItem miClearTypeIcon = new MenuItem(typeTableMenu, SWT.PUSH);
		miClearTypeIcon.setText(LabelConstants.CLEAR_IMAGE);
		miClearTypeIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miClearTypeIcon.addListener(SWT.Selection, l->updatePatrolTypeIcon((PatrolType)patrolTypeTblViewer.getStructuredSelection().getFirstElement(), null));
		
		
		MenuItem disableTypeItem = new MenuItem(typeTableMenu, SWT.PUSH);
		disableTypeItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		disableTypeItem.addListener(SWT.Selection, l->disablePatrolType());
		
		new MenuItem(typeTableMenu, SWT.SEPARATOR);

		MenuItem miDeleteType = new MenuItem(typeTableMenu, SWT.PUSH);
		miDeleteType.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDeleteType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDeleteType.addListener(SWT.Selection, l->deletePatrolType());
		
		typeTableMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean isSelected = !patrolTypeTblViewer.getStructuredSelection().isEmpty();
				miEditTypeKey.setEnabled(isSelected);
				miClearTypeIcon.setEnabled(isSelected);
				disableTypeItem.setEnabled(isSelected);
				miDeleteType.setEnabled(isSelected);
				if (!isSelected) return;
				
				Object x = ((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
				if (x instanceof PatrolType){
					if (((PatrolType) x).getIsActive()){
						disableTypeItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
						disableTypeItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));

					}else{
						disableTypeItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
						disableTypeItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));						
					}
				}	
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});

		
		
		Composite composite = new Composite(typeComp, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		btnAddType = createButton(composite, DialogConstants.ADD_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddType.addListener(SWT.Selection, e->addPatrolType());
		
		btnEditType = createButton(composite, DialogConstants.EDIT_KEY_BUTTON_TEXT,SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditType .setEnabled(false);
		btnEditType .addListener(SWT.Selection, e->editPatrolTypeKey());
		
		btnDisableType = new Button(composite, SWT.NONE);
		btnDisableType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisableType.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisableType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisableType.setEnabled(false);
		btnDisableType.addListener(SWT.Selection,e->disablePatrolType());
		
		btnDeleteType = createButton(composite, DialogConstants.DELETE_BUTTON_TEXT,SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDeleteType .setEnabled(false);
		btnDeleteType .addListener(SWT.Selection, e->deletePatrolType());
		
		patrolTypeTblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (patrolTypeTblViewer.getSelection().isEmpty()) {
					btnDisableType.setEnabled(false);
					btnEditType.setEnabled(false);
					btnDeleteType.setEnabled(false);
					miDeleteType.setEnabled(false);
					miEditTypeKey.setEnabled(false);
					
					return;
				}
				PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
				if (pt.getIsActive()){
					btnDisableType.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableType.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				}else{
					btnDisableType.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				}
				btnDisableType.setEnabled(true);
				btnEditType.setEnabled(true);
				btnDeleteType.setEnabled(true);		
				miDeleteType.setEnabled(true);
				miEditTypeKey.setEnabled(true);
			}
		});
		
		folder.setSelection(0);
		
		/* --------- Patrol Transport Type -------------- */
		CTabItem transportTab = new CTabItem(folder, SWT.NONE);
		Composite transportComp = new Composite(folder, SWT.NONE);
		transportTab.setControl(transportComp);
		transportComp.setLayout(new GridLayout(2, false));
		transportTab.setText(Messages.PatrolTypePropertyPage_TransportOptionsLabel);

		Composite tableTransportComp = new Composite(transportComp, SWT.NONE);
		tableTransportComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableLayout = new TableColumnLayout();
		tableTransportComp.setLayout(tableLayout);
		
		transportTblViewer = new TableViewer( tableTransportComp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		createTransportColumns(transportTblViewer);
		transportTblViewer.setContentProvider(ArrayContentProvider.getInstance());
		transportTblViewer.getTable().setHeaderVisible(true);
		transportTblViewer.getTable().setLinesVisible(true);
		transportTblViewer.setInput(transportTypes);
		transportTblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
				
				if (pt == null){
					btnDisableTransport.setEnabled(false);
					btnDeleteTransport.setEnabled(false);
					btnEditTransport.setEnabled(false);
					return;
				}
				if (pt.getIsActive()){
					btnDisableTransport.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				}else{
					btnDisableTransport.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				}
				btnDisableTransport.setEnabled(true);
				btnDeleteTransport.setEnabled(true);
				btnEditTransport.setEnabled(true);
				
			}
		});
		
		transportTblViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = transportTblViewer.getCell(new Point(event.x, event.y));
				if (cell == null) return;
				if (cell.getColumnIndex() == 4){
					editKey();
				}else if (cell.getColumnIndex() == 3){
					disableTransportType();
				}else if (cell.getColumnIndex() == 0) {
					editIcon();
				}
			}
		});

		customizeTableViewerEditor(transportTblViewer);
		
		composite = new Composite(transportComp, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		btnAddTransport = new Button(composite, SWT.NONE);
		btnAddTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAddTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddTransport.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddTransport.addListener(SWT.Selection, e->addTransportType(null));
		
		btnEditTransport = new Button(composite, SWT.NONE);
		btnEditTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEditTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEditTransport.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEditTransport.setEnabled(false);
		btnEditTransport.addListener(SWT.Selection, e->editKey());
		
		btnDisableTransport = new Button(composite, SWT.NONE);
		btnDisableTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisableTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisableTransport.setEnabled(false);
		btnDisableTransport.addListener(SWT.Selection, e->disableTransportType());
		
		btnDeleteTransport = new Button(composite, SWT.NONE);
		btnDeleteTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDeleteTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDeleteTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDeleteTransport.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteTransport.setEnabled(false);
		btnDeleteTransport.addListener(SWT.Selection, e->deleteTransportType());	
		
		//table menu
		Menu tableMenu = new Menu(transportTblViewer.getControl());
		transportTblViewer.getControl().setMenu(tableMenu);
		
		MenuItem miAdd = new MenuItem(tableMenu, SWT.CASCADE);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		
		Menu addType = new Menu(miAdd);
		miAdd.setMenu(addType);
		for (PatrolType pt: patrolTypes){
			MenuItem addtype = new MenuItem(addType, SWT.PUSH);
			addtype.setText(getName(pt) );
			addtype.addListener(SWT.Selection, l->addTransportType(pt));
			addtype.setImage( iconCache.getImage(pt) );
		}
		
		new MenuItem(tableMenu, SWT.SEPARATOR);
		
		MenuItem editKey = new MenuItem(tableMenu, SWT.PUSH);
		editKey.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editKey.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editKey.addListener(SWT.Selection, evt->{
			if (editIndex == -1) return;
			if (editIndex == 0) {
				editIcon();
			}else if (editIndex == transportTblViewer.getTable().getColumnCount()-1) {
				editKey();
			}else {
				transportTblViewer.editElement((PatrolTransportType) transportTblViewer.getStructuredSelection().getFirstElement(), editIndex);
			}
		});
		
		
		MenuItem clearIcon = new MenuItem(tableMenu, SWT.PUSH);
		clearIcon.setText(LabelConstants.CLEAR_IMAGE);
		clearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		clearIcon.addListener(SWT.Selection, l->updateIcon((PatrolTransportType)transportTblViewer.getStructuredSelection().getFirstElement(), null));
		
		MenuItem disableItem = new MenuItem(tableMenu, SWT.PUSH);
		disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		disableItem.addListener(SWT.Selection, l->disableTransportType());
		
		new MenuItem(tableMenu, SWT.SEPARATOR);

		MenuItem delete = new MenuItem(tableMenu, SWT.PUSH);
		delete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		delete.addListener(SWT.Selection, l->deleteTransportType());
		
		tableMenu.addMenuListener(new MenuListener() {			
			@Override
			public void menuShown(MenuEvent e) {
				boolean isSelected = !transportTblViewer.getSelection().isEmpty();
				delete.setEnabled(isSelected);
				editKey.setEnabled(isSelected);
				disableItem.setEnabled(isSelected);
				
				if (isSelected){
					Object x = ((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
					if (x instanceof PatrolTransportType){
						if (((PatrolTransportType) x).getIsActive()){
							disableItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
							disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
						}else{
							disableItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
							disableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
						}
					}
				}
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		transportTblViewer.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = transportTblViewer.getCell(transportTblViewer.getControl().toControl(evt.x,  evt.y));
			editIndex = -1;
			if (cell != null) editIndex = cell.getColumnIndex();	
		});
		
		Composite buttonComp = new Composite(container, SWT.NONE);
		buttonComp.setLayout(new GridLayout(2, true));
		((GridLayout)buttonComp.getLayout()).marginWidth = 0;
		((GridLayout)buttonComp.getLayout()).marginHeight = 0;
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Button btnImport = new Button(buttonComp, SWT.PUSH);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		btnImport.addListener(SWT.Selection, e->importTypes());
		
		Button btnExport = new Button(buttonComp, SWT.PUSH);
		btnExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		btnExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		btnExport.addListener(SWT.Selection, e->exportTypes());
		
		setTitle(TITLE);
		setMessage(Messages.PatrolTypePropertyPage_DialogMessage);
		return container;
	}
	
	private void exportTypes() {
        CsvExportDialog dialog = new CsvExportDialog(getShell(), new PatrolTransportCsvExportConfig());
        dialog.open();
	}
	
	@SuppressWarnings("unchecked")
	private void importTypes() {
        PatrolTransportCsvImportConfig config = new PatrolTransportCsvImportConfig();
        CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
        if (dialog.open() == Window.OK){

        	Object[] data = ((PatrolTransportCsvImporter)config.getImporter()).getImportedData();
        	List<PatrolType> importedTypes = (List<PatrolType>) data[0];
            Collection<PatrolTransportType> importedTtypes = (Collection<PatrolTransportType>) data[1];

            //update and validate patroltype keys
            for (PatrolType type : importedTypes) {
            	if (DataModelManager.INSTANCE.validateKey(type.getKeyId(), patrolTypes) != null){
                    type.setKeyId(DataModelManager.INSTANCE.generateKey(type.getKeyId(), patrolTypes));
                }
            	this.patrolTypes.add(type);
            }
            
            
            for (PatrolTransportType type : importedTtypes){
            	if (DataModelManager.INSTANCE.validateKey(type.getKeyId(), this.transportTypes) != null){
                    type.setKeyId(DataModelManager.INSTANCE.generateKey(type.getKeyId(), this.transportTypes));
                }

            	if (type.getPatrolType().getUuid() != null && type.getPatrolType().getUuid().equals(UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR))) {
            		//find a patrol type
            		String ptypekey = type.getPatrolType().getKeyId();
            		type.setPatrolType(null);
            		for (PatrolType ptype : this.patrolTypes) {
            			if (ptypekey.equalsIgnoreCase(ptype.getKeyId())) {
            				type.setPatrolType(ptype);
            				break;
            			}
            		}
            		if (type.getPatrolType() == null) {
            			//create a new patrol type
            			PatrolType temp = new PatrolType();
            			temp.setConservationArea(type.getConservationArea());
            			temp.setIsActive(true);
            			temp.setKeyId(ptypekey);
            			temp.setName(ptypekey);
            			temp.updateName(type.getConservationArea().getDefaultLanguage(), ptypekey);
            			
            			
            			if (DataModelManager.INSTANCE.validateKey(temp.getKeyId(), patrolTypes) != null){
            				temp.setKeyId(DataModelManager.INSTANCE.generateKey(temp.getKeyId(), transportTypes));
                        }
                    	this.patrolTypes.add(temp);
            		}
            		
            	}
           		this.transportTypes.add(type);
            }

            setChangesMade(true);
            transportTblViewer.refresh();
            patrolTypeTblViewer.refresh();
        }
	}
	private Button createButton(Composite parent, String text, Image image) {
		Button btnAddType = new Button(parent, SWT.NONE);
		btnAddType.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAddType.setImage(image);
		btnAddType.setText(text);
		return btnAddType;
	}

	private void disableTransportType(){
		PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		if (btnDisableTransport.getToolTipText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
			pt.setIsActive(false);
			btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableTransport.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
		}else{
			pt.setIsActive(true);
			btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableTransport.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));

		}
		transportTblViewer.refresh();
		setChangesMade(true);
	}
	
	private void disablePatrolType(){
		PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
		if (btnDisableType.getToolTipText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
			pt.setIsActive(false);
			btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableType.setToolTipText(DialogConstants.ENABLE_BUTTON_TEXT);
			btnDisableType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
		}else{
			pt.setIsActive(true);
			btnDisableType.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableType.setToolTipText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableType.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));

		}
		patrolTypeTblViewer.refresh();
		setChangesMade(true);
	}
	
	private void toggleRequiresPilot(){
		PatrolType pt = (PatrolType)patrolTypeTblViewer.getStructuredSelection().getFirstElement();
		pt.setRequiresPilot(!pt.getRequiresPilot());
		patrolTypeTblViewer.refresh();
		setChangesMade(true);
	}
	
	
	private void addTransportType(PatrolType type){
		if (type == null && patrolTypes.isEmpty()) return;
		
		PatrolTransportType newPtt = new PatrolTransportType();
		newPtt.setConservationArea(currentCa);
		newPtt.setIsActive(true);
		
		PatrolType newpt = type;
		if (newpt == null){	
			for (PatrolType pt : patrolTypes){
				if (pt.getIsActive()){
					newpt = pt;
					break;
				}
			}
			if (newpt == null) newpt = patrolTypes.get(0);
			
		}
		if (newpt.getTransportTypes() == null) newpt.setTransportTypes(new ArrayList<>());
		
		newPtt.setPatrolType(newpt);
		newpt.getTransportTypes().add(newPtt);
		newPtt.updateName(currentCa.getDefaultLanguage(), Messages.PatrolTypePropertyPage_DefaultTransportionTypeName);
		newPtt.setName(newPtt.findName(currentCa.getDefaultLanguage()));
		newPtt.setKeyId(DataModelManager.INSTANCE.generateKey(newPtt.getName(), transportTypes));
		newpt.getTransportTypes().add(newPtt);
		
		transportTypes.add(newPtt);
		transportTblViewer.refresh();
		setChangesMade(true);
	}
	
	private void addPatrolType(){
		PatrolType ptype = new PatrolType();
		ptype.setConservationArea(currentCa);
		ptype.setIsActive(true);
		ptype.setRequiresPilot(false);
		ptype.setMaxSpeed(PatrolType.MAX_SPEED_MAX_VALUE);
		ptype.setTransportTypes(new ArrayList<>());
		
		ptype.updateName(currentCa.getDefaultLanguage(), Messages.PatrolTypePropertyPage_DefaultName);
		ptype.setName(ptype.findName(currentCa.getDefaultLanguage()));
		ptype.setKeyId(DataModelManager.INSTANCE.generateKey(ptype.getName(), patrolTypes));
		
		
		patrolTypes.add(ptype);
		patrolTypeTblViewer.refresh();
		setChangesMade(true);
	}
	
	
	
	private void customizeTableViewerEditor(TableViewer viewer) {
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(viewer, new FocusCellHighlighter(viewer){});
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TableViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);
	}
	
	private void editKey(){
		PatrolTransportType x = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		String currentKey = x.getKeyId();
		InputDialog id = new KeyInputDialog(getShell(), currentKey,  transportTypes);
		int ret = id.open();
		if (ret != Window.CANCEL) {
			x.setKeyId(id.getValue());
			setChangesMade(true);
			transportTblViewer.refresh(x);
		}
	}
	
	private void editPatrolTypeKey(){
		PatrolType x = (PatrolType) patrolTypeTblViewer.getStructuredSelection().getFirstElement();
		if (x == null) return;
		
		String currentKey = x.getKeyId();
		InputDialog id = new KeyInputDialog(getShell(), currentKey,  patrolTypes);
		int ret = id.open();
		if (ret != Window.CANCEL) {
			x.setKeyId(id.getValue());
			setChangesMade(true);
			patrolTypeTblViewer.refresh(x);
		}
	}
	/*
	 * Creates station table columns
	 */
	private void createTypeColumns(final TableViewer viewer) {
		// Icon Column
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setResizable(true);
		column.setText(DialogConstants.ICON_TEXT);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setWidth( 32 + 20);
				
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}
					
			@Override
			public Image getImage(Object element) {
				if (element instanceof PatrolType pt) return iconCache.getImage(pt);
				return null;
			}
		});
					 
		/* Type Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.PatrolTypePropertyPage_TrackTypeHeader);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType pt) return pt.findName(languageViewer.getCurrentSelection());
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolType){
					if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
			
		});
		
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
				if (element instanceof PatrolType pt) return getName(pt);
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolType pt){
					String newName = (String)value;
					if (!pt.findName(languageViewer.getCurrentSelection()).equals(newName)){
						if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
							Integer matches = 0;
							for(PatrolType current : patrolTypes) {
								if( current != pt && current.findName(languageViewer.getCurrentSelection()).equalsIgnoreCase((newName).trim())){
									matches++;
								}
							} 
							if(matches > 0){
								//invalid agency name, don't update it.
								MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, Messages.PatrolTypePropertyPage_UniqueTrackType);
								setChangesMade(false);
							}else{
								pt.updateName(languageViewer.getCurrentSelection(), newName.trim());
								if (pt.getKeyId() == null || pt.getUuid() == null){
									pt.setKeyId(DataModelManager.INSTANCE.generateKey(newName, patrolTypes));
								}
								setChangesMade(true);
							}
						}else{
							//invalid agency name, don't update it.
							MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, 
									MessageFormat.format(Messages.PatrolTypePropertyPage_Error_InvalidTransportType, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, PatrolType.MAX_TRANSPORT_NAME_LENGTH}));
							setChangesMade(false);
						}				
					}
					viewer.refresh();
					transportTblViewer.refresh();
				}					
			}});
		
		/* Requires Pilot */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(REQUIRES_PILOT);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(2,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType pt){
					if (pt.getRequiresPilot()) {
						return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					}else {
						return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
					}
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolType){
					if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
			
		/* Active Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType){
					if (((PatrolType)element).getIsActive()){
						return ACTIVE_LABEL;
					}else{
						return DISABLED_LABEL;
					}
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolType){
					if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
			
		
		/* Max Speed Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.PatrolTypePropertyPage_MaxSpeed_ColumnHeader);
		column.setToolTipText(Messages.PatrolTypePropertyPage_MaxSpeed_ColumnTooltip);
		column.setResizable(true);
		column.setMoveable(true);
		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(2,ColumnWeightData.MINIMUM_WIDTH, true));
		
		final ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType){
					return String.valueOf(((PatrolType)element).getMaxSpeed());
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolType){
					if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		};
		viewerColumn.setLabelProvider(labelProvider);
		viewerColumn.setEditingSupport(new EditingSupport(viewer) {
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
					return labelProvider.getText(element);
				}

				@Override
				protected void setValue(Object element, Object value) {
					if (element instanceof PatrolType) {
						PatrolType pt = (PatrolType) element;
						Integer v = null;
						try {
							v = Integer.valueOf(String.valueOf(value));
						} catch (NumberFormatException e) {
							//nothing, "v" will remain null in case input is not a number
						}
						if (v == null || v < PatrolType.MAX_SPEED_MIN_VALUE || v > PatrolType.MAX_SPEED_MAX_VALUE) {
							reportError();
							return;
						}
						pt.setMaxSpeed(v);
						setChangesMade(true);
						viewer.refresh();
					}					
				}
				
				private void reportError() {
					MessageDialog.openError(getShell(), Messages.PatrolTypePropertyPage_InvalidMaxSpeed_DialogTitle, MessageFormat.format(Messages.PatrolTypePropertyPage_InvalidMaxSpeed_DialogMessage, PatrolType.MAX_SPEED_MIN_VALUE, PatrolType.MAX_SPEED_MAX_VALUE));
				}
			});
		
		
		/* Max Speed Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.TRANSTYPE_KEY);
		column.setToolTipText(Messages.PatrolTypePropertyPage_MaxSpeed_ColumnTooltip);
		column.setResizable(true);
		column.setMoveable(true);
		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
		
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolType pt) return pt.getKeyId();				
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolType){
					if (!((PatrolType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
		
	}
	
	private void editIcon() {
		PatrolTransportType mandate = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		
		IconSelectionDialog dialog = new IconSelectionDialog(transportTblViewer.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		updateIcon(mandate, dialog.getSelectedIcon());
	}
	
	private void editPatrolTypeIcon() {
		PatrolType mandate = (PatrolType)patrolTypeTblViewer.getStructuredSelection().getFirstElement();
		
		IconSelectionDialog dialog = new IconSelectionDialog(patrolTypeTblViewer.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		updatePatrolTypeIcon(mandate, dialog.getSelectedIcon());
	}
	
	private void updateIcon(PatrolTransportType type, Icon icon) {
		iconCache.clearCache(type);
		type.setIcon(icon);
		transportTblViewer.refresh();
		setChangesMade(true);
	}
	
	private void updatePatrolTypeIcon(PatrolType type, Icon icon) {
		iconCache.clearCache(type);
		type.setIcon(icon);
		patrolTypeTblViewer.refresh();
		setChangesMade(true);;
	}
	
	/*
	 * Creates station table columns
	 */
	private void createTransportColumns(final TableViewer viewer) {
		
		// Icon Column
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setResizable(true);
		column.setText(DialogConstants.ICON_TEXT);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
		column.setWidth( 32 + 20);
		
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ""; //$NON-NLS-1$
			}
			
			@Override
			public Image getImage(Object element) {
				if (!(element instanceof PatrolTransportType)) return null;
				PatrolTransportType tt = (PatrolTransportType) element;
				return iconCache.getImage(tt);
			}
		});
			
		
		
		/* Transport Type Name Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.PatrolTypePropertyPage_TransportType_ColumnHeader);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
			
		final ColumnLabelProvider lblProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType tt) return getName(tt);
				return super.getText(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		};
			
		viewerColumn.setLabelProvider(lblProvider);
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
				return lblProvider.getText(element);
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolTransportType){
					PatrolTransportType ttype = (PatrolTransportType)element;
					PatrolType pt = ttype.getPatrolType();
					
					if (!ttype.findName(languageViewer.getCurrentSelection()).equals((String)value)){
						if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
							Integer matches = 0;
							for (Iterator<PatrolTransportType> itr = pt.getTransportTypes().iterator(); itr.hasNext();) {
								PatrolTransportType a = itr.next();
								if( a != element && a.findName(languageViewer.getCurrentSelection()).equalsIgnoreCase(((String)value).trim())){
									matches++;
								}
							} 
							if(matches > 0){
								//invalid agency name, don't update it.
								MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, Messages.PatrolTypePropertyPage_Error_DuplicateTransportOption);
								setChangesMade(false);
							}else{
								ttype.updateName(languageViewer.getCurrentSelection(), ((String)value).trim());
								if (ttype.getKeyId() == null || ttype.getUuid() == null){
									ttype.setKeyId(DataModelManager.INSTANCE.generateKey((String)value, transportTypes));
								}
								setChangesMade(true);
							}
						}else{
							//invalid agency name, don't update it.
							MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, 
									MessageFormat.format(Messages.PatrolTypePropertyPage_Error_InvalidTransportType, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, PatrolType.MAX_TRANSPORT_NAME_LENGTH}));
							setChangesMade(false);
						}				
					}
					viewer.refresh();
				}					
			}});
		final TableViewerColumn vc2 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc2.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			transportTypes.sort((a,b)-> change * Collator.getInstance().compare(getName(a), getName(b)));
			viewer.refresh();
		});
		
		/* Patrol Type */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(Messages.PatrolTypePropertyPage_TrackTypeHeader);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(2,ColumnWeightData.MINIMUM_WIDTH, true));
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType p) return getName(p.getPatrolType());
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		
		viewerColumn.setEditingSupport(new EditingSupport(viewer){
			@Override
			protected CellEditor getCellEditor(Object element) {
				ComboBoxViewerCellEditor typeEditor = new ComboBoxViewerCellEditor(viewer.getTable(), SWT.READ_ONLY) {
					@Override
					protected Control createControl(Composite parent) {
						final Control control = super.createControl(parent);
						getViewer().getCCombo().addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								// if the list is not visible, assume the user is done
								if (!getViewer().getCCombo().getListVisible())
									// since I cannot access
									// applyEditorValueAndDeactivate();
									focusLost();
							}
						});
						return control;
					}
				};
				typeEditor.setActivationStyle(ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION );

				
				typeEditor.setLabelProvider(new LabelProvider(){
					public String getText(Object element){
						if (element instanceof PatrolType p) return getName(p);
						return super.getText(element);
					}
				});
				typeEditor.setContentProvider(ArrayContentProvider.getInstance());
				typeEditor.setInput(patrolTypes);
				return typeEditor;
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof PatrolTransportType type) return type.getPatrolType();					
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof PatrolTransportType ttype){
					PatrolType type = (PatrolType)value;
					
					ttype.getPatrolType().getTransportTypes().remove(ttype);
					ttype.setPatrolType(type);
					type.getTransportTypes().add(ttype);
					setChangesMade(true);
					viewer.refresh();
				}					
			}});
		
		final TableViewerColumn vc = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
						
			int change = dir == SWT.DOWN ? -1 : 1;
			transportTypes.sort((a,b)-> change * Collator.getInstance().compare(getName(a.getPatrolType()), getName(b.getPatrolType())));
			viewer.refresh();
		});
		
		/* Active Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					if (((PatrolTransportType)element).getIsActive()){
						return ACTIVE_LABEL;
					}else{
						return DISABLED_LABEL;
					}
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc1 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc1.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			
			int change = dir == SWT.DOWN ? -1 : 1;
			transportTypes.sort((a,b)-> change * Boolean.compare(a.getIsActive(), b.getIsActive()));
			viewer.refresh();
		});
		
		
		
		/* Key Column */
		viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		column = viewerColumn.getColumn();
		column.setText(LabelConstants.TRANSTYPE_KEY);
		column.setResizable(true);
		column.setMoveable(true);

		layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(1,ColumnWeightData.MINIMUM_WIDTH, true));
			
		viewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolTransportType){
					return ((PatrolTransportType) element).getKeyId();
				}
				return super.getText(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof PatrolTransportType){
					if (!((PatrolTransportType) element).getIsActive()) return getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
				
			}
		});
		final TableViewerColumn vc3 = viewerColumn;
		viewerColumn.getColumn().addListener(SWT.Selection, l->{
			viewer.getTable().setSortColumn(vc3.getColumn());
			int dir = viewer.getTable().getSortDirection();
			if (dir == SWT.UP){
				dir = SWT.DOWN;
			}else{
				dir = SWT.UP;
			}
			viewer.getTable().setSortDirection(dir);
			int change = dir == SWT.DOWN ? -1 : 1;
			transportTypes.sort((a,b)-> change * Collator.getInstance().compare(a.getKeyId(), b.getKeyId()));
			viewer.refresh();
		});
	}
	private HashSet<NamedItem> toDelete = new HashSet<>();
	
	private void deleteTransportType(){
		
		PatrolTransportType ttype = (PatrolTransportType) ((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		if (ttype == null){
			return;
		}

		boolean ok = MessageDialog.openConfirm(getShell(), Messages.PatrolTypePropertyPage_DeleteDialogTitle, MessageFormat.format(Messages.PatrolTypePropertyPage_DeleteWarningMessage, new Object[]{getName(ttype)}));
		if (!ok){
			return;
		}
		
		try(Session s = HibernateManager.openSession()){
			ok = true;
			if (ttype.getUuid() != null){
				if (!DeleteManager.canDelete(ttype, s)){
					ok = false;
				}
			}
			if (ok){
				ttype.getPatrolType().getTransportTypes().remove(ttype);
				toDelete.add(ttype);
				ttype.getPatrolType().getTransportTypes().remove(ttype);
				ttype.setPatrolType(null);
				transportTypes.remove(ttype);
				setChangesMade(true);
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.PatrolTypePropertyPage_Error_DeletingTransport + " " + ex.getLocalizedMessage(), new Object[]{ getName(ttype)}), ex); //$NON-NLS-1$
		}
		
		transportTblViewer.refresh();
		
	}
	
	private String getName(NamedItem item ) {
		if (item == null) return ""; //$NON-NLS-1$
		if (this.languageViewer == null) return item.getName();
		String x = item.findNameNull(languageViewer.getCurrentSelection());
		if ( x != null) return x;
		return item.getName();
	}
	
	private void deletePatrolType(){
		PatrolType ptype = (PatrolType) patrolTypeTblViewer.getStructuredSelection().getFirstElement();
		if (ptype == null) return;
		
		boolean ok = MessageDialog.openConfirm(getShell(), 
				Messages.PatrolTypePropertyPage_DeleteDialogTitle,
				MessageFormat.format(Messages.PatrolTypePropertyPage_DeleteConfirmation, new Object[]{ptype.getName()}));
		if (!ok){
			return;
		}
		
		try(Session s = HibernateManager.openSession()){
			ok = true;
			if (ptype.getUuid() != null){
				if (!DeleteManager.canDelete(ptype, s)){
					ok = false;
				}
			}
			if (ok){
				patrolTypes.remove(ptype);
				toDelete.add(ptype);
				setChangesMade(true);
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.PatrolTypePropertyPage_DeleteError, getName(ptype), ex.getMessage()), ex);
		}
		
		patrolTypeTblViewer.refresh();
		
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		List<PatrolTransportType> siblings = new ArrayList<>(transportTypes);
		
		//validate keys
		try{
			for (PatrolTransportType tt : transportTypes){
				siblings.remove(tt);
				String error = DataModelManager.INSTANCE.validateKey(tt.getKeyId(), siblings);
				siblings.add(tt);
				if (error != null){
					throw new Exception(error);
				}
			}
			List<PatrolType> siblings2 = new ArrayList<>(patrolTypes);
			for (PatrolType tt : patrolTypes){
				siblings2.remove(tt);
				String error = DataModelManager.INSTANCE.validateKey(tt.getKeyId(), siblings2);
				siblings2.add(tt);
				if (error != null){
					throw new Exception(error);
				}
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolTypePropertyPage_Error_SavingChanges1 + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
		
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.beginTransaction();
			try{
				for (NamedItem t : toDelete){
					if (t.getUuid() == null) continue;

					if (t instanceof PatrolType pt) {
						s.remove(s.get(PatrolType.class, pt.getUuid()));
					} 
				}
				s.flush();
				for (PatrolType tt : patrolTypes){
					HibernateManager.saveOrMerge(s,  tt.getIcon());
					if (tt.getUuid() == null) s.persist(tt);
				}
				for (PatrolTransportType tt : transportTypes){
					HibernateManager.saveOrMerge(s,  tt.getIcon());
					if (tt.getUuid() == null) s.persist(tt);
				}
				s.flush();
				
				for (PatrolTransportType tt : transportTypes){
					HibernateManager.saveOrMerge(s, tt);
				}
				s.flush();
				
				for (PatrolType tt : patrolTypes){
					tt.getTransportTypes().removeAll(toDelete);
					HibernateManager.saveOrMerge(s, tt);
				}
				s.flush();

				
				s.getTransaction().commit();
				toDelete.clear();
				setChangesMade(false);
				return true;
			}catch (Exception ex){
				s.getTransaction().rollback();
				SmartPatrolPlugIn.displayLog(Messages.PatrolTypePropertyPage_Error_SavingChanges1 + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			}
		}
		return false;
	}
}
