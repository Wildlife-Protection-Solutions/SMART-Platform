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
import java.util.HashSet;
import java.util.Iterator;

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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
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
	
	private WritableList patrolTypes = null;
	private WritableList patrolTransportTypes = null;

	private Button btnAddTransport;

	/**
	 * @param parent
	 * @param title
	 */
	public PatrolTypePropertyPage() {
		super(Display.getCurrent().getActiveShell(), Messages.PatrolTypePropertyPage_Dialog_Title);
	}

	@Override
	public boolean  close(){
		boolean canClose = super.close();
		if (canClose){
			if (patrolTransportTypes != null){
				patrolTransportTypes.dispose();
			}
			if (patrolTypes != null){
				patrolTypes.dispose();
			}
		}
		return canClose;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		patrolTypes = new WritableList(PatrolHibernateManager.getPatrolTypes(ca,
				getSession()), PatrolType.class);
		getSession().beginTransaction();
		for (Object t : patrolTypes){
			((PatrolType)t).getTransportTypes();
		}
		getSession().getTransaction().rollback();
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText(Messages.PatrolTypePropertyPage_LanguageLabel);

		languageViewer = new LanguageViewer(container, SWT.NONE, ca);
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
		patrolTypeTblViewer.setContentProvider(new ObservableListContentProvider());
		patrolTypeTblViewer.setInput(patrolTypes);
		patrolTypeTblViewer.getTable().setHeaderVisible(true);
		patrolTypeTblViewer.getTable().setLinesVisible(true);
//		tableViewer.setComparator(sorter);	
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

				if (patrolTransportTypes != null){
					patrolTransportTypes.dispose();
				}
				if (pt.getTransportTypes() == null){
					pt.setTransportTypes(new ArrayList<PatrolTransportType>());
				}
				patrolTransportTypes = new WritableList(pt.getTransportTypes(), PatrolTransportType.class);
				transportTblViewer.setInput(patrolTransportTypes);
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
		transportTblViewer.setContentProvider(new ObservableListContentProvider());
		
		transportTblViewer.getTable().setHeaderVisible(true);
		transportTblViewer.getTable().setLinesVisible(true);
		transportTblViewer.getTable().setEnabled(false);
		
//		tableViewer.setComparator(sorter);	
		transportTblViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				PatrolTransportType pt = (PatrolTransportType)((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
				
				if (pt == null){
					btnDisableTransport.setEnabled(false);
					btnDeleteTransport.setEnabled(false);
					return;
				}
				if (pt.getIsActive()){
					btnDisableTransport.setText(DialogConstants.DISABLE_BUTTON_TEXT);
				}else{
					btnDisableTransport.setText(DialogConstants.ENABLE_BUTTON_TEXT);
				}
				btnDisableTransport.setEnabled(true);
				btnDeleteTransport.setEnabled(true);
				
			}
		});
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
				newPtt.setConservationArea(ca);
				newPtt.setIsActive(true);
				newPtt.setPatrolType(pt.getType());
				newPtt.updateName(ca.getDefaultLanguage(), Messages.PatrolTypePropertyPage_DefaultTransportionTypeName);
				newPtt.setName(newPtt.findName(ca.getDefaultLanguage()));
				pt.getTransportTypes().add(newPtt);
				transportTblViewer.refresh();
				setChangesMade(true);
			}
		});
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
				}else{
					pt.setIsActive(true);
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
		
		setMessage(Messages.PatrolTypePropertyPage_DialogMessage);
		return container;
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
						return (((PatrolType)element).getType().getGuiName());
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
			
			/* Type Column */
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
						if (!ttype.findName(languageViewer.getCurrentSelection()).equals((String)value)){
							if(SmartUtils.isSimpleString(((String)value).trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, PatrolType.MAX_TRANSPORT_NAME_LENGTH)){
								Integer matches = 0;
								for (@SuppressWarnings("unchecked")	Iterator<PatrolTransportType> itr = patrolTransportTypes.iterator(); itr.hasNext();) {
									PatrolTransportType a = itr.next();
									if( a != element && a.findName(languageViewer.getCurrentSelection()).equals(((String)value).trim())){
										matches++;
									}
								} 
								if(matches > 0){
									//invalid agency name, don't update it.
									MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_TYPE_DIALOG_TITLE, Messages.PatrolTypePropertyPage_Error_DuplicateTransportOption);
									setChangesMade(false);
								}else{
									ttype.updateName(languageViewer.getCurrentSelection(), ((String)value).trim());
									setChangesMade(true);
								}
							}else{
								//invalid agency name, don't update it.
								MessageDialog.openError(Display.getDefault().getActiveShell(), INVALID_TYPE_DIALOG_TITLE, 
										MessageFormat.format(Messages.PatrolTypePropertyPage_Error_InvalidTransportType, new Object[]{SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
								setChangesMade(false);
							}
							
								
						}
						viewer.refresh();
					}					
				}});

		
	}
	private HashSet<PatrolTransportType> toDelete = new HashSet<PatrolTransportType>();
	
	private void deleteTransportType(){
		PatrolType pt = (PatrolType)((IStructuredSelection)patrolTypeTblViewer.getSelection()).getFirstElement();
		PatrolTransportType ttype = (PatrolTransportType) ((IStructuredSelection)transportTblViewer.getSelection()).getFirstElement();
		if (pt == null || ttype == null){
			return;
		}

		try{
			if (ttype.getUuid() != null){
				if (DeleteManager.canDelete(ttype, getSession())){
					patrolTransportTypes.remove(ttype);
					pt.getTransportTypes().remove(ttype);
					toDelete.add(ttype);
//					ttype.setPatrolType(null);
					setChangesMade(true);
				}
			}else{
				patrolTransportTypes.remove(ttype);
				pt.getTransportTypes().remove(ttype);
				ttype.setPatrolType(null);
			}
				
		}catch (Exception ex){
			SmartPlugIn.displayLog(getShell(), 
					MessageFormat.format(Messages.PatrolTypePropertyPage_Error_DeletingTransport, new Object[]{ ttype.getName()}), ex);
		}	
		
		transportTblViewer.refresh();
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean performSave() {
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
			SmartPatrolPlugIn.displayLog(Messages.PatrolTypePropertyPage_Error_SavingChanges + ex.getLocalizedMessage(), ex);
		}
		return false;
	}
}
