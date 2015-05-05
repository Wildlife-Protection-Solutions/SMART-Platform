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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.comparators.NullComparator;
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
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.BinaryType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.Team;
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
public class TeamPropertyPage extends AbstractPropertyJHeaderDialog {

	private LanguageViewer languageViewer;
	private TableViewer tableViewer;
	private TeamSorter sorter ; 
	private Button btnDisable;
	private Button btnDelete;
	private Button btnEditKey;
	private Composite container;
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	private List<Team> teams = null;
	private HashSet<Team> toDelete = new HashSet<Team>();
	private PatrolMandate[] mandates = null;
	private ConservationArea currentCa = null;
	
	private UUIDGenerator uuidGenerator; //for generating uuids for description field
	
	/*
	 * columns in the station table
	 */
	private enum Column {
		NAME(Team.NAME, 1),
		MANDATE(Team.MANDATE,1),
		DESCRIPTION(Team.DESCRIPTION,2),
		KEY(Team.KEY, 1);
		
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
	public TeamPropertyPage(Shell parent) {
		super(parent, Messages.TeamPropertyPage_Dialog_Title);
		this.currentCa = SmartDB.getCurrentConservationArea();
		
		/* get mandates */
		getSession().beginTransaction();
		List<PatrolMandate> ms =  null;
		try{
			ms = PatrolHibernateManager.getActiveMandates(currentCa, getSession());
			getSession().getTransaction().rollback();
		}catch (Exception ex){
			getSession().getTransaction().rollback();
			getSession().close();
			SmartPatrolPlugIn.displayLog(Messages.TeamPropertyPage_Error_LoadingMandates, ex);
			return;
		}
		Collections.sort(ms, new Comparator<PatrolMandate>(){
			@Override
			public int compare(PatrolMandate o1, PatrolMandate o2) {
				String a = o1.getName();
				String b = o2.getName();
				if (a != null) a = a.toLowerCase();
				if (b != null) b= b.toLowerCase();
				return Collator.getInstance().compare(a,b);
			}});
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
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		getSession().beginTransaction();
		try{
			teams = new ArrayList<Team>(PatrolHibernateManager.getTeams(currentCa, getSession()));
			getSession().getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.TeamPropertyPage_Error_LoadingTeams, ex);
			getSession().close();
		}
		Collections.sort(teams, new Comparator<Team>(){

			@Override
			public int compare(Team o1, Team o2) {
				String a = o1.getName();
				String b = o2.getName();
				if (a != null) a = a.toLowerCase();
				if (b != null) b = b.toLowerCase();
				return Collator.getInstance().compare(a,b);
			}});
		
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText(Messages.TeamPropertyPage_Language_Label);

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

		createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
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
					btnEditKey.setEnabled(false);
					return;
				}
				if (team.getIsActive()){
					btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDisable.setEnabled(true);
				btnDelete.setEnabled(true);
				btnEditKey.setEnabled(true);
			}
		});

		tableViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = tableViewer.getCell(new Point(event.x, event.y));
				if (cell != null && cell.getColumnIndex() == Column.KEY.ordinal()){
					editKey();
				}
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

		btnEditKey = new Button(composite, SWT.NONE);
		btnEditKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1,1));
		btnEditKey.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEditKey.setEnabled(false);
		btnEditKey.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				editKey();
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

		setTitle(Messages.TeamPropertyPage_PageName);
		setMessage(Messages.TeamPropertyPage_Dialog_Message);
		return container;
	}
	
	
	private void editKey(){
		Team x = (Team)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		String currentKey = findValue(Column.KEY,x);
		InputDialog id = new KeyInputDialog(getShell(), currentKey, teams);
		int ret = id.open();
		if (ret != Window.CANCEL) {
			updateValue(Column.KEY,x,id.getValue());
			tableViewer.refresh(x);
		}
	}
	
	private void deleteTeam(){
		Team team = (Team ) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (team == null){
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.TeamPropertyPage_ConfirmDeleteTitle, MessageFormat.format(Messages.TeamPropertyPage_ConfirmDeleteMessage, new Object[]{team.getName()}))){
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
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.TeamPropertyPage_Error_DeletingTeam + "\n\n" + ex.getLocalizedMessage(), new Object[]{team.getName()}), ex); //$NON-NLS-1$
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
			List<Team> siblings = new ArrayList<Team>(teams.size());
			siblings.addAll(teams);
			
			for (Iterator<?> iterator = teams.iterator(); iterator.hasNext();) {
				Team team = (Team) iterator.next();
				siblings.remove(team);
				String error = NamedKeyItem.validateKey(team.getKeyId(), siblings);
				siblings.add(team);
				if (error != null){
					throw new Exception(error);
				}
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
			s.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog(
					Messages.TeamPropertyPage_Error_SavingUpdates  + "\n" + ex.getLocalizedMessage(), //$NON-NLS-1$
					ex);
		}
		return false;
	}
	
	private void addTeam(){
		Team team = new Team();
		team.setConservationArea(currentCa);
		team.setIsActive(true);
		team.setMandate(null);
		
		team.updateName(currentCa.getDefaultLanguage(), Messages.TeamPropertyPage_DefaultNewTeamName);
		team.setName(team.findName(currentCa.getDefaultLanguage()));		
		team.updateDescription(currentCa.getDefaultLanguage(), Messages.TeamPropertyPage_DefaultNewTeamDescription);
		team.setDescription(team.findDescriptionNull(currentCa.getDefaultLanguage()));
		
		teams.add(team);
		setChangesMade(true);
		tableViewer.refresh();
		
		tableViewer.editElement(team, Column.NAME.ordinal());
		
	}
	private void disableTeam(boolean enable){
		Team team = (Team)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		team.setIsActive(enable);
		setChangesMade(true);
		tableViewer.refresh();
		
		if (!enable){
			btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}else{
			btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}
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
				if (x == null){
					return ""; //$NON-NLS-1$
				}
				return x;
			}
			return x;
		}else if (type == Column.MANDATE){
			if (mnd.getMandate() == null){
				return ""; //$NON-NLS-1$
			}
			return mnd.getMandate().findName(languageViewer.getCurrentSelection());
		}else if (type == Column.KEY){
			if (mnd.getKeyId() == null){
				return ""; //$NON-NLS-1$
			}
			return mnd.getKeyId();
		}
		return ""; //$NON-NLS-1$
	}

	private String validate(Column type, Team team, Object newValue){
		if (type == Column.NAME){
			if (!findValue(type, team).equals((String)newValue)){
				String newName = (String)newValue;
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Team.MAX_NAME_LENGTH)){
					Integer matches = 0;
					for (Iterator<Team> itr = teams.iterator(); itr.hasNext();) {
						Team a = itr.next();
						if( !a.equals(team) && a.findName(languageViewer.getCurrentSelection()).equals(newName.trim())){
							matches++;
						}
					} 
					if(matches > 0){
						//invalid name, don't update it.
						return Messages.TeamPropertyPage_DuplicateTeamNameError;					
					}
				}else{
					//invalid value, show error
					return 	MessageFormat.format(Messages.TeamPropertyPage_TeamNameError, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, Team.MAX_NAME_LENGTH});
				}
			}
		}else if (type == Column.MANDATE){
			return null;
		}else if (type == Column.DESCRIPTION){
			String newDesc = (String) newValue;
			if (newDesc != null && newDesc.length() > org.wcs.smart.ca.Label.MAX_LENGTH){
				return MessageFormat.format(Messages.TeamPropertyPage_DescriptionError, new Object[]{org.wcs.smart.ca.Label.MAX_LENGTH});
			}
		}else if (type == Column.KEY){
			String keyId = (String)newValue;
			if (!SmartUtils.isSimpleString(keyId, SmartUtils.RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX, Team.MAX_KEY_LENGTH, 1)){
				return MessageFormat.format(Messages.TeamPropertyPage_KeyErrorMessage,new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_SIMPLE_REGEX.textDesc, 1, Team.MAX_NAME_LENGTH});
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
	private void updateValue(Column type, Team mnd, Object newValue) {
		if (type == Column.NAME) {
			if (!findValue(type, mnd).equals((String)newValue)){
				mnd.updateName(languageViewer.getCurrentSelection(), (String)newValue);
				setChangesMade(true);
				
				if (mnd.getKeyId() == null){
					mnd.setKeyId(NamedKeyItem.generateKey((String)newValue, teams));
				}
			}
		}else if (type == Column.DESCRIPTION){
			if (!findValue(type, mnd).equals((String)newValue)){
				mnd.updateDescription(languageViewer.getCurrentSelection(), (String)newValue);
				setChangesMade(true);
			}
		}else if (type == Column.MANDATE){
			mnd.setMandate((PatrolMandate)newValue);
			setChangesMade(true);
		}else if (type == Column.KEY){
			if (!findValue(type,mnd).equals((String)newValue)){
				mnd.setKeyId((String)newValue);
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
					colum.weight, i);
			col.setLabelProvider(new TeamLabelProvider(colum));
			if (colum == Column.NAME || colum == Column.DESCRIPTION){
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
		private CellEditor editor;

		ComboTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
			this.editor = new MandateComboBoxCellEditor(viewer.getTable().getParent(),mandates, SWT.DROP_DOWN, languageViewer);
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
			return editor;
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
		private CellEditor editor;

		TextTableEditor(TableViewer viewer, Column column) {
			super(viewer);
			this.column = column;
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());	
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
		protected CellEditor getCellEditor(final Object element) {
			this.editor.setValidator(new ICellEditorValidator() {
				@Override
				public String isValid(Object value) {
					setErrorMessage(null);
					return validate(column, ((Team)element), (String)value);
				}
			});
			return this.editor;
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
			String x = findValue(column, (Team) element);
			if (x == null){
				return ""; //$NON-NLS-1$
			}
			return x;
		}
		 
		public Color getForeground(Object element){
			 if (((Team)element).getIsActive()){
				 return container.getDisplay().getSystemColor(SWT.COLOR_BLACK);
			 }else{
				 return container.getDisplay().getSystemColor(SWT.COLOR_GRAY);
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
			String a = findValue(column, s1);
			String b = findValue(column, s2);
			if (a != null) a = a.toLowerCase();
			if (b != null) b = b.toLowerCase();
			return nullStringComparator.compare(a,b);
		}
	};
}