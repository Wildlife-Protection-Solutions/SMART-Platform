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
package org.wcs.smart.query;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IEmployeeListener;
import org.wcs.smart.query.model.IQueryType;

/**
 * Employee delete listener for queries.  This listener
 * process the cross conservation area queries moving them
 * to a different user is available or deleting them all together
 * it no alternative user exists
 * 
 * @author Emily
 *
 */
public class QueryEmployeeListener implements IEmployeeListener {

	@Override
	public void beforeDelete(Employee e, Session s) {
		for (IQueryType qt : QueryTypeManager.INSTANCE.getAllQueryTypes()){
			checkQueryType(e, s, qt);
		}
		checkQueryFolder(e, s);
	}

	
	private void checkQueryType(Employee e, Session s, IQueryType queryType){
		//need to update any cross-ca queries associated with this user
		//to another user with the same userid
		StringBuilder innerSql = new StringBuilder();
		innerSql.append("select case when min(b.uuid) is not null then min(b.uuid) else :user end as newuuid from "); //$NON-NLS-1$
		innerSql.append(" Employee b "); //$NON-NLS-1$
		innerSql.append(" WHERE b.smartUserId = :userid AND "); //$NON-NLS-1$
		innerSql.append(" b.uuid != :user "); //$NON-NLS-1$
				
		StringBuilder sql = new StringBuilder();
		sql.append("update "); //$NON-NLS-1$
		sql.append(queryType.getHibernateClass().getSimpleName());
		sql.append(" qf set qf.owner = ( "); //$NON-NLS-1$
		sql.append(innerSql);
		sql.append(") where  qf.owner = :userid2 and qf.conservationArea.uuid = :ca " ); //$NON-NLS-1$
			
		Query q = s.createQuery(sql.toString());
		q.setParameter("userid", e.getSmartUserId()); //$NON-NLS-1$
		q.setParameter("user", e.getUuid()); //$NON-NLS-1$
		q.setParameter("userid2", e); //$NON-NLS-1$
		q.setParameter("ca", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.executeUpdate();	
		
		
		//at this point we cannot move it anywhere so we should just delete it
		sql = new StringBuilder();
		sql.append("Delete from "); //$NON-NLS-1$
		sql.append(queryType.getHibernateClass().getSimpleName());
		sql.append(" WHERE conservationArea.uuid = :ca1 AND owner = :owner"); //$NON-NLS-1$
		
		q = s.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("owner", e); //$NON-NLS-1$
		
		q.executeUpdate();
	}
	
	private void checkQueryFolder(Employee e, Session s){
		//need to update any cross-ca queries associated with this user
		//to another user with the same userid
		StringBuilder innerSql = new StringBuilder();
		innerSql.append("select case when min(b.uuid) is not null then min(b.uuid) else :user end as newuuid from "); //$NON-NLS-1$
		innerSql.append(" Employee b "); //$NON-NLS-1$
		innerSql.append(" WHERE b.smartUserId = :userid AND "); //$NON-NLS-1$
		innerSql.append(" b.uuid != :user "); //$NON-NLS-1$
				
		StringBuilder sql = new StringBuilder();
		sql.append("update QueryFolder "); //$NON-NLS-1$
		sql.append(" qf set qf.employee = ( "); //$NON-NLS-1$
		sql.append(innerSql);
		sql.append(") where qf.employee = :userid2 and qf.conservationArea.uuid = :ca " ); //$NON-NLS-1$
			
		Query q = s.createQuery(sql.toString());
		q.setParameter("userid", e.getSmartUserId()); //$NON-NLS-1$
		q.setParameter("user", e); //$NON-NLS-1$
		q.setParameter("userid2", e); //$NON-NLS-1$
		q.setParameter("ca", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.executeUpdate();	
		
		
		//at this point we cannot move it anywhere so we should just delete it
		
		//set parent folder to null so we don't have recursive issues when deleting
		sql = new StringBuilder();
		sql.append("Update from QueryFolder set parentFolder = null "); //$NON-NLS-1$
		sql.append(" WHERE conservationArea.uuid = :ca1 AND employee = :owner"); //$NON-NLS-1$
		q = s.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("owner", e); //$NON-NLS-1$
		q.executeUpdate();
		
		sql = new StringBuilder();
		sql.append("Delete from QueryFolder "); //$NON-NLS-1$
		sql.append(" WHERE conservationArea.uuid = :ca1 AND employee = :owner"); //$NON-NLS-1$
		q = s.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("owner", e); //$NON-NLS-1$
		
		q.executeUpdate();
	}
}
