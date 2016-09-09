package org.wcs.smart.i2.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;

public abstract class EntitySearchJob extends Job{

	public EntitySearchJob() {
		super("Entity Search");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		List<IntelEntity> entities = null;
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			entities = s.createCriteria(IntelEntity.class).list();
			for (IntelEntity e : entities){
				e.getEntityType().getName();
				for (IntelEntityAttributeValue v : e.getAttributes()){
					v.getAttribute().getName();
					if (v.getAttributeListItem()!= null) v.getAttributeListItem().getName();
				}
			}
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			onError(ex);
			return Status.OK_STATUS;
		}finally{
			s.close();	
		}
		onLoaded(entities);
		return Status.OK_STATUS;
	}

	
	public abstract void onError(Exception ex);
	
	public abstract void onLoaded(List<IntelEntity> entities);
}
