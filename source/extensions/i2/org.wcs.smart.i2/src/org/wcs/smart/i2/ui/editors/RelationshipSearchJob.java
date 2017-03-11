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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;

/**
 * Searches for relationships that support the given entity types.
 * 
 * @author Emily
 *
 */
public abstract class RelationshipSearchJob extends Job{

	private IntelEntityType srcType, targetType;
	protected List<IntelRelationshipType> rtypes;
	
	public RelationshipSearchJob(IntelEntityType srcType, IntelEntityType targetType) {
		super("relationship type search"); //$NON-NLS-1$
		setSystem(true);
		this.srcType = srcType;
		this.targetType = targetType;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Session s = HibernateManager.openSession();
		rtypes = new ArrayList<IntelRelationshipType>();
		try{
			rtypes.addAll(s.createCriteria(IntelRelationshipType.class)
			.add(Restrictions.and(
				Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
				Restrictions.or (
					Restrictions.and(Restrictions.eq("sourceEntityType", srcType),  //$NON-NLS-1$
						Restrictions.eq("targetEntityType", targetType)), //$NON-NLS-1$
					Restrictions.and(Restrictions.eq("sourceEntityType", targetType),  //$NON-NLS-1$
						Restrictions.eq("targetEntityType", srcType)), //$NON-NLS-1$
					Restrictions.and(Restrictions.isNull("sourceEntityType"), //$NON-NLS-1$
						Restrictions.isNull("targetEntityType")), //$NON-NLS-1$
					Restrictions.and(Restrictions.isNull("sourceEntityType"), //$NON-NLS-1$
						Restrictions.or(
							Restrictions.eq("targetEntityType", srcType), //$NON-NLS-1$
							Restrictions.eq("targetEntityType", targetType) //$NON-NLS-1$
						)),
					Restrictions.and(Restrictions.isNull("targetEntityType"), //$NON-NLS-1$
						Restrictions.or(
							Restrictions.eq("sourceEntityType", srcType), //$NON-NLS-1$
							Restrictions.eq("sourceEntityType", targetType) //$NON-NLS-1$
						))		
			))).list());
			for (IntelRelationshipType i : rtypes){
				i.getSourceEntityType();
				i.getTargetEntityType();
				if (i.getRelationshipGroup() != null){
					i.getRelationshipGroup().getName();
				}
				i.getAttributes().size();
			}
		}finally{
			s.close();
		}
		afterLoad();
		
		return Status.OK_STATUS;
	} 
	
	protected abstract void afterLoad();
}
