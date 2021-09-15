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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyMissionProxy;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Composite that contains {@link ComboViewer} and "Filter" button which launches
 * dialog that filters {@link ComboViewer} input content.
 * 
 * @author egouge
 * @since 4.0.1
 */
public class MissionFilteredComboViewer extends FilteredComboViewer<Mission> {

    private SurveyFilter filter;
	private LoadMissionIdJob loadMissionJob;

	private LabelProvider missionLblProvider;
	private List<Mission> additionalMissions;
	
	/**
	 * 
	 * @param parent
	 * @param addMissions list of missions not yet in database that should be
	 * added to list (irregardless of filter)  Can be null
	 */
	public MissionFilteredComboViewer(Composite parent, List<Mission> addMissions) {
		super(parent);
		this.additionalMissions = addMissions;
	}
	
	private synchronized LoadMissionIdJob getJob(){
		if (loadMissionJob == null){
			loadMissionJob = new LoadMissionIdJob();
		}
		return loadMissionJob;
	}
	
	@Override
	public void updateContent() {
		LoadMissionIdJob job = getJob();
		job.cancel();
		Mission currentPatrol = getSelection();
		if (currentPatrol != null) {
			job.setPreselectedPatrol(currentPatrol);
		}
		job.schedule();		
	}

	public synchronized SurveyFilter getFilter() {
		if (filter == null){
			filter = SurveyFilter.newInstance();
		}
		return filter;
	}
	

	@Override
	protected String getTooltip() {
		return Messages.MissionFilteredComboViewer_tooltip;
	}

	@Override
	protected LabelProvider getLabelProvider() {
		if (missionLblProvider == null){
			missionLblProvider = new LabelProvider() {
		    	@Override
		    	public String getText(Object element) {
		    		if (element instanceof Mission) {
		    			return ((Mission)element).getId();
		    		}
		    		return super.getText(element);
		    	}
			};
		}
		return missionLblProvider;
	}

	@Override
	protected void showFilterDialog() {
		SurveyFilterDialog dialog = new SurveyFilterDialog(getShell(), this, filter);
		dialog.open();
	}

	@Override
	protected void loadListItems() {
		getJob().setPreselectedPatrol(currentSelection);
		getJob().schedule();
	}
	
	/**
	 * Job is used to fill some list viewer with data
	 * 
	 * @author elitvin
	 *
	 */
	private class LoadMissionIdJob extends Job {
 
        private Mission preselectedMission;
    	
        public LoadMissionIdJob() {
            super("Load Mission IDs"); //$NON-NLS-1$
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            final List<Mission> data = loadMissionIds();
            if (viewer == null || viewer.getControl().isDisposed() || isDisposed()){
                return Status.OK_STATUS;
            }
            getDisplay().asyncExec(new Runnable(){
                @Override
                public void run() {
                    if (viewer.getControl().isDisposed()){
                        return ;
                    }
                    viewer.setInput(data);
                    if (preselectedMission != null) {
//                    	setStopSelectionPropogation(true);
                    	viewer.setSelection(new StructuredSelection(preselectedMission));
                    }
                    MissionFilteredComboViewer.this.getParent().getParent().layout(true);
                }});
            return Status.OK_STATUS;
        }
 
        private List<Mission> loadMissionIds() {
        	try(Session s = HibernateManager.openSession()){
        		List<SurveyMissionProxy> items = getFilter().executeQuery(s);
        		
        		List<Mission> missions = new ArrayList<Mission>();
        		boolean defaultPresent = preselectedMission == null; //indicated if default patrol id is in filtered list
        		for (SurveyMissionProxy survey : items) {
        			for (SurveyMissionProxy mission : survey.getMissions()) {
        			
	        			Mission p = new Mission();
	        			p.setUuid(mission.getUuid());
	        			p.setId(mission.getId());
	        			p.setStartDate(mission.getStartDate());
	        			p.setEndDate(mission.getEndDate());
	        			
	        			defaultPresent = defaultPresent || p.equals(preselectedMission);
	        			missions.add(p);
        			}
        		}
        		if (additionalMissions != null){
        			for (Mission m : additionalMissions){
        				if (m == preselectedMission){
        					defaultPresent = true;
        				}
        			}
        			missions.addAll(additionalMissions);
        		}
        		
        		if (!defaultPresent) {
        			//we don't want to reset selection to null if previously selected patrol is not in filtered list
        			//this is why we add it to result list
        			missions.add(preselectedMission);
        		}
        		return missions;
        	}
        }
        
        public void setPreselectedPatrol(Mission preselectedMission) {
			this.preselectedMission = preselectedMission;
		}
    }



}
