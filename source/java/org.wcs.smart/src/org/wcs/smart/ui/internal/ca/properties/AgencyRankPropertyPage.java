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

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.export.AgencyCsvImporter;
import org.wcs.smart.export.config.impl.AgencyCsvExportConfig;
import org.wcs.smart.export.config.impl.AgencyCsvImportConfig;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Rank and Agency list property page
 * 
 * @author Emily Gouge
 *
 */
public class AgencyRankPropertyPage extends AbstractPropertyJHeaderDialog{

	private static final String INVALID_NAME_DIALOG_TITLE = Messages.AgencyRankPropertyPage_InvalidName_DialogTitle;
	
	/* ui components */
	private LanguageViewer cmbLanguage;
	private TableViewer tblAgencies;
	private TableViewer tblRank;
	private Button btnDeleteRank;
	private Button btnAddRank;

	/* agencies and rank lists */
	private Agency current = null;
	private List<Rank> currentRankSet;
	private List<Agency> agencies ;
	private HashSet<Agency> toDelete;
	
	private AgencySorter agencySorter;
	private RankSorter rankSorter;
	
	private ConservationArea currentCa;
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	/*
	 * columns of agency table
	 */
	private enum AgencyColumn{
		NAME( LabelConstants.AGENCY_NAME, 1);
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
		NAME( LabelConstants.RANK_NAME, 1);
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
	public AgencyRankPropertyPage(Shell parent) {
		super(parent, Messages.AgencyRankPropertyPage_DialogTitle);
		toDelete = new HashSet<Agency>();
		this.currentCa = SmartDB.getCurrentConservationArea();
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(3, false));
		
		/* Language */
		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(Messages.AgencyRankPropertyPage_Language_Label);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		cmbLanguage = new LanguageViewer(container, SWT.NONE,currentCa);
		Combo lblLanguage = cmbLanguage.getCombo();
		lblLanguage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 3, 1));
		cmbLanguage.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tblAgencies.refresh();
				if (currentRankSet != null){
					tblRank.refresh();
				}
			}
		});
		
		/* Agency */
		Label lblAgencies = new Label(container, SWT.NONE);
		lblAgencies.setText(Messages.AgencyRankPropertyPage_Agencies_Label);
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
		btnAddAgency.setText(Messages.AgencyRankPropertyPage_Add_Button);
		btnAddAgency.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAgency();
			}
		});
		final Button btnDeleteAgency = new Button(buttons, SWT.NONE);
		btnDeleteAgency.setText(Messages.AgencyRankPropertyPage_Delete_Button);
		btnDeleteAgency.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteAgency();
			}
		});
		btnDeleteAgency.setEnabled(false);
		
		tblAgencies.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = ((IStructuredSelection)tblAgencies.getSelection());
				if (selection.isEmpty()){
					current = null;
					currentRankSet = null;
					btnDeleteAgency.setEnabled(false);
					enableRank(false);
				}else{
					Agency agent = (Agency)selection.getFirstElement();
					currentRankSet = agent.getRanks();
					tblRank.setInput(currentRankSet);
					current = agent;
					enableRank(true);
					btnDeleteAgency.setEnabled(true);
				}
			}
		});
		
		
		/* Rank */
		Label lblRanks = new Label(container, SWT.NONE);
		lblRanks.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		lblRanks.setText(Messages.AgencyRankPropertyPage_Ranks_Label);
		
		owner = new Composite(container, SWT.NONE);
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2,1));
		owner.setLayout(new TableColumnLayout());
		tblRank = createRankTableViewer(owner);
		tblRank.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDeleteRank.setEnabled(!tblRank.getSelection().isEmpty());
			}
		});
		
		Composite buttons_1 = new Composite(container, SWT.NONE);
		buttons_1.setLayout(new GridLayout(1,true));
		buttons_1.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1,1));
		
		btnAddRank = new Button(buttons_1, SWT.NONE);
		btnAddRank.setText(Messages.AgencyRankPropertyPage_AddRank_Button);
		btnAddRank.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addRank();
				
			}
		});
		btnDeleteRank = new Button(buttons_1, SWT.NONE);
		btnDeleteRank.setText(Messages.AgencyRankPropertyPage_DeleteRank_Button);
		btnDeleteRank.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteRank();
			}
		});
		enableRank(false);
		
		tblAgencies.refresh();
		Button btnImport = new Button(container, SWT.NONE);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				AgencyCsvImportConfig config = new AgencyCsvImportConfig();
				CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
				int ret = dialog.open();
				if (ret == IDialogConstants.CANCEL_ID) {
					return;
				} else {
					Collection<Agency> importedData = ((AgencyCsvImporter)config.getImporter()).getImportedData();
					if (importedData != null && importedData.size() > 0){
						agencies.addAll(importedData);
						tblAgencies.refresh();
						setChangesMade(true);
//						tblRank.refresh();
					}
				}
			}
		});
		Button btnExport = new Button(container, SWT.NONE);
		btnExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		btnExport.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				CsvExportDialog dialog = new CsvExportDialog(getShell(), new AgencyCsvExportConfig());
				dialog.open();
			}
		});
		setMessage(Messages.AgencyRankPropertyPage_DialogMessage);
		setTitle(Messages.AgencyRankPropertyPage_PageTitle);
		return container;
	}

	private void resetAgencyList() {
		Session s = getSession();
		s.beginTransaction();
		List<Agency> lst = Collections.<Agency>emptyList();
		try{
			lst = HibernateManager.getAgencies(currentCa,s );
		}finally{
			s.getTransaction().rollback();
		}
		Collections.sort(lst, new Comparator<Agency>(){

			@Override
			public int compare(Agency o1, Agency o2) {
				String a = o1.getName();
				if (a != null) a = a.toLowerCase();
				String b = o2.getName();
				if (b != null) b = b.toLowerCase();
				return Collator.getInstance().compare(a, b);
			}});
		
		agencies = lst;
		
		
	}
	
	/**
	 * Finds the agency value for the given column 
	 * for the current selected language
	 */
	private String findAgencyValue(AgencyColumn col, Agency element){
		if (col == AgencyColumn.NAME){
			String x = element.findNameNull(cmbLanguage.getCurrentSelection());
			if (x == null){
				x = element.getName();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
			}
			return x;
		}
		return null;
	}
	/**
	 * Updates the agency value for the given column with the new name
	 */
	private void updateAgencyValue(AgencyColumn col, Agency element, String newName){
		if (col == AgencyColumn.NAME){
			String name = findAgencyValue(col, element);
			if (name == null || !name.equals(newName)){
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Agency.MAX_AGENCY_LENGTH)){
					Integer matches = 0;
					for (Iterator<Agency> itr = agencies.iterator(); itr.hasNext();) {
						Agency a = itr.next();
						if( a != element && 
								a.findName(cmbLanguage.getCurrentSelection()).equals(newName.trim())){
							matches++;
						}
					} 
					if(matches > 0){
						//invalid agency name, don't update it.
						MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_NAME_DIALOG_TITLE, Messages.AgencyRankPropertyPage_Error_DuplicateAgency);
						setChangesMade(false);
					}else{
						element.updateName(cmbLanguage.getCurrentSelection(), newName.trim());
						setChangesMade(true);
					}
				}else{
					//invalid agency name, don't update it.
					MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_NAME_DIALOG_TITLE, Messages.AgencyRankPropertyPage_Error_InvalidName + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
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
			String x = element.findNameNull(cmbLanguage.getCurrentSelection());
			if (x == null){
				x = element.getName();
				if (x == null){
					return ""; //$NON-NLS-1$
				}
			}
			return x;
		}
		return null;
	}
	/**
	 * Updates the agency value for the given column with the new name
	 */
	private void updateRankValue(RankColumn col, Rank element, String newName){
		if (col == RankColumn.NAME){
			String rank = findRankValue(col, element);
			if (rank == null || !rank.equals(newName)){
						
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Agency.MAX_AGENCY_LENGTH)){
					Integer matches = 0;
					for (Iterator<Rank> itr = currentRankSet.iterator(); itr.hasNext();) {
						Rank a = itr.next();
						if( a != element && a.findName(cmbLanguage.getCurrentSelection()).equals(newName.trim())){
							matches++;
						}
					} 

					if(matches > 0){
						//invalid agency name, don't update it.
						MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_NAME_DIALOG_TITLE, Messages.AgencyRankPropertyPage_Error_DuplicateRank);
						setChangesMade(false);
					}else{
						element.updateName(cmbLanguage.getCurrentSelection(), newName.trim());
						setChangesMade(true);
					}
				}else{
					//invalid agency name, don't update it.
					MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_NAME_DIALOG_TITLE, Messages.AgencyRankPropertyPage_Error_InvalidRankName + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
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
		btnDeleteRank.setEnabled(!tblRank.getSelection().isEmpty());
	}
	
	/**
	 * Creates the agency table.
	 * 
	 */
	private TableViewer createAgencyTableViewer(Composite parent){
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		resetAgencyList();
		
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

		return tableViewer;
	}
	

	/**
	 * creates the rank table
	 */
	private TableViewer createRankTableViewer(Composite parent){
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
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
			for(Iterator<?> iterator = agencies.iterator(); iterator.hasNext();){
				Agency agt = (Agency) iterator.next();
				s.saveOrUpdate(agt);
			}
			
			tx.commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.AgencyRankPropertyPage_Error_Save + ex.getLocalizedMessage(), ex);
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
			currentRankSet = null;
			agencies = null;
		}
		return ret;
	}
	
	
	private void addAgency() {
		agencySorter.setSortColumn(null, null);	//we want to make sure this is added at the end 
		
		final Agency agency = new Agency();
		org.wcs.smart.ca.Label lbl = new org.wcs.smart.ca.Label();
		lbl.setLanguage(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		lbl.setValue(Messages.AgencyRankPropertyPage_DefaultAgencyName);
		lbl.setElement(agency);
		agency.getNames().add(lbl);
		agency.setName(lbl.getValue());
		agency.setConservationArea(currentCa);
		agencies.add(agency);
		
		tblAgencies.setSelection(new StructuredSelection(agency));
		
		tblAgencies.refresh();
		setChangesMade(true);
	}
 
	private void deleteAgency() {
		for (Iterator<?> iterator = ((IStructuredSelection)tblAgencies.getSelection()).iterator(); iterator.hasNext();) {
			Agency type = (Agency) iterator.next();
			try{
				if (type.getUuid() != null){
					if (DeleteManager.canDelete(type, getSession())){
						agencies.remove(type);
						toDelete.add(type);
						setChangesMade(true);
					}
				}else{
					agencies.remove(type);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.AgencyRankPropertyPage_Error_DeleteAgency + type.getName() + ".\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			lbl.setLanguage(SmartDB.getCurrentConservationArea().getDefaultLanguage());
			lbl.setValue(Messages.AgencyRankPropertyPage_DefaultRankName);
			lbl.setElement(rank);
			rank.getNames().add(lbl);
			rank.setAgency(current);
			rank.setName(lbl.getValue());
			current.getRanks().add(rank);
		//	currentRankSet.add(rank);
			
			tblRank.setSelection(new StructuredSelection(rank));
		}
		
		tblRank.refresh();
		setChangesMade(true);
		
	}
	private void deleteRank() {
		if (tblRank.getSelection().isEmpty()){
			//nothing to delete
			return;
		}
		Rank r =(Rank) ((IStructuredSelection)tblRank.getSelection()).getFirstElement();
		try{
		
			if (current != null){
				if (r.getUuid() != null){
					if (DeleteManager.canDelete(r, getSession())){
						currentRankSet.remove(r);
						current.getRanks().remove(r);
						setChangesMade(true);
					}
				}else{
					currentRankSet.remove(r);
					current.getRanks().remove(r);
				}
			}
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.AgencyRankPropertyPage_Error_DeleteRank + r.getName() + ".\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			String a = findAgencyValue(column, s1);
			if (a != null) a = a.toLowerCase();
			
			String b = findAgencyValue(column, s2);
			if (b != null) b = b.toLowerCase();
			return nullStringComparator.compare(a,b);
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
			String a = findRankValue(column, s1);
			if (a != null) a = a.toLowerCase();
			
			String b = findRankValue(column, s2);
			if (b != null) b = b.toLowerCase();
			return nullStringComparator.compare(a,b);
		}
	};
}
