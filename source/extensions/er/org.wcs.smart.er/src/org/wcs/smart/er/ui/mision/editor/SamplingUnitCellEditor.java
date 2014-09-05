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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.model.Mission;
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
	
	private List<SamplingUnit> input = new ArrayList<SamplingUnit>();

    private LoadSamplingUnitJob loadValues = new LoadSamplingUnitJob();

	public SamplingUnitCellEditor(Composite parent) {
		super(parent, new String[]{});
	}

	public void setInput(Mission mission) {
		setInput(mission.getSurvey().getSurveyDesign());
	}
	
	private void setInput(SurveyDesign surveyDesign) {
		loadValues.cancel();
		loadValues.setSurveyDesign(surveyDesign);
		loadValues.schedule();
		try {
			loadValues.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		input = loadValues.getUnits();
		List<String> items = new ArrayList<String>();
		for (SamplingUnit su : input) {
			items.add(getStringValue(su));
		}
		setItems(items.toArray(new String[items.size()]));
	}

	public SamplingUnit getSamplingUnit(int index) {
		if (index < input.size())
			return input.get(index);
		return null;
	}

	public String getStringValue(SamplingUnit unit) {
		return unit != null ? unit.getId() : "(None)";
	}

	public Integer getIndex(SamplingUnit unit) {
		return input.indexOf(unit);
	}
	
	private class LoadSamplingUnitJob extends Job {
		
		private SurveyDesign surveyDesign;
		private List<SamplingUnit> units;
		
		public LoadSamplingUnitJob() {
			super("loading sampling units");
		}
		
		public void setSurveyDesign(SurveyDesign surveyDesign) {
			this.surveyDesign = surveyDesign;
		}

		public List<SamplingUnit> getUnits() {
			return units;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session s = HibernateManager.openSession();
			try {
				//load units
				units = s.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", surveyDesign)).list(); //$NON-NLS-1$
				units.add(0, null);
			} finally {
				s.close();
			}
			return Status.OK_STATUS;
		}
		
	}
}
