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
package org.wcs.smart.patrol.internal.ui.importwp.csv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.importwp.DateFormat;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Option composite for importing waypoints from csv.  
 * 
 * @author Jeff
 * since 2.0
 *
 */
public class ImportCSVDetailsComposite extends Composite{
	boolean valid;
	CsvHeader[] columnNames;
	
	private Label lbl;
	private Label lblX;
	private Label lblY;
	private Label lblDate;
	private Label lblTime;
	private Label lblId;
	private Label lblComments;
	private Label lblP;
	private Label lblD;

	private ComboViewer cmbColumnSelectorX;
	private ComboViewer cmbColumnSelectorY;
	private ComboViewer cmbColumnSelectorDate;
	private ComboViewer cmbColumnSelectorTime;
	private ComboViewer cmbColumnSelectorComments;
	private ComboViewer cmbColumnSelectorId;
	
	private ComboViewer lstProjections;
	private Projection[] projections;
	
	private ComboViewer cmbColumnSelectorDateFormat;
	private DateFormat[] dateFormats;
	
	private Button skipHeaders;
	
	
	private ControlDecoration cdX;
	private ControlDecoration cdY;
	private ControlDecoration cdDate;
	private ControlDecoration cdTime;
	private ControlDecoration cdDateFormat;
	
		
	public ImportCSVDetailsComposite(Composite parent, CsvHeader[] columnNames) {
		super(parent, SWT.NONE);
		this.columnNames = columnNames;
		
		Session session = HibernateManager.openSession();
		//load projection list
		session.beginTransaction();
		List<Projection> tmp = HibernateManager.getCaProjectionList(session);
		this.projections = tmp.toArray(new Projection[tmp.size()]);
		session.getTransaction().commit();
		
		dateFormats = new DateFormat[6];
		dateFormats[0] = new DateFormat("d/M/y"); //$NON-NLS-2$
		dateFormats[1] = new DateFormat("d-M-y"); //$NON-NLS-2$
		dateFormats[2] = new DateFormat("M/d/y"); //$NON-NLS-2$
		dateFormats[3] = new DateFormat("M-d-y"); //$NON-NLS-2$
		dateFormats[4] = new DateFormat("y/M/d"); //$NON-NLS-2$
		dateFormats[5] = new DateFormat("y-M-d"); //$NON-NLS-2$

		createControls(parent);
	}
	
	private void createControls(Composite parent) {
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setLayout(new GridLayout(4, false));
		
		GridData gd0 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd0.horizontalIndent = 5;
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false,1 ,1);
		gd.horizontalIndent = 5;
		
		Group main = new Group(this, SWT.NONE );
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		main.setLayout(new GridLayout(2, false));
		main.setText("Select Required Columns Containing:");
		
		lblX = new Label(main, SWT.NONE);
		lblX.setText("X coordinate:");
		lblX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorX = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorX.getCombo().setLayoutData(gd0);
		cmbColumnSelectorX.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorX.setInput(columnNames);
		cmbColumnSelectorX.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
//				ISelection selection = cmbColumnSelectorX.getSelection();
//				if (!selection.isEmpty()) {
//				   IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
//				   ((CsvHeader) structuredSelection.getFirstElement()).setSelected(true);
//				}
				validate();
			}
		});

		lblY = new Label(main, SWT.NONE);
		lblY.setText("Y coordinate:");
		lblY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorY = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorY.getCombo().setLayoutData(gd0);
		cmbColumnSelectorY.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorY.setInput(columnNames);
		cmbColumnSelectorY.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
								
				validate();
			}
		});
		
		
		lblDate = new Label(main, SWT.NONE);
		lblDate.setText("Date:");
		lblDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorDate = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorDate.getCombo().setLayoutData(gd0);
		cmbColumnSelectorDate.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorDate.setInput(columnNames);
		cmbColumnSelectorDate.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				validate();
			}
		});

		lblTime = new Label(main, SWT.NONE);
		lblTime.setText("Time (HH:MM:SS) :");
		lblTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorTime = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorTime.getCombo().setLayoutData(gd0);
		cmbColumnSelectorTime.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorTime.setInput(columnNames);
		cmbColumnSelectorTime.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				validate();
			}
		});
		Group optional = new Group(this, SWT.NONE );
		optional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		optional.setLayout(new GridLayout(2, false));
		optional.setText("Optional Columns to Load:");
	
		lblId = new Label(optional, SWT.NONE);
		lblId.setText("Waypoint ID:");
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorId = new ComboViewer(optional, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorId.getCombo().setLayoutData(gd0);
		cmbColumnSelectorId.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorId.setInput(columnNames);
		cmbColumnSelectorId.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				validate();
			}
		});

		lblComments = new Label(optional, SWT.NONE);
		lblComments.setText("Comments Field:");
		lblComments.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorComments = new ComboViewer(optional, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorComments.getCombo().setLayoutData(gd0);
		cmbColumnSelectorComments.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorComments.setInput(columnNames);
		cmbColumnSelectorComments.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				
				validate();
			}
		});
		Group additional = new Group(this, SWT.NONE );
		additional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		additional.setLayout(new GridLayout(2, false));
		additional.setText("Data Specifications:");

		
		Label lblP = new Label(additional, SWT.NONE);
		lblP.setText("Coordinate Projection:");
		lblP.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lstProjections = new ComboViewer(additional, SWT.READ_ONLY);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true,1,1);
		gd2.widthHint = 100;
		lstProjections.getControl().setLayoutData(gd2);
		lstProjections.setLabelProvider(ProjectionLabelProvider.getInstance());
		lstProjections.setContentProvider(ArrayContentProvider.getInstance());
		lstProjections.setInput(projections);
		Projection defaultProj = projections.length > 0 ? projections[0] : null;
		for (int i = 0; i < projections.length; i ++){
			if (projections[i].getIsDefault() ){
				defaultProj = projections[i];
				break;
			}
		}
		if (defaultProj != null){
			lstProjections.setSelection(new StructuredSelection(defaultProj));			
		}
		lstProjections.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		
		Label lblD = new Label(additional, SWT.NONE);
		lblD.setText("Date Format:");
		lblD.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		cmbColumnSelectorDateFormat = new ComboViewer(additional, SWT.DROP_DOWN );
		cmbColumnSelectorDateFormat.getCombo().setLayoutData(gd);
		cmbColumnSelectorDateFormat.setContentProvider(new ArrayContentProvider());
		cmbColumnSelectorDateFormat.setInput(dateFormats);
		cmbColumnSelectorDateFormat.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		cmbColumnSelectorDateFormat.getCombo().addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();	
			}
		});

		
		skipHeaders = new Button(additional, SWT.CHECK);
		skipHeaders.setText("Skip the first row of column heading when importing.");
		skipHeaders.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2,1));
		
		
		//------------------------------------
		//Validation & Control decorations etc
		
		cdX = createDecoration(cmbColumnSelectorX.getControl());
		cdY = createDecoration(cmbColumnSelectorY.getControl());
		cdDate = createDecoration(cmbColumnSelectorDate.getControl());
		cdTime = createDecoration(cmbColumnSelectorTime.getControl());
		cdDateFormat = createDecoration(cmbColumnSelectorDateFormat.getControl());
		
	}

	public void addColumnsListener(ISelectionChangedListener listener) {
		cmbColumnSelectorX.addSelectionChangedListener(listener);
		cmbColumnSelectorY.addSelectionChangedListener(listener);
		cmbColumnSelectorDate.addSelectionChangedListener(listener);
		cmbColumnSelectorTime.addSelectionChangedListener(listener);
		cmbColumnSelectorDateFormat.addSelectionChangedListener(listener);
	}
	
		
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		lblX.setEnabled(enabled);

		cmbColumnSelectorX.getCombo().setEnabled(enabled);
		
	}
	
	
	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	private void validate() {
		valid = true;
		if(cmbColumnSelectorX.getSelection().isEmpty()){
			cdX.show();
			cdX.setDescriptionText("Required data missing, select a column");
			valid = false;
		}else{
			cdX.hide();
		}

		if(cmbColumnSelectorY.getSelection().isEmpty()){
			cdY.show();
			cdY.setDescriptionText("Required data missing, select a column");
			valid = false;
		}else{
			cdY.hide();
		}

		if(cmbColumnSelectorDate.getSelection().isEmpty()){
			cdDate.show();
			cdDate.setDescriptionText("Required data missing, select a column");
			valid = false;
		}else{
			cdDate.hide();
		}
		if(cmbColumnSelectorTime.getSelection().isEmpty()){
			cdTime.show();
			cdTime.setDescriptionText("Required data missing, select a column");
			valid = false;
		}else{
			cdTime.hide();
		}
		if(lstProjections.getSelection().isEmpty()){
			valid = false;
		}		
		if(cmbColumnSelectorDateFormat.getSelection().isEmpty()){
			cdDateFormat.show();
			String format = getDateFormat();
			if(format == ""){
				cdDateFormat.setDescriptionText("Required data missing, select a column");
				valid = false;
				return;
			}
			cdDateFormat.hide();
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(format);
			} catch (Exception e) {
				cdDateFormat.show();
				cdDateFormat.setDescriptionText("Invalid Date Format");
				valid = false;
			}
		}else{
			cdDateFormat.hide();
		}
		
	}

	public boolean isValid() {
		return valid;
	}
	

	
	
	public int getXColumnNumber(){
		ISelection selection = cmbColumnSelectorX.getSelection();
		return getColNum(selection);
	}
	public int getYColumnNumber(){
		ISelection selection = cmbColumnSelectorY.getSelection();
		return getColNum(selection);
	}
	public int getTimeColumnNumber(){
		ISelection selection = cmbColumnSelectorTime.getSelection();
		return getColNum(selection);
	}
	public int getDateColumnNumber(){
		ISelection selection = cmbColumnSelectorDate.getSelection();
		return getColNum(selection);
	}
	public int getIdColumnNumber(){
		ISelection selection = cmbColumnSelectorId.getSelection();
		return getColNum(selection);
	}
	public int getCommentsColumnNumber(){
		ISelection selection = cmbColumnSelectorComments.getSelection();
		return getColNum(selection);
	}
	
	public String getDateFormat(){
		ISelection selection = cmbColumnSelectorDateFormat.getSelection();
		if (!selection.isEmpty()) {
			   IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
			   DateFormat date = (DateFormat)structuredSelection.getFirstElement();
			   return date.toString();
		}
		return cmbColumnSelectorDateFormat.getCombo().getText();
	}
	
	public Projection GetProjection(){
		ISelection selection = lstProjections.getSelection();
		if (!selection.isEmpty()) {
			   IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
			   return (Projection)structuredSelection.getFirstElement();
		}
		return null;
	}
	
	public boolean skipHeaders(){
		return skipHeaders.getSelection();
	}
	
	private int getColNum(ISelection selection){
		if (!selection.isEmpty()) {
		   IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
		   CsvHeader hselection = (CsvHeader) structuredSelection.getFirstElement();
		   return hselection.getColumnNumber();
		}
		return -1;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	
	
}



