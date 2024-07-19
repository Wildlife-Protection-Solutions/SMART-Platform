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
package org.wcs.smart.cybertracker.patrol.ui;

import java.text.MessageFormat;
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
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Displays the smart mobile device ids associated with the patrol if applicable
 * @since 8.1.0
 */
public class PatrolEditorContribution implements IPatrolEditorContribution {

	
	private Patrol patrol;
	
	private ObjectDeviceLinkComposite composite;
	
	public PatrolEditorContribution() {
	}
	
	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent, boolean canEdit) {
		composite = new ObjectDeviceLinkComposite(parent, toolkit, Messages.PatrolEditorContribution_objectType, Messages.PatrolEditorContribution_PatrolLegField);
		return composite;
	}
	
	
	@Override
	public String getName() {
		return ObjectDeviceLinkComposite.getTitle();
	}

	@Override
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
		loadData.schedule();

	}
	
	private Job loadData = new Job(Messages.PatrolEditorContribution_loadingmobiledetails) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<String[]> legId2Device = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				session.createQuery("FROM CtPatrolLink l join l.patrolLeg pl WHERE pl.patrol = :patrol", CtPatrolLink.class)  //$NON-NLS-1$
					.setParameter("patrol", patrol) //$NON-NLS-1$
					.list()
					.forEach(link->legId2Device.add(new String[] {MessageFormat.format("{0} - {1}", link.getPatrolLeg().getId(), SmartLabelProvider.getShortLabel(link.getPatrolLeg().getLeader().getMember())) , link.getDeviceId()}));				 //$NON-NLS-1$
			}
			composite.setData(patrol.getConservationArea(), legId2Device);
			
			return Status.OK_STATUS;
		}
		
	};

}
