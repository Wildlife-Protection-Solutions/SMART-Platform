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

public abstract class RelationshipSearchJob extends Job{

	private IntelEntityType srcType, targetType;
	protected List<IntelRelationshipType> rtypes;
	
	public RelationshipSearchJob(IntelEntityType srcType, IntelEntityType targetType) {
		super("relationship type search");
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
				Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()),
				Restrictions.or (
					Restrictions.and(Restrictions.eq("sourceEntityType", srcType), 
							Restrictions.eq("targetEntityType", targetType)),
					Restrictions.and(Restrictions.eq("sourceEntityType", targetType), 
							Restrictions.eq("targetEntityType", srcType)),
							Restrictions.isNull("sourceEntityType"),
							Restrictions.isNull("targetEntityType")
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
