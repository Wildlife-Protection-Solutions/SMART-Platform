package org.wcs.smart.i2;

import java.text.Collator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.model.IntelEntityType;

public enum EntityTypeManager {
	INSTANCE;
	
	private EntityTypeManager(){
		
	}
	
	public List<IntelEntityType> getEntityTypes(Session session, ConservationArea ca){
		List<IntelEntityType> types = session.createCriteria(IntelEntityType.class)
			.add(Restrictions.eq("conservationArea", ca))
			.list();
		
		types.sort((IntelEntityType a, IntelEntityType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		
		return types;
	}
	
	public void canDelete(IntelEntityType type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception("Unknown error occurrs while deleteing entity type.");
		}
	}
	
	public void deleteEntityType(IntelEntityType type, Session session) throws Exception{
		canDelete(type, session);
		//TODO: test if it deletes the name associated with it
		session.delete(type);
	}
}
