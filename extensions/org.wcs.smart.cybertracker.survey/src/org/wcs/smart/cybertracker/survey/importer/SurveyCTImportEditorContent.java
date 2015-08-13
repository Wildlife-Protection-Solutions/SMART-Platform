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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.wcs.smart.cybertracker.importer.IImportEditorContent;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.IDataMeta;
import org.wcs.smart.cybertracker.model.ImportError;
import org.wcs.smart.cybertracker.model.ImportError.ErrorType;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey.SurveyMeta;

/**
 * Provides functionality and GUI that is specific for survey import from CyberTracker.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SurveyCTImportEditorContent implements IImportEditorContent {

	private Text lblStartDate;
	private Text lblEndDate;
	private Text lblMembers;
	private Text lblComment;
	private Text lblLeader;
	
	private ControlDecoration cdStartDate;
	private ControlDecoration cdEndDate;
	private ControlDecoration cdMembers;
	private ControlDecoration cdComment;
	private ControlDecoration cdLeader;
	
	private Map<CTSurveyUIMeta, EditorContentLabelProvider> labelProviderMap = new HashMap<CTSurveyUIMeta, EditorContentLabelProvider>();

	/**
	 * Metadata for patrols that is displayed in details window.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private enum CTSurveyUIMeta {
		START_DATE,
		END_DATE,
		COMMENT,
		LEADER,
		MEMBERS,
		SIGHT_COUNT;
	}
	
	private EditorContentLabelProvider getLabelProvider(CTSurveyUIMeta column) {
		EditorContentLabelProvider lp = labelProviderMap.get(column);
		if (lp == null) {
			lp = new EditorContentLabelProvider(column);
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
		
		toolkit.createLabel(left, Messages.CTPatrolTableContainer_LeaderLabel);
		lblLeader = toolkit.createText(left, ""); //$NON-NLS-1$
		lblLeader.setEditable(false);
		lblLeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblLeader.getLayoutData()).horizontalIndent = indent;
		cdLeader = createDecoration(lblLeader);
		
		Label l = toolkit.createLabel(left, Messages.CTPatrolTableContainer_MembersLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblMembers = toolkit.createText(left, "", SWT.WRAP | SWT.V_SCROLL); //$NON-NLS-1$
		lblMembers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lblMembers.getLayoutData()).widthHint = 150;
		((GridData)lblMembers.getLayoutData()).heightHint = 150;
		((GridData)lblMembers.getLayoutData()).horizontalIndent = indent;
		lblMembers.setEditable(false);
		cdMembers = createDecoration(lblMembers);
		
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

	@Override
	public void inputChanged(Object selection) {
		Text[] lbls = new Text[]{lblStartDate, lblEndDate, lblComment, lblLeader, lblMembers};
		CTSurveyUIMeta[] cols = new CTSurveyUIMeta[]{CTSurveyUIMeta.START_DATE,CTSurveyUIMeta.END_DATE,CTSurveyUIMeta.COMMENT};
		if (selection instanceof CyberTrackerSurvey){
			
			for (int i = 0; i < cols.length; i ++){
				String text = getLabelProvider(cols[i]).getText(selection);
				if (text == null){
					text =""; //$NON-NLS-1$
				}
				((Text)lbls[i]).setText(text);
			}
			
			CyberTrackerSurvey patrol = ((CyberTrackerSurvey) selection);
			
			lblLeader.setText(patrol.getLeader() == null ? "" : patrol.getLeader().getFullLabel()); //$NON-NLS-1$
			StringBuilder sbMembers = new StringBuilder();
			for (Employee e : ((CyberTrackerSurvey) selection).getMembers()){
				sbMembers.append(e.getFullLabel());
				sbMembers.append("\n"); //$NON-NLS-1$
			}
			lblMembers.setText(sbMembers.toString());
			
			
			Map<IDataMeta, List<ImportError>> problems = ((CyberTrackerSurvey) selection).getProblems();
			updateCd(cdStartDate, problems.get(SurveyMeta.START_DATE));
			updateCd(cdEndDate, problems.get(SurveyMeta.END_DATE));
			updateCd(cdComment, problems.get(SurveyMeta.COMMENT));
			updateCd(cdLeader, problems.get(SurveyMeta.LEADER));
			updateCd(cdMembers, problems.get(SurveyMeta.MEMBERS));
			
		}else{
			for (int i = 0; i < lbls.length; i ++){
				((Text)lbls[i]).setText(""); //$NON-NLS-1$
			}
			ControlDecoration[] cds = {cdStartDate, cdEndDate, cdComment, cdLeader, cdMembers};
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
	public List<ICyberTrackerData> handleAdd(Shell shell, IStructuredSelection selection) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Label provider for details panel fields
	 * @author elitvin
	 * @since 4.0.0
	 */
	private class EditorContentLabelProvider {

		private CTSurveyUIMeta column;
		
		public EditorContentLabelProvider(CTSurveyUIMeta column) {
			this.column = column;
		}
		public String getText(Object element) {
			if (element instanceof CyberTrackerSurvey) {
				CyberTrackerSurvey ctPatrol = (CyberTrackerSurvey) element;
				switch (column) {
				case START_DATE:return dateAsString(ctPatrol.getStartDate());
				case END_DATE: 	return dateAsString(ctPatrol.getEndDate());
				case COMMENT:	return ctPatrol.getComment();
				case LEADER:	return ctPatrol.getCtLeader();
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
