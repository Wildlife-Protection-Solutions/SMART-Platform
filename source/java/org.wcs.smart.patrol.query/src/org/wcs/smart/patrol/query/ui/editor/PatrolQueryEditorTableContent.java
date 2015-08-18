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
package org.wcs.smart.patrol.query.ui.editor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.types.PatrolQueryType;
import org.wcs.smart.patrol.query.ui.querytable.PatrolTableColumn;
import org.wcs.smart.query.common.ui.QueryResultsTable;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.ProgressAreaComposite;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;

/**
 * A class that manages a table that display 
 * the tabular results of a patrol query.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolQueryEditorTableContent {

	private QueryDateFilterComposite dateComposite;
	private ProgressAreaComposite progressComp;
	
	private QueryResultsTable resultsTable;
	private Composite runQueryComp;
	private Composite tableComp;;
	private Composite stackComposite;
	private Form frmQueryArea;
	private PatrolQueryResultsEditor editor;
	
	private Hyperlink runQueryLink;
	private QueryHeaderComposite compQueryName;
	private Label lblNumResults;
	private Label lblNumPatrols;
	
	/**
	 * Creates a new editor area
	 * @param parent parent composite
	 * @param editor parent editor 
	 * @param toolkit page formtoolkit
	 */
	public PatrolQueryEditorTableContent(Composite parent, PatrolQueryResultsEditor editor, FormToolkit toolkit) {
		createContent(parent, toolkit);
		this.editor = editor;
	}

	/**
	 * Initializes the form values with the query information
	 * @param query the patrol query to initialize data with
	 */
	public void initValues(PatrolQuery query) {
		updateName(query);
		resultsTable.initQuery(query);
		resultsTable.updateVisible(query.getQueryColumns(Locale.getDefault(), null));
	}

	/**
	 * Updates the patrol query name
	 * @param query
	 */
	public void updateName(PatrolQuery query){
		compQueryName.setText(query.getName(), query.getId());
	}
	/**
	 * @return the date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateComposite.getDateFilter();
	}
	
	/**
	 * validates the date filter
	 */
	public void validate(){
		dateComposite.validate();
	}
	
	/**
	 * Updates the results table data.  This
	 * runs in the display thread.
	 * 
	 * @param items new results
	 */
	public void setTableData(final Collection<PatrolQueryResultItem> items) {
		
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (tableComp.isDisposed()){
					//window closed nothing to update
					return;
				}
				if (items == null ){
					showCancelled();
				}else{
					lblNumResults.setText(String.valueOf(items.size()));
					lblNumPatrols.setText(String.valueOf(computeNumberOfPatrols(items)));
					lblNumResults.getParent().getParent().layout();
					resultsTable.setInput(items);
					showTable();
				}
			}
		});
	}
	
	private int computeNumberOfPatrols(Collection<PatrolQueryResultItem> items){
		HashSet<Integer> keys = new HashSet<Integer>();
		int cnt = 0;
		for (PatrolQueryResultItem it : items){
			int key = it.getPatrolUuid().hashCode();
			if (!keys.contains(key)){
				cnt++;
				keys.add(key);
			}
		}
		return cnt;
	}

	/**
	 * Sets the state of the progress area to cancelled
	 * which disables the progress bar, cancel button and
	 * updates the text to "cancelled"
	 */
	private void showCancelled(){
		showProgressArea();
		progressComp.showCancelled();
	}
	
	/**
	 * Shows the progress bar and message area and hides
	 * the table area.  Enables all progress elements.
	 * <p>
	 * Must be called from display thread.
	 * </p>
	 */
	public void showProgressArea() {
		((StackLayout) stackComposite.getLayout()).topControl = progressComp;
		stackComposite.layout();
		
		progressComp.setEnabled(true);
	}

	/**
	 * Shows the results table & hides the progress area.
	 * <p>
	 * Must be called from display thread.
	 * </p>
	 */
	public void showTable() {
		((StackLayout) stackComposite.getLayout()).topControl = tableComp;
		stackComposite.layout();
	}

	/**
	 * @return creates a new progress monitor
	 * for displaying the progress in the progress
	 * area.
	 */
	public IProgressMonitor createProgressMonitor(){
		return progressComp.createProgressMonitor();
	}
		
	private void createNameHeader(Composite main, FormToolkit toolkit) {
		compQueryName = new QueryHeaderComposite(main,Messages.PatrolQueryEditorTableContent_PatrolQueryLabel, 
				toolkit, frmQueryArea.getFont(), frmQueryArea.getForeground());
		compQueryName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		compQueryName.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				editor.getQuery().setName(event.text);
				editor.setDirty(true);
			}});
	}
	
	/**
	 * Creates the main content area 
	 * @param parent
	 */
	private void createContent(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		frmQueryArea = toolkit.createForm(container);
		frmQueryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,1, 1));
		
		layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		frmQueryArea.getBody().setLayout(layout);
		
		
		createNameHeader(frmQueryArea.getBody(), toolkit);
		
		// --- Query Properties ----
		Composite queryProp = toolkit.createComposite(frmQueryArea.getBody(), SWT.NONE);
		
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginRight = 5;
		layout.marginLeft = 5;
		queryProp.setLayout(layout);
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateComposite = new QueryDateFilterComposite(queryProp, PatrolQueryType.validDateFields(), IDateFilter.DATE_FILTERS);
		dateComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));

		dateComposite.adapt(toolkit);
		
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, Messages.PatrolQueryEditorTableContent_QueryPropertiesLabel,SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		editQueryProp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				QueryPropertiesDialog dialog = new QueryPropertiesDialog(editor.getSite().getShell(), editor.getQuery());
				if (dialog.open() == Window.OK){
					initValues(editor.getQueryInternal());
					editor.setDirty(true);
				}
			}
		});
		
		// --- Stack Panel ---
		// Here we either show the table or the progress dialog
		stackComposite = toolkit.createComposite(frmQueryArea.getBody());
		stackComposite.setLayout(new StackLayout());
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		runQueryComp = createRunQueryComp(stackComposite, toolkit);
		tableComp = createTableResultsComposite(stackComposite, toolkit);
		progressComp = new ProgressAreaComposite(stackComposite);
		progressComp.adapt(toolkit);
		((StackLayout) stackComposite.getLayout()).topControl = runQueryComp;		
	}

	/**
	 * Creates the results table widget
	 * @param parent
	 * @return
	 */
	private Composite createTableResultsComposite(Composite parent, FormToolkit toolkit) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(1, false));
		
		Composite comp = toolkit.createComposite(main);
		GridLayout layout = new GridLayout(7,false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		comp.setLayout(layout);
		
		toolkit.createLabel(comp,  Messages.PatrolQueryEditorTableContent_NumberOfPatrolsLabel);
		lblNumPatrols = toolkit.createLabel(comp, Messages.PatrolQueryEditorTableContent_NALabel);
		
		toolkit.createLabel(comp,  "  "); //$NON-NLS-1$
		Label l = toolkit.createLabel(comp, "", SWT.SEPARATOR | SWT.VERTICAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		((GridData)l.getLayoutData()).heightHint = 20;
		toolkit.createLabel(comp,  "  "); //$NON-NLS-1$
		
		toolkit.createLabel(comp,  Messages.PatrolQueryEditorTableContent_NumberofRecordsLabel);
		lblNumResults = toolkit.createLabel(comp, Messages.PatrolQueryEditorTableContent_NALabel);
	
		resultsTable = new QueryResultsTable(){
			@Override
			public CellLabelProvider getLabelProvider(QueryColumn column) {
				return PatrolTableColumn.getLabelProvider(column);
			}
		};

		TableViewer viewer = resultsTable.createTable(main);
		viewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(viewer.getTable());

		return main;
	}
	
	/**
	 * Creates the initial composite that prompts users
	 * to run query.
	 * 
	 * @param parent
	 * @return
	 */
	private Composite createRunQueryComp(Composite parent, FormToolkit toolkit){
		Composite main = toolkit.createComposite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		runQueryLink = toolkit.createHyperlink(main, Messages.PatrolQueryEditorTableContent_RunQueryLink, SWT.NONE);
		runQueryLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				editor.refreshQuery();
			}
		});
		return main;
	}
	
	/**
	 * @return the results table
	 */
	public QueryResultsTable getQueryResultsTable() {
		return resultsTable;
	}
	
	public void setFocus(){
		runQueryLink.setFocus();
	}

}
