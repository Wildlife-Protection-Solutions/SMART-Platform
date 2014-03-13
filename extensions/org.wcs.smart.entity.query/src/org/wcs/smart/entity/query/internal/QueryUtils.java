package org.wcs.smart.entity.query.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;


public class QueryUtils {

	public static List<EntityType> getEntityTypes(final String keys) throws Exception{
		
		final List<EntityType> types = new ArrayList<EntityType>();
		Job j = new Job("load entity type"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String[] bits = keys.split(",");
				Session s = HibernateManager.openSession();
				try{
					for (int i = 0; i < bits.length; i++){
						Query q = s.createQuery("FROM EntityType WHERE keyId = :key and conservationArea = :ca");
						q.setParameter("key", bits[0]);
						q.setParameter("ca", SmartDB.getCurrentConservationArea());
						List<?> items = q.list();
						if (items.size() == 0){
							return new Status(IStatus.ERROR, EntityQueryPlugIn.PLUGIN_ID, MessageFormat.format("No Entity Type found for key {0}.", new Object[]{bits[0]}));
						}else if (items.size() > 1){
							return new Status(IStatus.ERROR, EntityQueryPlugIn.PLUGIN_ID, MessageFormat.format("Multiple entity types found for key {0}.  This is an error, please ensure all Entity Types have unique keys.", new Object[]{bits[0]}));
						}else {
							EntityType et = (EntityType) items.get(0);
							types.add (et);
							et.getName();
							for (EntityAttribute ea : et.getAttributes()){
								ea.getName();
								ea.getDmAttribute().getName();
							}
						}
					}
					return Status.OK_STATUS;
				}finally{
					s.close();
				}
			}
			
		};
		j.setSystem(false);
		
		try {
			j.join();
		} catch (InterruptedException e) {
			throw new Exception("Could not load entity type. " + e.getMessage());
		}
		
		if (j.getResult() != Status.OK_STATUS){
			throw new Exception("Could not load entity type. " + j.getResult().getMessage());
		}
		return types;
	}
}
