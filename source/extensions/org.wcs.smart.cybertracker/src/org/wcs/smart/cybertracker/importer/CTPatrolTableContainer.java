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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.swt.widgets.FileDialog;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
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
	
	private static final int HEIGHT_HINT = 300;
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
		MEMBERS(Messages.CTPatrolTableColumn_Members),
		SIGHT_COUNT(Messages.CTPatrolTableColumn_SightCount);
		
		private String guiName;
		CTPatrolTableColumn(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}

	private PatrolImporter patrolImporter;
	private PatrolLegImporter legImporter;

	private TableViewer viewer;
	private Button btnAsPatrol;
	private Button btnAsLeg;
	
	private CyberTrackerImporter importer = new CyberTrackerImporter();
	
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
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean selected = !viewer.getSelection().isEmpty();
				btnAsPatrol.setEnabled(selected);
				btnAsLeg.setEnabled(selected);
			}
		});
		
		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayout(new GridLayout(4, false));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button btnImport = new Button(buttons, SWT.NONE);
		btnImport.setText(Messages.CyberTrackerImportDialog_Button_Import);
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(false);
			}
		});
		btnImport.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, false, false));

		Button btnImportPda = new Button(buttons, SWT.NONE);
		btnImportPda.setText(Messages.CyberTrackerImportDialog_Button_ImportFromDevice);
		btnImportPda.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(true);
			}
		});
		btnImportPda.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false));
		
		btnAsPatrol = new Button(buttons, SWT.NONE);
		btnAsPatrol.setText(Messages.CTPatrolTableContainer_Button_AsPatrol);
		btnAsPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsPatrol();
			}
		});
		btnAsPatrol.setLayoutData(new GridData(SWT.TRAIL, SWT.CENTER, false, false));
		btnAsPatrol.setEnabled(false);

		btnAsLeg = new Button(buttons, SWT.NONE);
		btnAsLeg.setText(Messages.CTPatrolTableContainer_Button_AsLeg);
		btnAsLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsLeg();
			}
		});
		btnAsLeg.setLayoutData(new GridData(SWT.TRAIL, SWT.CENTER, false, false));
		btnAsLeg.setEnabled(false);
	}

	private void performImport(final boolean fromPda) {
		File dialogFile = null;
		if (!fromPda) {
			dialogFile = selectFile();
			if (dialogFile == null)
				return;
			if (!dialogFile.exists()) {
				MessageDialog.openError(getShell(), Messages.CyberTrackerImportDialog_Error_Title, Messages.CyberTrackerImportDialog_Error_Message);
				return;
			}
		}
		
		final File file = dialogFile;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, 100);
					try {
						List<CyberTrackerPatrol> data = fromPda ? importer.importPdaData(monitor) : importer.importData(file, monitor);
						addTableData(data);
					} catch (Exception e) {
//						displayError("Error", "Error occured while importing data from CyberTracker into SMART.");
						e.printStackTrace();
						return;
					}
//					displayInfo("CyberTracker Import", "Import successfully completed.");
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	protected void handleAddAsPatrol() {
		if (patrolImporter == null)
			patrolImporter = new PatrolImporter();
		final List<Patrol> addedList = new ArrayList<Patrol>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		try {
			final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					int patrolsCount = selection.size();
					int counter = 1;
					monitor.beginTask(Messages.CTPatrolTableContainer_AddPatrol_TaskName, patrolsCount);
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						monitor.subTask(MessageFormat.format(Messages.CTPatrolTableContainer_AddPatrol_SubTaskName, counter, patrolsCount));
						CyberTrackerPatrol ctp = (CyberTrackerPatrol) i.next();
						Patrol p = patrolImporter.importData(ctp);
						if (p != null) {
							addedList.add(p);
							tableInputData.remove(ctp);
						}
						monitor.worked(1);
						counter++;
					}
					monitor.done();
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.CTPatrolTableContainer_Patrol_Error, e);
			e.printStackTrace();
		}
		
		if (!addedList.isEmpty()) {
			String ids = ""; //$NON-NLS-1$
			for (Iterator<Patrol> i = addedList.iterator(); i.hasNext();) {
				Patrol p = i.next();
				ids += p.getId();
				if (i.hasNext())
					ids += ", "; //$NON-NLS-1$
			}
			MessageDialog.openInformation(getShell(), Messages.CTPatrolTableContainer_Patrol_Success_Title, MessageFormat.format(Messages.CTPatrolTableContainer_Patrol_Success_Message, ids)); 
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

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
		try {
			final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			final Patrol patrol = selectorDialog.getSelectedPatrol();
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					int legsCount = selection.size();
					int counter = 1;
					monitor.beginTask(MessageFormat.format(Messages.CTPatrolTableContainer_AddLeg_TaskName, patrol.getId()), legsCount);
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						monitor.subTask(MessageFormat.format(Messages.CTPatrolTableContainer_AddLeg_SubTaskName, counter, legsCount));
						CyberTrackerPatrol ctp = (CyberTrackerPatrol) i.next();
						legImporter.importData(patrol, ctp);
						tableInputData.remove(ctp);
						monitor.worked(1);
						counter++;
					}
					monitor.done();
					CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_Leg_Success_Title, Messages.CTPatrolTableContainer_Leg_Success_Message);
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.CTPatrolTableContainer_Leg_Error, e);
			e.printStackTrace();
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

	private File selectFile() {
		FileDialog fd = new FileDialog(getShell(), SWT.MULTI | SWT.OPEN);
		fd.setFilterExtensions(new String[]{"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[]{Messages.CyberTrackerImportDialog_XmlFiles, Messages.CyberTrackerImportDialog_AllFiles});
		String f = fd.open();
		if (f != null) {
			return new File(f);
		}
		return null;
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
