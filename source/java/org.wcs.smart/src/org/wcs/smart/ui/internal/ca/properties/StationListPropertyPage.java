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
package org.wcs.smart.ui.internal.ca.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.export.StationCsvImporter;
import org.wcs.smart.export.config.impl.StationCsvExportConfig;
import org.wcs.smart.export.config.impl.StationCsvImportConfig;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.IconSelectionDialog;
import org.wcs.smart.ui.IconSelectionDialog.Type;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Dialog for managing conservation area station 
 * list.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class StationListPropertyPage extends AbstractPropertyJHeaderDialog {

	private ConservationArea currentCa = null;
	private List<Station> stations = null;
	private HashSet<Station> toDelete = new HashSet<Station>();

	private LanguageViewer cmbLanguage;
	private TableViewer tableViewer;
	private StationSorter sorter;
	private Button btnDisable, btnDelete;
	private MenuItem miDisable, miDelete, miEdit, miClearIcon;
	
	private static NullComparator nullStringComparator = new NullComparator();

	private int editIndex = -1;
	private IconCache iconCache;
	
	/*
	 * columns in the station table
	 */
	private enum Column {
		ICON(DialogConstants.ICON_TEXT, 1),
		NAME(SmartLabelProvider.STATION_NAME, 1), 
		DESCIPTION(SmartLabelProvider.STATION_DESCRIPTION, 3);

		String name;
		int bounds;

		Column(String name, int bounds) {
			this.name = name;
			this.bounds = bounds;
		}
	};
	

	/**
	 * Create the property page.
	 */
	public StationListPropertyPage(Shell parent) {
		super(parent, Messages.StationListPropertyPage_Dialog_Title);
		this.currentCa = SmartDB.getCurrentConservationArea();
	}

	
	
	/**
	 * Create contents of the property page.
	 * 
	 * @param parent
	 */
	@Override
	public Composite createContent(Composite parent) {
		iconCache = new IconCache(parent);
		
		try(Session s = HibernateManager.openSession()){
			stations = new ArrayList<Station>(HibernateManager.getStations(currentCa,s));
			Collections.sort(stations);
			stations.forEach(station ->station.getNames().size());
			s.get(ConservationArea.class, currentCa.getUuid()).getLanguages().size();
		}
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		lblNewLabel.setText(Messages.StationListPropertyPage_LanguageLabel);

		cmbLanguage = new LanguageViewer(container, SWT.NONE, currentCa);
		cmbLanguage.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		cmbLanguage.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tableViewer.refresh();
			}
		});
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)composite2.getLayoutData()).heightHint = 150;

		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		
		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(stations);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		

		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(tableViewer, new FocusCellHighlighter(tableViewer){});
		
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tableViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(tableViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		
		tableViewer.getTable().addListener(SWT.MouseDoubleClick, event->{
			ViewerCell cell = tableViewer.getCell(new Point(event.x, event.y));
			if (cell == null) return;
			if (cell.getColumnIndex() == Column.ICON.ordinal()) editIcon();
		});
		
		
		sorter = new StationSorter();
		tableViewer.setComparator(sorter);
		
		
		Menu mnu = new Menu(tableViewer.getTable());
		tableViewer.getTable().setMenu(mnu);
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addStation());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection, e->{
			if (editIndex == -1) return;
			if (editIndex == Column.ICON.ordinal()) {
				editIcon();
			}else {
				tableViewer.editElement((Station) tableViewer.getStructuredSelection().getFirstElement(), editIndex);
			}
		});
		
		miClearIcon = new MenuItem(mnu, SWT.PUSH);
		miClearIcon.setText(DialogConstants.CLEAR_IMAGE_TEXT);
		miClearIcon.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miClearIcon.addListener(SWT.Selection, e->{
			Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
			updateIcon(stn, null);
		});
		
		miDisable = new MenuItem(mnu, SWT.PUSH);
		miDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		miDisable.addListener(SWT.Selection, e->{
			Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
			if (stn != null) stn.setIsActive(!stn.getIsActive());
			updateButtons();
			tableViewer.refresh();
			setChangesMade(true);
		});
		
		tableViewer.getTable().addListener(SWT.MenuDetect, evt->{
			ViewerCell cell = tableViewer.getCell(tableViewer.getControl().toControl(evt.x,  evt.y));
			editIndex = -1;
			if (cell != null) editIndex = cell.getColumnIndex();	
		});
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,1, 1));
		((GridLayout)composite.getLayout()).marginWidth = 0;
		((GridLayout)composite.getLayout()).marginHeight = 0;

		Button btnAddTransport = new Button(composite, SWT.NONE);
		btnAddTransport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAddTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,1, 1));
		btnAddTransport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAddTransport.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddTransport.addListener(SWT.Selection, e->addStation());
		
		btnDisable = new Button(composite, SWT.NONE);
		btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDisable.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
		btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisable.setEnabled(false);
		btnDisable.addListener(SWT.Selection, e->{
			Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
			if (stn != null) stn.setIsActive(!stn.getIsActive());
			updateButtons();
			tableViewer.refresh();
			setChangesMade(true);
		});
		
		
		
	
		
		if (PermissionManager.INSTANCE.canDelete(Station.class)){
			new MenuItem(mnu, SWT.SEPARATOR);
			
			miDelete = new MenuItem(mnu, SWT.PUSH);
			miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addListener(SWT.Selection, e->deleteStation());
			
			btnDelete = new Button(composite, SWT.NONE);
			btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
			btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			btnDelete.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			btnDelete.setEnabled(false);
			btnDelete.addListener(SWT.Selection, e->deleteStation());
		}
		
		
		
		Label l2 = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		l2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button tiImport = new Button(composite, SWT.PUSH);
		tiImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		tiImport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tiImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		tiImport.addListener(SWT.Selection, e->importStations());

		
		Button tiExport = new Button(composite, SWT.PUSH);
		tiExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		tiExport.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tiExport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		tiExport.addListener(SWT.Selection, e->exportStations());


		
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		
		setTitle(Messages.StationListPropertyPage_PageName);
		setMessage(Messages.StationListPropertyPage_Dialog_Message);
		return container;

	}
	
	
	private void editIcon() {
		Station team = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		
		IconSelectionDialog dialog = new IconSelectionDialog(tableViewer.getControl().getShell(), Type.SELECT);
		if (dialog.open()  != Window.OK) return ;
		updateIcon(team, dialog.getSelectedIcon());
	}
	
	private void updateIcon(Station team, Icon icon) {
		iconCache.clearCache();
		team.setIcon(icon);
		tableViewer.refresh();
		setChangesMade(true);
	}
	
	private void updateButtons() {
		Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (stn != null){
			if (btnDelete != null) btnDelete.setEnabled(true);
			if (miDelete != null) miDelete.setEnabled(true);
			if (miEdit != null) miEdit.setEnabled(true);
			if (miClearIcon != null) miClearIcon.setEnabled(true);
			
			miDisable.setEnabled(true);
			btnDisable.setEnabled(true);
			if (stn.getIsActive()){
				btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				miDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
				miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
			}else{
				btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				miDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				btnDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
				miDisable.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON));
			}
		}	
	}
	
	private void exportStations() {
		CsvExportDialog dialog = new CsvExportDialog(getShell(), new StationCsvExportConfig());
		dialog.open();
	}
	
	private void importStations() {
		StationCsvImportConfig config = new StationCsvImportConfig();
		CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
		int ret = dialog.open();
		if (ret == IDialogConstants.CANCEL_ID) {
			return;
		} else {
			Collection<Station> importedData = ((StationCsvImporter)config.getImporter()).getImportedData();
			if (importedData != null && importedData.size() > 0){
				stations.addAll(importedData);
				tableViewer.refresh();
				setChangesMade(true);
			}
		}
	}
	
	private void deleteStation(){
		Station s = (Station) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (s == null){
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.StationListPropertyPage_ConfirmDeleteTitle, MessageFormat.format(Messages.StationListPropertyPage_ConfirmDeleteMessage, new Object[]{s.getName()}))){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
			if (s.getUuid() != null){
				if (DeleteManager.canDelete(s, session)){
					stations.remove(s);
					toDelete.add(s);
					setChangesMade(true);
				}
			}else{
				stations.remove(s);
			}
				
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.StationListPropertyPage_Error_CouldNotDelete + s.getName() + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
		}
		
		tableViewer.refresh();
		
	}
	
	private void addStation() {
		sorter.setSortColumn(null, null);	//we want to make sure this is added at the end 
		final Station x = new Station();
		x.setConservationArea(currentCa);
		x.setIsActive(true);
		x.updateName(currentCa.getDefaultLanguage(), Messages.StationListPropertyPage_Default_NewStationName);
		x.setName(x.findName(currentCa.getDefaultLanguage()));

		stations.add(x);
		setChangesMade(true);
		
		tableViewer.refresh();
		tableViewer.setSelection(new StructuredSelection(x));
		
		
	}

	/**
	 * Finds the property for the given column from the station.
	 * 
	 * @param type the property required
	 * @param stn station 
	 * @return string value of the requested property
	 */
	private String findLangValue(Column type, Station stn) {
		Language lang = cmbLanguage.getCurrentSelection();
		if (type == Column.NAME) {
			String value = stn.findNameNull(lang);
			if (value == null){
				value = stn.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			}
			return value;
		} else if (type == Column.DESCIPTION) {
			
			try(Session s = HibernateManager.openSession()){
				String value = stn.findDescriptionNull(s, lang);
				if (value == null){
					value = stn.findDescriptionNull(s, SmartDB.getCurrentConservationArea().getDefaultLanguage());
					if (value == null){
						value = ""; //$NON-NLS-1$
					}
				}
				return value;
			}
		}
		return ""; //$NON-NLS-1$
	}

	private String validate(Column column, Station station, String newName){
		setErrorMessage(null);
		if (column == Column.NAME){
			if (!findLangValue(column, station).equals(newName.trim())){

				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Station.MAX_STATION_NAME_LENGTH)){
					Integer matches = 0;
					for (Iterator<Station> itr = stations.iterator(); itr.hasNext();) {
						Station a = itr.next();
						if( !a.equals(station) && a.findName(cmbLanguage.getCurrentSelection()).equalsIgnoreCase(newName.trim())){
							matches++;
						}
					} 
					if(matches > 0){
						return Messages.StationListPropertyPage_Error_CannotDuplicate;
					}
				}else{
					return	MessageFormat.format(
								Messages.StationListPropertyPage_Error_InvalidName,
								new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, Station.MAX_STATION_NAME_LENGTH});
				}
			}
		}else if (column == Column.DESCIPTION){
			if (!findLangValue(column, station).equals(newName.trim())){
				if (newName.trim().length() > Station.MAX_STATION_DESC_LENGTH){
					return MessageFormat.format(Messages.StationListPropertyPage_Error_InvalidDescription,
								new Object[]{Station.MAX_STATION_DESC_LENGTH});
				}
			}
		}
		return null;
	}
	
	/**
	 * Updates the given station object with the new value.
	 * @param type
	 * @param stn
	 * @param newValue
	 */
	private void updateLangValue(Column type, Station stn, String newValue) {
		Language lang = cmbLanguage.getCurrentSelection();
		if (type == Column.NAME) {
			if (stn.findName(lang).equals(newValue)){
				//no modification made
				return;
			}
			if (validate(type, stn, newValue) == null){
				stn.updateName(lang, newValue.trim());
				setChangesMade(true);
			}
		} else if (type == Column.DESCIPTION) {
			String existing = null;
			
			try(Session s = HibernateManager.openSession()){
				existing = stn.findDescriptionNull(s, lang);
			}
			if (newValue.equals(existing)){
				//no modification made
				return;
			}
			if (validate(type, stn, newValue) == null){
				try(Session s = HibernateManager.openSession()){
					stn.updateDescription(s, lang, newValue.trim());
				}
				setChangesMade(true);
			}
			
		}
	}

	/*
	 * Creates station table columns
	 */
	private void createColumns(TableViewer viewer) {

		for (int i = 0; i < Column.values().length; i++) {
			final Column colum = Column.values()[i];
			final TableViewerColumn col = createTableViewerColumn(viewer, colum.name,
					colum.bounds, i);
			col.setLabelProvider(new StationLabelProvider(colum));
			
			if (colum == Column.DESCIPTION || colum == Column.NAME) {
				final TextTableEditor ed = new TextTableEditor(viewer, colum);
				ed.editor.addListener(new ICellEditorListener() {
					
					@Override
					public void editorValueChanged(boolean oldValidState, boolean newValidState) {
						setErrorMessage(ed.editor.getErrorMessage());
					}
					
					@Override
					public void cancelEditor() {
						setErrorMessage(null);
					}
					
					@Override
					public void applyEditorValue() {
						setErrorMessage(null);
						
					}
				});
				col.setEditingSupport(ed);
			}
					
			col.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					StationListPropertyPage.this.sorter.setSortColumn(colum, col.getColumn());
				}
				
			});

		}
	}

	private TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int weight, int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable()
				.getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight,
				ColumnWeightData.MINIMUM_WIDTH, true));

	
		
		return viewerColumn;
	}

	/**
	 * Saves the station changes to the database
	 */
	@Override
	protected boolean performSave() {
		try (Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			Transaction tx = s.beginTransaction();
			try {
				for(Station station : toDelete){
					s.remove(station);
				}
				// add/update stations
				for (int i = 0; i < stations.size(); i++) {
					Station stn = (Station) stations.get(i);
					
					for (org.wcs.smart.ca.DescriptionLabel lbl : stn.getDescriptions(s)) {
						if (lbl.getElementuuid() == null) {
							if (stn.getDescUuid() == null) {
								UUID uuid = UuidUtils.generateUuid((SessionImplementor) s);
								stn.setDescUuid(uuid);
							}
							lbl.setElement(stn.getDescUuid());
							s.persist(lbl);
						}else {
							s.merge(lbl);	
						}						
					}
					
					HibernateManager.saveOrMerge(s, stn.getIcon());			
					stn = HibernateManager.saveOrMerge(s, stn);	
				}
	
				tx.commit();
				toDelete.clear();
				setChangesMade(false);
				return true;
			} catch (RuntimeException ex) {
				tx.rollback();
				SmartPlugIn.displayLog(Messages.StationListPropertyPage_Error_Saving + ex.getLocalizedMessage(), ex);
			}
		}
		return false;
	}

	private class TextTableEditor extends EditingSupport {
		private Column column;
		private TableViewer viewer;
		private TextCellEditor editor;

		TextTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());
		}

		@Override
		protected void setValue(Object element, Object value) {
			updateLangValue(column, (Station) element, (String) value);
			viewer.refresh();
		}

		@Override
		protected Object getValue(Object element) {
			return findLangValue(column, (Station) element);
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			editor.setValidator(new ICellEditorValidator() {
				
				@Override
				public String isValid(Object value) {
					return validate(column, (Station)element, (String)value);
				}
			});
			return editor;
			
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
	
	
	class StationSorter extends ViewerComparator{
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
			StationListPropertyPage.this.tableViewer.getTable().setSortDirection(direction);
			StationListPropertyPage.this.tableViewer.getTable().setSortColumn(tcolumn);
			StationListPropertyPage.this.tableViewer.refresh();
		}
			
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null){
				//no sort column picked
				return 0;
			}
			if (direction == SWT.UP){
				return -compareValue((Station)object1, (Station)object2);
			}else{
				return compareValue((Station)object1, (Station)object2);
			}

		}
		
		
		private int compareValue(Station s1, Station s2){
			if (s1 == null && s2 == null){
				return 0;
			}else if (s1== null && s2 != null){
				return 1;
			}else if (s1 != null && s2 == null){
				return -1;
			}			
			String ss1 = findLangValue(column, s1);
			String ss2 = findLangValue(column, s2);
			if (ss1 != null){
				ss1 = ss1.toLowerCase();
			}
			if (ss2 != null){
				ss2 = ss2.toLowerCase();
			}
			return nullStringComparator.compare(ss1, ss2);
		}
	};
	
	class StationLabelProvider extends ColumnLabelProvider implements IColorProvider{ 
		private Column column;
		
		public StationLabelProvider(Column column){
			this.column = column;
		}
		
		@Override
		public String getText(Object element) {
			return findLangValue(column, (Station) element);
		}
		 
		@Override
		public Color getForeground(Object element){
			 if (((Station)element).getIsActive()){
				 return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			 }else{
				 return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			 }
		 }
		
		public Color getBackground(Object element){
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (column != Column.ICON) return null;
			if (element instanceof IconItem) return iconCache.getImage((IconItem) element);
			return null;
		}
	}
}
