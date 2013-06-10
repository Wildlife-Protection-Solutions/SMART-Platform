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
package org.wcs.smart.cybertracker.importer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Container for table containing imported data from CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableContainer extends Composite {
	
	private static final int HEIGHT_HINT = 250;
	
	/**
	 * The supported patrol types.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	public enum CTPatrolTableColumn {
		TYPE("Type"),
		TRANSPORT("Transport"),
		ARMED("Armed"),
		MANDATE("Mandate"),
		TEAM("Team"),
		STATION("Station"),
		OBJECTIVE("Objective"),
		COMMENT("Comment");
		
		private String guiName;
		CTPatrolTableColumn(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	private TableViewer viewer;
	
	List<CyberTrackerPatrol> tableInputData = new ArrayList<CyberTrackerPatrol>();
	
	/**
	 * @param parent
	 * @param style
	 */
	public CTPatrolTableContainer(Composite parent, int style) {
		super(parent, style);
		createControls();
	}
	
	private void createControls() {
		GridLayout layout = new GridLayout();
		this.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = HEIGHT_HINT;
		this.setLayoutData(gd);
		
		viewer = new TableViewer(this, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLayoutData(gd);
		
		viewer.setItemCount(0);
		addColumns(viewer);
		viewer.setInput(tableInputData);
		
		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayout(new GridLayout(2, false));
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		
		Button btnAsPatrol = new Button(buttons, SWT.NONE);
		btnAsPatrol.setText("Add As New Patrol");
		btnAsPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: implement
			}
		});
		btnAsPatrol.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		Button btnAsLeg = new Button(buttons, SWT.NONE);
		btnAsLeg.setText("Add As New Leg");
		btnAsLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: implement
			}
		});
		btnAsLeg.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
	}
	
	private void addColumns(TableViewer viewer) {
		for (CTPatrolTableColumn column : CTPatrolTableColumn.values()) {
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			viewerColumn.getColumn().setText(column.getGuiName());
			viewerColumn.getColumn().setWidth(100);
			viewerColumn.setLabelProvider(new CTPatrolTableCellLabelProvider(column));
		}
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public void addTableData(List<CyberTrackerPatrol> data) {
		tableInputData.addAll(data);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
}
