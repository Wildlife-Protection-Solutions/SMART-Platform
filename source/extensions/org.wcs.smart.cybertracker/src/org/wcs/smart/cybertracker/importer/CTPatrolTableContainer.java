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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Container for table containing imported data from CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableContainer extends Composite {
	
	private static final int HEIGHT_HINT = 250;
	private static final int COLUMN_WIDTH = 80;
	
	/**
	 * The supported patrol types.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	public enum CTPatrolTableColumn {
		START_DATE(Messages.CTPatrolTableColumn_StartDate),
		END_DATE(Messages.CTPatrolTableColumn_EndDate),
		TYPE(Messages.CTPatrolTableColumn_Type),
		TRANSPORT(Messages.CTPatrolTableColumn_Transport),
		ARMED(Messages.CTPatrolTableColumn_Armed),
		MANDATE(Messages.CTPatrolTableColumn_Mandate),
		TEAM(Messages.CTPatrolTableColumn_Team),
		STATION(Messages.CTPatrolTableColumn_Station),
		OBJECTIVE(Messages.CTPatrolTableColumn_Objective),
		COMMENT(Messages.CTPatrolTableColumn_Comment),
		LEADER(Messages.CTPatrolTableColumn_Leader),
		PILOT(Messages.CTPatrolTableColumn_Pilot),
		MEMBERS(Messages.CTPatrolTableColumn_Members);
		
		private String guiName;
		CTPatrolTableColumn(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	private TableViewer viewer;
	private PatrolImporter patrolImporter;
	private PatrolLegImporter legImporter;
	
	private List<CyberTrackerPatrol> tableInputData = new ArrayList<CyberTrackerPatrol>();
	
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
		btnAsPatrol.setText(Messages.CTPatrolTableContainer_Button_AsPatrol);
		btnAsPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsPatrol();
			}
		});
		btnAsPatrol.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		Button btnAsLeg = new Button(buttons, SWT.NONE);
		btnAsLeg.setText(Messages.CTPatrolTableContainer_Button_AsLeg);
		btnAsLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsLeg();
			}
		});
		btnAsLeg.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
	}
	
	protected void handleAddAsPatrol() {
		if (patrolImporter == null)
			patrolImporter = new PatrolImporter();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		try {
			final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						CyberTrackerPatrol ctp = (CyberTrackerPatrol) i.next();
						patrolImporter.importData(ctp);
						tableInputData.remove(ctp);
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), "Save patrol operation was aborted", e);
			e.printStackTrace();
		}
		
		refreshViewer();
	}

	protected void handleAddAsLeg() {
		PatrolSelectorDialog selectorDialog = new PatrolSelectorDialog(getShell());
		if (selectorDialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		if (legImporter == null)
			legImporter = new PatrolLegImporter();
		//TODO: implement

		final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
		Patrol patrol = selectorDialog.getSelectedPatrol();
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			CyberTrackerPatrol ctp = (CyberTrackerPatrol) i.next();
			legImporter.importData(patrol, ctp);
			tableInputData.remove(ctp);
		}
		
		refreshViewer();
	}
	
	private void addColumns(TableViewer viewer) {
		for (CTPatrolTableColumn column : CTPatrolTableColumn.values()) {
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			viewerColumn.getColumn().setText(column.getGuiName());
			viewerColumn.getColumn().setWidth(COLUMN_WIDTH);
			viewerColumn.setLabelProvider(new CTPatrolTableCellLabelProvider(column));
		}
	}

	public TableViewer getViewer() {
		return viewer;
	}

	public void addTableData(List<CyberTrackerPatrol> data) {
		tableInputData.addAll(data);
		refreshViewer();
	}
	
	private void refreshViewer() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
}
