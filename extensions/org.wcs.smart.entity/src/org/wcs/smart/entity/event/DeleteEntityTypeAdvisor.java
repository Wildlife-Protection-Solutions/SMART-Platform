package org.wcs.smart.entity.event;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;

/**
 * Advisor for entity types.
 * 
 * @author Emily
 *
 */
public class DeleteEntityTypeAdvisor implements IDeleteAdvisor {

	public DeleteEntityTypeAdvisor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof EntityType)){
			return "Invalid object type.";
		}
		EntityType toDelete = (EntityType)object;
		
		//Determines if the associated attribute
		//can be deleted
		try{
			DeleteManager.canDelete(toDelete.getDmAttribute(), session);			
		}catch (Exception ex){
			return MessageFormat.format("The Entity Type {0} cannot be deleted, as the associated data model attribute {1} cannot be deleted.", new Object[]{toDelete.getName(), toDelete.getDmAttribute().getName()})
					+ "\n\n" + ex.getMessage();
		}
		
		//validate that all entities can be deleted
		for (Entity e : toDelete.getEntities()){
			try{
				DeleteManager.canDelete(e, session);			
			}catch (Exception ex){
				return MessageFormat.format("The Entity Type {0} cannot be deleted, as the associated entity {1} cannot be deleted.", new Object[]{toDelete.getName(), e.getId()})
						+ "\n\n" + ex.getMessage();
			}	
		}
		
		return null;
	}

}
