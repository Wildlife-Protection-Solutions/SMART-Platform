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
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.ErrorType;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.ImportError;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Container for table containing imported data from CyberTracker application
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableContainer extends Composite {
	
	private static final int HEIGHT_HINT = 300;
	private static final int DEFAULT_COLUMN_WIDTH = 80;
	
	/**
	 * The supported patrol types.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	public enum CTPatrolTableColumn {
		IMPORT_NOTE ("",	20), //$NON-NLS-1$
		START_DATE	(Messages.CTPatrolTableColumn_StartDate,	120),
		END_DATE	(Messages.CTPatrolTableColumn_EndDate,		120),
		TYPE		(Messages.CTPatrolTableColumn_Type),
		TRANSPORT	(Messages.CTPatrolTableColumn_Transport),
		ARMED		(Messages.CTPatrolTableColumn_Armed, 		50),
//		MANDATE		(Messages.CTPatrolTableColumn_Mandate),
		TEAM		(Messages.CTPatrolTableColumn_Team),
		STATION		(Messages.CTPatrolTableColumn_Station),
//		OBJECTIVE	(Messages.CTPatrolTableColumn_Objective),
		COMMENT		(Messages.CTPatrolTableColumn_Comment),
		LEADER		(Messages.CTPatrolTableColumn_Leader),
		PILOT		(Messages.CTPatrolTableColumn_Pilot),
		MEMBERS		(Messages.CTPatrolTableColumn_Members),
		SIGHT_COUNT	(Messages.CTPatrolTableColumn_SightCount, 	50);
		
		private String guiName;
		private int width;
		CTPatrolTableColumn(String guiName) {
			this(guiName, DEFAULT_COLUMN_WIDTH);
		}
		CTPatrolTableColumn(String guiName, int width) {
			this.guiName = guiName;
			this.width = width;
		}
		public String getGuiName() {
			return this.guiName;
		}
		public int getWidth() {
			return width;
		}
	}

	private PatrolImporter patrolImporter;
	private PatrolLegImporter legImporter;

	private TableViewer viewer;
	private Button btnAsPatrol;
	private Button btnAsLeg;
	private Button btnRemove;
	
	private CyberTrackerImporter importer = new CyberTrackerImporter();
	
	private List<CyberTrackerPatrol> tableInputData = new ArrayList<CyberTrackerPatrol>();
	
	private Text lblStartDate;
	private Text lblEndDate;
	private Text lblPatrolType;
	private Text lblTransportType;
	private Text lblArmed;
	private Text lblTeam;
	private Text lblStation;
	private Text lblMembers;
	private Text lblMandate;
	private Text lblObjective;
	private Text lblComment;
	private Text lblLeader;
	private Text lblPilot;
	
	private ControlDecoration cdStartDate;
	private ControlDecoration cdEndDate;
	private ControlDecoration cdPatrolType;
	private ControlDecoration cdTransportType;
	private ControlDecoration cdArmed;
	private ControlDecoration cdTeam;
	private ControlDecoration cdStation;
	private ControlDecoration cdMembers;
	private ControlDecoration cdMandate;
	private ControlDecoration cdObjective;
	private ControlDecoration cdComment;
	private ControlDecoration cdLeader;
	private ControlDecoration cdPilot;	
	
	/**
	 * @param parent
	 * @param style
	 */
	public CTPatrolTableContainer(Composite parent, int style, FormToolkit toolkit) {
		super(parent, style);
		createControls(toolkit);
	}
	
	private void createControls(FormToolkit toolkit) {
		toolkit.adapt(this);
		
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		this.setLayout(layout);
		
		Section data = toolkit.createSection(this, Section.TITLE_BAR | Section.EXPANDED);
		layout = new GridLayout();
		data.setLayout(layout);
		data.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		data.setText(Messages.CTPatrolTableContainer_PatrolDataSectionHeader);
		
		Composite dataComp = toolkit.createComposite(data);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		dataComp.setLayout(layout);
		dataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		data.setClient(dataComp);
		
		/* Import Data */		
		Composite buttons = toolkit.createComposite(dataComp);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button btnImport = toolkit.createButton(buttons, Messages.CyberTrackerImportDialog_Button_Import, SWT.PUSH);
		btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(false);
			}
		});

		Button btnImportPda = toolkit.createButton(buttons, Messages.CyberTrackerImportDialog_Button_ImportFromDevice, SWT.PUSH);
		btnImportPda.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnImportPda.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(true);
			}
		});
		
		viewer = new TableViewer(dataComp, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.paintBordersFor(viewer.getTable());
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = HEIGHT_HINT;
		viewer.getTable().setLayoutData(gd);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		
		viewer.setItemCount(0);
		addColumns(viewer);
		viewer.setInput(tableInputData);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean selected = !viewer.getSelection().isEmpty();
				btnAsPatrol.setEnabled(selected);
				btnAsLeg.setEnabled(selected);
				btnRemove.setEnabled(selected);
				if (selected){
					updatePatrolDetails(((IStructuredSelection)viewer.getSelection()).getFirstElement());
				}else{
					updatePatrolDetails(null);
				}
			}
		});
		final MenuManager mgr = new MenuManager();
		Action asPatrolAction = new Action(Messages.CTPatrolTableContainer_Button_AsPatrol){
			@Override
			public void run() {
				handleAddAsPatrol();
			}
		};
		Action asLegAction = new Action(Messages.CTPatrolTableContainer_Button_AsLeg){
			@Override
			public void run() {
				handleAddAsLeg();
			}
		};
		Action deleteAction = new Action(DialogConstants.DELETE_BUTTON_TEXT){
			@Override
			public void run() {
				handleRemove();
			}
		};
		mgr.add(asPatrolAction);
		mgr.add(asLegAction);
		mgr.add(deleteAction);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				updateContextMenu(viewer.getControl().getMenu(), !viewer.getSelection().isEmpty());
			}
		});
		viewer.getControl().setMenu(mgr.createContextMenu(viewer.getControl()));
		mgr.updateAll(true);
		
		
		/* Import Button Panel */
		buttons = new Composite(dataComp, SWT.NONE);
		buttons.setLayout(new GridLayout(3, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAsPatrol = toolkit.createButton(buttons,Messages.CTPatrolTableContainer_Button_AsPatrol, SWT.PUSH);
		btnAsPatrol.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsPatrol();
			}
		});
		btnAsPatrol.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAsPatrol.setEnabled(false);

		btnAsLeg = toolkit.createButton(buttons,Messages.CTPatrolTableContainer_Button_AsLeg,SWT.PUSH);
		btnAsLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddAsLeg();
			}
		});
		btnAsLeg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAsLeg.setEnabled(false);

		btnRemove = toolkit.createButton(buttons,DialogConstants.DELETE_BUTTON_TEXT,SWT.PUSH);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemove.setEnabled(false);
		
		/* Patrol Details Section */
		final Section details = toolkit.createSection(this, Section.TITLE_BAR | Section.COMPACT | Section.TWISTIE);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		details.setText(Messages.CTPatrolTableContainer_PatrolDetailsSectionHeader);
		details.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (details.isExpanded()){
					details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
				}else{
					details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				details.getParent().layout(true, true);
			}
		});
		details.setClient(createPatrolDetailsComposite(details, toolkit));
	}

	private void updateContextMenu(Menu menu, boolean enabled) {
		if (menu != null) {
			for (MenuItem item : menu.getItems()) {
				item.setEnabled(enabled);
			}
		}
	}
	
	private void performImport(final boolean fromPda) {
		File[] dialogFiles = null;
		if (!fromPda) {
			dialogFiles = selectFile();
			if (dialogFiles == null) return;
		}
		
		final File[] files = dialogFiles;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, 1);
					List<CyberTrackerPatrol> data;
					int code = 0;
					try {
						if (fromPda) {
							CyberTrackerImportResult result = importer.importPdaData(monitor);
							data = result.getData();
							code = result.getReturnCode();
						}else{
							monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, files.length);
							data = new ArrayList<CyberTrackerPatrol>();
							for (int i = 0; i < files.length; i ++){
								File currentFile = files[i];
								if (!currentFile.exists()){
									final File fcurrentFile = currentFile;
									getShell().getDisplay().syncExec(new Runnable(){
										@Override
										public void run() {
											MessageDialog.openError(getShell(), Messages.CyberTrackerImportDialog_Error_Title,MessageFormat.format( Messages.CyberTrackerImportDialog_Error_Message, new Object[]{fcurrentFile.toString()}));
										}});
														
								}else{
									data.addAll(importer.importFileData(files[i], new SubProgressMonitor(monitor,1)));
								}
								monitor.worked(1);
							}
						}
						addTableData(data);
					} catch (Exception e) {
						CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title, MessageFormat.format(Messages.CTPatrolTableContainer_ImportError_Message, e.getMessage()), e);
						return;
					}
					int count = data != null ? data.size() : 0;
					if (count > 0) {
						CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_InfoDialog_Title, MessageFormat.format(Messages.CTPatrolTableContainer_ImportCompleted, count));
					} else {
						CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_InfoDialog_Title, getEmptyDownloadMessage(code));
					}
				}

			});
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title, Messages.CTPatrolTableContainer_Error_ImportAborted, e);
		}

	}

	private String getEmptyDownloadMessage(int code) {
		switch (code) {
		case ICyberTrackerConstants.DOWNLOAD_CODE_NO_CONNECTION:
			return Messages.CTPatrolTableContainer_DownloadCode_300;
		case ICyberTrackerConstants.DOWNLOAD_CODE_NO_DATA:
			return Messages.CTPatrolTableContainer_DownloadCode_301;
		case ICyberTrackerConstants.DOWNLOAD_CODE_SUCCESS:
			return Messages.CTPatrolTableContainer_DownloadCode_302;
		default:
			return Messages.CTPatrolTableContainer_NoDataImported;
		}
	}
	
	protected void handleAddAsPatrol() {
		if (patrolImporter == null)
			patrolImporter = new PatrolImporter();
		final List<Patrol> addedList = new ArrayList<Patrol>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell().getDisplay().getActiveShell());
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
			CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title,Messages.CTPatrolTableContainer_Patrol_Error, e);
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

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			final StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			final Patrol patrol = selectorDialog.getSelectedPatrol();
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					int legsCount = selection.size();
					int counter = 1;
					int successCount = 0;
					monitor.beginTask(MessageFormat.format(Messages.CTPatrolTableContainer_AddLeg_TaskName, patrol.getId()), legsCount);
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						monitor.subTask(MessageFormat.format(Messages.CTPatrolTableContainer_AddLeg_SubTaskName, counter, legsCount));
						CyberTrackerPatrol ctp = (CyberTrackerPatrol) i.next();
						if (legImporter.importData(patrol, ctp)) {
							successCount++;
							tableInputData.remove(ctp);
						}
						monitor.worked(1);
						counter++;
					}
					monitor.done();
					if (successCount > 0) {
						CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_Leg_Success_Title, MessageFormat.format(Messages.CTPatrolTableContainer_Leg_Success_Message, successCount, patrol.getId()));
					}
				}
			});
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title, Messages.CTPatrolTableContainer_Leg_Error, e);
		}

		refreshViewer();
	}

	private void handleRemove() {
		boolean isOk = MessageDialog.openQuestion(getShell(), Messages.CTPatrolTableContainer_DeleteWarn_Title, Messages.CTPatrolTableContainer_DeleteWarn_Message);
		if (isOk) {
			IStructuredSelection sel = (IStructuredSelection)viewer.getSelection();
			if (!sel.isEmpty()) {
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					tableInputData.remove(iterator.next());
				}
			}
			viewer.refresh();
		}
	}
	
	private void addColumns(TableViewer viewer) {
		for (CTPatrolTableColumn column : CTPatrolTableColumn.values()) {
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			viewerColumn.getColumn().setText(column.getGuiName());
			viewerColumn.getColumn().setWidth(column.getWidth());
			viewerColumn.setLabelProvider(new CTPatrolTableCellLabelProvider(column));
		}
	}

	private File[] selectFile() {
		CyberTrackerFileImportDialog fileDialog = new CyberTrackerFileImportDialog(getShell());
		if (fileDialog.open() == IDialogConstants.OK_ID)
			return fileDialog.getSelectedFiles();
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
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
	
	private void updatePatrolDetails(Object selection){
		Text[] lbls = new Text[]{lblStartDate, lblEndDate,lblPatrolType,lblTransportType, lblArmed,lblTeam,lblStation,lblComment, lblObjective, lblMandate, lblLeader, lblPilot, lblMembers};
		CTPatrolTableColumn[] cols = new CTPatrolTableColumn[]{CTPatrolTableColumn.START_DATE,CTPatrolTableColumn.END_DATE,CTPatrolTableColumn.TYPE,CTPatrolTableColumn.TRANSPORT,CTPatrolTableColumn.ARMED,CTPatrolTableColumn.TEAM,CTPatrolTableColumn.STATION,CTPatrolTableColumn.COMMENT};
		if (selection instanceof CyberTrackerPatrol){
			
			for (int i = 0; i < cols.length; i ++){
				String text = ((CTPatrolTableCellLabelProvider)viewer.getLabelProvider(cols[i].ordinal())).getText(selection);
				if (text == null){
					text =""; //$NON-NLS-1$
				}
				((Text)lbls[i]).setText(text);
			}
			
			CyberTrackerPatrol patrol = ((CyberTrackerPatrol) selection);
			
			lblObjective.setText(patrol.getObjective()==null?"":patrol.getObjective());			 //$NON-NLS-1$
			lblMandate.setText(patrol.getMandate() == null ? "" : patrol.getMandate().getName()); //$NON-NLS-1$
			lblLeader.setText(patrol.getLeader() == null ? "" : patrol.getLeader().getFullLabel()); //$NON-NLS-1$
			lblPilot.setText(patrol.getPilot() == null ? "" : patrol.getPilot().getFullLabel()); //$NON-NLS-1$
			StringBuilder sbMembers = new StringBuilder();
			for (Employee e : ((CyberTrackerPatrol) selection).getMembers()){
				sbMembers.append(e.getFullLabel());
				sbMembers.append("\n"); //$NON-NLS-1$
			}
			lblMembers.setText(sbMembers.toString());
			
			
			Map<PatrolMeta, List<ImportError>> problems = ((CyberTrackerPatrol) selection).getProblems();
			updateCd(cdStartDate, problems.get(PatrolMeta.START_DATE));
			updateCd(cdEndDate, problems.get(PatrolMeta.END_DATE));
			updateCd(cdPatrolType, problems.get(PatrolMeta.TYPE));
			updateCd(cdTransportType, problems.get(PatrolMeta.TRANSPORT));
			updateCd(cdArmed, problems.get(PatrolMeta.ARMED));
			updateCd(cdMandate, problems.get(PatrolMeta.MANDATE));
			updateCd(cdTeam, problems.get(PatrolMeta.TEAM));
			updateCd(cdStation, problems.get(PatrolMeta.STATION));
			updateCd(cdObjective, problems.get(PatrolMeta.OBJECTIVE));
			updateCd(cdComment, problems.get(PatrolMeta.COMMENT));
			updateCd(cdLeader, problems.get(PatrolMeta.LEADER));
			updateCd(cdPilot, problems.get(PatrolMeta.PILOT));
			updateCd(cdMembers, problems.get(PatrolMeta.MEMBERS));
			
		}else{
			for (int i = 0; i < lbls.length; i ++){
				((Text)lbls[i]).setText(""); //$NON-NLS-1$
			}
			ControlDecoration[] cds = {cdStartDate, cdEndDate, cdPatrolType, cdTransportType, cdArmed, cdMandate, cdTeam, cdStation, cdObjective, cdComment, cdLeader, cdPilot, cdMembers};
			for (int i = 0; i < cds.length; i ++){
				cds[i].hide();
			}
			
		}
	}
	
	private void updateCd(ControlDecoration cd, List<ImportError> errors){
		if (errors == null || errors.size() == 0){
			cd.hide();
			return;
		}
		boolean error = false;
		StringBuilder sb = new StringBuilder();
		for (ImportError err: errors){
			if (sb.length() > 0){
				sb.append("\n"); //$NON-NLS-1$
			}
			sb.append(err.getMessage());	
			if(err.getType() == ErrorType.ERROR){
				error = true;
			}
		}
		if (error){
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		}else{
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		}
		cd.setDescriptionText(sb.toString());
		cd.show();
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		cd.hide();
		return cd;
	}
	
	private Composite createPatrolDetailsComposite(Composite parent, FormToolkit toolkit){
		ScrolledComposite scrolled = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL);
		scrolled.setLayout(new GridLayout(1, false));
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scrolled);
		scrolled.setExpandHorizontal(true);
		
		Composite main = toolkit.createComposite(scrolled);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		scrolled.setContent(main);
		
		Composite left = toolkit.createComposite(main);
		left.setLayout(new GridLayout(2, false));
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite right = toolkit.createComposite(main);
		right.setLayout(new GridLayout(2, false));
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		int indent = 10;
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_StartDateLabel);
		lblStartDate = toolkit.createText(left, ""); //$NON-NLS-1$
		lblStartDate.setEditable(false);
		lblStartDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblStartDate.getLayoutData()).horizontalIndent = indent;
		cdStartDate = createDecoration(lblStartDate);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_EndDateLabel);
		lblEndDate = toolkit.createText(left, ""); //$NON-NLS-1$
		lblEndDate.setEditable(false);
		lblEndDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblEndDate.getLayoutData()).horizontalIndent = indent;
		cdEndDate = createDecoration(lblEndDate);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_PatrolTypeLabel);
		lblPatrolType = toolkit.createText(left, ""); //$NON-NLS-1$
		lblPatrolType.setEditable(false);
		lblPatrolType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblPatrolType.getLayoutData()).horizontalIndent = indent;
		cdPatrolType = createDecoration(lblPatrolType);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_TransportTypeLabel);
		lblTransportType = toolkit.createText(left, ""); //$NON-NLS-1$
		lblTransportType.setEditable(false);
		lblTransportType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblTransportType.getLayoutData()).horizontalIndent = indent;
		cdTransportType = createDecoration(lblTransportType);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_ArmedLabel);
		lblArmed = toolkit.createText(left, ""); //$NON-NLS-1$
		lblArmed.setEditable(false);
		lblArmed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblArmed.getLayoutData()).horizontalIndent = indent;
		cdArmed = createDecoration(lblArmed);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_LeaderLabel);
		lblLeader = toolkit.createText(left, ""); //$NON-NLS-1$
		lblLeader.setEditable(false);
		lblLeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblLeader.getLayoutData()).horizontalIndent = indent;
		cdLeader = createDecoration(lblLeader);
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_PilotLabel);
		lblPilot = toolkit.createText(left, ""); //$NON-NLS-1$
		lblPilot.setEditable(false);
		lblPilot.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblPilot.getLayoutData()).horizontalIndent = indent;
		cdPilot = createDecoration(lblPilot);
		
		Label l = toolkit.createLabel(left, Messages.CTPatrolTableContainer_MembersLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblMembers = toolkit.createText(left, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		lblMembers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lblMembers.getLayoutData()).widthHint = 150;
		((GridData)lblMembers.getLayoutData()).heightHint = 150;
		((GridData)lblMembers.getLayoutData()).horizontalIndent = indent;
		lblMembers.setEditable(false);
		cdMembers = createDecoration(lblMembers);
		
		toolkit.createLabel(right, Messages.CTPatrolTableContainer_TeamLabel);
		lblTeam = toolkit.createText(right, ""); //$NON-NLS-1$
		lblTeam.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblTeam.setEditable(false);
		((GridData)lblTeam.getLayoutData()).horizontalIndent = indent;
		cdTeam = createDecoration(lblTeam);
		
		toolkit.createLabel(right, Messages.CTPatrolTableContainer_StationLabel);
		lblStation = toolkit.createText(right, ""); //$NON-NLS-1$
		lblStation.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		lblStation.setEditable(false);
		((GridData)lblStation.getLayoutData()).horizontalIndent = indent;
		cdStation = createDecoration(lblStation);
		
		toolkit.createLabel(right, Messages.CTPatrolTableContainer_MandateLabel);
		lblMandate = toolkit.createText(right, ""); //$NON-NLS-1$
		lblMandate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lblMandate.setEditable(false);
		((GridData)lblMandate.getLayoutData()).horizontalIndent = indent;
		cdMandate = createDecoration(lblMandate);
		
		l = toolkit.createLabel(right, Messages.CTPatrolTableContainer_ObjectiveLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblObjective = toolkit.createText(right, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		lblObjective.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lblObjective.getLayoutData()).heightHint = 150;
		((GridData)lblObjective.getLayoutData()).widthHint = 150;
		((GridData)lblObjective.getLayoutData()).horizontalIndent = indent;
		lblObjective.setEditable(false);
		cdObjective = createDecoration(lblObjective);
		
		l = toolkit.createLabel(right, Messages.CTPatrolTableContainer_CommentLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblComment = toolkit.createText(right, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		lblComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lblComment.getLayoutData()).widthHint = 150;
		((GridData)lblComment.getLayoutData()).heightHint = 150;
		((GridData)lblComment.getLayoutData()).horizontalIndent = indent;
		lblComment.setEditable(false);
		cdComment = createDecoration(lblComment);
		
		main.setSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, 300);
		return scrolled;
	}
}
