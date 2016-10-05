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
package org.wcs.smart.i2;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;

/**
 * Entity manager
 * 
 * @author Emily
 *
 */
public enum EntityManager {
	
	INSTANCE;
	
	public void deleteEntity(IntelEntity entity, Session session){
		//TODO:
		session.delete(entity);
	}
	

	/**
	 * Finds all entity location that occur during the two dates provided the dFilter array.
	 * If not dates provided returns all records.
	 * 
	 * @param session
	 * @param dFilter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<IntelEntityLocation> getEntityLocations(Session session, UUID entityUuid, Date[] dFilter){
		List<IntelEntityLocation> alllocations = null;
		
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			Query q = session.createQuery("FROM IntelEntityLocation WHERE id.entity.uuid = :uuid and id.location.dateTime between :d1 and :d2");
			q.setParameter("uuid", entityUuid);
			q.setParameter("d1", dFilter[0]);
			q.setParameter("d2", dFilter[1]);
			alllocations = q.list();
		}else{
			alllocations = session.createCriteria(IntelEntityLocation.class)
				.add(Restrictions.eq("id.entity.uuid", entityUuid))
				.list();
		}
		return alllocations;
	}
}
