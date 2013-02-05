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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.BinaryType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
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
		NAME(Station.NAME, 1), 
		DESCIPTION(Station.DESCRIPTION, 3);

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
	public StationListPropertyPage() {
		super(Display.getCurrent().getActiveShell(), Messages.StationListPropertyPage_Dialog_Title);
		
		uuidGenerator = UUIDGenerator
				.buildSessionFactoryUniqueIdentifierGenerator();
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY,
				StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS,
				UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new BinaryType(), prop, null);
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
			stations = new ArrayList<Station>(HibernateManager.getStations(ca,getSession()));
		}finally{
			getSession().getTransaction().rollback();
		}

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText(Messages.StationListPropertyPage_LanguageLabel);

		cmbLanguage = new LanguageViewer(container, SWT.NONE, ca);
		Combo lblLanguage = cmbLanguage.getCombo();
		lblLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 2, 1));
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
		
		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);

		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(stations);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
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
			SmartPlugIn.displayLog(getShell(), Messages.StationListPropertyPage_Error_CouldNotDelete + s.getName(), ex);
		}	
		
		tableViewer.refresh();
		
	}
	
	private void addStation() {
		sorter.setSortColumn(null, null);	//we want to make sure this is added at the end 
		
		final Station x = new Station();
		x.setConservationArea(ca);
		x.setIsActive(true);
		x.updateName(ca.getDefaultLanguage(), Messages.StationListPropertyPage_Default_NewStationName);
		x.setName(x.findName(ca.getDefaultLanguage()));

		stations.add(x);
		setChangesMade(true);
		
		tableViewer.setSelection(new StructuredSelection(x));
		tableViewer.refresh();
		
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
				value = stn.getName();
			}
			return value;
		} else if (type == Column.DESCIPTION) {
			String value = stn.findDescriptionNull(lang);
			if (value == null){
				value = stn.getDescription();
				if (value == null){
					value = ""; //$NON-NLS-1$
				}
			}
			return value;
		}
		return ""; //$NON-NLS-1$
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
			if (!findLangValue(type, stn).equals(newValue.trim())){

				if(SmartUtils.isSimpleString(newValue.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Station.MAX_STATION_NAME_LENGTH)){
					Integer matches = 0;
					for (Iterator<Station> itr = stations.iterator(); itr.hasNext();) {
						Station a = itr.next();
						if( a != stn && a.findName(cmbLanguage.getCurrentSelection()).equals(newValue.trim())){
							matches++;
						}
					} 
					if(matches > 0){
						//invalid station name, don't update it.
						MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.StationListPropertyPage_InvalidName_DialogTitle, Messages.StationListPropertyPage_Error_CannotDuplicate);
					}else{
						stn.updateName(lang, newValue.trim());
						setChangesMade(true);
					}
				}else{
					//invalid value, show error 
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.StationListPropertyPage_InvalidName_DialogTitle, 
							MessageFormat.format(
									Messages.StationListPropertyPage_Error_InvalidName,
									new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
				}
			}
		} else if (type == Column.DESCIPTION) {
			if (!findLangValue(type, stn).equals(newValue.trim())){
				if(SmartUtils.isSimpleString(newValue.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Station.MAX_STATION_DESC_LENGTH, 0)){
					stn.updateDescription(lang, newValue.trim());
					setChangesMade(true);
				}else{
					MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.StationListPropertyPage_InvalidDescription_DialogTitle, 
							MessageFormat.format(Messages.StationListPropertyPage_Error_InvalidDescription,
									new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
					setChangesMade(false);
				}
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
			col.setEditingSupport(new TextTableEditor(viewer, colum));
			
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

				for (org.wcs.smart.ca.DescriptionLabel lbl : stn.getDescriptions()) {
					if (lbl.getElementuuid() == null) {
						if (stn.getDescUuid() == null) {
							byte[] uuid = (byte[]) uuidGenerator.generate(
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
			SmartPlugIn.displayLog(getShell(),
					Messages.StationListPropertyPage_Error_Saving + ex.getLocalizedMessage(), ex);
			s.close();
		}
		return false;
	}

	private class TextTableEditor extends EditingSupport {
		private Column column;
		private TableViewer viewer;

		TextTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
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
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(viewer.getTable());
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
			return nullStringComparator.compare(findLangValue(column, s1),	findLangValue(column, s2));
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
