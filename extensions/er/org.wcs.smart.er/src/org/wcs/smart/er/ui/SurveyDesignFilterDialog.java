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
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;

/**
 * Filter dialog for managing a SurveyDesignFilter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyDesignFilterDialog extends SmartFilterDialog {

	//current filter
	private SurveyDesignFilter filter;
	
	private Button chActive;
	private Button chInactive;
	
	/**
	 * Create the dialog.
	 * @param parent parent shell
	 * @param filter the filter to update
	 */
	public SurveyDesignFilterDialog(Shell parent, IUpdatableView view, SurveyDesignFilter current) {
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
		
		Group g = new Group(main, SWT.NONE);
		g.setText(Messages.SurveyDesignFilterDialog_StateLabel);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		chActive = new Button(g, SWT.CHECK);
		chActive.setText(State.ACTIVE.getGuiName());
		chInactive = new Button(g, SWT.CHECK);
		chInactive.setText(State.INACTIVE.getGuiName());
		
		
		setTitle(Messages.SurveyDesignFilterDialog_Title);
		getShell().setText(Messages.SurveyDesignFilterDialog_Title);
		setMessage(Messages.SurveyDesignFilterDialog_Message);
		
		updateControlsValues();
		return main;
	}

	
}
