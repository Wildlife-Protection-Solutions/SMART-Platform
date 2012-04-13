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
package org.wcs.smart.query.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.parser.internal.DateFilter;
import org.wcs.smart.query.ui.querytable.QueryResultsTable;

/**
 * A class that manages the tabular query editor
 * results page.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEditorTableContent {

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private QueryDateFilterComposite dateComposite;
	private QueryResultsTable resultsTable;
	private Label lblStatus;
	private ProgressBar progresBar;
	private Composite tableComp;
	private Composite progressComp;
	private Composite stackComposite;
	private Form frmQueryArea;
	private QueryResultsEditor editor;
	
	/**
	 * Creates a new editor area
	 * @param parent parent composite
	 * @param editor parent editor 
	 */
	public QueryEditorTableContent(Composite parent, QueryResultsEditor editor) {
		createContent(parent);
		this.editor = editor;
	}

	/**
	 * Initializes the form values with the query information
	 * @param query the waypoint query to initialize data with
	 */
	private void initValues(WaypointQuery query) {
		frmQueryArea.setText("Query: " + query.getName());
		resultsTable.updateVisible(query.getVisibleColumns());
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
	 * @param items new results
	 */
	public void setTableData(final List<QueryResultItem> items) {
		
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				resultsTable.setInput(items);
				showTable();
			}
		});
	}

	/**
	 * Shows the progress bar and message area and hides
	 * the table area.
	 * <p>
	 * Must be called from display thread.
	 * </p>
	 */
	public void showProgressArea() {
		((StackLayout) stackComposite.getLayout()).topControl = progressComp;
		stackComposite.layout();
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
	 * @return a progress monitor that displays the progress
	 * in the progress bar and progress message area.
	 */
	public IProgressMonitor getProgressMonitor() {

		return new IProgressMonitor() {
			private String taskName = null;
			private boolean isCancelled = false;
			@Override
			public void worked(final int work) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						int newValue = progresBar.getSelection() + work;
						if (newValue > progresBar.getMaximum()) {
							newValue = progresBar.getMaximum();
						}
						if (newValue < progresBar.getMinimum()) {
							newValue = progresBar.getMinimum();
						}
						progresBar.setSelection(newValue);
					}
				});
			}

			@Override
			public void subTask(final String name) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						lblStatus.setText(taskName + " - " + name);
					}
				});
			}

			@Override
			public void setTaskName(String name) {
				this.taskName = name;
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						lblStatus.setText(taskName);
					}
				});
			}

			@Override
			public void setCanceled(boolean value) {
				this.isCancelled = value;
			}

			@Override
			public boolean isCanceled() {
				return isCancelled;
			}

			@Override
			public void internalWorked(double work) {
			}

			@Override
			public void done() {
			}

			@Override
			public void beginTask(final String name, final int totalWork) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						progresBar.setMinimum(0);
						progresBar.setMaximum(totalWork);
						progresBar.setSelection(0);
						setTaskName(name);

					}
				});

			}
		};
	}

	
	
	/**
	 * Creates the main content area 
	 * @param parent
	 */
	private void createContent(Composite parent) {
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
		frmQueryArea.setText("Query");

		// --- Query Properties ----
		Composite queryProp = toolkit.createComposite(frmQueryArea.getBody(), SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginRight = 5;
		layout.marginLeft = 5;
		queryProp.setLayout(layout);
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		dateComposite = new QueryDateFilterComposite(queryProp);
		dateComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true));
		dateComposite.adapt(toolkit);
		toolkit.adapt(dateComposite);
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, "query properties...",SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		editQueryProp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				QueryPropertiesDialog dialog = new QueryPropertiesDialog(editor.getSite().getShell(), editor.getQuery(), resultsTable.getColumns());
				if (dialog.open() == Window.OK){
					initValues(editor.getQuery());
				}
			}
		});
		
		// --- Stack Panel ---
		// Here we either show the table or the progress dialog
		stackComposite = toolkit.createComposite(frmQueryArea.getBody());
		stackComposite.setLayout(new StackLayout());
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		tableComp = createTableResultsComposite(stackComposite);
		progressComp = createProcessingComposite(stackComposite);
		((StackLayout) stackComposite.getLayout()).topControl = tableComp;
	}

	/**
	 * Creates the results table widget
	 * @param parent
	 * @return
	 */
	private Composite createTableResultsComposite(Composite parent) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(1, false));
		resultsTable = new QueryResultsTable();

		TableViewer viewer = resultsTable.createTable(main);
		viewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(viewer.getTable());

		return main;
	}

	/**
	 * Creates the status area widget
	 * @param parent
	 * @return
	 */
	private Composite createProcessingComposite(Composite parent) {
		Composite main = toolkit.createComposite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progresBar = new ProgressBar(main, SWT.SMOOTH | SWT.HORIZONTAL);
		toolkit.adapt(progresBar, false, false);
		progresBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblStatus = toolkit.createLabel(main, "");
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return main;
	}

	/**
	 * @return the results table
	 */
	public QueryResultsTable getQueryResultsTable() {
		return resultsTable;
	}

}
