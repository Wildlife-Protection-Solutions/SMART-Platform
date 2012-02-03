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

import java.util.Properties;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.layout.TableColumnLayout;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Dialog for managing conservation area station 
 * list.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class StationListPropertyPage extends AbstractPropertyJHeaderDialog {

	public static final String ID = "org.wcs.smart.ca.StationListPropertyPage";

	private WritableList stations = null;
	//private HashSet<Station> toDelete = new HashSet<Station>();

	private LanguageViewer cmbLanguage;

	private TableViewer tableViewer;
	private StationSorter sorter;
	private Button btnDisable; 
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	private UUIDGenerator uuidGenerator = null;

	public static Color gray = null;
	public static Color black = null;
	
	
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
		super(Display.getCurrent().getActiveShell(), "Station List");
		
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
	
		gray = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);
		black = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
		
		
		stations = new WritableList(HibernateManager.getStations(ca,
				getSession()), Station.class);

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText("Language:");

		cmbLanguage = new LanguageViewer(container, SWT.NONE, ca);
		Combo lblLanguage = cmbLanguage.getCombo();
		lblLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 2, 1));

		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);

		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION);

		createColumns(tableViewer);

		tableViewer.setContentProvider(new ObservableListContentProvider());
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
		btnAdd.setText("Add");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addStation();
			}

		});

//		Button btnDelete = new Button(composite, SWT.NONE);
//		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
//				false, 1, 1));
//		btnDelete.setText("Delete");
//		btnDelete.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				deleteStation(tableViewer);
//			}
//		});
		
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
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Station stn = (Station)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (stn != null){
					btnDisable.setEnabled(true);
					if (stn.getIsActive()){
						btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}else{
						btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}
				}	
			}
		});

		setMessage("Manage the list of stations related to the conservation area.");
		return container;
		
		
	}

	private void addStation() {
		sorter.setSortColumn(null, null);	//we want to make sure this is added at the end 
		
		Station x = new Station();
		x.setConservationArea(ca);
		x.setIsActive(true);
		
		org.wcs.smart.ca.Label nameLabel = new org.wcs.smart.ca.Label();
		nameLabel.setElementuuid(x.getUuid());
		nameLabel.setLanguageuuid(ca.getDefaultLanguage().getUuid());
		nameLabel.setValue("New Station");
		x.getNames().add(nameLabel);

		org.wcs.smart.ca.Label descLabel = new org.wcs.smart.ca.Label();
		descLabel.setElementuuid(x.getUuid());
		descLabel.setLanguageuuid(ca.getDefaultLanguage().getUuid());
		descLabel.setValue("Description ");
		x.getDescriptions().add(descLabel);

		stations.add(x);
		setChangesMade(true);
		
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
			return stn.findName(lang);
		} else if (type == Column.DESCIPTION) {
			return stn.findDescription(lang);
		}
		return "";
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
			if (!findLangValue(type, stn).equals(newValue)){
				stn.updateName(lang, newValue);
				setChangesMade(true);
			}
		} else if (type == Column.DESCIPTION) {
			if (!findLangValue(type, stn).equals(newValue)){
				stn.updateDescription(lang, newValue);
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
//			for (Station stn : toDelete) {
//				s.update(stn);
//				// remove all labels
//				Set<org.wcs.smart.ca.Label> toDelete = new HashSet<org.wcs.smart.ca.Label>();
//				toDelete.addAll(stn.getNames());
//				toDelete.addAll(stn.getDescriptions());
//				for (Iterator<org.wcs.smart.ca.Label> iterator = toDelete
//						.iterator(); iterator.hasNext();) {
//					org.wcs.smart.ca.Label lbl = (org.wcs.smart.ca.Label) iterator
//							.next();
//					s.update(lbl);
//					s.delete(lbl);
//				}
//				// remove station
//				s.delete(stn);
//			}
//			toDelete.clear();

			// add/update stations
			for (int i = 0; i < stations.size(); i++) {
				Station stn = (Station) stations.get(i);
				s.saveOrUpdate(stn);
				for (org.wcs.smart.ca.Label lbl : stn.getNames()) {
					if (lbl.getElementuuid() == null) {
						lbl.setElementuuid(stn.getUuid());
					}
					s.saveOrUpdate(lbl);
				}

				for (org.wcs.smart.ca.Label lbl : stn.getDescriptions()) {
					if (lbl.getElementuuid() == null) {
						if (stn.getDescUuid() == null) {
							byte[] uuid = (byte[]) uuidGenerator.generate(
									(SessionImplementor) s, lbl);
							stn.setDescUuid(uuid);
							s.saveOrUpdate(stn);
						}
						lbl.setElementuuid(stn.getDescUuid());
					}
					s.saveOrUpdate(lbl);
				}

			}

			tx.commit();
			setChangesMade(false);
			return true;
		} catch (RuntimeException ex) {
			tx.rollback();
			SmartPlugIn.displayLog(getShell(),
					"Error saving stations. " + ex.getLocalizedMessage(), ex);
			s.close();
		}
		return false;
	}

	@Override
	public boolean close() {
		boolean ret = super.close();
		if (stations != null) {
			stations.dispose();
			gray.dispose();
			black.dispose();
		}
		
		return ret;
	}

//	
//	@Override
//	public void performDefaults() {
//		session.beginTransaction();
//		try {
//			for (Iterator<Station> iterator = stations.iterator(); iterator.hasNext();) {
//				Station stn = (Station) iterator.next();
//				if (stn.getUuid() != null) {
//					stn.evitNames(session);
//					for (Iterator<org.wcs.smart.ca.Label> iterator2 = stn.getDescriptions().iterator(); iterator2
//							.hasNext();) {
//						org.wcs.smart.ca.Label lbl = (org.wcs.smart.ca.Label) iterator2
//								.next();
//						session.evict(lbl);
//					}
//					session.evict(stn);
//				}
//			}
//			session.getTransaction().commit();
//
//			stations.clear();
//			toDelete.clear();
//			stations.addAll(HibernateManager.getStations(ca,
//					session));
//
//		} catch (RuntimeException ex) {
//			SmartPlugIn.displayLog(
//					"Error loading defaults for stations. "
//							+ ex.getLocalizedMessage(), ex);
//			session.getTransaction().rollback();
//			session.close();
//		}
//
//		tableViewer.refresh();
//		super.performDefaults();
//	}
//
//	private void deleteStation(final TableViewer tableViewer) {
//		IStructuredSelection sel = ((IStructuredSelection) tableViewer
//				.getSelection());
//		for (Iterator iterator = sel.iterator(); iterator.hasNext();) {
//			Station type = (Station) iterator.next();
//			stations.remove(type);
//			if (type.getUuid() != null) {
//				toDelete.add(type);
//			}
//		}
//		setChangesMade(true);
//	}

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
				 return StationListPropertyPage.black;
			 }else{
				 return StationListPropertyPage.gray;
			 }
		 }
		
		public Color getBackground(Object element){
			 return null;
		 }
	}
}
