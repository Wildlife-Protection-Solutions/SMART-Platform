/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.diagram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfileEntityType;

/**
 * Job for loading {@link IntelEntityType}.
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public abstract class LoadEntityTypeJob extends Job {

	private boolean activeProfiles = true;
	
	public LoadEntityTypeJob(boolean activeProfiles) {
		super(Messages.LoadEntityTypeJob_Title);
		this.activeProfiles = activeProfiles;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IntelEntityType> types = new ArrayList<>();
		try(Session session = HibernateManager.openSession()) {
			types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea()));
			
			if (activeProfiles) {
				for (Iterator<IntelEntityType> iterator = types.iterator(); iterator.hasNext();) {
					IntelEntityType intelEntityType = (IntelEntityType) iterator.next();
					boolean ok = false;
					for (IntelProfileEntityType p : intelEntityType.getProfiles()) {
						if (ProfilesManager.INSTANCE.getActiveProfiles().contains(p.getProfile())) {
							ok = true;
							break;
						}
					}
					if (!ok) iterator.remove();
				}
			}
		}
		
		processData(types);

		return Status.OK_STATUS;
	}

	protected abstract void processData(List<IntelEntityType> types);
}
