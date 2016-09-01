package org.wcs.smart.i2;

import java.text.Collator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;

public enum IntelAttributeManager {
	INSTANCE;
	
	private IntelAttributeManager(){
		
	}
	
	public List<IntelAttribute> getAttributes(Session session, ConservationArea ca){
		List<IntelAttribute> types = session.createCriteria(IntelAttribute.class)
			.add(Restrictions.eq("conservationArea", ca))
			.list();
		
		types.sort((IntelAttribute a, IntelAttribute b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		
		return types;
	}
	
	public void canDelete(IntelAttribute type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception("Unknown error occurrs while deleteing entity type.");
		}
	}
	
	public void deleteAttribute(IntelAttribute type, Session session) throws Exception{
		canDelete(type, session);
		//TODO: test if it deletes the name associated with it
		session.delete(type);
	}
}
