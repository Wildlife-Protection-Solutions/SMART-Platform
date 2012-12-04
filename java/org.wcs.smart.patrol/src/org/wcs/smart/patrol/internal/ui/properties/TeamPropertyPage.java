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

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.BinaryType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Property page for managing patrol teams.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TeamPropertyPage extends AbstractPropertyJHeaderDialog {

	private LanguageViewer languageViewer;
	private TableViewer tableViewer;
	private TeamSorter sorter ; 
	private Button btnDisable;
	private Button btnDelete;
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	private WritableList teams = null;
	private HashSet<Team> toDelete = new HashSet<Team>();
	private PatrolMandate[] mandates = null;
	
	private UUIDGenerator uuidGenerator; //for generating uuids for description field
	
	/*
	 * columns in the station table
	 */
	private enum Column {
		NAME(Team.NAME, 1),
		MANDATE(Team.MANDATE,1),
		DESCRIPTION(Team.DESCRIPTION,2);
		
		String name;
		int weight;
		
		Column(String name, int weight) {
			this.name = name;
			this.weight = weight;
		}
	};
	
	/**
	 * @param parent
	 * @param title
	 */
	public TeamPropertyPage() {
		super(Display.getCurrent().getActiveShell(), Messages.TeamPropertyPage_Dialog_Title);
		
		/* get mandates */
		getSession().beginTransaction();
		List<PatrolMandate> ms =  null;
		try{
			ms = PatrolHibernateManager.getActiveMandates(ca, getSession());
			getSession().getTransaction().rollback();
		}catch (Exception ex){
			getSession().getTransaction().rollback();
			getSession().close();
			SmartPatrolPlugIn.displayLog(Messages.TeamPropertyPage_Error_LoadingMandates, ex);
			return;
		}
		ms.add(0, null);
		mandates = ms.toArray(new PatrolMandate[ms.size()]);

		uuidGenerator = UUIDGenerator
				.buildSessionFactoryUniqueIdentifierGenerator();
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY,
				StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS,
				UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new BinaryType(), prop, null);
	}

	@Override
	public boolean  close(){
		boolean canClose = super.close();
		return canClose;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		getSession().beginTransaction();
		try{
			teams = new WritableList(PatrolHibernateManager.getTeams(ca, getSession()), Team.class);
			getSession().getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.TeamPropertyPage_Error_LoadingTeams, ex);
			getSession().close();
		}
		
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText(Messages.TeamPropertyPage_Language_Label);

		languageViewer = new LanguageViewer(container, SWT.NONE, ca);
		languageViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tableViewer.refresh();
			}
		});
	
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);

		tableViewer = new TableViewer(composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);

		createColumns(tableViewer);

		tableViewer.setContentProvider(new ObservableListContentProvider());
		tableViewer.setInput(teams);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		sorter = new TeamSorter();
		tableViewer.setComparator(sorter);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Team team = (Team)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (team == null){
					btnDisable.setEnabled(false);
					btnDelete.setEnabled(false);
					return;
				}
				if (team.getIsActive()){
					btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDisable.setEnabled(true);
				btnDelete.setEnabled(true);
			}
		});
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
				addTeam();
			}

		});

		btnDisable = new Button(composite, SWT.NONE);
		btnDisable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisable.setEnabled(false);
		btnDisable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				disableTeam(btnDisable.getText().equals(DialogConstants.ENABLE_BUTTON_TEXT));
			}
		});
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteTeam();
			}
		});

		setMessage(Messages.TeamPropertyPage_Dialog_Message);
		return container;
	}
	
	private void deleteTeam(){
		Team team = (Team ) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (team == null){
			return;
		}

		try{
			if (team.getUuid() != null){
				if (DeleteManager.canDelete(team, getSession())){
					teams.remove(team);
					toDelete.add(team);
					setChangesMade(true);
				}
			}else{
				teams.remove(team);
			}
				
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(),
					MessageFormat.format(Messages.TeamPropertyPage_Error_DeletingTeam, new Object[]{team.getName()}), ex);
		}
		tableViewer.refresh();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		Session s = getSession();
		try {
			s.beginTransaction();
			
			for (Team t : toDelete){
				s.delete(t);
			}
			
			for (Iterator<?> iterator = teams.iterator(); iterator.hasNext();) {
				Team team = (Team) iterator.next();
				s.saveOrUpdate(team);
				
				for (org.wcs.smart.ca.DescriptionLabel lbl : team.getDescriptions()) {
					if (lbl.getElementuuid() == null) {
						if (team.getDescUuid() == null) {
							byte[] uuid = (byte[]) uuidGenerator.generate((SessionImplementor) s, lbl);
							team.setDescUuid(uuid);
							s.saveOrUpdate(team);
						}
						lbl.setElement(team.getDescUuid());
					}
					s.saveOrUpdate(lbl);
				}
				
			}
			s.getTransaction().commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(
					Messages.TeamPropertyPage_Error_SavingUpdates + ex.getLocalizedMessage(),
					ex);
		}
		return false;
	}
	
	private void addTeam(){
		Team team = new Team();
		team.setConservationArea(ca);
		team.setIsActive(true);
		team.setMandate(null);
		
		team.updateName(ca.getDefaultLanguage(), Messages.TeamPropertyPage_DefaultNewTeamName);
		team.setName(team.findName(ca.getDefaultLanguage()));		
		team.updateDescription(ca.getDefaultLanguage(), Messages.TeamPropertyPage_DefaultNewTeamDescription);
		team.setDescription(team.findDescriptionNull(ca.getDefaultLanguage()));
		
		teams.add(team);
		setChangesMade(true);
		tableViewer.refresh();
		
	}
	private void disableTeam(boolean enable){
		Team team = (Team)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		team.setIsActive(enable);
		setChangesMade(true);
		tableViewer.refresh();
	}
	
	/**
	 * Finds the property for the given column from the station.
	 * 
	 * @param type the property required
	 * @param stn station 
	 * @return string value of the requested property
	 */
	private String findValue(Column type, Team mnd) {
		if (type == Column.NAME) {
			String x =  mnd.findNameNull(languageViewer.getCurrentSelection());
			if (x == null){
				x = mnd.getName();
			}
			return x;
		}else if (type == Column.DESCRIPTION){
			String x = mnd.findDescriptionNull(languageViewer.getCurrentSelection());
			if (x == null){
				x = mnd.getDescription();
			}
			return x;
		}else if (type == Column.MANDATE){
			if (mnd.getMandate() == null){
				return ""; //$NON-NLS-1$
			}
			return mnd.getMandate().findName(languageViewer.getCurrentSelection());
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Updates the given station object with the new value.
	 * @param type
	 * @param stn
	 * @param newValue
	 */
	private void updateValue(Column type, Team mnd, Object newValue) {
		if (type == Column.NAME) {
			if (!findValue(type, mnd).equals((String)newValue)){
				mnd.updateName(languageViewer.getCurrentSelection(), (String)newValue);
				setChangesMade(true);
			}
		}else if (type == Column.DESCRIPTION){
			if (!findValue(type, mnd).equals((String)newValue)){
				mnd.updateDescription(languageViewer.getCurrentSelection(), (String)newValue);
				setChangesMade(true);
			}
		}else if (type == Column.MANDATE){
			mnd.setMandate((PatrolMandate)newValue);
			setChangesMade(true);
		}
	}
	
	/*
	 * Creates station table columns
	 */
	private void createColumns(TableViewer viewer) {

		for (int i = 0; i < Column.values().length; i++) {
			final Column colum = Column.values()[i];
			final TableViewerColumn col = createTableViewerColumn(viewer, colum.name,
					colum.weight, i);
			
			
			col.setLabelProvider(new TeamLabelProvider(colum));
			if (colum == Column.NAME || colum == Column.DESCRIPTION){
				col.setEditingSupport(new TextTableEditor(viewer, colum));
			}else if (colum == Column.MANDATE){
				col.setEditingSupport(new ComboTableEditor(viewer, colum));
			}	
			
			col.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					sorter.setSortColumn(colum, col.getColumn());
				}});
		}

		
	}
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
	
	private class ComboTableEditor extends EditingSupport {
		private Column column;
		private TableViewer viewer;
		

		ComboTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (column == Column.MANDATE){
				updateValue(column, (Team) element, (PatrolMandate) value);
			}else{
				updateValue(column, (Team) element, (String) value);
			}
			viewer.refresh();
		}

		@Override
		protected Object getValue(Object element) {
			if (column == Column.MANDATE){
				return ((Team)element).getMandate();
			}
			return findValue(column, (Team) element);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return  new MandateComboBoxCellEditor(viewer.getTable().getParent(),mandates, SWT.DROP_DOWN, languageViewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			if (element instanceof Team){
				return ((Team)element).getIsActive();
			}
			return false;
		}
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
			updateValue(column, (Team) element, (String) value);
			viewer.refresh();
		}

		@Override
		protected Object getValue(Object element) {
			return findValue(column, (Team) element);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(viewer.getTable());
		}

		@Override
		protected boolean canEdit(Object element) {
			if (element instanceof Team){
				return ((Team)element).getIsActive();
			}
			return false;
		}
	}
	
	class TeamLabelProvider extends ColumnLabelProvider implements IColorProvider{ 
		private Column column;
		public TeamLabelProvider(Column column){
			this.column = column;
		}
		@Override
		public String getText(Object element) {
			return findValue(column, (Team) element);
		}
		 
		public Color getForeground(Object element){
			 if (((Team)element).getIsActive()){
				 return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			 }else{
				 return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			 }
		 }
		
		public Color getBackground(Object element){
			 return null;
		 }
	}
	class TeamSorter extends ViewerComparator{
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
			if (column == null){
				//no sort column picked
				return 0;
			}
			if (direction == SWT.UP){
				return -compareValue((Team)object1, (Team)object2);
			}else{
				return compareValue((Team)object1, (Team)object2);
			}

		}
		
		
		private int compareValue(Team s1, Team s2){
			if (s1 == null && s2 == null){
				return 0;
			}else if (s1== null && s2 != null){
				return 1;
			}else if (s1 != null && s2 == null){
				return -1;
			}			
			return nullStringComparator.compare(findValue(column, s1),	findValue(column, s2));
		}
	};
}