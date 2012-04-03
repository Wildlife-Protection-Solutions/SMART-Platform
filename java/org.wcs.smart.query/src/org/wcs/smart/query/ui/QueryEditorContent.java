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
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.ui.querytable.QueryResultsTable;

/**
 * TODO Purpose of
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryEditorContent {

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

//	private Text txtQueryName;
//	private Hyperlink editQueryName;

	private QueryResultsTable resultsTable;
	private Label lblStatus;
	private ProgressBar progresBar;

	private Composite tableComp;

	private Composite progressComp;

	private Composite stackComposite;

	private Form frmQueryArea;
	private QueryResultsEditor editor;
	
	public QueryEditorContent(Composite parent, QueryResultsEditor editor) {
		createContent(parent);
		this.editor = editor;
	}

	private void initValues(WaypointQuery query) {
//		txtQueryName.setText(query.getName());
//		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
//		gd.widthHint = txtQueryName.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
//		txtQueryName.setLayoutData(gd);
		
		frmQueryArea.setText("Query: " + query.getName());
		
		if (query.getTableColumns().size() > 0){
			for (int i = 0; i < resultsTable.getColumns().length; i ++){
				if (query.getTableColumns().contains(resultsTable.getColumns()[i].getColumn())){
					resultsTable.getColumns()[i].show();
				}else{
					resultsTable.getColumns()[i].hide();
				}
				
			}
		}
	}

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
	 * Must be called from display thread.
	 */
	public void showProgressArea() {
		((StackLayout) stackComposite.getLayout()).topControl = progressComp;
		stackComposite.layout();
	}

	/**
	 * Must be called from display thread.
	 */
	public void showTable() {
		((StackLayout) stackComposite.getLayout()).topControl = tableComp;
		stackComposite.layout();
	}

	public IProgressMonitor getProgressMonitor() {

		return new IProgressMonitor() {

			String taskName = null;
			boolean isCancelled = false;

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
				// TODO Auto-generated method stub

			}

			@Override
			public void done() {
				// TODO Auto-generated method stub

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

	private void createContent(Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);

		toolkit.paintBordersFor(container);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		frmQueryArea = toolkit.createForm(container);
		frmQueryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
				1, 1));
		frmQueryArea.getBody().setLayout(new GridLayout(1, false));
		frmQueryArea.setText("Query");
//		// --- Query Name ----
//		Composite nameComp = toolkit.createComposite(frmQueryArea.getBody());
//		nameComp.setLayout(new GridLayout(3, false));
//
//		toolkit.createLabel(nameComp, "Query: ");
//		txtQueryName = toolkit.createText(nameComp, "");
//		txtQueryName.setEditable(false);
//		editQueryName = toolkit.createHyperlink(nameComp, "edit...", SWT.NONE);

		// --- Query Properties ----
		Composite queryProp = toolkit.createComposite(frmQueryArea.getBody());
		queryProp.setLayout(new GridLayout(1, false));
		queryProp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink editQueryProp = toolkit.createHyperlink(queryProp, "query properties...",SWT.NONE);
		editQueryProp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		editQueryProp.addHyperlinkListener(new IHyperlinkListener() {
			
			@Override
			public void linkExited(HyperlinkEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
				// TODO Auto-generated method stub
				
			}
			
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

}
