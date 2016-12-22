package org.wcs.smart.i2;

import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.IntelRecordQuery;

public enum QueryManager {

	INSTANCE;
	
	public IntelRecordQuery deleteQuery(UUID queryUuid){
		IntelRecordQuery removed = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			removed = (IntelRecordQuery) s.get(IntelRecordQuery.class, queryUuid);
			if (removed == null) throw new Exception("Query not found - could not delete query.");
			
			Query wsQuery = s.createQuery("DELETE FROM IntelWorkingSetQuery WHERE id.query = :query");
			wsQuery.setParameter("query", removed);
			wsQuery.executeUpdate();
			
			s.delete(removed);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error deleteing query: " + ex.getMessage(), ex);
			return null;
		}finally{
			s.close();
		}
		
		return removed;
	}
}
