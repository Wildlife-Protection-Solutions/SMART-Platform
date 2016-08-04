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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.mission.export.IMissionFilteringView;
import org.wcs.smart.er.ui.mission.export.MissionFilterDialog;
import org.wcs.smart.er.ui.mission.export.MissionViewFilter;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Composite that contains {@link ComboViewer} and "Filter" button which launches
 * dialog that filters {@link ComboViewer} input content.
 * 
 * @author egouge
 * @since 4.0.1
 */
public class MissionFilteredComboViewer extends FilteredComboViewer<Mission> implements IMissionFilteringView {

    private MissionViewFilter filter;
	private LoadMissionIdJob loadMissionJob;

	private LabelProvider missionLblProvider;
	
	public MissionFilteredComboViewer(Composite parent) {
		super(parent);
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

	public synchronized MissionViewFilter getFilter() {
		if (filter == null){
			filter = new MissionViewFilter();
		}
		return filter;
	}
	

	@Override
	protected String getTooltip() {
		return "filter missions displayed in drop down list";
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
		MissionFilterDialog dialog = new MissionFilterDialog(getShell(), this);
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
            super("Load Mission IDs");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (viewer == null || viewer.getControl().isDisposed()){
                return Status.OK_STATUS;
            }
            final List<Mission> data = loadMissionIds();
            
            getDisplay().asyncExec(new Runnable(){
                @Override
                public void run() {
                    if (viewer.getControl().isDisposed()){
                        return ;
                    }
                    viewer.setInput(data);
                    if (preselectedMission != null) {
                    	setStopSelectionPropogation(true);
                    	viewer.setSelection(new StructuredSelection(preselectedMission));
                    }
                    MissionFilteredComboViewer.this.getParent().getParent().layout(true);
                }});
            return Status.OK_STATUS;
        }
 
        private List<Mission> loadMissionIds() {
        	Session s = HibernateManager.openSession();
        	try {
        		Query query = getFilter().buildQuery(s);
        		List<?> results = query.list(); // mission uuid, mission id, mission start date, mission end date, survey id, survey uuid, survey design name
        		List<Mission> missions = new ArrayList<Mission>(results.size()+1);
        		boolean defaultPresent = preselectedMission == null; //indicated if default patrol id is in filtered list
        		for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
        			Object[] data = (Object[]) iterator.next();
        			Mission p = new Mission();
        			p.setUuid((UUID)data[0]);
        			p.setId((String)data[1]);
        			p.setStartDate((Date)data[2]);
        			p.setEndDate((Date)data[3]);
        			defaultPresent = defaultPresent || p.equals(preselectedMission);
        			missions.add(p);
        		}
        		if (!defaultPresent) {
        			//we don't want to reset selection to null if previously selected patrol is not in filtered list
        			//this is why we add it to result list
        			missions.add(preselectedMission);
        		}
        		return missions;
        	} finally {
        		s.close();
        	}
        }
        
        public void setPreselectedPatrol(Mission preselectedMission) {
			this.preselectedMission = preselectedMission;
		}
    }



}
