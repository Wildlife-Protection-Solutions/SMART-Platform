/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.ObjectDeviceLinkComposite;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.IMissionEditorContribution;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Displays the smart mobile device ids associated with the patrol if applicable
 * @since 8.1.0
 */
public class MissionEditorContribution implements IMissionEditorContribution {

	
	private Mission mission;
	
	private ObjectDeviceLinkComposite composite;
	
	public MissionEditorContribution() {
	}
	
	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {
		composite = new ObjectDeviceLinkComposite(parent, toolkit, Messages.MissionEditorContribution_missionobject, "MissionId"); //$NON-NLS-1$
		return composite;
	}
	
	
	@Override
	public String getName() {
		return ObjectDeviceLinkComposite.getTitle();
	}

	@Override
	public void setMission(Mission mission) {
		this.mission = mission;
		loadData.schedule();

	}
	
	private Job loadData = new Job(Messages.MissionEditorContribution_loadingmobiledetails) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<String[]> legId2Device = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				
				session.createQuery("FROM CtMissionLink l  WHERE l.mission = :mission", CtMissionLink.class) //$NON-NLS-1$
					.setParameter("mission", mission) //$NON-NLS-1$
					.list()
					.forEach(link->legId2Device.add(new String[] {link.getMission().getId(), link.getDeviceId()}));
									
			}
			composite.setData(mission.getSurvey().getSurveyDesign().getConservationArea(), legId2Device);
			
			return Status.OK_STATUS;
		}
		
	};

}
