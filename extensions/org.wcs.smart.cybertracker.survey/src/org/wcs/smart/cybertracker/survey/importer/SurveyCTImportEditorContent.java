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
package org.wcs.smart.cybertracker.survey.importer;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.IDataMeta;
import org.wcs.smart.cybertracker.model.ImportError;
import org.wcs.smart.cybertracker.model.ImportError.ErrorType;
import org.wcs.smart.cybertracker.survey.importer.SurveyCTLabelProvider.CTSurveyUIMeta;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey.SurveyMeta;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Provides functionality and GUI that is specific for survey import from CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCTImportEditorContent implements IImportEditorContent {
	
	private MissionImporter missionImporter;

	private Text lblStartDate;
	private Text lblEndDate;
	private Text lblSurveyDesign;
	private Text lblMembers;
	private Text lblComment;
	private Text lblLeader;
	
	private ControlDecoration cdStartDate;
	private ControlDecoration cdEndDate;
	private ControlDecoration cdSurveyDesign;
	private ControlDecoration cdMembers;
	private ControlDecoration cdComment;
	private ControlDecoration cdLeader;
	
	private Map<CTSurveyUIMeta, SurveyCTLabelProvider> labelProviderMap = new HashMap<CTSurveyUIMeta, SurveyCTLabelProvider>();

	
	private SurveyCTLabelProvider getLabelProvider(CTSurveyUIMeta column) {
		SurveyCTLabelProvider lp = labelProviderMap.get(column);
		if (lp == null) {
			lp = new SurveyCTLabelProvider(column);
			labelProviderMap.put(column, lp);
		}
		return lp;
	}

	@Override
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
		toolkit.createLabel(left, Messages.SurveyCTImportEditorContent_SurveyDesign);
		lblSurveyDesign = toolkit.createText(left, ""); //$NON-NLS-1$
		lblSurveyDesign.setEditable(false);
		lblSurveyDesign.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblSurveyDesign.getLayoutData()).horizontalIndent = indent;
		cdSurveyDesign = createDecoration(lblSurveyDesign);

		toolkit.createLabel(left, Messages.SurveyCTImportEditorContent_StartDate);
		lblStartDate = toolkit.createText(left, ""); //$NON-NLS-1$
		lblStartDate.setEditable(false);
		lblStartDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblStartDate.getLayoutData()).horizontalIndent = indent;
		cdStartDate = createDecoration(lblStartDate);
		
		toolkit.createLabel(left, Messages.SurveyCTImportEditorContent_EndDate);
		lblEndDate = toolkit.createText(left, ""); //$NON-NLS-1$
		lblEndDate.setEditable(false);
		lblEndDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblEndDate.getLayoutData()).horizontalIndent = indent;
		cdEndDate = createDecoration(lblEndDate);
		
		toolkit.createLabel(left, Messages.SurveyCTImportEditorContent_Leader);
		lblLeader = toolkit.createText(left, ""); //$NON-NLS-1$
		lblLeader.setEditable(false);
		lblLeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblLeader.getLayoutData()).horizontalIndent = indent;
		cdLeader = createDecoration(lblLeader);
		
		Label l = toolkit.createLabel(left, Messages.SurveyCTImportEditorContent_Members);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblMembers = toolkit.createText(left, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		lblMembers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lblMembers.getLayoutData()).widthHint = 150;
		((GridData)lblMembers.getLayoutData()).heightHint = 150;
		((GridData)lblMembers.getLayoutData()).horizontalIndent = indent;
		lblMembers.setEditable(false);
		cdMembers = createDecoration(lblMembers);
		
		l = toolkit.createLabel(right, Messages.SurveyCTImportEditorContent_Comment);
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

	@Override
	public void inputChanged(Object selection) {
		Text[] lbls = new Text[]{lblStartDate, lblEndDate, lblComment, lblSurveyDesign, lblLeader, lblMembers};
		CTSurveyUIMeta[] cols = new CTSurveyUIMeta[]{CTSurveyUIMeta.START_DATE,CTSurveyUIMeta.END_DATE,CTSurveyUIMeta.COMMENT};
		if (selection instanceof CyberTrackerSurvey){
			
			for (int i = 0; i < cols.length; i ++){
				String text = getLabelProvider(cols[i]).getText(selection);
				if (text == null){
					text =""; //$NON-NLS-1$
				}
				((Text)lbls[i]).setText(text);
			}
			
			CyberTrackerSurvey survey = ((CyberTrackerSurvey) selection);
			
			lblLeader.setText(survey.getLeader() == null ? "" : SmartLabelProvider.getShortLabel(survey.getLeader())); //$NON-NLS-1$
			StringBuilder sbMembers = new StringBuilder();
			for (Employee e : ((CyberTrackerSurvey) selection).getMembers()){
				sbMembers.append(SmartLabelProvider.getShortLabel(e));
				sbMembers.append("\n"); //$NON-NLS-1$
			}
			lblMembers.setText(sbMembers.toString());
			lblSurveyDesign.setText(survey.getSurveyDesign() == null ? "" : survey.getSurveyDesign().getName()); //$NON-NLS-1$
			
			
			Map<IDataMeta, List<ImportError>> problems = ((CyberTrackerSurvey) selection).getProblems();
			updateCd(cdStartDate, problems.get(SurveyMeta.START_DATE));
			updateCd(cdEndDate, problems.get(SurveyMeta.END_DATE));
			updateCd(cdSurveyDesign, problems.get(SurveyMeta.SURVEY_DESIGN));
			updateCd(cdComment, problems.get(SurveyMeta.COMMENT));
			updateCd(cdLeader, problems.get(SurveyMeta.LEADER));
			updateCd(cdMembers, problems.get(SurveyMeta.MEMBERS));
			
		}else{
			for (int i = 0; i < lbls.length; i ++){
				((Text)lbls[i]).setText(""); //$NON-NLS-1$
			}
			ControlDecoration[] cds = {cdStartDate, cdEndDate, cdSurveyDesign, cdComment, cdLeader, cdMembers};
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

	@Override
	public List<ICyberTrackerData> handleAdd(Shell shell, final IStructuredSelection selection) {
		final List<ICyberTrackerData> processedList = new ArrayList<ICyberTrackerData>();
		String validationError = validateSelection(selection);
		if (validationError != null) {
			MessageDialog.openError(shell, Messages.SurveyCTImportEditorContent_Error, validationError);
			return processedList;
		}

		SurveyDesign surveyDesign = ((CyberTrackerSurvey)selection.getFirstElement()).getSurveyDesign();
		if (surveyDesign == null) {
			//TODO: allow user to fix survey design
			throw new IllegalStateException("Unknown survey design"); //$NON-NLS-1$
		}
		
		MissionSelectorDialog missionDialog = new MissionSelectorDialog(shell, surveyDesign);
		if (missionDialog.open() != IDialogConstants.OK_ID) {
			return processedList;
		}
		
		Survey survey = null;
		Mission mission = null;
		String newSurveyId = null;
		if (missionDialog.isNew()) {
			SurveySelectorDialog surveyDialog = new SurveySelectorDialog(shell, surveyDesign);
			if (surveyDialog.open() != IDialogConstants.OK_ID) {
				return processedList;
			}
			survey = surveyDialog.isNew() ? null : surveyDialog.getSelectedSurvey();
			newSurveyId = surveyDialog.getNewId();
		} else {
			//use survey from selected mission
			mission = missionDialog.getSelectedMission();
			survey = mission.getSurvey();
		}
		
		if (missionImporter == null)
			missionImporter = new MissionImporter();
		final List<Mission> addedList = new ArrayList<Mission>();
		final Survey targetSurvey = survey;
		final Mission targetMission = mission;
		final String idForNewSurvey = newSurveyId;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell.getDisplay().getActiveShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					int missionsCount = selection.size();
					int counter = 1;
					monitor.beginTask(Messages.SurveyCTImportEditorContent_TaskName_AddMissions, missionsCount);
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						monitor.subTask(MessageFormat.format(Messages.SurveyCTImportEditorContent_Task_AddMission, counter, missionsCount));
						CyberTrackerSurvey ctp = (CyberTrackerSurvey) i.next();
						Mission p = missionImporter.importData(ctp, targetMission, targetSurvey, idForNewSurvey);
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
			CyberTrackerPlugIn.displayError(Messages.SurveyCTImportEditorContent_Error,Messages.SurveyCTImportEditorContent_AboartErrorMessage, e);
		}
		
		if (!addedList.isEmpty()) {
			String ids = ""; //$NON-NLS-1$
			for (Iterator<Mission> i = addedList.iterator(); i.hasNext();) {
				Mission p = i.next();
				ids += p.getId();
				if (i.hasNext())
					ids += ", "; //$NON-NLS-1$
			}
			MessageDialog.openInformation(shell, Messages.SurveyCTImportEditorContent_SuccessDialog_Title, MessageFormat.format(Messages.SurveyCTImportEditorContent_SuccessDialog_Message, ids)); 
		}
		return processedList;
	}

	protected String validateSelection(IStructuredSelection selection) {
		//validate that all selected surveys are from the same survey design, otherwise mission/survey selection is ambiguous
		String keyID = ((CyberTrackerSurvey)selection.getFirstElement()).getSurveyDesignKey();
		boolean isSame = true;
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			CyberTrackerSurvey data = (CyberTrackerSurvey) i.next();
			if (keyID != null) {
				if (!keyID.equals(data.getSurveyDesignKey())) {
					isSame = false;
					break;
				}
			} else {
				//keyID is null, this is exceptional case when surveyDesign associated with CT data was deleted before import
				if (data.getSurveyDesignKey() != null) {
					isSame = false;
					break;
				}
			}
		}
		if (!isSame) {
			return Messages.SurveyCTImportEditorContent_UnableToImport;
		}
		return null;
	}

}
