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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;

/**
 * Conservation area delete handler for queries and folders
 * @author egouge
 * @since 1.0.0
 */
public class QueryCaDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed before the conservation area is deleted
	 */
	public static final int EXECUTE_ORDER = 1;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		for (IQueryType queryType : QueryTypeManager.INSTANCE.getAllQueryTypes()){
			monitor.subTask(MessageFormat.format(Messages.QueryCaDeleteHandler_DeleteProgressMessage, new Object[]{queryType.getGuiName()}));
			moveCrossCaUserQueries(ca, session, queryType);
			deleteQueries(queryType, ca, session);
		}
		
		monitor.subTask(Messages.QueryCaDeleteHandler_Progress_DeletingQueryFolders);
		deleteQueryFolders(ca, session);		
	}

	
	private void deleteQueries(IQueryType queryType, ConservationArea ca, Session session) throws Exception{
		Query q = session.createQuery("delete from " + queryType.getHibernateClass().getSimpleName() + " where conservationArea = :ca"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	private void deleteQueryFolders(ConservationArea ca, Session session) throws Exception{
		moveCrossCaQueryFolders(ca, session);
		
		//first update parent folders to null; otherwise derby throws and error
		Query q = session.createQuery("update QueryFolder set parentFolder = null WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from QueryFolder where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	
	/*
	 * Checks and moves or deletes query folders that are cross-ca queries associated 
	 * with a user in the current conservation 
	 */
	private void moveCrossCaQueryFolders(ConservationArea ca, Session session) throws Exception{

		StringBuilder sql = new StringBuilder();
		sql.append("update QueryFolder qf set qf.employee = ("); //$NON-NLS-1$
		sql.append("select min(c.uuid) from ");//$NON-NLS-1$
		sql.append(" Employee b, Employee c" ); //$NON-NLS-1$
		sql.append(" WHERE b.smartUserId = c.smartUserId " ); //$NON-NLS-1$
		sql.append(" and b.conservationArea != c.conservationArea "); //$NON-NLS-1$
		sql.append(" and qf.employee.uuid = b.uuid "); //$NON-NLS-1$
		sql.append(" and qf.conservationArea.uuid = :ca1 "); //$NON-NLS-1$
		sql.append(" and b.conservationArea = :ca2)" ); //$NON-NLS-1$
		sql.append(	"WHERE qf.uuid in (select h.uuid from "); //$NON-NLS-1$
		sql.append(" QueryFolder h, Employee i "); //$NON-NLS-1$
		sql.append(" where h.employee.smartUserId = i.smartUserId "); //$NON-NLS-1$
		sql.append(" and h.employee.conservationArea != i.conservationArea "); //$NON-NLS-1$
		sql.append(" and h.conservationArea = :ca1 and "); //$NON-NLS-1$
		sql.append(" h.employee.conservationArea = :ca2)"); //$NON-NLS-1$
		
		Query q = session.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("ca2", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		//here any query folders still associated with a 
		//user from the current conservation area can be removed
		
		//first set parent folder to null; otherwise derby throws an error 
		q = session.createQuery("update QueryFolder set parentFolder = null WHERE uuid in (select b.uuid from QueryFolder b where b.conservationArea.uuid = :ca1 and b.employee.conservationArea = :ca2)");  //$NON-NLS-1$
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA);  //$NON-NLS-1$
		q.setParameter("ca2", ca);  //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from QueryFolder WHERE uuid in (select b.uuid from QueryFolder b WHERE b.conservationArea.uuid = :ca1 and b.employee.conservationArea = :ca2)");  //$NON-NLS-1$
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA);  //$NON-NLS-1$
		q.setParameter("ca2", ca);  //$NON-NLS-1$
		q.executeUpdate();
		
	}
	
	/*
	 * Checks and moves or deletes queries that are cross-ca queries associated 
	 * with a user in the current conservation 
	 */
	private void moveCrossCaUserQueries(ConservationArea ca, Session session, IQueryType queryType) throws Exception{

		//for all query folder owned by somebody in this ca but
		//associated with the cross ca Conservation Area we update
		//the owner to any other user with the same query id 
		StringBuilder innerSql = new StringBuilder();
		innerSql.append("select min(c.uuid) as newuuid from "); //$NON-NLS-1$
		innerSql.append(" Employee b, Employee c "); //$NON-NLS-1$
		innerSql.append(" WHERE b.smartUserId = c.smartUserId "); //$NON-NLS-1$
		innerSql.append(" and b.conservationArea != c.conservationArea "); //$NON-NLS-1$
		innerSql.append(" and qf.owner.uuid = b.uuid "); //$NON-NLS-1$
		innerSql.append(" and qf.conservationArea.uuid = :ca1 "); //$NON-NLS-1$
		innerSql.append(" and b.conservationArea = :ca2 ");		 //$NON-NLS-1$
		
		StringBuilder wheresql = new StringBuilder();
		wheresql.append("select h.uuid from "); //$NON-NLS-1$
		wheresql.append(queryType.getHibernateClass().getSimpleName());
		wheresql.append(" h, Employee i  "); //$NON-NLS-1$
		wheresql.append(" where i.smartUserId = h.owner.smartUserId and "); //$NON-NLS-1$
		wheresql.append(" h.owner.conservationArea != i.conservationArea "); //$NON-NLS-1$
		wheresql.append(" and h.conservationArea = :ca1 "); //$NON-NLS-1$
		wheresql.append(" and h.owner.conservationArea = :ca2"); //$NON-NLS-1$
		
		StringBuilder sql = new StringBuilder();
		sql.append("update "); //$NON-NLS-1$
		sql.append(queryType.getHibernateClass().getSimpleName());
		sql.append(" qf set qf.owner = ( "); //$NON-NLS-1$
		sql.append(innerSql);
		sql.append(") where qf.uuid in (" ); //$NON-NLS-1$
		sql.append(wheresql.toString());
		sql.append(")"); //$NON-NLS-1$
	
		Query q = session.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("ca2", ca); //$NON-NLS-1$
		q.executeUpdate();
	
		//here any query folders still associated with a 
		//user from the current conservation area can be removed
		sql = new StringBuilder();
		sql.append("delete from "); //$NON-NLS-1$
		sql.append( queryType.getHibernateClass().getSimpleName() );
		sql.append( " a WHERE a.uuid in ("); //$NON-NLS-1$
		sql.append(" select b.uuid from "); //$NON-NLS-1$
		sql.append( queryType.getHibernateClass().getSimpleName() );
		sql.append( " b WHERE a.conservationArea.uuid = :ca1 and "); //$NON-NLS-1$
		sql.append(" a.owner.conservationArea = :ca2)"); //$NON-NLS-1$
		
		q = session.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("ca2", ca); //$NON-NLS-1$
		q.executeUpdate();
		
	}

}
