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

import java.text.Collator;
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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.internal.Messages;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Editor part of displaying the results of manual validation routine.
 * 
 * @author Emily
 *
 */
public class ManualResultsEditor extends TableMapQaErrorComposite {
	
	public static final String ID = "org.wcs.smart.qa.data.validatation.manual"; //$NON-NLS-1$

	private DateFilterDropDownComposite dateFilter;
	private CheckboxTableViewer tblRoutines;
	
	private Composite stackPanel;
	private ProgressAreaComposite progressComposite;
	private Label infoLabel;
	
	private Font boldFont, normalFont;
	
	private StackPanelItem progressStackItem;
	private StackPanelItem optionsStackItem;
	private StackPanelItem resultsStackItem;
	
	private ValidationEngine lastValidationEngine;

	private FormToolkit toolkit = null;

	private Composite detailsComposite;
	private Listener detailsSizeListener;
	
	private TableViewerColumn sortColumn = null;
	private int sortDirection = -1;
	
	public static IEditorInput MANUAL_VALIDATION_INPUT =  new IEditorInput() {
		
		@Override
		public <T> T getAdapter(Class<T> adapter) {
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
	
	public ManualResultsEditor(){
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
		Job j = new Job(Messages.ManualResultsEditor_JobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor = progressComposite.createProgressMonitor();
				Collection<QaError> errors = null;
				try(Session s = HibernateManager.openSession()){
					errors = engine.validate(s, monitor);
				}
				Collection<QaError> ferrors = errors;
				Display.getDefault().syncExec(()->{
					List<Exception> exceptions = engine.getExceptions();
					if (!exceptions.isEmpty()){
						StringBuilder sb = new StringBuilder();
						for (Exception ex : exceptions){
							QaPlugIn.log(ex.getMessage(), ex);
							sb.append(ex.getMessage());
							sb.append("\n"); //$NON-NLS-1$
						}
						String message = Messages.ManualResultsEditor_ValidationErrorMsg + "\n"; //$NON-NLS-1$
						message += sb.toString();
						if (message.length() > 700){
							message =message.substring(0, 700) + "..."; //$NON-NLS-1$
						}
						MessageDialog.openError(getSite().getShell(), Messages.ManualResultsEditor_ErrorDialogTitle, message);
					}
					setResults(ferrors);
				});
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
	}
	
	/**
	 * Displays the progress tab
	 */
	public void showProgress(){
		if (infoLabel != null){
			infoLabel.dispose();
			infoLabel = null;
		}
		progressComposite.setVisible(true);
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
		btnRefresh.setToolTipText(Messages.ManualResultsEditor_RefreshTooltip);
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
		form.setText(Messages.ManualResultsEditor_FormName);		
		form.getBody().setLayout(new GridLayout());
	
		Composite header = toolkit.createComposite(form.getBody());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).horizontalSpacing = 10;
		//((GridLayout)header.getLayout()).marginHeight = 8;
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		Hyperlink lOptions = toolkit.createHyperlink(header, Messages.ManualResultsEditor_OptionsHeaderLbl, SWT.NONE);
		Hyperlink lResults = toolkit.createHyperlink(header, Messages.ManualResultsEditor_ResultsHeaderLbl, SWT.NONE);
		Label spacer = toolkit.createLabel(header, ""); //$NON-NLS-1$
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
		infoLabel = toolkit.createLabel(progressPanel, Messages.ManualResultsEditor_InitialMessage);
		
		progressComposite = new ProgressAreaComposite(progressPanel);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		progressComposite.setVisible(false);
		
		Composite resultsPanel = toolkit.createComposite(stackPanel);
		resultsPanel.setLayout(new GridLayout());
		((GridLayout)resultsPanel.getLayout()).marginWidth = 0;
		((GridLayout)resultsPanel.getLayout()).marginHeight = 0;
		
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
	
	private void updateRoutineDetails(){
		for (Control c : detailsComposite.getChildren()){
			c.dispose();
		}
		if (detailsSizeListener != null){
			detailsComposite.removeListener(SWT.Resize, detailsSizeListener);
			detailsSizeListener = null;
		}
		
		if (tblRoutines.getSelection().isEmpty()) return;
		DataValidator r = (DataValidator) ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
		if (r == null) return;
		
		detailsComposite.setLayout(new GridLayout());
//		((GridLayout)detailsComposite.getLayout()).marginWidth = 0;
//		((GridLayout)detailsComposite.getLayout()).marginHeight= 0;
		
		int widthHint = 250;
		Label l = toolkit.createLabel(detailsComposite, r.getRoutine().getName() + " \n" + r.getDataProvider().getName(Locale.getDefault()), SWT.WRAP); //$NON-NLS-1$
		l.setFont(boldFont);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = widthHint;
		
		ScrolledComposite scroll = new ScrolledComposite(detailsComposite, SWT.V_SCROLL );
		scroll.setExpandVertical(true);
//		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)scroll.getLayoutData()).widthHint = widthHint;
		
		toolkit.adapt(scroll);
		
		Composite textArea = toolkit.createComposite(scroll, SWT.NONE);
		scroll.setContent(textArea);
		textArea.setLayout(new GridLayout());
		((GridLayout)textArea.getLayout()).marginWidth = 0;
		((GridLayout)textArea.getLayout()).marginHeight= 0;
		textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		l = toolkit.createLabel(textArea, r.getRoutine().getRoutineType().getName(Locale.getDefault()), SWT.WRAP);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = widthHint;
		
		if (r.getRoutine().getDescription() != null && !r.getRoutine().getDescription().isEmpty()){
			l = toolkit.createLabel(textArea, "\n" + Messages.ManualResultsEditor_DescriptionLbl + "\n" + r.getRoutine().getDescription(), SWT.WRAP); //$NON-NLS-1$ //$NON-NLS-2$ 
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = widthHint;
		}
		String params = r.getParameterDescription();
		if (params != null && !params.isEmpty()){
			l = toolkit.createLabel(textArea, "\n" + Messages.ManualResultsEditor_ParametersLbl + "\n" + params, SWT.WRAP); //$NON-NLS-1$ //$NON-NLS-2$ 
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = widthHint;
		}
		
		l = toolkit.createLabel(textArea, "\n" + Messages.ManualResultsEditor_DataTypesLbl + "\n" + r.getDataProvider().getName(Locale.getDefault()), SWT.WRAP); //$NON-NLS-1$ //$NON-NLS-2$ 
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = widthHint;
		
		detailsSizeListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (scroll.isDisposed()) return;
				int width = detailsComposite.getSize().x - scroll.getVerticalBar().getSize().x - 15;
				textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
				scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));	
			}
		};
		detailsComposite.getParent().layout(true, true);
			
		int width = detailsComposite.getSize().x - scroll.getVerticalBar().getSize().x - 15;
		textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
		scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));
	}
		
	private void createParameterArea(Composite parent){
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Button btnExecute = toolkit.createButton(panel, Messages.ManualResultsEditor_ValidateButton, SWT.PUSH);
		btnExecute.addListener(SWT.Selection, e->validate());
		
		Composite dFilter = toolkit.createComposite(panel);
		dFilter.setLayout(new GridLayout(2, false));
		dFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dFilter.getLayout()).marginWidth = 0;
		((GridLayout)dFilter.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(dFilter, Messages.ManualResultsEditor_DatesLabel);
		DateFilter[] dFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_MONTH,
				DateFilter.CUSTOM};
		
		dateFilter = new DateFilterDropDownComposite(dFilter, dFilters, DateFilter.LAST_30_DAYS,true);
		toolkit.adapt(dateFilter);
		
		Composite tableArea = toolkit.createComposite(panel, SWT.NONE);
		tableArea.setLayout(new GridLayout(2, false));
		((GridLayout)tableArea.getLayout()).marginWidth = 0;
		((GridLayout)tableArea.getLayout()).marginHeight = 0;
		tableArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Table tbl = toolkit.createTable(tableArea, SWT.CHECK| SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblRoutines = new CheckboxTableViewer(tbl);//, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblRoutines.getTable());
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance()); 
		//tblRoutines.getTable().setLinesVisible(true);
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
		ColumnLabelProvider dataLabelProvider = new ColumnLabelProvider(){
			
			public String getText(Object element){
				if (element instanceof DataValidator){
					return ((DataValidator) element).getDataProvider().getName(Locale.getDefault());
				}
				return super.getText(element);
			}
			
			public Image getImage(Object element){
				if (element instanceof DataValidator){
					return InternalExtensionManager.INSTANCE.getImage(((DataValidator) element).getDataProvider());
				}
				return super.getImage(element);
			}
		};
		dataColumn.setLabelProvider(dataLabelProvider);
		dataColumn.getColumn().setWidth(200);
		dataColumn.getColumn().setText(Messages.ManualResultsEditor_DataToValidateColumnName);
		dataColumn.getColumn().addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (sortColumn == dataColumn){
					sortDirection = -1 * sortDirection;
				}else{
					sortColumn = dataColumn;
				}
				tblRoutines.getTable().setSortColumn(sortColumn.getColumn());
				tblRoutines.getTable().setSortDirection(sortDirection > 0 ? SWT.UP : SWT.DOWN);
				tblRoutines.refresh();
			}});
		
		TableViewerColumn routineColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		ColumnLabelProvider routineLabelProvider = new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine v = ((DataValidator)element).getRoutine();
					return v.getName() + " (" + v.getRoutineType().getName(Locale.getDefault()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		};
		routineColumn.setLabelProvider(routineLabelProvider);
		routineColumn.getColumn().setWidth(700);
		routineColumn.getColumn().setText(Messages.ManualResultsEditor_RoutinecolumnName);
		routineColumn.getColumn().addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (sortColumn == routineColumn){
					sortDirection = -1 * sortDirection;
				}else{
					sortColumn = routineColumn;
				}
				tblRoutines.getTable().setSortColumn(sortColumn.getColumn());
				tblRoutines.getTable().setSortDirection(sortDirection > 0 ? SWT.UP : SWT.DOWN);
				tblRoutines.refresh();
			}});
		
		tblRoutines.setComparator(new ViewerComparator(){
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (sortColumn == null) return 0;
				if (!(e1 instanceof DataValidator && e2 instanceof DataValidator)) return 0;
				
				DataValidator v1 = (DataValidator) e1;
				DataValidator v2 = (DataValidator) e2;
				
				if (sortColumn == routineColumn){
					return sortDirection * Collator.getInstance().compare(routineLabelProvider.getText(v1), routineLabelProvider.getText(v2));
				}else if (sortColumn == dataColumn){
					return sortDirection * Collator.getInstance().compare(dataLabelProvider.getText(v1), dataLabelProvider.getText(v2));
				}
				return 0;
			}
		});
		
		detailsComposite = toolkit.createComposite(tableArea, SWT.BORDER);
		detailsComposite.setLayout(new GridLayout());
		detailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblRoutines.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRoutineDetails();
			}
		});
		
		//fix the size of the description column
		tableArea.addListener(SWT.Resize, e->{
			Point size = tableArea.getSize();
	            
			int right = 0;
	        int left = 0;
	        if (size.x < 500){
	        	right = left = size.x/2;
	        }else{
	        	right = 250;
	            left = size.x - right;
	        }
	        ((GridData)tblRoutines.getControl().getLayoutData()).widthHint = left;
	        ((GridData)detailsComposite.getLayoutData()).widthHint = right;
		});

		Menu mnuRoutines = new Menu(tblRoutines.getControl());
		MenuItem selectAll = new MenuItem(mnuRoutines, SWT.PUSH);
		selectAll.setText(Messages.ManualResultsEditor_SelectAllLabel);
		selectAll.addListener(SWT.Selection,  e-> tblRoutines.setAllChecked(true));
		MenuItem deselectAll = new MenuItem(mnuRoutines, SWT.PUSH);
		deselectAll.setText(Messages.ManualResultsEditor_DeSelectAllLabel);
		deselectAll.addListener(SWT.Selection,  e-> tblRoutines.setAllChecked(false));
		tblRoutines.getControl().setMenu(mnuRoutines);
		
		Composite bottomPanel = new Composite(panel, SWT.NONE);
		bottomPanel.setLayout(new GridLayout(4, false));
		bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)bottomPanel.getLayout()).marginWidth = 0;
		((GridLayout)bottomPanel.getLayout()).marginHeight = 0;
		
		Hyperlink hlink = toolkit.createHyperlink(bottomPanel, Messages.ManualResultsEditor_SelectAllLabel, SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				tblRoutines.setAllChecked(true);
			}
		});
		
		Label l = toolkit.createLabel(bottomPanel, "", SWT.SEPARATOR | SWT.VERTICAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)l.getLayoutData()).heightHint = 10;
		
		hlink = toolkit.createHyperlink(bottomPanel, Messages.ManualResultsEditor_DeSelectAllLabel, SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				tblRoutines.setAllChecked(false);
			}
		});
		
		
		hlink = toolkit.createHyperlink(bottomPanel, Messages.ManualResultsEditor_refreshLink, SWT.NONE);
		hlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
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
			MessageDialog.openInformation(getSite().getShell(), Messages.ManualResultsEditor_NoRoutinesTitle, Messages.ManualResultsEditor_NoRoutinesMsg);
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
			MessageDialog.openInformation(getSite().getShell(), Messages.ManualResultsEditor_InvalidDatesTitles, Messages.ManualResultsEditor_InvalidDatesMsg);
			return;
		}
		
		lastValidationEngine = new ValidationEngine(Locale.getDefault());
		for (Object x  : tblRoutines.getCheckedElements()){
			if (x instanceof DataValidator){
				ValidationTask task = new ValidationTask(((DataValidator) x).getRoutine(), ((DataValidator) x).getDataProvider(), startDate, endDate, SmartDB.getCurrentConservationArea(), Locale.getDefault());
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
	
	Job j = new Job(Messages.ManualResultsEditor_LoadRoutinesJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<DataValidator> routines = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				List<QaRoutine> dbroutines = QueryFactory.buildQuery(s, QaRoutine.class, "conservationArea", SmartDB.getCurrentConservationArea()).getResultList(); //$NON-NLS-1$
				Collection<IQaDataProvider> providers = RoutineExtensionManager.INSTANCE.getDataProviders();
				for (IQaDataProvider p : providers){
					for (QaRoutine r : dbroutines){
						if (p.supportsRoutine(r.getRoutineType())){
							routines.add(new DataValidator(r, p, r.getRoutineType().getParameterSummary(r, Locale.getDefault(), s)));
						}
						for (QaRoutineParameter pp : r.getParameters()){
							pp.getParameterId();
							pp.getStringValue();
						}
					}
				}
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
				if (routines.size() > 0){
					tblRoutines.setSelection(new StructuredSelection(routines.get(0)));
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class DataValidator{
		
		private QaRoutine routine;
		private IQaDataProvider data;
		private String parameterDescription;
		
		public DataValidator(QaRoutine routine, IQaDataProvider data, String parameterDescription){
			this.routine = routine;
			this.data = data;
			this.parameterDescription = parameterDescription;
		}		
		public QaRoutine getRoutine(){ return routine; }
		public IQaDataProvider getDataProvider(){ return data; }
		public String getParameterDescription(){ return parameterDescription; }
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