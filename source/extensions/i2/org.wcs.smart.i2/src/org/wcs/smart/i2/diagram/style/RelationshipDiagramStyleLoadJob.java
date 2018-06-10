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
package org.wcs.smart.i2.diagram.style;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Job for loading {@link RelationshipDiagramStyle}.
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public abstract class RelationshipDiagramStyleLoadJob extends Job {
	
	private UUID uuid;

	public RelationshipDiagramStyleLoadJob() {
		super("Loading relationship diagram style");
	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		RelationshipDiagramStyle style = null;
		UUID id = uuid;
		if (id != null) {
			try(Session session = HibernateManager.openSession()) {
				style = RelationshipDiagramManager.INSTANCE.getStyle(session, id);
			}
		}
		
		processData(style);

		return Status.OK_STATUS;
	}

	protected abstract void processData(RelationshipDiagramStyle style);
	
}
