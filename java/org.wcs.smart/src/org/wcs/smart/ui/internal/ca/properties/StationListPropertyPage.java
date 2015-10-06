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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.UUIDBinaryType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.export.StationCsvImporter;
import org.wcs.smart.export.config.impl.StationCsvExportConfig;
import org.wcs.smart.export.config.impl.StationCsvImportConfig;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

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
	private Button btnDisable; 
	private Button btnDelete;
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	private UUIDGenerator uuidGenerator = null;
	 
	
	/*
	 * columns in the station table
	 */
	private enum Column {
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
		uuidGenerator = UUIDGenerator
				.buildSessionFactoryUniqueIdentifierGenerator();
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY,
				StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS,
				UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new UUIDBinaryType(), prop, null);
	}

	/**
	 * Create contents of the property page.
	 * 
	 * @param parent
	 */
	@Override
	public Composite createContent(Composite parent) {
		getSession().beginTransaction();
		try{
			stations = new ArrayList<Station>(HibernateManager.getStations(currentCa,getSession()));
			Collections.sort(stations, new Comparator<Station>() {
				@Override
				public int compare(Station o1, Station o2) {
					String name1 = o1.getName();
					if (name1 != null){
						name1 = name1.toLowerCase();
					}
					String name2 = o2.getName();
					if (name2 != null){
						name2 = name2.toLowerCase();
					}
					return nullStringComparator.compare(name1, name2);
				}
			});
		}finally{
			getSession().getTransaction().rollback();
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

		
		sorter = new StationSorter();
		tableViewer.setComparator(sorter);
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,
				1, 1));

		Button btnAdd = new Button(composite, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addStation();
			}

		});
		
		btnDisable = new Button(composite, SWT.NONE);
		btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		btnDisable.setEnabled(false);
		btnDisable.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (btnDisable.getText().equals(DialogConstants.DISABLE_BUTTON_TEXT)){
					stn.setIsActive(false);
					btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}else{
					stn.setIsActive(true);
					btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}
				tableViewer.refresh();
				setChangesMade(true);
			}
		});
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteStation();
			}
		});
		
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (stn != null){
					btnDisable.setEnabled(true);
					btnDelete.setEnabled(true);
					if (stn.getIsActive()){
						btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}else{
						btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}
				}	
			}
		});
		
		Composite btnPanel = new Composite(container, SWT.NONE);
		btnPanel.setLayout(new GridLayout(2, false));
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3,1));
		
		Button btnImport = new Button(btnPanel, SWT.PUSH);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
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
			
		});
		
		Button btnExport = new Button(btnPanel, SWT.PUSH);
		btnExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		btnExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CsvExportDialog dialog = new CsvExportDialog(getShell(), new StationCsvExportConfig());
				dialog.open();
			}
			
		});

		setTitle(Messages.StationListPropertyPage_PageName);
		setMessage(Messages.StationListPropertyPage_Dialog_Message);
		return container;

	}
	
	private void deleteStation(){
		Station s = (Station) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (s == null){
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.StationListPropertyPage_ConfirmDeleteTitle, MessageFormat.format(Messages.StationListPropertyPage_ConfirmDeleteMessage, new Object[]{s.getName()}))){
			return;
		}

		try{
			if (s.getUuid() != null){
				if (DeleteManager.canDelete(s, getSession())){
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
			String value = stn.findDescriptionNull(getSession(), lang);
			if (value == null){
				value = stn.findDescriptionNull(getSession(), SmartDB.getCurrentConservationArea().getDefaultLanguage());
				if (value == null){
					value = ""; //$NON-NLS-1$
				}
			}
			return value;
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
			if (newValue.equals(stn.findDescriptionNull(getSession(), lang))){
				//no modification made
				return;
			}
			if (validate(type, stn, newValue) == null){
				stn.updateDescription(getSession(), lang, newValue.trim());
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
	protected boolean  performSave() {
		Session s = getSession();
		Transaction tx = s.beginTransaction();
		try {
			for(Station station : toDelete){
				s.delete(station);
			}
			// add/update stations
			for (int i = 0; i < stations.size(); i++) {
				Station stn = (Station) stations.get(i);
				s.saveOrUpdate(stn);

				for (org.wcs.smart.ca.DescriptionLabel lbl : stn.getDescriptions(getSession())) {
					if (lbl.getElementuuid() == null) {
						if (stn.getDescUuid() == null) {
							UUID uuid = (UUID) uuidGenerator.generate(
									(SessionImplementor) s, lbl);
							stn.setDescUuid(uuid);
							s.saveOrUpdate(stn);
						}
						lbl.setElement(stn.getDescUuid());
					}
					s.saveOrUpdate(lbl);
				}

			}

			tx.commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		} catch (RuntimeException ex) {
			tx.rollback();
			SmartPlugIn.displayLog(Messages.StationListPropertyPage_Error_Saving + ex.getLocalizedMessage(), ex);
			s.close();
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
	}
}
