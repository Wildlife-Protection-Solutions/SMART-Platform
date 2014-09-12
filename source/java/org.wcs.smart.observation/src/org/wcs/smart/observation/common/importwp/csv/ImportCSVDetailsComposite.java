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
package org.wcs.smart.observation.common.importwp.csv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Option composite for importing waypoints from csv.  
 * 
 * @author Jeff
 * since 2.0
 *
 */
public class ImportCSVDetailsComposite extends Composite{
	
	private boolean valid;	
	
	private Label lblX;
	private Label lblY;
	private Label lblDate;
	private Label lblTime;
	private Label lblId;
	private Label lblComments;
	
	
	private ComboViewer cmbColumnSelectorX;
	private ComboViewer cmbColumnSelectorY;
	private ComboViewer cmbColumnSelectorDate;
	private ComboViewer cmbColumnSelectorTime;
	private ComboViewer cmbColumnSelectorComments;
	private ComboViewer cmbColumnSelectorId;
	
	private ComboViewer lstProjections;
	private Projection[] projections;
	
	private ComboViewer cmbColumnSelectorDateFormat;
	private String[] dateFormats;
	
	private Button skipHeaders;
	
	
	private ControlDecoration cdX;
	private ControlDecoration cdY;
	private ControlDecoration cdDate;
	private ControlDecoration cdTime;
	private ControlDecoration cdDateFormat;
	private List<Listener> changeListeners = new ArrayList<Listener>();
	
	private String configFileName;
		
	public ImportCSVDetailsComposite(Composite parent) {
		super(parent, SWT.NONE);
		
		Session session = HibernateManager.openSession();
		try{
			//	load projection list
			session.beginTransaction();
			try{
				List<Projection> tmp = HibernateManager.getCaProjectionList(session);
				this.projections = tmp.toArray(new Projection[tmp.size()]);
			}finally{
				session.getTransaction().commit();
			}
		}finally{
			session.close();
		}
		
		
		dateFormats = new String[6];
		dateFormats[0] = new String("d/M/y"); //$NON-NLS-1$
		dateFormats[1] = new String("d-M-y"); //$NON-NLS-1$
		dateFormats[2] = new String("M/d/y"); //$NON-NLS-1$
		dateFormats[3] = new String("M-d-y"); //$NON-NLS-1$
		dateFormats[4] = new String("y/M/d"); //$NON-NLS-1$
		dateFormats[5] = new String("y-M-d"); //$NON-NLS-1$

		createControls(parent);
	}
	
	private void createControls(Composite parent) {
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setLayout(new GridLayout(4, false));
		
		GridData gd0 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd0.horizontalIndent = 5;
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false,1 ,1);
		gd.horizontalIndent = 5;
		
		Group main = new Group(this, SWT.NONE );
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		main.setLayout(new GridLayout(2, false));
		main.setText(Messages.ImportCSVDetailsComposite_0);
		
		lblX = new Label(main, SWT.NONE);
		lblX.setText(Messages.ImportCSVDetailsComposite_1);
		lblX.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblX.setToolTipText(Messages.ImportCSVDetailsComposite_XYTooltip);
		ISelectionChangedListener validateListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();	
			}
		};
		
		cmbColumnSelectorX = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorX.getCombo().setLayoutData(gd0);
		cmbColumnSelectorX.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorX.addSelectionChangedListener(validateListener);
		
		lblY = new Label(main, SWT.NONE);
		lblY.setText(Messages.ImportCSVDetailsComposite_2);
		lblY.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblY.setToolTipText(Messages.ImportCSVDetailsComposite_XYTooltip);
		
		cmbColumnSelectorY = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorY.getCombo().setLayoutData(gd0);
		cmbColumnSelectorY.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorY.addSelectionChangedListener(validateListener);
		
		
		lblDate = new Label(main, SWT.NONE);
		lblDate.setText(Messages.ImportCSVDetailsComposite_3);
		lblDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblDate.setToolTipText(Messages.ImportCSVDetailsComposite_DateTooltip);
		
		cmbColumnSelectorDate = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorDate.getCombo().setLayoutData(gd0);
		cmbColumnSelectorDate.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorDate.addSelectionChangedListener(validateListener);
		
		lblTime = new Label(main, SWT.NONE);
		lblTime.setText(Messages.ImportCSVDetailsComposite_4);
		lblTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblTime.setToolTipText(Messages.ImportCSVDetailsComposite_TimeFormatToolTip);
				
		cmbColumnSelectorTime = new ComboViewer(main, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorTime.getCombo().setLayoutData(gd0);
		cmbColumnSelectorTime.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorTime.addSelectionChangedListener(validateListener);
		
		Group optional = new Group(this, SWT.NONE );
		optional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		optional.setLayout(new GridLayout(2, false));
		optional.setText(Messages.ImportCSVDetailsComposite_5);
	
		lblId = new Label(optional, SWT.NONE);
		lblId.setText(Messages.ImportCSVDetailsComposite_6);
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorId = new ComboViewer(optional, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorId.getCombo().setLayoutData(gd0);
		cmbColumnSelectorId.setContentProvider(ArrayContentProvider.getInstance());		
		cmbColumnSelectorId.addSelectionChangedListener(validateListener);

		lblComments = new Label(optional, SWT.NONE);
		lblComments.setText(Messages.ImportCSVDetailsComposite_7);
		lblComments.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbColumnSelectorComments = new ComboViewer(optional, SWT.READ_ONLY | SWT.BORDER);
		cmbColumnSelectorComments.getCombo().setLayoutData(gd0);
		cmbColumnSelectorComments.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorComments.addSelectionChangedListener(validateListener);
		
		Group additional = new Group(this, SWT.NONE );
		additional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		additional.setLayout(new GridLayout(2, false));
		additional.setText(Messages.ImportCSVDetailsComposite_8);

		
		Label lblP = new Label(additional, SWT.NONE);
		lblP.setText(Messages.ImportCSVDetailsComposite_9);
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
		lstProjections.addSelectionChangedListener(validateListener);
		
		
		Label lblD = new Label(additional, SWT.NONE);
		lblD.setText(Messages.ImportCSVDetailsComposite_10);
		lblD.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		lblD.setToolTipText(Messages.ImportCSVDetailsComposite_DateFormatTooltip);
		
		cmbColumnSelectorDateFormat = new ComboViewer(additional, SWT.DROP_DOWN );
		cmbColumnSelectorDateFormat.getCombo().setLayoutData(gd);
		cmbColumnSelectorDateFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbColumnSelectorDateFormat.setInput(dateFormats);
		cmbColumnSelectorDateFormat.getCombo().setText(Messages.ImportCSVDetailsComposite_DateDefaultOption);
		cmbColumnSelectorDateFormat.addSelectionChangedListener(validateListener);
		cmbColumnSelectorDateFormat.getCombo().addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();	
			}
		});
		
		skipHeaders = new Button(additional, SWT.CHECK);
		skipHeaders.setText(Messages.ImportCSVDetailsComposite_11);
		skipHeaders.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2,1));
		
		
		//------------------------------------
		//Validation & Control decorations etc
		
		cdX = createDecoration(cmbColumnSelectorX.getControl());
		cdY = createDecoration(cmbColumnSelectorY.getControl());
		cdDate = createDecoration(cmbColumnSelectorDate.getControl());
		cdTime = createDecoration(cmbColumnSelectorTime.getControl());
		cdDateFormat = createDecoration(cmbColumnSelectorDateFormat.getControl());
	
		validate();
	}

	public void setConfigData(CSVImportConfiguration config) {
		CsvHeader[] columnNames = config.getAvailableColumns();
		if (columnNames != null) {
			cmbColumnSelectorX.setInput(columnNames);
			cmbColumnSelectorY.setInput(columnNames);
			cmbColumnSelectorDate.setInput(columnNames);
			cmbColumnSelectorTime.setInput(columnNames);
			cmbColumnSelectorId.setInput(columnNames);
			cmbColumnSelectorComments.setInput(columnNames);
		}
		configFileName = config.getFilename();
	}
	
	/**
	 * fired when any field is changed; after the data
	 * is validated so isValid field will be correct
	 * @param listener
	 */
	public void addChangeListener(Listener listener){
		changeListeners.add(listener);
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
	public void validate() {
		valid = true;
		if(cmbColumnSelectorX.getSelection().isEmpty()){
			cdX.show();
			cdX.setDescriptionText(Messages.ImportCSVDetailsComposite_12);
			valid = false;
		}else{
			cdX.hide();
		}

		if(cmbColumnSelectorY.getSelection().isEmpty()){
			cdY.show();
			cdY.setDescriptionText(Messages.ImportCSVDetailsComposite_13);
			valid = false;
		}else{
			cdY.hide();
		}

		if(cmbColumnSelectorDate.getSelection().isEmpty()){
			cdDate.show();
			cdDate.setDescriptionText(Messages.ImportCSVDetailsComposite_14);
			valid = false;
		}else{
			cdDate.hide();
		}
		if(cmbColumnSelectorTime.getSelection().isEmpty()){
			cdTime.show();
			cdTime.setDescriptionText(Messages.ImportCSVDetailsComposite_15);
			valid = false;
		}else{
			cdTime.hide();
		}
		if(lstProjections.getSelection().isEmpty()){
			valid = false;
		}		
		if(cmbColumnSelectorDateFormat.getSelection().isEmpty()){
			String format = getDateFormat();
			if(format.equals("")){ //$NON-NLS-1$
				cdDateFormat.show();
				cdDateFormat.setDescriptionText(Messages.ImportCSVDetailsComposite_16);
				valid = false;
			}else{
				try{
					new SimpleDateFormat(format);
					cdDateFormat.hide();
				}catch(Exception ex){
					cdDateFormat.setDescriptionText(Messages.ImportCSVDetailsComposite_InvalidDateFormat + ex.getLocalizedMessage());
					cdDateFormat.show();
					valid = false;
				}
				
			}
		}else{
			cdDateFormat.hide();
		}
		
		
		for (Listener l : changeListeners){
			l.handleEvent(null);
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
			   String date = (String)structuredSelection.getFirstElement();
			   return date;
		}
		return cmbColumnSelectorDateFormat.getCombo().getText();
	}
	
	public Projection getProjection(){
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

	public String getConfigFileName() {
		return configFileName;
	}
}



