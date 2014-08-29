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
package org.wcs.smart.er.ui.survey;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * New survey dialog.
 * 
 * @author Emily
 *
 */
public class NewSurveyDialog extends TitleAreaDialog{

	private ComboViewer cmbDesign;
	private ControlDecoration cdDesign;
	private Text txtId;
	private ControlDecoration cdId;
	private DateTime startDate;
	private ControlDecoration cdStart;
	private DateTime endDate;
	private ControlDecoration cdEnd;
	private byte[] parentSurveyDesignUuid;
	private Session session;
	
	private Survey newSurvey;
	
	public NewSurveyDialog(Shell parentShell, 
			byte[] parentSurveyDesignUuid) {
		super(parentShell);
		this.session = HibernateManager.openSession();
		this.parentSurveyDesignUuid = parentSurveyDesignUuid;
	}
	
	/**
	 * 
	 * @return the new survey created or null if none created
	 */
	public Survey getSurvey(){
		return newSurvey;
	}

	@Override
	public boolean close(){
		try{
			session.close();
		}catch (Exception ex){
			EcologicalRecordsPlugIn.log(ex.getMessage(), ex);
		}
		return super.close();
	}

	/**
	 * Saves the new survey then closes the dialog
	 */
	@Override
	protected void okPressed() {
		if (save()){
			super.okPressed();
		}
	}
	
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		validate();
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(2, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(part, SWT.NONE);
		l.setText("Parent Survey Design:");
		
		cmbDesign = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDesign.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		cmbDesign.setContentProvider(ArrayContentProvider.getInstance());
		cmbDesign.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbDesign.getControl().getLayoutData()).widthHint = 100;
		cdDesign = createDecoration(cmbDesign.getControl());
		cmbDesign.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		l = new Label(part, SWT.NONE);
		l.setText("Survey ID:");
		
		txtId = new Text(part, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtId.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
		cdId = createDecoration(txtId);
		
		l = new Label(part, SWT.NONE);
		l.setText("Start Date:");
		
		SelectionListener listener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		};
		startDate = new DateTime(part, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
		startDate.addSelectionListener(listener);
		cdStart = createDecoration(startDate);
		
		l = new Label(part, SWT.NONE);
		l.setText("End Date:");
		
		endDate = new DateTime(part, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
		endDate.addSelectionListener(listener);
		cdEnd = createDecoration(endDate);
		
		setTitle("New Survey");
		getShell().setText("New Survey");
		setMessage("Creates a new survey");
		
		initControls();
		
		
		return parent;
	}
	
	private ControlDecoration createDecoration(Control parent){
		ControlDecoration cd = new ControlDecoration(parent, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	private void setIcon(ControlDecoration cd, boolean error){
		if (error){
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		}else{
			cd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
		}
	}
	
	private boolean save(){
		Survey s = new Survey();
		s.setEndDate(SmartUtils.getDate(endDate));
		s.setStartDate(SmartUtils.getDate(startDate));
		s.setId(txtId.getText());
		s.setMissions(new ArrayList<Mission>());
		s.setSurveyDesign(  (SurveyDesign) ((StructuredSelection)cmbDesign.getSelection()).getFirstElement() );
		
		session.beginTransaction();
		try{
			session.save(s);
			session.getTransaction().commit();
			newSurvey = s;
			try{
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_ADDED, s);
			}catch (Exception ex){
				EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);	
			}
			return true;
		}catch (Exception ex){
			try{
				session.getTransaction().rollback();
			}catch (Exception ex2){
				EcologicalRecordsPlugIn.log(ex2.getMessage(), ex2);
			}
			EcologicalRecordsPlugIn.displayLog("Error creating new survey." + "\n\n" + ex.getMessage(), ex);
		}
		return false;
	}
	
	private void validate(){
		boolean error = false;
		if (cmbDesign.getSelection().isEmpty()){
			cdDesign.setDescriptionText("A survey design must be selected.");
			cdDesign.show();
			error = true;
		}else{
			cdDesign.hide();
		}
		
		if (!SmartUtils.isSimpleString(txtId.getText(), RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Survey.ID_MAX_LENGTH)){
			cdId.setDescriptionText(MessageFormat.format("The id must be between 1 and {0} characters and only contain {1}.", new Object[]{Survey.ID_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc}));
			cdId.show();
			error = true;
		}else{
			cdId.hide();
		}
		
		Date start = SmartUtils.getDate(startDate);
		Date end = SmartUtils.getDate(endDate);
		if (end.before(start)){
			setIcon(cdEnd, true);
			cdEnd.setDescriptionText("End date is before start date");
			cdEnd.show();
			error = true;
		}else{
			cdEnd.hide();
		}
		
		cdStart.hide();
		
		if (!cdEnd.isVisible() && !cmbDesign.getSelection().isEmpty()){
			SurveyDesign design = (SurveyDesign) ((IStructuredSelection)cmbDesign.getSelection()).getFirstElement();
		
		
			boolean datewarn = false;
			if (design.getStartDate() != null){
				//ensure start date is not before design start date
				//ensure end date is not before design start date
				if (start.before(design.getStartDate())){
					datewarn = true;
				}
				if (end.before(design.getStartDate())){
					datewarn = true;
				}
			}
			if (design.getEndDate() != null){
				//ensure start date is not after end date
				//ensure end date is not after end date
				if (start.after(design.getEndDate())){
					datewarn = true;
				}
				if (end.after(design.getEndDate())){
					datewarn = true;
				}
			}
			
			if (datewarn){
				setIcon(cdEnd, false);
				cdEnd.setDescriptionText(
						MessageFormat.format("The date range of the survey is outside that of the design {0} to {1}.",
								new Object[]{
									design.getStartDate() == null ? "N/A" : DateFormat.getDateInstance().format(design.getStartDate()),
											design.getEndDate() == null ? "N/A" : DateFormat.getDateInstance().format(design.getEndDate())
									}
								));
				cdEnd.show();
			}
		}
		if (getButton(OK) != null){
			getButton(OK).setEnabled(!error);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void initControls(){
		List<SurveyDesign> designs = 
				session.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("state", SurveyDesign.State.ACTIVE)).list(); //$NON-NLS-1$
		cmbDesign.setInput(designs);
		
		if (parentSurveyDesignUuid != null){
			for (SurveyDesign d : designs){
				if (Arrays.equals(d.getUuid(), parentSurveyDesignUuid)){
					cmbDesign.setSelection(new StructuredSelection(d));
					break;
				}
			}
		}
		
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
