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
package org.wcs.smart.query.common.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.ProgressAreaComposite;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.QueryHeaderComposite;
import org.wcs.smart.query.ui.QueryPropertiesDialog;


/**
 * A class that manages a table that display 
 * the tabular results of a observaton query.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEditorTableContent {

	private QueryDateFilterComposite dateComposite;
	private ProgressAreaComposite progressComp;
	
	private QueryLazyResultsTable resultsTable;
	private Composite runQueryComp;
	private Composite tableComp;
	private Composite stackComposite;
	private Form frmQueryArea;
	private QueryResultsEditor editor;
	
	private Hyperlink runQueryLink;
	private QueryHeaderComposite compQueryName;
	private ISummaryInfo infoSection;
	/**
	 * Creates a new editor area
	 * @param parent parent composite
	 * @param editor parent editor 
	 */
	public QueryEditorTableContent(Composite parent, QueryResultsEditor editor, FormToolkit toolkit) {
		this.editor = editor;
		createContent(parent, toolkit);
	}

	/**
	 * Initializes the form values with the query information
	 * @param query the observation query to initialize data with
	 */
	public void initValues(SimpleQuery query) {
		if (stackComposite.isDisposed()) return;
		setQueryName(query);
		resultsTable.initQuery(query, editor);
	}

	/**
	 * Validates the date composite.
	 */
	public void validate(){
		this.dateComposite.validate();
	}
	
	public void setQueryName(SimpleQuery query){
		compQueryName.setText(query.getName(), query.getId(),
				QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey()).getGuiName());
	}
	/**
	 * @return the date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateComposite.getDateFilter();
	}
	
	/**
	 * Updates the results table data.  This
	 * runs in the display thread.
	 * 
	 * @param items new results; null if query cancelled and nothing to display
	 */
	public void setTableData(final IPagedQueryResultSet items) {
		if (tableComp.isDisposed()) return;
		tableComp.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (tableComp.isDisposed()){
					//window closed nothing to update
					return;
				}
				if (items == null) {
					showCancelled();
				} else {
					if (infoSection != null){
						infoSection.updateControls(items);
					}
					resultsTable.setInput(items);
					resultsTable.updateColumnsVisibility(editor.getQueryInternal(), editor);
					showTable();
				}
			}
		});
	}
	
	/**
	 * Sets table input and info section to null 
	 */
	public void clear(){
		resultsTable.setInput((IPagedQueryResultSet)null);
		infoSection.updateControls(null);
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
		compQueryName = new QueryHeaderComposite(main, toolkit, frmQueryArea.getFont(), frmQueryArea.getForeground());
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
		
		IQueryType type = QueryTypeManager.INSTANCE.findQueryType(editor.getQuery().getTypeKey());
		dateComposite = new QueryDateFilterComposite(queryProp, type.getDateFilterOptions(), IDateFilter.DATE_FILTERS);
		dateComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		dateComposite.adapt(toolkit);
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, Messages.QueryEditorTableContent_QueryPropertiesLable,SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		editQueryProp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				QueryPropertiesDialog dialog = new QueryPropertiesDialog(editor.getSite().getShell(), editor.getQuery());
				if (dialog.open() == Window.OK){
					editor.queryPropertiesChange();
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
		
		int size = 1;
		if (editor.canEditResults()) size = 2;
		main.setLayout(new GridLayout(size, false));
		
		infoSection = createInfoSection();
		if (infoSection != null){
			Composite comp = toolkit.createComposite(main);
			infoSection.createControls(comp, toolkit);
			comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		if (editor.canEditResults()){
			Hyperlink hl = toolkit.createHyperlink(main, "edit results", SWT.NONE);
			hl.setData(Boolean.FALSE);
			hl.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					hl.setData(!(Boolean)hl.getData());
					editor.setEditMode((boolean)hl.getData());
					if ((Boolean)hl.getData()){
						hl.setText("exit edit mode");
					}else{
						hl.setText("edit results");
					}
					hl.getParent().layout(true);
					resultsTable.setEditMode(editor.getEditMode());
					
				}
			});
		}
		
		
		resultsTable = new QueryLazyResultsTable(){
			@Override
			public CellLabelProvider getLabelProvider(QueryColumn column, List<QueryColumn> allColumns) {
				return editor.getColumnLabelProvider(column, allColumns);
			}
			
			@Override
			public EditingSupport getEditingSupport(ColumnViewer viewer, QueryColumn column) {
				return editor.getEditingSupport(viewer, column);
			}
		};

		TableViewer viewer = resultsTable.createTable(main);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, size, 1));
		toolkit.adapt(viewer.getTable());

		editor.getSite().setSelectionProvider(viewer);
		
		return main;
	}
	
	private ISummaryInfo createInfoSection(){
		return editor.createInfoSection();
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
		
		runQueryLink = toolkit.createHyperlink(main, Messages.QueryEditorTableContent_RunQueryLink, SWT.NONE);
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
	public QueryLazyResultsTable getQueryResultsTable() {
		return resultsTable;
	}
	
	/**
	 * Sets the focus on the run query link
	 */
	public void setFocus() {
		runQueryLink.setFocus();
	}
	
	
}
