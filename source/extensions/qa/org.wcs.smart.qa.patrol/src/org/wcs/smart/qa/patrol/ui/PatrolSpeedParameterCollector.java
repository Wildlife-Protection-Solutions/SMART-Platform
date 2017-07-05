/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.patrol.routine.PatrolSpeedRoutineType;
import org.wcs.smart.qa.ui.configure.IParameterCollector;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * A parameter collector for getting the polygon to use for validating
 * waypoint locations.
 * 
 * @author Emily
 *
 */
public class PatrolSpeedParameterCollector extends IParameterCollector {

	private Text txtMaxSpeed;
	
	private CheckboxTableViewer tblTransportTypes;
	
	private ControlDecoration cdMax;
	private ControlDecoration cdTt;
	
	private boolean isValid = false;
	
	@Override
	public void createUi(Composite composite) {

		Composite panel = new Composite(composite, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(panel, SWT.NONE);
		l.setText("Maximum Speed (km/h):");
		
		txtMaxSpeed = new Text(panel, SWT.BORDER);
		txtMaxSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtMaxSpeed.addListener(SWT.Modify, e->validate());
		
		l = new Label(panel, SWT.NONE);
		l.setText("Patrol Transport Types:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		
		cdTt = new ControlDecoration(l, SWT.RIGHT | SWT.TOP);
		cdTt.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdTt.hide();
		
		tblTransportTypes = CheckboxTableViewer.newCheckList(panel, SWT.BORDER);
		tblTransportTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblTransportTypes.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof PatrolTransportType){
					return ((PatrolTransportType) element).getName();
				}
				return super.getText(element);
			}
		});
		tblTransportTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblTransportTypes.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				validate();
			}
		});
		tblTransportTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		cdMax = new ControlDecoration(txtMaxSpeed, SWT.LEFT | SWT.TOP);
		cdMax.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdMax.hide();
		
		loadTransportTypes();
		validate();
	}

	public boolean isValid(){
		return this.isValid;
	}
	
	private void validate(){
		isValid = true;
		try{
			cdMax.hide();
			Double.parseDouble(txtMaxSpeed.getText());
		}catch (Exception ex){
			isValid = false;
			cdMax.setDescriptionText("Invalid maximum speed value");
			cdMax.show();	
		}
		
		cdTt.hide();
		if (tblTransportTypes.getCheckedElements().length == 0){
			isValid = false;
			cdTt.setDescriptionText("At least one transport type must be selected");
			cdTt.show();
		}else{
			boolean found = false;
			for (Object x : tblTransportTypes.getCheckedElements()){
				if (x instanceof PatrolTransportType){
					found = true;
					break;
				}
			}
			if (!found){
				isValid = false;
				cdTt.setDescriptionText("At least one transport type must be selected");
				cdTt.show();
			}
		}
		
		fireListeners();
	}
	
	private void loadTransportTypes(){
		Job j = new Job("init transport types"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final List<PatrolTransportType> transportTypes = new ArrayList<>();
				Session s = HibernateManager.openSession();
				try{
					transportTypes.addAll(s.createCriteria(PatrolTransportType.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.list());
					for (PatrolTransportType t : transportTypes){
						t.getName().length();
					}
				}catch (Exception ex){
					QaPlugIn.displayLog("Unable to read conservation area boundary layers from the database: " + ex.getMessage(), ex);
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					tblTransportTypes.setInput(transportTypes);
					initializeTypes();
					validate();
				});
				return Status.OK_STATUS;
			} 
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	
	private QaRoutineParameter initTypesParam;
	
	private void initializeTypes(){
		if (initTypesParam == null) return;
		
		String[] bits = initTypesParam.getStringValue().split(PatrolSpeedRoutineType.PARAM_SEP);
		Set<String> keys = new HashSet<>();
		for (String bit : bits) keys.add(bit);
		
		List<PatrolTransportType> types = new ArrayList<>();
		Object items = tblTransportTypes.getInput();
		if (items instanceof List){
			for (Object item : (List)items){
				if (item instanceof PatrolTransportType){
					if (keys.contains(((PatrolTransportType) item).getKeyId())){
						types.add((PatrolTransportType)item);
					}
				}
			}
		}
		tblTransportTypes.setCheckedElements(types.toArray());
	}
	
	@Override
	public void initUi(QaRoutine routine) {
		QaRoutineParameter maxSpeedParam = (QaRoutineParameter)routine.findParameter(PatrolSpeedRoutineType.MAX_SPEED_PARAM_ID);
		QaRoutineParameter typesParam = (QaRoutineParameter)routine.findParameter(PatrolSpeedRoutineType.PATROL_TYPES_PARAM_ID);
		
		if (maxSpeedParam != null){
			txtMaxSpeed.setText(maxSpeedParam.getStringValue());
		}
		this.initTypesParam = typesParam;
		initializeTypes();
	}

	@Override
	public void updateParameters(QaRoutine routine) {
		Double maxSpeed = Double.parseDouble(txtMaxSpeed.getText());
		QaRoutineParameter speedParam = (QaRoutineParameter)routine.findParameter(PatrolSpeedRoutineType.MAX_SPEED_PARAM_ID);
		if (speedParam == null){
			speedParam = new QaRoutineParameter();
			speedParam.setParameterId(PatrolSpeedRoutineType.MAX_SPEED_PARAM_ID);
			speedParam.setQaRoutine(routine);
			if (routine.getParameters() == null){
				routine.setParameters(new ArrayList<>());
			}
			routine.getParameters().add(speedParam);
		}
		speedParam.setStringValue(String.valueOf(maxSpeed));
		
		QaRoutineParameter typesParam = (QaRoutineParameter)routine.findParameter(PatrolSpeedRoutineType.PATROL_TYPES_PARAM_ID);
		if (typesParam == null){
			typesParam = new QaRoutineParameter();
			typesParam.setParameterId(PatrolSpeedRoutineType.PATROL_TYPES_PARAM_ID);
			typesParam.setQaRoutine(routine);
			if (routine.getParameters() == null){
				routine.setParameters(new ArrayList<>());
			}
			routine.getParameters().add(typesParam);
		}
		StringBuilder sb = new StringBuilder();
		for (Object x : tblTransportTypes.getCheckedElements()){
			if (x instanceof PatrolTransportType){
				sb.append(((PatrolTransportType) x).getKeyId());
				sb.append(",");
			}
		}
		if (sb.length() > 0){
			sb.deleteCharAt(sb.length() - 1);
		}
		typesParam.setStringValue(sb.toString());
	}

}
