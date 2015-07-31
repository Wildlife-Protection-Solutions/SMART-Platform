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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.IUpdatableView;
import org.wcs.smart.common.filter.SmartFilterDialog;
import org.wcs.smart.common.filter.StringFilterComposite;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign.State;
import org.wcs.smart.er.ui.surveydesign.editor.SurveyDesignEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Dialog for managing survey filter.
 * @author Emily
 *
 */
public class SurveyFilterDialog extends SmartFilterDialog  {

	private StringFilterComposite.TextField surveyField = new StringFilterComposite.TextField(Messages.SurveyFilterDialog_SurveyIdFieldLabel, "id"); //$NON-NLS-1$
	
	//current filter
	private SurveyFilter filter;
	
	private Button opActive;
	private Button opInactive;
	private Button opAll;
	private Button opSelected;
	
	private CheckboxTableViewer lstDesigns ;
	
	private StringFilterComposite nameFilter ;
	private DateFilterComposite dateComp ;
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
		if (opActive.getSelection()){
			filter.setSurveyState(State.ACTIVE);
		}else if (opInactive.getSelection()){
			filter.setSurveyState(State.INACTIVE);
		}else if (opAll.getSelection()){
			filter.setSurveyState(null);
			filter.setSurveyDesignKeyFilters(null);
		}else if (opSelected.getSelection()){
			filter.setSurveyState(null);
			Object[] c = lstDesigns.getCheckedElements();
			String[] keys = new String[c.length];
			int i = 0;
			for (Object cc : c){
				keys[i++] = ((SurveyDesignEditorInput)cc).getSurveyDesignKey();
			}
			filter.setSurveyDesignKeyFilters(keys);
			
			if (keys.length == 0){
				MessageDialog.openWarning(getParentShell(), Messages.SurveyFilterDialog_WarnTitle, Messages.SurveyFilterDialog_WarnInfo); 
			}
		}
		
		filter.setDateFilter(dateComp.getDateFilterForModel(), dateComp.getStartDateForModel(), dateComp.getEndDateForModel());
		filter.setSurveyNameFilter(nameFilter.getComparisonForModel(), nameFilter.getFilterValueForModel());
	}

	/**
	 * Updates the widgets with the values from the current filter
	 */
	@Override
	protected void updateControlsValues(){
		State state = filter.getSurveyStateFilter();
		
		opActive.setSelection(false);
		opInactive.setSelection(false);
		opAll.setSelection(false);
		opSelected.setSelection(false);
		if (state != null){
			if (state == State.ACTIVE){
				opActive.setSelection(true);
			}else if (state == State.INACTIVE){
				opInactive.setSelection(true);
			}
		}else{
			if (filter.getDesignKeys() == null){
				opAll.setSelection(true);
			}else{
				opSelected.setSelection(true);
				initDesignSelection();
			}
		}
		
		dateComp.applyState(filter.getDateFilter(), filter.getStartDate(), filter.getEndDate());
		nameFilter.applyState(filter.getSurveyNameComparator(), filter.getSurveyNameFilter(), surveyField);
		updateDesignEnabled();
	}
	
	private void initDesignSelection(){
		if (filter.getSurveyStateFilter() == null && filter.getDesignKeys() != null){
			lstDesigns.setAllChecked(false);
			
			Object x = lstDesigns.getInput();
			if (x instanceof List){
				List<?> items = (List<?>) x;
				for (Object i : items){
					if (i instanceof SurveyDesignEditorInput){
						String key = ((SurveyDesignEditorInput)i).getSurveyDesignKey();
						for (String fKey : filter.getDesignKeys()){
							if (fKey.equals(key)){
								lstDesigns.setChecked(i, true);
								break;
							}
						}
					}
				}
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
		g.setText(Messages.SurveyFilterDialog_DatesGroup);
		g.setLayout(new GridLayout());
		((GridLayout)g.getLayout()).marginWidth = 0;
		((GridLayout)g.getLayout()).marginHeight = 0;
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		dateComp = new DateFilterComposite(g, SWT.NONE, this);
		
		
		g = new Group(main, SWT.NONE);
		g.setText(Messages.SurveyFilterDialog_IdGroup);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)g.getLayout()).marginWidth = 0;
		((GridLayout)g.getLayout()).marginHeight = 0;
		nameFilter = new StringFilterComposite(g, SWT.NONE, new StringFilterComposite.TextField[]{surveyField});
		
		
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDesignEnabled();	
			}
		};
		g = new Group(main, SWT.NONE);
		g.setText(Messages.SurveyFilterDialog_DesignGroup);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		

		Composite tmp = new Composite(g, SWT.NONE);
		tmp.setLayout(new GridLayout());
		tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		opAll = new Button(tmp, SWT.RADIO);
		opAll.setText(Messages.SurveyFilterDialog_AllDesignsLabel);
		opAll.addSelectionListener(listener);
		
		opActive = new Button(tmp, SWT.RADIO);
		opActive.setText(MessageFormat.format(Messages.SurveyFilterDialog_StateOnlyLabel, new Object[]{State.ACTIVE.getGuiName(Locale.getDefault())}));
		opActive.addSelectionListener(listener);
		
		opInactive = new Button(tmp, SWT.RADIO);
		opInactive.setText(MessageFormat.format(Messages.SurveyFilterDialog_StateOnlyLabel, new Object[]{State.INACTIVE.getGuiName(Locale.getDefault())}));
		opInactive.addSelectionListener(listener);
		
		opSelected = new Button(tmp, SWT.RADIO);
		opSelected.setText(Messages.SurveyFilterDialog_SelectedLabel);
		opSelected.addSelectionListener(listener);
		
		Composite tmp2 = new Composite(tmp, SWT.NONE);
		tmp2.setLayout(new GridLayout());
		((GridLayout)tmp2.getLayout()).marginWidth = 20;
		tmp2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstDesigns = CheckboxTableViewer.newCheckList(tmp2, SWT.BORDER);
		lstDesigns.setContentProvider(ArrayContentProvider.getInstance());
		lstDesigns.setLabelProvider(SurveyDesignLabelProvider.getInstance());
		lstDesigns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstDesigns.getControl().getLayoutData()).heightHint = 60;
		
		
		lstDesigns.setInput(new String[]{Messages.SurveyFilterDialog_LoadingLabel});
		lstDesigns.getControl().setEnabled(false);
		
		opAll.setSelection(true);
//		lstDesignOps.setSelection(new StructuredSelection(DesignOps.ACTIVE));
		
		setTitle(Messages.SurveyFilterDialog_Title);
		getShell().setText(Messages.SurveyFilterDialog_Title);
		setMessage(Messages.SurveyFilterDialog_Message);
		
		loadDesign.schedule();
		updateControlsValues();
		return main;
	}

	private void updateDesignEnabled(){
//		DesignOps op = (DesignOps) ((StructuredSelection)lstDesignOps.getSelection()).getFirstElement();
//		lstDesigns.getControl().setEnabled(op == DesignOps.SELECTED);
		
		lstDesigns.getControl().setEnabled(opSelected.getSelection());
	}
	
	
	Job loadDesign = new Job(Messages.SurveyFilterDialog_LoadJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<SurveyDesignEditorInput> all = new ArrayList<SurveyDesignEditorInput>(); 
			Session s = HibernateManager.openSession();
			try{
				all.addAll(SurveyHibernateManager.getInstance().getSurveyDesignEditorInputs(s, null));
			}finally{
				s.close();
			}
			
			getShell().getDisplay().asyncExec(new Runnable(){
				@Override
				public void run() {
					lstDesigns.setInput(all);
					initDesignSelection();
				}});
			return Status.OK_STATUS;
		}
		
	};
}
