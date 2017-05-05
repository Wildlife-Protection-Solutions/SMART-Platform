/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.entity.event;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityType;

/**
 * Advisor for deleting entity types.  Validates
 * that all entities associated with the entity can be
 * deleted.
 * 
 * @author Emily
 *
 */
public class DeleteEntityTypeAdvisor implements IDeleteAdvisor {

	public DeleteEntityTypeAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof EntityType)){
			return Messages.DeleteEntityTypeAdvisor_InvalidType;
		}
		EntityType toDelete = (EntityType)object;
		
		
		//validate that all entities can be deleted
		for (Entity e : toDelete.getEntities()){
			try{
				DeleteManager.canDelete(e, session);			
			}catch (Exception ex){
				return MessageFormat.format(Messages.DeleteEntityTypeAdvisor_CannotDelete, new Object[]{toDelete.getName(), e.getId()})
						+ "\n\n" + ex.getMessage(); //$NON-NLS-1$
			}	
		}
		
		return null;
	}

}
