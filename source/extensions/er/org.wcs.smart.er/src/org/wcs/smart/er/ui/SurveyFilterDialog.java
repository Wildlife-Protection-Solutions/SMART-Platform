package org.wcs.smart.er.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.er.hibernate.SurveyDesignFilter;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;

public class SurveyFilterDialog extends SmartFilterDialog {

	//current filter
	private SurveyFilter filter;
	
	private Button chActive;
	private Button chInactive;
	
	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public SurveyFilterDialog(Shell parent, IUpdatableView view, SurveyFilter current) {
		super(parent, view);
		this.filter = current;
	}

	
	@Override
	protected void resetFilterModel() {
		filter.setDefaults();
	}
	
	/*
	 * Updates the current filter with the values from the user
	 */
	@Override
	protected void updateFilterModel(){
		List<State> states = new ArrayList<State>();
		if (chActive.getSelection()){
			states.add(SurveyDesign.State.ACTIVE);
		}
		if (chInactive.getSelection()){
			states.add(SurveyDesign.State.INACTIVE);
		}
		filter.setSurveyStates(states.toArray(new State[states.size()]));
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
		chActive.setSelection(false);
		chInactive.setSelection(false);
		for (State s : filter.getSurveyStateFilters()){
			if (s == State.ACTIVE){
				chActive.setSelection(true);
			}else if (s == State.INACTIVE){
				chInactive.setSelection(true);
			}
		}
	}
	
	/**
	 * Create contents of the dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g = new Group(main, SWT.DEFAULT);
		g.setText("Survey Design State");
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		chActive = new Button(g, SWT.CHECK);
		chActive.setText(State.ACTIVE.getGuiName());
		chInactive = new Button(g, SWT.CHECK);
		chInactive.setText(State.INACTIVE.getGuiName());
		
		
		setTitle("Survey Design Filter");
		getShell().setText("Survey Design Filter");
		setMessage("Filters the list of survey design.");
		
		updateControlsValues();
		return main;
	}

	
}
