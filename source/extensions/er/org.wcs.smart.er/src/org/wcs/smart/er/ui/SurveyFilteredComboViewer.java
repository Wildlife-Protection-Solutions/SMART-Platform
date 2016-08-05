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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.SmartUtils.RegExLevel;

/**
 * Composite that contains {@link ComboViewer} and "Filter" button which launches
 * dialog that filters {@link ComboViewer} input content.
 * 
 * @author egouge
 * @since 4.0.1
 */
public class SurveyFilteredComboViewer extends FilteredComboViewer<Survey> {

    private SurveyFilter filter;
	private LoadSurveyIdJob loadSurveyJob;

	private LabelProvider surveyLblProvider;
	private boolean createNew;
	
	private Listener addSurveyListener;
	
	private List<Survey> createdSurveys;
	private List<Survey> additionalSurveys;
	
	/**
	 * 
	 * @param parent
	 * @param sd initial survey design; must be provided
	 * @param createNew if users can create new surveys;  a single
	 * new survey listener can be added to detect these new surveys
	 */
	public SurveyFilteredComboViewer(Composite parent, final SurveyDesign sd, boolean createNew) {
		super(parent);
		this.createNew = createNew;
		this.createdSurveys = new ArrayList<Survey>();

		getFilter().setDateFilter(DateFilter.LAST_60_DAYS, null, null);
		getFilter().setSurveyState(null);
		getFilter().setSurveyDesignKeyFilters(new String[]{sd.getKeyId()});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					if (! (((IStructuredSelection)selection).getFirstElement() instanceof Survey)){
						InputDialog id = new InputDialog(getShell(), Messages.SurveyFilteredComboViewer_Title, Messages.SurveyFilteredComboViewer_Message, "", new IInputValidator() { //$NON-NLS-1$
							@Override
							public String isValid(String newText) {
								if (!SmartUtils.isSimpleString(newText, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Survey.ID_MAX_LENGTH)){
									return MessageFormat.format(Messages.SurveyIdPage_IdError, new Object[]{Survey.ID_MAX_LENGTH, RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc});
								}
								return null;
							}
						});
						if (id.open() == InputDialog.OK){
							Survey s = new Survey();
							s.setMissions(new ArrayList<Mission>());
							s.setId(id.getValue());
							s.setSurveyDesign(sd);
							createdSurveys.add(s);
							getJob().setPreselectedSurvey(s);
							
							Event evt =  new Event();
							evt.widget = SurveyFilteredComboViewer.this;
							addSurveyListener.handleEvent(evt);
						}
						updateContent();
					}
				}
				
			}
		});
	}
	
	/**
	 * Sets a single listener that is fired when a new survey is added
	 * @param listener
	 */
	public void setNewSurveyListener(Listener listener){
		this.addSurveyListener = listener;
	}
	
	/**
	 * 
	 * @return list of new surveys created by the user
	 */
	public List<Survey> getCreatedSurveys(){
		return this.createdSurveys;
	}
	
	/**
	 * 
	 * @param surveys sets a list of additional surveys that should be added to the list
	 * irregardless of the current filter
	 */
	public void setAdditionalSurveys(List<Survey> surveys){
		this.additionalSurveys = surveys;
	}
	
	private synchronized LoadSurveyIdJob getJob(){
		if (loadSurveyJob == null){
			loadSurveyJob = new LoadSurveyIdJob();
		}
		return loadSurveyJob;
	}
	
	@Override
	public void updateContent() {
		LoadSurveyIdJob job = getJob();
		job.cancel();
		
		Object currentSurvey = getSelection();
		if (currentSurvey instanceof Survey && currentSurvey!= null) {
			job.setPreselectedSurvey((Survey)currentSurvey);
		}
		job.schedule();		
	}

	public synchronized SurveyFilter getFilter() {
		if (filter == null){
			filter = new SurveyFilter();
		}
		return filter;
	}
	
	@Override
	protected String getTooltip() {
		return Messages.SurveyFilteredComboViewer_tooltip;
	}

	@Override
	protected LabelProvider getLabelProvider() {
		if (surveyLblProvider == null){
			surveyLblProvider = new LabelProvider() {
		    	@Override
		    	public String getText(Object element) {
		    		if (element instanceof Survey) {
		    			return ((Survey)element).getId();
		    		}
		    		return super.getText(element);
		    	}
		    };
		}
		return surveyLblProvider;
	}

	@Override
	protected void showFilterDialog() {
		SurveyFilterDialog dialog = new SurveyFilterDialog(getShell(), this, getFilter());
		dialog.open();
	}

	@Override
	protected void loadListItems() {
		getJob().setPreselectedSurvey(currentSelection);
		getJob().schedule();
	}
	
	/**
	 * Job is used to fill some list viewer with data
	 * 
	 * @author elitvin
	 *
	 */
	private class LoadSurveyIdJob extends Job {
 
        private Survey preselectedSurvey;
    	
        public LoadSurveyIdJob() {
            super("Load Survey IDs"); //$NON-NLS-1$
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (viewer == null || viewer.getControl().isDisposed()){
                return Status.OK_STATUS;
            }
            final List<Survey> data = loadSurveyIds();
            final List<Object> input = new ArrayList<Object>();
            input.addAll(data);
            if (createNew){
            	input.add(Messages.SurveyFilteredComboViewer_NewSurveyLabel);
            }
            
            getDisplay().asyncExec(new Runnable(){
                @Override
                public void run() {
                    if (viewer.getControl().isDisposed()){
                        return ;
                    }
                    viewer.setInput(input);
                    if (preselectedSurvey != null) {
                    	viewer.setSelection(new StructuredSelection(preselectedSurvey));
                    }
                    SurveyFilteredComboViewer.this.getParent().getParent().layout(true);
                }});
            return Status.OK_STATUS;
        }
 
        private List<Survey> loadSurveyIds() {
        		//{survey uuid, survey id, start date, survey design name, sd uuid}
        	Session session = HibernateManager.openSession();
        	try{
        		Query query = getFilter().buildQuery(session);
        		List<?> results = query.list();
        		List<Survey> surveys = new ArrayList<Survey>(results.size()+1);
        		boolean defaultPresent = preselectedSurvey == null; //indicated if default patrol id is in filtered list
        		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
        			Object[] data = (Object[]) iterator.next();
        			
        			Survey temp = new Survey();
        			temp.setUuid((UUID)data[0]);
        			temp.setId((String)data[1]);
        			temp.setStartDate((Date)data[2]);

        			SurveyDesign tmp = new SurveyDesign();
        			tmp.setName((String)data[3]);
        			tmp.setUuid((UUID)data[4]);
        			temp.setSurveyDesign(tmp);
        			
        			defaultPresent = defaultPresent || temp.equals(preselectedSurvey);
        			surveys.add(temp);
        		}
        		if (additionalSurveys != null){
        			for (Survey s : additionalSurveys){
        				if (s == preselectedSurvey){
        					defaultPresent = true;
        					break;
        				}
        			}
        			surveys.addAll(additionalSurveys);
        		}
        		
        		if (!defaultPresent) {
        			//we don't want to reset selection to null if previously selected patrol is not in filtered list
        			//this is why we add it to result list
        			surveys.add(preselectedSurvey);
        		}
        		
        		return surveys;
        	}finally{
        		session.close();
        	}
        }
        
        public void setPreselectedSurvey(Survey preselectedSurvey) {
			this.preselectedSurvey = preselectedSurvey;
		}
    }


}
