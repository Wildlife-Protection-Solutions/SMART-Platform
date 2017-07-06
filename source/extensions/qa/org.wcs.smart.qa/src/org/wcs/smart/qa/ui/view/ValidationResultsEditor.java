/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Editor part of displaying the results of manual validation routine.
 * 
 * @author Emily
 *
 */
public class ValidationResultsEditor extends TableMapQaErrorComposite {
	
	public static final String ID = "org.wcs.smart.qa.data.validatation.manual"; //$NON-NLS-1$

	private DateFilterDropDownComposite dateFilter;
	private CheckboxTableViewer tblRoutines;
	
	private Composite stackPanel;
	private ProgressAreaComposite progressComposite;
	
	private Font boldFont, normalFont;
	
	private StackPanelItem progressStackItem;
	private StackPanelItem optionsStackItem;
	private StackPanelItem resultsStackItem;
	
	private ValidationEngine lastValidationEngine;

	private FormToolkit toolkit = null;
	
	public static IEditorInput MANUAL_VALIDATION_INPUT =  new IEditorInput() {
		
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return null;
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}
		
		@Override
		public boolean exists() {
			return false;
		}
	};
	
	public ValidationResultsEditor(){
		super();
		
		tableColumns = new ResultTableColumn[]{
				ResultTableColumn.STATUS,
				ResultTableColumn.DATA_TYPE,
				ResultTableColumn.ROUTINE,
				ResultTableColumn.OBJECT_ID,
				ResultTableColumn.DESC,
				ResultTableColumn.FIX
		};
	}
		
	private void executeValidation(final ValidationEngine engine){
		clearResults();
		showProgress();
		Job j = new Job("validation job"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor = progressComposite.createProgressMonitor();
				Collection<QaError> errors = null;
				Session s = HibernateManager.openSession();
				try{
					errors = engine.validate(s, monitor);
				}finally{
					s.close();
				}
				Collection<QaError> ferrors = errors;
				Display.getDefault().syncExec(()->{
					List<Exception> exceptions = engine.getExceptions();
					if (!exceptions.isEmpty()){
						StringBuilder sb = new StringBuilder();
						for (Exception ex : exceptions){
							QaPlugIn.log(ex.getMessage(), ex);
							sb.append(ex.getMessage());
							sb.append("\n");
						}
						String message = "The following errors occured while validating data:\n";
						message += sb.toString();
						if (message.length() > 700){
							message =message.substring(0, 700) + "...";
						}
						MessageDialog.openError(getSite().getShell(), "Error", message);
					}
					setResults(ferrors);
				});
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
	}
	
	
	public void showProgress(){
		progressStackItem.show();
	}
	
	
	
	public void setResults(Collection<QaError> results){
		for (QaError r : results){
			if (r.getUuid() == null){
				r.setUuid(UUID.randomUUID());
			}
		}
		super.setResults(results);
		resultsStackItem.show();
	}	
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	protected void createHeaderToolbar(Composite parent){
		ToolBar tb = new ToolBar(parent,  SWT.NONE);
		ToolItem btnRefresh = new ToolItem(tb, SWT.PUSH);
	//	Button btnRefresh = toolkit.createButton(topComp, "", SWT.PUSH);
		btnRefresh.setToolTipText("Re-run qa routines against the same data.");
		btnRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		btnRefresh.addListener(SWT.Selection, e->{
			if (lastValidationEngine != null){
				executeValidation(lastValidationEngine);
			}
		});
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit =  new FormToolkit(parent.getDisplay());
		
		Form form = toolkit.createForm(parent);
		form.setText("Manual Data Validation ");
		
		form.getBody().setLayout(new GridLayout());
		
		Composite header = toolkit.createComposite(form.getBody());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).horizontalSpacing = 10;
		//((GridLayout)header.getLayout()).marginHeight = 8;
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		Hyperlink lOptions = toolkit.createHyperlink(header, "Options", SWT.NONE);
		Hyperlink lResults = toolkit.createHyperlink(header, "Results", SWT.NONE);
		Label spacer = toolkit.createLabel(header, "");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		normalFont = lOptions.getFont();
		FontData fd = normalFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(lOptions.getDisplay(), fd);
		lOptions.addListener(SWT.Dispose, e->boldFont.dispose());
		
		lOptions.setBackground(header.getBackground());
		lResults.setBackground(header.getBackground());
		spacer.setBackground(header.getBackground());
		
		lOptions.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				optionsStackItem.show();
			}
		});
		
		lResults.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				resultsStackItem.show();
			}
		});

		
		stackPanel = toolkit.createComposite(form.getBody(), SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		Composite optionsPanel = toolkit.createComposite(stackPanel);
		optionsPanel.setLayout(new GridLayout());
		createParameterArea(optionsPanel);
		
		Composite progressPanel = toolkit.createComposite(stackPanel);
		progressPanel.setLayout(new GridLayout());
		progressComposite = new ProgressAreaComposite(progressPanel);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite resultsPanel = toolkit.createComposite(stackPanel);
		resultsPanel.setLayout(new GridLayout());
		
		resultsStackItem = new StackPanelItem(lResults, resultsPanel);
		optionsStackItem = new StackPanelItem(lOptions, optionsPanel);
		progressStackItem = new StackPanelItem(lResults, progressPanel);

		super.createPartControl(resultsPanel);
		
		optionsStackItem.show();
	}


	
	@Override
    public void dispose() {
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
		super.dispose();
	}

	@Override
	public EditorPart getParentEditor() {
		return this;
	}

	
	private void createParameterArea(Composite parent){
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Button btnExecute = toolkit.createButton(panel, "Validate Data...", SWT.PUSH);
		btnExecute.addListener(SWT.Selection, e->validate());
		
		Composite dFilter = toolkit.createComposite(panel);
		dFilter.setLayout(new GridLayout(2, false));
		dFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dFilter.getLayout()).marginWidth = 0;
		((GridLayout)dFilter.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(dFilter, "Dates:");
		DateFilter[] dFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_MONTH,
				DateFilter.CUSTOM};
		
		dateFilter = new DateFilterDropDownComposite(dFilter, dFilters, DateFilter.LAST_30_DAYS);
		toolkit.adapt(dateFilter);
		
		Table tbl = toolkit.createTable(panel, SWT.CHECK| SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblRoutines = new CheckboxTableViewer(tbl);//, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblRoutines.getTable());
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance()); 
		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.getTable().setHeaderVisible(true);
		tblRoutines.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblRoutines.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					Object selection = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
					boolean newValue = !tblRoutines.getChecked(selection);
					
					for (Iterator<?>iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
						DataValidator type = (DataValidator) iterator.next();
						tblRoutines.setChecked(type, newValue);
					}
					tblRoutines.refresh();
					e.doit = false;
				}
			}
		});
		
		TableViewerColumn checkColumn = new TableViewerColumn(tblRoutines, SWT.CHECK);
		checkColumn.getColumn().setWidth(30);
		checkColumn.setLabelProvider(new ColumnLabelProvider(){});
		
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
		
		TableViewerColumn paramColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		paramColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine routine = ((DataValidator)element).getRoutine();
					return routine.getRoutineType().getParameterSummary(routine);
				}
				return super.getText(element);
			}
		});
		paramColumn.getColumn().setWidth(150);
		paramColumn.getColumn().setText("Routine Parameters");
		
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
		
		Hyperlink hlink = toolkit.createHyperlink(panel, "refresh list", SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				loadRoutines();
			}
		});
		hlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);

		
		loadRoutines();
	}

	private void validate(){
		if (tblRoutines.getCheckedElements().length == 0){
			MessageDialog.openInformation(getSite().getShell(), "No Routines Selected", "No validation routines selected.  Nothing to validate.");
			return;
		}
		
		Date startDate = null;
		Date endDate = null;
		if (dateFilter.getDateFilter() == DateFilter.CUSTOM){
			startDate = dateFilter.getCustomStartDate();
			endDate = dateFilter.getCustomEndDate();
		}else{
			startDate = dateFilter.getDateFilter().getStartDate();
			endDate = dateFilter.getDateFilter().getEndDate();
		}
		
		if (startDate.after(endDate)){
			MessageDialog.openInformation(getSite().getShell(), "Invalid Dates", "Start date is after end date.  Cannot validate data.");
			return;
		}
		
		lastValidationEngine = new ValidationEngine(Locale.getDefault());
		for (Object x  : tblRoutines.getCheckedElements()){
			if (x instanceof DataValidator){
				ValidationTask task = new ValidationTask(((DataValidator) x).getRoutine(), ((DataValidator) x).getDataProvider(), startDate, endDate, SmartDB.getCurrentConservationArea());
				lastValidationEngine.addValidationTask(task);
			}
		}
		executeValidation(lastValidationEngine);
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
						for (QaRoutineParameter pp : r.getParameters()){
							pp.getParameterId();
							pp.getStringValue();
						}
					}
				}
			}finally{
				s.close();
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
				tblRoutines.setAllChecked(true);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class DataValidator{
		
		private QaRoutine routine;
		private IQaDataProvider data;
		
		public DataValidator(QaRoutine routine, IQaDataProvider data){
			this.routine = routine;
			this.data = data;
		}		
		public QaRoutine getRoutine(){ return routine; }
		public IQaDataProvider getDataProvider(){ return data; }
	}
	
	private class StackPanelItem{
		Hyperlink  lblHeader;
		Composite control;
		
		public StackPanelItem(Hyperlink  lblHeader, Composite control){
			this.lblHeader = lblHeader;
			this.control = control;
		}
		
		public void show(){
			for (Control c : lblHeader.getParent().getChildren()){
				c.setFont(normalFont);
			}
			lblHeader.setFont(boldFont);
			if (tblResults.getInput() == null && this == resultsStackItem){
				((StackLayout)stackPanel.getLayout()).topControl = progressStackItem.control;
			}else{
				((StackLayout)stackPanel.getLayout()).topControl = this.control;
			}
			lblHeader.getParent().layout();
			stackPanel.layout();
		}
	}
}