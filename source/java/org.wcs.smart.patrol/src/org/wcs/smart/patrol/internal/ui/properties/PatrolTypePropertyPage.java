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
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.export.dialog.CsvCaImportDialog;
import org.wcs.smart.export.dialog.CsvExportDialog;
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
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.KeyInputDialog;
import org.wcs.smart.ui.properties.LanguageViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Property page for managaing patrol types and
 * transport types.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolTypePropertyPage extends AbstractPropertyJHeaderDialog {

	private static final String INVALID_TYPE_DIALOG_TITLE = Messages.PatrolTypePropertyPage_InvalidType_DialogTitle;
	private static final String DISABLED_LABEL = Messages.PatrolTypePropertyPage_DisabledLabel;
	private static final String ACTIVE_LABEL = Messages.PatrolTypePropertyPage_ActiveLabel;
	
	private LanguageViewer languageViewer;
	private TableViewer patrolTypeTblViewer;
	private TableViewer transportTblViewer;
	private Button btnDisableType;
	private Button btnDisableTransport;
	private Button btnDeleteTransport;
	private Button btnEditKeyTransport;
	
	private List<PatrolType> patrolTypes = null;
	private ConservationArea currentCa = null;
	
	private Button btnAddTransport;

	/**
	 * @param parent
	 * @param title
	 */
	public PatrolTypePropertyPage(Shell parent) {
		super(parent, Messages.PatrolTypePropertyPage_Dialog_Title);
		this.currentCa = SmartDB.getCurrentConservationArea();
	}

	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		patrolTypes = new ArrayList<PatrolType>(PatrolHibernateManager.getPatrolTypes(currentCa, getSession()));
		getSession().beginTransaction();
		try{
			//ensure all types are laziy loaded
			for (Object t : patrolTypes){
				if (((PatrolType)t).getTransportTypes() != null){
					((PatrolType)t).getTransportTypes().size();
				}
			}
		}finally{
			getSession().getTransaction().rollback();
		}
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText(Messages.PatrolTypePropertyPage_LanguageLabel);

		languageViewer = new LanguageViewer(container, SWT.NONE, currentCa);
		languageViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		languageViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				transportTblViewer.refresh();
			}
		});
		Label lblType = new Label(container, SWT.NONE);
		lblType.setText(Messages.PatrolTypePropertyPage_TypesLabel);
		lblType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3,1));
		
		Composite composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		TableColumnLayout tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		patrolTypeTblViewer = new TableViewer( composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		createTypeColumns(patrolTypeTblViewer);
		patrolTypeTblViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolTypeTblViewer.setInput(patrolTypes);
		patrolTypeTblViewer.getTable().setHeaderVisible(true);
		patrolTypeTblViewer.getTable().setLinesVisible(true);
		patrolTypeTblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
				
				if (pt.getIsActive()){
					btnDisableType.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDisableType.setEnabled(true);
				
				btnAddTransport.setEnabled(pt.getIsActive());
				btnDisableTransport.setEnabled(false);
				transportTblViewer.getTable().setEnabled(pt.getIsActive());

				if (pt.getTransportTypes() == null){
					pt.setTransportTypes(new ArrayList<PatrolTransportType>());
				}
				transportTblViewer.setInput(pt.getTransportTypes());
				transportTblViewer.refresh();
				
			}
		});
		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,
				1, 1));

		btnDisableType = new Button(composite, SWT.NONE);
		btnDisableType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisableType.setEnabled(false);
		btnDisableType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
				if (btnDisableType.getText() == DialogConstants.DISABLE_BUTTON_TEXT){
					pt.setIsActive(false);
					btnDisableType.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}else{
					pt.setIsActive(true);
					btnDisableType.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}
				patrolTypeTblViewer.refresh();
				
				btnAddTransport.setEnabled(pt.getIsActive());
				btnDisableTransport.setEnabled(pt.getIsActive());
				transportTblViewer.getTable().setEnabled(pt.getIsActive());

				setChangesMade(true);
			}
		});

		
		/* --------- Patrol Transport Type -------------- */
		lblType = new Label(container, SWT.NONE);
		lblType.setText(Messages.PatrolTypePropertyPage_TransportOptionsLabel);
		lblType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3,1));
		
		composite2 = new Composite(container, SWT.NONE);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		tableLayout = new TableColumnLayout();
		composite2.setLayout(tableLayout);
		transportTblViewer = new TableViewer( composite2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		createTransportColumns(transportTblViewer);
		transportTblViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		transportTblViewer.getTable().setHeaderVisible(true);
		transportTblViewer.getTable().setLinesVisible(true);
		transportTblViewer.getTable().setEnabled(false);
		
		transportTblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
				
				if (pt == null){
					btnDisableTransport.setEnabled(false);
					btnDeleteTransport.setEnabled(false);
					btnEditKeyTransport.setEnabled(false);
					return;
				}
				if (pt.getIsActive()){
					btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDisableTransport.setEnabled(true);
				btnDeleteTransport.setEnabled(true);
				btnEditKeyTransport.setEnabled(true);
				
			}
		});
		transportTblViewer.getTable().addListener(SWT.MouseDoubleClick, new Listener(){
			@Override
			public void handleEvent(Event event) {
				ViewerCell cell = transportTblViewer.getCell(new Point(event.x, event.y));
				if (cell != null && cell.getColumnIndex() == 2){
					editKey();
				}
			}
		});
		
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(transportTblViewer, new FocusCellHighlighter(transportTblViewer){});
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(transportTblViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TableViewerEditor.create(transportTblViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR | ColumnViewerEditor.KEYBOARD_ACTIVATION);

		
		composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false,
				1, 1));
		btnAddTransport = new Button(composite, SWT.NONE);
		btnAddTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAddTransport.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAddTransport.setEnabled(false);
		btnAddTransport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
				PatrolTransportType newPtt = new PatrolTransportType();
				newPtt.setConservationArea(currentCa);
				newPtt.setIsActive(true);
				newPtt.setPatrolType(pt.getType());
				newPtt.updateName(currentCa.getDefaultLanguage(), Messages.PatrolTypePropertyPage_DefaultTransportionTypeName);
				newPtt.setName(newPtt.findName(currentCa.getDefaultLanguage()));
				pt.getTransportTypes().add(newPtt);
				transportTblViewer.refresh();
				setChangesMade(true);
			}
		});
		btnEditKeyTransport = new Button(composite, SWT.NONE);
		btnEditKeyTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnEditKeyTransport.setText(DialogConstants.EDIT_KEY_BUTTON_TEXT);
		btnEditKeyTransport.setEnabled(false);
		btnEditKeyTransport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editKey();
			}
		});;
		
		btnDisableTransport = new Button(composite, SWT.NONE);
		btnDisableTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
		btnDisableTransport.setEnabled(false);
		btnDisableTransport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
				if (btnDisableTransport.getText() == DialogConstants.DISABLE_BUTTON_TEXT){
					pt.setIsActive(false);
					btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}else{
					pt.setIsActive(true);
					btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}
				transportTblViewer.refresh();
				setChangesMade(true);
			}
		});
		btnDeleteTransport = new Button(composite, SWT.NONE);
		btnDeleteTransport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		btnDeleteTransport.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDeleteTransport.setEnabled(false);
		btnDeleteTransport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteTransportType();
			}
		});
		
		
		Composite buttonComp = new Composite(container, SWT.NONE);
		buttonComp.setLayout(new GridLayout(2, true));
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,false,3,1));
		
		Button btnImport = new Button(buttonComp, SWT.PUSH);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolTransportCsvImportConfig config = new PatrolTransportCsvImportConfig();
				CsvCaImportDialog dialog = new CsvCaImportDialog(getShell(), config);
				if (dialog.open() == Window.OK){
					Collection<PatrolTransportType> types = ((PatrolTransportCsvImporter)config.getImporter()).getImportedData();
					for (PatrolTransportType t : types){
						for (PatrolType type : patrolTypes){
							if (type.getType().equals(t.getPatrolType())){
								//new to validate keys
								Collection<PatrolTransportType> siblings = getAllTransportTypes();
								if (DataModelManager.INSTANCE.validateKey(t.getKeyId(), siblings) != null){
									t.setKeyId(DataModelManager.INSTANCE.generateKey(t.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()), siblings));
								}
								type.getTransportTypes().add(t);
							}
						}
					}
					setChangesMade(true);
					transportTblViewer.refresh();
				}
				
			}
		});
		
		Button btnExport = new Button(buttonComp, SWT.PUSH);
		btnExport.setText(DialogConstants.EXPORT_BUTTON_TEXT);
		btnExport.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				CsvExportDialog dialog = new CsvExportDialog(getShell(), new PatrolTransportCsvExportConfig());
				dialog.open();
			}
		});
		
		setTitle(SmartUtils.formatStringForLabel(Messages.PatrolTypePropertyPage_PageName));
		setMessage(Messages.PatrolTypePropertyPage_DialogMessage);
		return container;
	}

	/**
	 * 
	 * @return a collection of all transport types
	 */
	private List<PatrolTransportType> getAllTransportTypes(){
		List<PatrolTransportType> siblings = new ArrayList<PatrolTransportType>();
		for (PatrolType l : patrolTypes){
			if (l.getTransportTypes() != null) {
				siblings.addAll(l.getTransportTypes());
			}
		}
		return siblings;
	}
	
	private void editKey(){
		List<PatrolTransportType> siblings = getAllTransportTypes();
		PatrolTransportType x = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		String currentKey = x.getKeyId();
		InputDialog id = new KeyInputDialog(getShell(), currentKey,  siblings);
		int ret = id.open();
		if (ret != Window.CANCEL) {
			x.setKeyId(id.getValue());
			setChangesMade(true);
			transportTblViewer.refresh(x);
		}
	}
	/*
	 * Creates station table columns
	 */
	private void createTypeColumns(TableViewer viewer) {
		
		/* Active Column */
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
			TableColumn column = viewerColumn.getColumn();
			column.setText(ACTIVE_LABEL);
			column.setResizable(true);
			column.setMoveable(true);

			TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
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
			});
			
			/* Type Column */
			viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
			column = viewerColumn.getColumn();
			column.setText(Messages.PatrolTypePropertyPage_PatrolType_ColumnHeader);
			column.setResizable(true);
			column.setMoveable(true);

			layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
			layout.setColumnData(column, new ColumnWeightData(3,ColumnWeightData.MINIMUM_WIDTH, true));
			
			viewerColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof PatrolType){
						return LabelConstants.getLabel( (((PatrolType)element).getType()));
					}
					return super.getText(element);
				}
			});

		
	}
	
	
	/*
	 * Creates station table columns
	 */
	private void createTransportColumns(final TableViewer viewer) {
		
		/* Active Column */
		TableViewerColumn viewerColumn = new TableViewerColumn(viewer,SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(ACTIVE_LABEL);
		column.setResizable(true);
		column.setMoveable(true);

		TableColumnLayout layout = (TableColumnLayout) viewer.getTable().getParent().getLayout();
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
				if (element instanceof PatrolTransportType){
					String x = (((PatrolTransportType)element).findNameNull(languageViewer.getCurrentSelection()));
					if (x == null){
						x = (((PatrolTransportType)element).getName());
					}
					return x;
				}
				return super.getText(element);
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
					PatrolType pt = null;
					for (PatrolType t : patrolTypes){
						if (t.getType().equals(ttype.getPatrolType())){
							pt = t;
							break;
						}
					}
					if (!ttype.findName(languageViewer.getCurrentSelection()).equals((String)value)){
						if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
							Integer matches = 0;
							for (Iterator<PatrolTransportType> itr = pt.getTransportTypes().iterator(); itr.hasNext();) {
								PatrolTransportType a = itr.next();
								if( a != element && a.findName(languageViewer.getCurrentSelection()).equals(((String)value).trim())){
									matches++;
								}
							} 
							if(matches > 0){
								//invalid agency name, don't update it.
								MessageDialog.openError(getShell(), INVALID_TYPE_DIALOG_TITLE, Messages.PatrolTypePropertyPage_Error_DuplicateTransportOption);
								setChangesMade(false);
							}else{
								ttype.updateName(languageViewer.getCurrentSelection(), ((String)value).trim());
								if (ttype.getKeyId() == null){
									ttype.setKeyId(DataModelManager.INSTANCE.generateKey((String)value, getAllTransportTypes()));
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
		});
	}
	private HashSet<PatrolTransportType> toDelete = new HashSet<PatrolTransportType>();
	
	private void deleteTransportType(){
		PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
		PatrolTransportType ttype = (PatrolTransportType) ((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		if (pt == null || ttype == null){
			return;
		}

		boolean ok = MessageDialog.openConfirm(getShell(), Messages.PatrolTypePropertyPage_DeleteDialogTitle, MessageFormat.format(Messages.PatrolTypePropertyPage_DeleteWarningMessage, new Object[]{ttype.getName()}));
		if (!ok){
			return;
		}
		
		try{
			if (ttype.getUuid() != null){
				if (DeleteManager.canDelete(ttype, getSession())){
					pt.getTransportTypes().remove(ttype);
					toDelete.add(ttype);
//					ttype.setPatrolType(null);
					setChangesMade(true);
				}
			}else{
				pt.getTransportTypes().remove(ttype);
				ttype.setPatrolType(null);
			}
				
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.PatrolTypePropertyPage_Error_DeletingTransport + " " + ex.getLocalizedMessage(), new Object[]{ ttype.getName()}), ex); //$NON-NLS-1$
		}	
		
		transportTblViewer.refresh();
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
		List<PatrolTransportType> siblings = getAllTransportTypes();
		
		//validate keys
		try{
			for (Iterator<?> iterator = this.patrolTypes.iterator(); iterator.hasNext();) {
				PatrolType type = (PatrolType) iterator.next();
				if (type.getTransportTypes() != null){
					for (PatrolTransportType tt : type.getTransportTypes()){
						siblings.remove(tt);
						String error = DataModelManager.INSTANCE.validateKey(tt.getKeyId(), siblings);
						siblings.add(tt);
						if (error != null){
							throw new Exception(error);
						}
					}
				}
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolTypePropertyPage_Error_SavingChanges + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
			return false;
		}
		
		Session s = getSession();
		s.beginTransaction();
		try{
			for (PatrolTransportType t : toDelete){
				s.delete(t);
			}

			for (Iterator<?> iterator = this.patrolTypes.iterator(); iterator.hasNext();) {
				PatrolType type = (PatrolType) iterator.next();
				s.saveOrUpdate(type);
				if (type.getTransportTypes() != null){
					for (PatrolTransportType tt : type.getTransportTypes()){
						s.saveOrUpdate(tt);
					}
				}
			}
			s.getTransaction().commit();
			toDelete.clear();
			setChangesMade(false);
			return true;
		}catch (Exception ex){
			s.getTransaction().rollback();
			s.close();
			SmartPatrolPlugIn.displayLog(Messages.PatrolTypePropertyPage_Error_SavingChanges + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
		}
		return false;
	}
}
