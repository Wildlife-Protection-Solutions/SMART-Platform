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
package org.wcs.smart.intelligence.ui.source;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for managing intelligence source types list.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SourceTypesPropertyDialog extends AbstractPropertyJHeaderDialog {

	private LanguageViewer cmbLanguage;
	private TableViewer tableViewer;
	private SourceTypeSorter sorter; 
	private Button btnDisable;
	private Button btnDelete;
	private Button btnEditKey;
	
	
	private static NullComparator nullStringComparator = new NullComparator();
	
	private List<IntelligenceSource> sourceTypes = null;
	private HashSet<IntelligenceSource> toDelete = new HashSet<IntelligenceSource>();
	private ConservationArea currentCa = null;
	
	/*
	 * columns in the station table
	 */
	private enum Column {
		KEY(IntelligenceSource.KEY,		1),
		NAME(IntelligenceSource.NAME,	2);
		
		String name;
		int size;

		Column(String name, int size) {
			this.name = name;
			this.size = size;
		}
	};
	
	public SourceTypesPropertyDialog(Shell shell) {
		super(shell, Messages.SourceTypesPropertyDialog_Title);
		this.currentCa = SmartDB.getCurrentConservationArea();
	}

	@Override
	protected Composite createContent(Composite parent) {
		sourceTypes = new ArrayList<IntelligenceSource>(IntelligenceHibernateManager.getSourceTypes(currentCa, getSession()));
		Collections.sort(sourceTypes, new Comparator<IntelligenceSource>(){
			@Override
			public int compare(IntelligenceSource o1, IntelligenceSource o2) {
				String a = o1.getName();
				String b = o2.getName();
				if (a != null) a = a.toLowerCase();
				if (b != null) b = b.toLowerCase();
				return Collator.getInstance().compare(a,b);
			}});
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(Messages.SourceTypesPropertyDialog_Language);

		cmbLanguage = new LanguageViewer(container, SWT.NONE, currentCa);
		cmbLanguage.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbLanguage.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tableViewer.refresh();
			}
		});
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		((GridData)composite2.getLayoutData()).heightHint = 100;

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
		tableViewer.setInput(sourceTypes);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		sorter = new SourceTypeSorter();
		tableViewer.setComparator(sorter);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IntelligenceSource md = (IntelligenceSource)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
				if (md == null){
					btnDisable.setEnabled(false);
					btnDelete.setEnabled(false);
					btnEditKey.setEnabled(false);
					return;
				}
				if (md.getIsActive()){
					btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDelete.setEnabled(true);
				btnDisable.setEnabled(true);
				btnEditKey.setEnabled(true);
			}
		});
		tableViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (tableViewer.getCell(new Point(event.x, event.y)).getColumnIndex() == Column.KEY.ordinal()) {
					editKey();
				}
			}
		});
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));

		Button btnAdd = new Button(composite, SWT.NONE);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSource();
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
				disableSource(btnDisable.getText().equals(DialogConstants.ENABLE_BUTTON_TEXT));
			}
		});
		
		btnDelete = new Button(composite, SWT.NONE);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,false, 1, 1));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSource();
			}
		});
		
		Label lblinfo = new Label(container, SWT.WRAP | SWT.NONE);
		lblinfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)lblinfo.getLayoutData()).widthHint = 250;
		lblinfo.setText(MessageFormat.format(Messages.SourceTypesPropertyDialog_PatrolKeyInfo1,
				new Object[]{IntelligenceSource.PATROL_KEY, IntelligenceSource.INFORMANT_KEY}));
		
		setTitle(Messages.SourceTypesPropertyDialog_Title);
		setMessage(Messages.SourceTypesPropertyDialog_Message);
		return container;
	}

	private void createColumns(TableViewer viewer) {
		for (int i = 0; i < Column.values().length; i++) {
			final Column colum = Column.values()[i];
			final TableViewerColumn col = createTableViewerColumn(viewer, colum.name, colum.size, i);
			col.setLabelProvider(new SourceTypeLabelProvider(colum));
			final TextTableEditor editor = new TextTableEditor(viewer, colum);
			if (colum == Column.NAME){
				editor.editor.addListener(new ICellEditorListener() {
					@Override
					public void editorValueChanged(boolean oldValidState, boolean newValidState) {
						setErrorMessage(editor.editor.getErrorMessage());
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
				col.setEditingSupport(editor);
			}
			
			col.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					sorter.setSortColumn(colum, col.getColumn());
				}
			});
		}
	}

	private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int weight, int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
		layout.setColumnData(column, new ColumnWeightData(weight,ColumnWeightData.MINIMUM_WIDTH, true));
		return viewerColumn;
	}

	private void editKey(){
		IntelligenceSource x = (IntelligenceSource)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		String currentKey = findLangValue(Column.KEY,x);
		InputDialog id = new KeyInputDialog(getShell(), currentKey, sourceTypes);
		int ret = id.open();
		if (ret != Window.CANCEL) {
			updateLangValue(Column.KEY,x,id.getValue());
			tableViewer.refresh(x);
		}
	}

	private void addSource(){
		IntelligenceSource mandate = new IntelligenceSource();
		mandate.setConservationArea(currentCa);
		mandate.setIsActive(true);
		mandate.updateName(currentCa.getDefaultLanguage(), Messages.SourceTypesPropertyDialog_DefaultName);
		mandate.setName(mandate.findName(currentCa.getDefaultLanguage()));
		sourceTypes.add(mandate);
		setChangesMade(true);
		tableViewer.refresh();
		
		tableViewer.editElement(mandate, Column.NAME.ordinal());
	}
	
	private void deleteSource(){
		IntelligenceSource source = (IntelligenceSource) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		if (source == null){
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.SourceTypesPropertyDialog_Delete, MessageFormat.format(Messages.SourceTypesPropertyDialog_DeleteConfirmation, new Object[]{source.getName()}))){
			return;
		}
		try{
			if (source.getUuid() != null){
				if (DeleteManager.canDelete(source, getSession())){
					sourceTypes.remove(source);
					toDelete.add(source);
					setChangesMade(true);
				}
			}else{
				sourceTypes.remove(source);
			}
				
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog( 
					MessageFormat.format(Messages.SourceTypesPropertyDialog_CannotDelete_Error + "\n\n" + ex.getLocalizedMessage(), new Object[]{source.getName()}), ex); //$NON-NLS-1$
		}	
		
		tableViewer.refresh();
		
	}
	
	private void disableSource(boolean enable){
		IntelligenceSource md = (IntelligenceSource)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		md.setIsActive(enable);
		setChangesMade(true);
		tableViewer.refresh();
		if (enable){
			btnDisable.setText(DialogConstants.DISABLE_BUTTON_TEXT);
		}else{
			btnDisable.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		}
	}
	
	@Override
	protected boolean performSave() {
		Session s = getSession();
		try {
			s.beginTransaction();
			for (IntelligenceSource m : toDelete){
				s.delete(m);
			}
			ArrayList<IntelligenceSource> siblings = new ArrayList<IntelligenceSource>();
			for (Iterator<?> iterator = sourceTypes.iterator(); iterator.hasNext();) {
				IntelligenceSource pm = (IntelligenceSource) iterator.next();
				siblings.remove(pm);
				String error = NamedKeyItem.validateKey(pm.getKeyId(), siblings);
				siblings.add(pm);
				if (error != null){
					throw new Exception(error);
				}
				s.saveOrUpdate(pm);
			}
			s.getTransaction().commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		} catch (Exception ex) {
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			SmartPatrolPlugIn.displayLog(Messages.SourceTypesPropertyDialog_SaveError + ex.getLocalizedMessage(), ex);
		}
		return false;
	}

	private String validate(Column column, IntelligenceSource source, String newName){
		if (column == Column.NAME){
			if (!findLangValue(column, source).equals(newName)){
				if(SmartUtils.isSimpleString(newName.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, IntelligenceSource.MAX_NAME_LENGTH)){
					Integer matches = 0;
					for (Iterator<IntelligenceSource> itr = sourceTypes.iterator(); itr.hasNext();) {
						IntelligenceSource a = itr.next();
						if( a != source && a.findName(cmbLanguage.getCurrentSelection()).equals(newName.trim())){
							matches++;
						}
					} 
					if(matches > 0){
						//invalid name, don't update it.
						return Messages.SourceTypesPropertyDialog_SourceDuplicate;
					}
				}else{
					//invalid value, show error
					return 	MessageFormat.format(Messages.SourceTypesPropertyDialog_InvalidNameSize_Error, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc, IntelligenceSource.MAX_NAME_LENGTH});
				}
				
			}
		}
		return null;
	}

	/**
	 * Finds the property for the given column from the source type.
	 * 
	 * @param type the property required
	 * @param stn station 
	 * @return string value of the requested property
	 */
	private String findLangValue(Column type, IntelligenceSource source) {
		if (type == Column.NAME) {
			String name = source.findNameNull(cmbLanguage.getCurrentSelection());
			if (name == null){
				name = source.getName();
			}
			return name;
		}else if (type == Column.KEY){
			if (source.getKeyId() == null){
				return ""; //$NON-NLS-1$
			}
			return source.getKeyId();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Updates the given source type object with the new value.
	 * @param type
	 * @param stn
	 * @param newValue
	 */
	private void updateLangValue(Column type, IntelligenceSource mnd, String newValue) {
		if (type == Column.NAME) {
			if (!findLangValue(type, mnd).equals(newValue)){
				String error = validate(type, mnd, newValue);
				if (error != null){
					MessageDialog.openError(getShell(), Messages.SourceTypesPropertyDialog_InvalidName, error);
				}else{
					
					mnd.updateName(cmbLanguage.getCurrentSelection(), newValue.trim());
					if (mnd.getKeyId() == null){
						mnd.setKeyId(NamedKeyItem.generateKey(newValue, sourceTypes));
					}
					setChangesMade(true);
				}
			}
		}else if (type == Column.KEY){
			if (!findLangValue(type, mnd).equals(newValue)){
				mnd.setKeyId((String)newValue);
				setChangesMade(true);
			}
		}
	}

//---------------------------------------------------------------------
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
			String error = validate(column, ((IntelligenceSource)element), (String)value);
			if (error == null){
				updateLangValue(column, (IntelligenceSource) element, (String) value);
				viewer.refresh();
			}else{
				MessageDialog.openError(getShell(), Messages.SourceTypesPropertyDialog_InvalidName, error);
			}
		}

		@Override
		protected Object getValue(Object element) {
			return findLangValue(column, (IntelligenceSource) element);
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			this.editor.setValidator(new ICellEditorValidator() {
				@Override
				public String isValid(Object value) {
					setErrorMessage(null);
					return validate(column, ((IntelligenceSource)element), (String)value);
				}
			});
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
	
	private class SourceTypeLabelProvider extends ColumnLabelProvider implements IColorProvider{ 
		private Column column;
		public SourceTypeLabelProvider(Column column){
			this.column = column;
		}
		@Override
		public String getText(Object element) {
			return findLangValue(column, (IntelligenceSource) element);
		}
		 
		public Color getForeground(Object element){
			 if (((IntelligenceSource)element).getIsActive()){
				 return getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK);
			 }else{
				 return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			 }
		 }
		
		public Color getBackground(Object element){
			 return null;
		 }
	}
	
	
	private class SourceTypeSorter extends ViewerComparator{
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
				return -compareValue((IntelligenceSource)object1, (IntelligenceSource)object2);
			}else{
				return compareValue((IntelligenceSource)object1, (IntelligenceSource)object2);
			}
		}
		
		private int compareValue(IntelligenceSource s1, IntelligenceSource s2){
			if (s1 == null && s2 == null){
				return 0;
			}else if (s1== null && s2 != null){
				return 1;
			}else if (s1 != null && s2 == null){
				return -1;
			}			
			String a = findLangValue(column, s1);
			String b = findLangValue(column, s2);
			if (a != null) a = a.toLowerCase();
			if (b != null) b = b.toLowerCase();
			return nullStringComparator.compare(a,b);
		}
	};
	
}
