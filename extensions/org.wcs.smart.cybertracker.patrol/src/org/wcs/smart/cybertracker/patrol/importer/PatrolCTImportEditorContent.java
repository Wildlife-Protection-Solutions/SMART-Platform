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
package org.wcs.smart.cybertracker.patrol.importer;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.importer.IImportEditorContent;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.IDataMeta;
import org.wcs.smart.cybertracker.model.ImportError;
import org.wcs.smart.cybertracker.model.ImportError.ErrorType;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol.PatrolMeta;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Provides functionality and GUI that is specific for patrol import from CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class PatrolCTImportEditorContent implements IImportEditorContent {

	private PatrolImporter patrolImporter;
	private PatrolLegImporter legImporter;

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
	
	private Map<CTPatrolUIMeta, EditorContentLabelProvider> labelProviderMap = new HashMap<CTPatrolUIMeta, EditorContentLabelProvider>();

	/**
	 * Metadata for patrols that is displayed in details window.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private enum CTPatrolUIMeta {
		START_DATE,
		END_DATE,
		TYPE,
		TRANSPORT,
		ARMED,
//		MANDATE,
		TEAM,
		STATION	,
//		OBJECTIVE,
		COMMENT,
		LEADER,
		PILOT,
		MEMBERS,
		SIGHT_COUNT;
	}
	
	private EditorContentLabelProvider getLabelProvider(CTPatrolUIMeta column) {
		EditorContentLabelProvider lp = labelProviderMap.get(column);
		if (lp == null) {
			lp = new EditorContentLabelProvider(column);
			labelProviderMap.put(column, lp);
		}
		return lp;
	}

	public Composite createDetailsComposite(Composite parent, FormToolkit toolkit) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
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

		return main;
	}

	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		cd.hide();
		return cd;
	}
	
	public void inputChanged(Object selection) {
		Text[] lbls = new Text[]{lblStartDate, lblEndDate,lblPatrolType,lblTransportType, lblArmed,lblTeam,lblStation,lblComment, lblObjective, lblMandate, lblLeader, lblPilot, lblMembers};
		CTPatrolUIMeta[] cols = new CTPatrolUIMeta[]{CTPatrolUIMeta.START_DATE,CTPatrolUIMeta.END_DATE,CTPatrolUIMeta.TYPE,CTPatrolUIMeta.TRANSPORT,CTPatrolUIMeta.ARMED,CTPatrolUIMeta.TEAM,CTPatrolUIMeta.STATION,CTPatrolUIMeta.COMMENT};
		if (selection instanceof CyberTrackerPatrol){
			
			for (int i = 0; i < cols.length; i ++){
				String text = getLabelProvider(cols[i]).getText(selection);
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
			
			
			Map<IDataMeta, List<ImportError>> problems = ((CyberTrackerPatrol) selection).getProblems();
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
	
	/**
	 * 
	 * @return list of objects that were added
	 */
	public List<ICyberTrackerData> handleAdd(Shell shell, final IStructuredSelection selection) {
		//TODO:
		return handleAddAsPatrol(shell, selection);
	}
	
	protected List<ICyberTrackerData> handleAddAsPatrol(Shell shell, final IStructuredSelection selection) {
		if (patrolImporter == null)
			patrolImporter = new PatrolImporter();
		final List<Patrol> addedList = new ArrayList<Patrol>();
		final List<ICyberTrackerData> processedList = new ArrayList<ICyberTrackerData>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell.getDisplay().getActiveShell());
		try {
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
							processedList.add(ctp);
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
			MessageDialog.openInformation(shell, Messages.CTPatrolTableContainer_Patrol_Success_Title, MessageFormat.format(Messages.CTPatrolTableContainer_Patrol_Success_Message, ids)); 
		}
		return processedList;
	}

	protected List<ICyberTrackerData> handleAddAsLeg(Shell shell, final IStructuredSelection selection) {
		final List<ICyberTrackerData> processedList = new ArrayList<ICyberTrackerData>();
		PatrolSelectorDialog selectorDialog = new PatrolSelectorDialog(shell);
		if (selectorDialog.open() != IDialogConstants.OK_ID) {
			return processedList;
		}
		if (legImporter == null)
			legImporter = new PatrolLegImporter();

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
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
							processedList.add(ctp);
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
		return processedList;
	}
	

	/**
	 * Label provider for details panel fields
	 * @author elitvin
	 * @since 4.0.0
	 */
	private class EditorContentLabelProvider {

		private CTPatrolUIMeta column;
		
		public EditorContentLabelProvider(CTPatrolUIMeta column) {
			this.column = column;
		}
		public String getText(Object element) {
			if (element instanceof CyberTrackerPatrol) {
				CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
				switch (column) {
				case START_DATE:return dateAsString(ctPatrol.getStartDate());
				case END_DATE: 	return dateAsString(ctPatrol.getEndDate());
				case TYPE: 		return ctPatrol.getPatrolType() != null ? ctPatrol.getPatrolType().getGuiName() : ""; //$NON-NLS-1$
				case TRANSPORT:	return ctPatrol.getCtTransport();
				case ARMED: 	return ctPatrol.isArmed() ? Messages.CTPatrolTableCellLabelProvider_Armed_Yes : Messages.CTPatrolTableCellLabelProvider_Armed_No;
//				case MANDATE:	return asString(ctPatrol.getMandate());
				case TEAM: 		return ctPatrol.getCtTeam();
				case STATION:	return ctPatrol.getCtStation();
//				case OBJECTIVE: return ctPatrol.getObjective();
				case COMMENT:	return ctPatrol.getComment();
				case LEADER:	return ctPatrol.getCtLeader();
				case PILOT:		return ctPatrol.getCtPilot();
				case MEMBERS:	return asString(ctPatrol.getCtMembers(), "; "); //$NON-NLS-1$
				case SIGHT_COUNT:return String.valueOf(ctPatrol.getSData().size());
				}
			}
			return "unknown meta: " + column; //$NON-NLS-1$
		}
		
		private String asString(List<String> members, String separator) {
			StringBuilder result = new StringBuilder();
			for (Iterator<String> i = members.iterator(); i.hasNext();) {
				String e = i.next();
				result.append(e);
				if (i.hasNext())
					result.append(separator);
			}
			return result.toString();
		}

		private String dateAsString(Date date) {
			if (date == null) {
				return ""; //$NON-NLS-1$
			}
			return DateFormat.getDateTimeInstance().format(date);
		}
		
	}
}
