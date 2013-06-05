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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableContainer extends Composite {

	private TableViewer viewer;
	
	List<CyberTrackerPatrol> tableData = new ArrayList<CyberTrackerPatrol>();
	
	/**
	 * @param parent
	 * @param style
	 */
	public CTPatrolTableContainer(Composite parent, int style) {
		super(parent, style);
		createControls();
	}
	
	private void createControls() {
		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = new TableViewer(this, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer.setItemCount(0);
		addColumn(viewer, "da");
		addColumn(viewer, "net");
		addColumn(viewer, "mb");

		viewer.setInput(tableData);
	}
	
	private void addColumn(TableViewer viewer, String name) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
	    column.getColumn().setText(name);
	    column.getColumn().setWidth(100);
	    column.setLabelProvider(new CTPatrolTableCellLabelProvider());
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public void addTableData(List<CyberTrackerPatrol> data) {
		tableData.addAll(data);
//		viewer.setInput(tableData);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
}
