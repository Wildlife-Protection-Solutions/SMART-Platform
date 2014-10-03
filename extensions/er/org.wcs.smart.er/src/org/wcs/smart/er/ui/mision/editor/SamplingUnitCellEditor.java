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
package org.wcs.smart.er.ui.mision.editor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Cell Editor for {@link SamplingUnit} selection
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SamplingUnitCellEditor extends ComboBoxCellEditor {
	
	private List<Object> input = new ArrayList<Object>();

    private LoadSamplingUnitJob loadValues = new LoadSamplingUnitJob();

    private Date missionDate; 
    private boolean samplingUnitsOnly;
    
    public SamplingUnitCellEditor(Composite parent, boolean samplingUnitsOnly) {
		super(parent, new String[]{}, SWT.READ_ONLY);
		this.samplingUnitsOnly = samplingUnitsOnly;
	}

	public void setInput(Mission mission, Date missionDate) {
		this.missionDate = missionDate;
		setInput(mission.getSurvey().getSurveyDesign());
	}
	
	private void setInput(SurveyDesign surveyDesign) {
		loadValues.cancel();
		loadValues.setSurveyDesign(surveyDesign);
		loadValues.schedule();
		try {
			loadValues.join();
		} catch (InterruptedException e) {
			EcologicalRecordsPlugIn.log("SamplingUnit load failed.", e); //$NON-NLS-1$
			e.printStackTrace();
		}
		input = loadValues.getUnits();
		List<String> items = new ArrayList<String>();
		for (Object su : input) {
			items.add(getStringValue(su));
		}
		setItems(items.toArray(new String[items.size()]));
	}

	public Object getSamplingUnit(int index) {
		if (index < 0) return null;
		if (index < input.size())
			return input.get(index);
		return null;
	}

	public String getStringValue(Object unit) {
		if (unit == null){
			return Messages.SamplingUnitCellEditor_None;
		}else if (unit instanceof SamplingUnit){
			return ((SamplingUnit) unit).getId();
		}else if (unit instanceof MissionTrack){
			return ((MissionTrack) unit).getId();
		}
		return Messages.SamplingUnitCellEditor_None;
	}

	public Integer getIndex(Object unit) {
		return input.indexOf(unit);
	}
	
	private class LoadSamplingUnitJob extends Job {
		
		private SurveyDesign surveyDesign;
		private List<Object> units;
		
		public LoadSamplingUnitJob() {
			super(Messages.SamplingUnitCellEditor_LoadJobTitle);
		}
		
		public void setSurveyDesign(SurveyDesign surveyDesign) {
			this.surveyDesign = surveyDesign;
		}

		public List<Object> getUnits() {
			return units;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try {
				//load units
				List<Object> items = SurveyHibernateManager.getInstance().getSamplingUnits(surveyDesign, s);
				units = new ArrayList<Object>();
				for (Object i : items){
					if (i instanceof SamplingUnit){
						units.add(i);
					}else if (i instanceof MissionTrack){
						if (!samplingUnitsOnly){
							if (((MissionTrack) i).getDate().compareTo(missionDate) == 0){
								units.add(i);
							}
						}
					}
				}
				units.add(0, null);
			} finally {
				s.close();
			}
			return Status.OK_STATUS;
		}
		
	}
}
