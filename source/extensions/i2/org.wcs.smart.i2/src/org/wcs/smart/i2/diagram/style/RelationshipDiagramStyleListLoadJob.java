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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Job for loading all {@link RelationshipDiagramStyle}.
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public abstract class RelationshipDiagramStyleListLoadJob extends Job {
	
	public RelationshipDiagramStyleListLoadJob() {
		super(Messages.RelationshipDiagramStyleListLoadJob_Title);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final List<RelationshipDiagramStyle> styleList = new ArrayList<RelationshipDiagramStyle>();
		try(Session session = HibernateManager.openSession()) {
			styleList.addAll(RelationshipDiagramManager.INSTANCE.getStyles(session));
		}
		
		processData(styleList);

		return Status.OK_STATUS;
	}

	protected abstract void processData(List<RelationshipDiagramStyle> styles);
	
}
