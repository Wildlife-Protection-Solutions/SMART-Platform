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

import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.collections.comparators.NullComparator;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Rank and Agency list property page
 * 
 * @author Emily Gouge
 *
 */
public class AgencyRankPropertyPage extends AbstractPropertyJHeaderDialog{

	public static final String ID = "org.wcs.smart.ca.AgencyRankPropPage";
	/* ui components */
	private LanguageViewer cmbLanguage;
	private TableViewer tblAgencies;
	private TableViewer tblRank;
	private Button btnDeleteRank;
	private Button btnAddRank;

	/* agencies and rank lists */
	private Agency current = null;
	private WritableList currentRankSet;
	private WritableList agencies ;
	private HashSet<Agency> toDelete;
	
	private AgencySorter agencySorter;
	private RankSorter rankSorter;
	
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	/*
	 * columns of agency table
	 */
	private enum AgencyColumn{
		NAME( Agency.NAME, 1);
		String name;
		int bounds;
		AgencyColumn(String name, int bounds){
			this.name = name;
			this.bounds = bounds;
		}
	};
	
	/*
	 * columns of rank table
	 */
	private enum RankColumn{
		NAME( Rank.NAME, 1);
		String name;
		int bounds;
		RankColumn(String name, int bounds){
			this.name = name;
			this.bounds = bounds;
		}
	};
	
	/**
	 * Creates a new agency and rank property page
	 */
	public AgencyRankPropertyPage() {
		super(Display.getCurrent().getActiveShell(), "Agency and Rank List");
		toDelete = new HashSet<Agency>();
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(3, false));
		
		/* Language */
		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("Language:");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		cmbLanguage = new LanguageViewer(container, SWT.NONE,ca);
		Combo lblLanguage = cmbLanguage.getCombo();
		lblLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 3, 1));
		
		
		/* Agency */
		Label lblAgencies = new Label(container, SWT.NONE);
		lblAgencies.setText("Agencies:");
		lblAgencies.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3,1));
		
		Composite owner = new Composite(container, SWT.NONE);
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2,1));
		owner.setLayout(new TableColumnLayout());
		tblAgencies = createAgencyTableViewer(owner);
		
		//add/remove buttons
		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayout(new GridLayout(1,true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1,1));
		
		Button btnAddAgency = new Button(buttons, SWT.NONE);
		btnAddAgency.setText("Add Agency");
		btnAddAgency.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAgency();
			}
		});
		Button btnDeleteAgency = new Button(buttons, SWT.NONE);
		btnDeleteAgency.setText("Delete Agency");
		btnDeleteAgency.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteAgency();
			}
		});
		
		
		tblAgencies.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = ((IStructuredSelection)tblAgencies.getSelection());
				if (selection.isEmpty()){
					current = null;
					if (currentRankSet != null){
						currentRankSet.dispose();
					}
					currentRankSet = null;
					enableRank(false);
				}else{
					Agency agent = (Agency)selection.getFirstElement();
					getSession().beginTransaction();
					currentRankSet = new WritableList(agent.getRanks(), Rank.class);
					getSession().getTransaction().rollback();
					tblRank.setInput(currentRankSet);
					current = agent;
					enableRank(true);
				}
			}
		});
		
		
		/* Rank */
		Label lblRanks = new Label(container, SWT.NONE);
		lblRanks.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		lblRanks.setText("Ranks:");
		
		owner = new Composite(container, SWT.NONE);
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2,1));
		owner.setLayout(new TableColumnLayout());
		tblRank = createRankTableViewer(owner);
		
		Composite buttons_1 = new Composite(container, SWT.NONE);
		buttons_1.setLayout(new GridLayout(1,true));
		buttons_1.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1,1));
		
		btnAddRank = new Button(buttons_1, SWT.NONE);
		btnAddRank.setText("Add Rank");
		btnAddRank.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addRank();
				
			}
		});
		btnDeleteRank = new Button(buttons_1, SWT.NONE);
		btnDeleteRank.setText("Delete Rank");
		btnDeleteRank.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteRank();
			}
		});
		enableRank(false);
		
		tblAgencies.refresh();
		
		setMessage("Agency and Rank for Employees.");
		return container;
	}
	
	/**
	 * Finds the agency value for the given column 
	 * for the current selected language
	 */
	private String findAgencyValue(AgencyColumn col, Agency element){
		if (col == AgencyColumn.NAME){
			return element.findName(cmbLanguage.getCurrentSelection());
		}
		return null;
	}
	/**
	 * Updates the agency value for the given column with the new name
	 */
	private void updateAgencyValue(AgencyColumn col, Agency element, String newName){
		if (col == AgencyColumn.NAME){
			if (!findAgencyValue(col, element).equals(newName)){
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Agency.MAX_AGENCY_LENGTH)){
					Integer matches = 0;
					for (@SuppressWarnings("unchecked")	Iterator<Agency> itr = agencies.iterator(); itr.hasNext();) {
						Agency a = itr.next();
						if( a != element && a.findName(cmbLanguage.getCurrentSelection()).compareTo(newName.trim())==0){
							matches++;
						}
					} 
					if(matches > 0){
						//invalid agency name, don't update it.
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Invalid Name", "Agency Name cannot be a duplicate.");
						setChangesMade(false);
					}else{
						element.updateName(cmbLanguage.getCurrentSelection(), newName.trim());
						setChangesMade(true);
					}
				}else{
					//invalid agency name, don't update it.
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Invalid Name", "Agency Name must not be blank, nor contain characters other than " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
					setChangesMade(false);
				}
			}
		}
	}
	
	/**
	 * Finds the rank value for the given column 
	 * for the current selected language
	 */
	private String findRankValue(RankColumn col, Rank element){
		if (col == RankColumn.NAME){
			return element.findName(cmbLanguage.getCurrentSelection());
		}
		return null;
	}
	/**
	 * Updates the agency value for the given column with the new name
	 */
	private void updateRankValue(RankColumn col, Rank element, String newName){
		if (col == RankColumn.NAME){
			if (!findRankValue(col, element).equals(newName)){
						
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Agency.MAX_AGENCY_LENGTH)){
					Integer matches = 0;
					for (@SuppressWarnings("unchecked")	Iterator<Rank> itr = currentRankSet.iterator(); itr.hasNext();) {
						Rank a = itr.next();
						if( a != element && a.findName(cmbLanguage.getCurrentSelection()).compareTo(newName.trim())==0){
							matches++;
						}
					} 

					if(matches > 0){
						//invalid agency name, don't update it.
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Invalid Name", "Rank cannot be a duplicate.");
						setChangesMade(false);
					}else{
						element.updateName(cmbLanguage.getCurrentSelection(), newName.trim());
						setChangesMade(true);
					}
				}else{
					//invalid agency name, don't update it.
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Invalid Name", "Rank Name must not be blank, nor contain characters other than " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
					setChangesMade(false);
				}


			}
		}
	}
	
	/**
	 * enables rank table and buttons
	 */
	private void enableRank(boolean enable){
		tblRank.getTable().setEnabled(enable);
		btnAddRank.setEnabled(enable);
		btnDeleteRank.setEnabled(enable);
	}
	
	/**
	 * Creates the agency table.
	 * 
	 */
	private TableViewer createAgencyTableViewer(Composite parent){
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tableViewer.setContentProvider(new ObservableListContentProvider());
		
		getSession().beginTransaction();
		agencies = new WritableList(HibernateManager.getAgencies(ca, getSession()), Agency.class);
		getSession().getTransaction().rollback();
		
		tableViewer.setInput(agencies);
		
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		
		for (int i = 0; i < AgencyColumn.values().length; i ++){
			final AgencyColumn colum = AgencyColumn.values()[i];
			final TableViewerColumn col = createTableViewerColumn(tableViewer, colum.name, colum.bounds, i);
			col.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return findAgencyValue(colum, (Agency)element);
				}
			});
			col.setEditingSupport(new AgencyTextTableEditor(tableViewer,colum));
			col.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					agencySorter.setSortColumn(colum, col.getColumn());
				}
				
			});
			
		}
		agencySorter = new AgencySorter();
		tableViewer.setComparator(agencySorter);
		return tableViewer;
	}
	

	/**
	 * creates the rank table
	 */
	private TableViewer createRankTableViewer(Composite parent){
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tableViewer.setContentProvider(new ObservableListContentProvider());

		
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		for (int i = 0; i < RankColumn.values().length; i ++){
			final RankColumn colum = RankColumn.values()[i];
			final TableViewerColumn col = createTableViewerColumn(tableViewer, colum.name, colum.bounds, i);
			col.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return findRankValue(colum, (Rank)element);
				}
			});
			col.setEditingSupport(new RankTextTableEditor(tableViewer,colum));
			col.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					rankSorter.setSortColumn(colum, col.getColumn());
				}
				
			});
		}
		rankSorter = new RankSorter();
		tableViewer.setComparator(rankSorter);
		return tableViewer;
	}
	
	/*
	 * creates a table column
	 */
	private TableViewerColumn createTableViewerColumn(TableViewer viewer,
			String title, int weight, int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);
		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight,ColumnWeightData.MINIMUM_WIDTH, true));
		return viewerColumn;
	}
	
	/**
	 * Saves the current changes to the database.
	 * @return true if successful; false if fails
	 */
	@Override
	protected boolean performSave(){
		Session s = getSession();
		Transaction tx = s.beginTransaction();
		try{
			for (Iterator<Agency> iterator = toDelete.iterator(); iterator.hasNext();) {
				Agency agt = (Agency) iterator.next();
				if(agt.getUuid() != null){
					s.delete(agt);
				}
			}				
			for(Iterator<Agency> iterator = agencies.iterator(); iterator.hasNext();){
				Agency agt = (Agency) iterator.next();
				s.saveOrUpdate(agt);
			}
			
			tx.commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(),"Error saving agency/rank updates.  Please close and re-open dialog." + ex.getLocalizedMessage(), ex);
			tx.rollback();
			s.close();			
		}
		return false;
	}
	
	/**
	 * Disposes of rank and agencies sets
	 */
	@Override
	public boolean close(){
		boolean ret = super.close();
		if (ret){
			if (currentRankSet != null){
				currentRankSet.dispose();
			}
			if (agencies != null ){
				agencies.dispose();
			}
		}
		return ret;
	}
	
	
	private void addAgency() {
		agencySorter.setSortColumn(null, null);	//we want to make sure this is added at the end 
		
		final Agency agency = new Agency();
		org.wcs.smart.ca.Label lbl = new org.wcs.smart.ca.Label();
		lbl.setLanguage(cmbLanguage.getCurrentSelection());
		lbl.setValue("New Agency");
		lbl.setElement(agency);
		agency.getNames().add(lbl);
		agency.setConservationArea(ca);
		agencies.add(agency);
		
		tblAgencies.setSelection(new StructuredSelection(agency));
		
		tblAgencies.refresh();
		setChangesMade(true);
	}
 
	private void deleteAgency() {
		for (Iterator iterator = ((IStructuredSelection)tblAgencies.getSelection()).iterator(); iterator.hasNext();) {
			Agency type = (Agency) iterator.next();
			agencies.remove(type);
			if (type.getUuid() != null){
				toDelete.add(type);	
			}
		}
		tblAgencies.refresh();
		setChangesMade(true);
	}

	private void addRank() {
		if (current != null){
			rankSorter.setSortColumn(null, null);	//we want to make sure this is added at the end
			
			final Rank rank = new Rank();
			org.wcs.smart.ca.Label lbl = new org.wcs.smart.ca.Label();
			lbl.setLanguage(cmbLanguage.getCurrentSelection());
			lbl.setValue("New Rank");
			lbl.setElement(rank);
			rank.getNames().add(lbl);
			rank.setAgency(current);
			
			current.getRanks().add(rank);
		//	currentRankSet.add(rank);
			
			tblRank.setSelection(new StructuredSelection(rank));
		}
		
		tblRank.refresh();
		setChangesMade(true);
		
	}
	private void deleteRank() {
		Rank r =(Rank) ((IStructuredSelection)tblRank.getSelection()).getFirstElement();
		if (current != null){
			currentRankSet.remove(r);
			current.getRanks().remove(r);
		}
		tblRank.refresh();
		setChangesMade(true);
	}

	/**
	 * Agency table editor 
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	private class AgencyTextTableEditor extends EditingSupport{
		private AgencyColumn column ;
		private TableViewer viewer;
		
		AgencyTextTableEditor(TableViewer  viewer, AgencyColumn column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
		}
		
		@Override
		protected void setValue(Object element, Object value) {
			updateAgencyValue(column, (Agency)element, (String)value);
			viewer.refresh();
		}
		
		@Override
		protected Object getValue(Object element) {
			return findAgencyValue(column, (Agency)element);
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
	/**
	 * Rank table editor 
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	private class RankTextTableEditor extends EditingSupport{
		private RankColumn column ;
		private TableViewer viewer;
		
		RankTextTableEditor(TableViewer  viewer, RankColumn column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
		}
		
		@Override
		protected void setValue(Object element, Object value) {
			updateRankValue(column, (Rank)element, (String)value);
			viewer.refresh();
		}
		
		@Override
		protected Object getValue(Object element) {
			return findRankValue(column, (Rank)element);
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
	
	
	class AgencySorter extends ViewerComparator{
		private AgencyColumn column = null;
		private int direction = SWT.DOWN;
		
		public void setSortColumn(AgencyColumn sort, TableColumn tcolumn){
			
			if (column != null &&column == sort){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = sort;
			AgencyRankPropertyPage.this.tblAgencies.getTable().setSortDirection(direction);
			AgencyRankPropertyPage.this.tblAgencies.getTable().setSortColumn(tcolumn);
			AgencyRankPropertyPage.this.tblAgencies.refresh();
		}
			
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null){
				//no sort column picked
				return 0;
			}
			if (direction == SWT.UP){
				return -compareValue((Agency)object1, (Agency)object2);
			}else{
				return compareValue((Agency)object1, (Agency)object2);
			}

		}
		private int compareValue(Agency s1, Agency s2){
			if (s1 == null && s2 == null){
				return 0;
			}else if (s1== null && s2 != null){
				return 1;
			}else if (s1 != null && s2 == null){
				return -1;
			}
			return nullStringComparator.compare(findAgencyValue(column, s1), findAgencyValue(column, s2));
		}
	};
	
	class RankSorter extends ViewerComparator{
		private RankColumn column = null;
		private int direction = SWT.DOWN;
		
		public void setSortColumn(RankColumn sort, TableColumn tcolumn){
		
			if (column != null && column == sort){
				if (direction == SWT.DOWN){
					direction = SWT.UP;
				}else{
					direction = SWT.DOWN;
				}
			}
			this.column = sort;
			AgencyRankPropertyPage.this.tblRank.getTable().setSortDirection(direction);
			AgencyRankPropertyPage.this.tblRank.getTable().setSortColumn(tcolumn);
			AgencyRankPropertyPage.this.tblRank.refresh();
		}
			
		
		@Override
		public int compare(Viewer viewer, Object object1, Object object2){
			if (column == null){
				//no sort column picked
				return 0;
			}
			if (direction == SWT.UP){
				return -compareValue((Rank)object1, (Rank)object2);
			}else{
				return compareValue((Rank)object1, (Rank)object2);
			}

		}
		private int compareValue(Rank s1, Rank s2){
			if (s1 == null && s2 == null){
				return 0;
			}else if (s1== null && s2 != null){
				return 1;
			}else if (s1 != null && s2 == null){
				return -1;
			}
			return nullStringComparator.compare(findRankValue(column, s1), findRankValue(column, s2));
		}
	};
}
