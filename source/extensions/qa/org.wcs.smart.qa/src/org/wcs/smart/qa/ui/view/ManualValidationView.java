package org.wcs.smart.qa.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.ui.properties.DialogConstants;

public class ManualValidationView {

	public static final String ID = "org.wcs.smart.qa.validatation.manual.parameters"; //$NON-NLS-1$
	
	private FormToolkit  toolkit;

	private DateFilterDropDownComposite dateFilter;
	private TableViewer tblRoutines;
	
	@Inject
	private IEventBroker eventBroker;
	
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@PreDestroy
	public void dispose(){
	}
	
	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form pageForm = toolkit.createForm(parent);
		pageForm.setText("Manual Data Validation");
		pageForm.getBody().setLayout(new GridLayout());
		
		Composite panel = toolkit.createComposite(pageForm.getBody(), SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		toolkit.createLabel(panel, "Date Filter:");
		DateFilter[] dFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_MONTH,
				DateFilter.CUSTOM};
		
		dateFilter = new DateFilterDropDownComposite(panel, dFilters, DateFilter.LAST_30_DAYS);
		toolkit.adapt(dateFilter);
		
		toolkit.createLabel(panel, "Data to Validate:");
		
		
		tblRoutines = new TableViewer(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblRoutines.getTable());
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance());
		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.getTable().setHeaderVisible(true);
		tblRoutines.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblRoutines.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.SPACE){
					boolean newSelection =  !((DataValidator)((IStructuredSelection)tblRoutines.getSelection()).getFirstElement()).isSelected;
					for (Iterator<?>iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
						DataValidator type = (DataValidator) iterator.next();
						type.isSelected = newSelection;
						
					}
					tblRoutines.refresh();
				}
			}
		});
		
		TableViewerColumn checkColumn = new TableViewerColumn(tblRoutines, SWT.CHECK);
		checkColumn.getColumn().setWidth(30);
		checkColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					if (((DataValidator) element).isSelected()){
						return "YES"; 
							
//						return Character.toString((char)0x2611);
					}else{
						return "NO";
//						return Character.toString((char)0x2610);
					}
				}
				return "";
			}
		});
		
		checkColumn.setEditingSupport(new EditingSupport(checkColumn.getViewer()) {
			CellEditor editor = new CheckboxCellEditor(tblRoutines.getTable());;
			
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof DataValidator){
					((DataValidator)element).isSelected = (Boolean)value;
					tblRoutines.refresh();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof DataValidator){
					return ((DataValidator) element).isSelected();
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return element instanceof DataValidator;
			}
		});
		
		TableViewerColumn dataColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		dataColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					return ((DataValidator) element).getDataProvider().getName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		dataColumn.getColumn().setWidth(150);
		dataColumn.getColumn().setText("Data To Validate");
		
		TableViewerColumn routineColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		routineColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine v = ((DataValidator)element).getRoutine();
					return v.getName() + " (" + v.getRoutineType().getName(Locale.getDefault()) + ")";
				}
				return super.getText(element);
			}
		});
		routineColumn.getColumn().setWidth(150);
		routineColumn.getColumn().setText("Routine To Perform");
		
		TableViewerColumn descColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		descColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine v = ((DataValidator)element).getRoutine();
					return v.getDescription();
				}
				return super.getText(element);
			}
		});
		descColumn.getColumn().setWidth(150);
		descColumn.getColumn().setText("Routine Description");
		
		tblRoutines.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);
		
		Hyperlink hlink = toolkit.createHyperlink(panel, "refresh", SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				loadRoutines();
			}
		});
		
		
		
		Button btnExecute = toolkit.createButton(panel, "EXECUTE", SWT.PUSH);
		btnExecute.addListener(SWT.Selection, e->validate());
		loadRoutines();
	}
	
	
	private void validate(){
		Date startDate = null;
		Date endDate = null;
		if (dateFilter.getDateFilter() == DateFilter.CUSTOM){
			startDate = dateFilter.getCustomStartDate();
			endDate = dateFilter.getCustomEndDate();
		}else{
			startDate = dateFilter.getDateFilter().getStartDate();
			endDate = dateFilter.getDateFilter().getEndDate();
		}
		ValidationEngine engine = new ValidationEngine();
		
		//TODO: validate this cast
		List<Object> items = (List<Object>) tblRoutines.getInput();
		for (Object x  : items){
			if (x instanceof DataValidator && ((DataValidator) x).isSelected()){
				ValidationTask task = new ValidationTask(((DataValidator) x).getRoutine(), ((DataValidator) x).getDataProvider(), startDate, endDate, SmartDB.getCurrentConservationArea());
				engine.addValidationTask(task);
			}
		}
		eventBroker.send("SMART_QA/MANUAL/EXECUTE", engine);
	}
	
	@Focus
	public void setFocus() {
		dateFilter.setFocus();
	}
	
	/*
	 * Loads all possible record sources from db and populates 
	 * provided combo
	 * @param cmbSource
	 */
	private void loadRoutines(){
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);
		j.setSystem(true);
		j.schedule();
	}
	
	Job j = new Job("load routines"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<DataValidator> routines = new ArrayList<>();
			Session s = HibernateManager.openSession();
			try{
				List<QaRoutine> dbroutines = s.createCriteria(QaRoutine.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.list();
				
				Collection<IQaDataProvider> providers = RoutineExtensionManager.INSTANCE.getDataProviders();
				for (IQaDataProvider p : providers){
					for (QaRoutine r : dbroutines){
						if (p.supportsRoutine(r.getRoutineType())){
							routines.add(new DataValidator(r, p));
						}
					}
				}
			}finally{
				s.close();
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
			});
			return Status.OK_STATUS;
		}
		
	};
	private class DataValidator{
		private QaRoutine routine;
		private IQaDataProvider data;
		private boolean isSelected;
		
		public DataValidator(QaRoutine routine, IQaDataProvider data){
			this.routine = routine;
			this.data = data;
		}
		
		public QaRoutine getRoutine(){ return routine; }
		public IQaDataProvider getDataProvider(){ return data; }
		public boolean isSelected(){ return this.isSelected;  }
		public void setSelected(boolean isSelected) { this.isSelected = isSelected; }
		
	}
	
	public static class ManualValidationViewWrapper extends DIViewPart<ManualValidationView>{
		public ManualValidationViewWrapper(){
			super(ManualValidationView.class);
		}
	}
}
