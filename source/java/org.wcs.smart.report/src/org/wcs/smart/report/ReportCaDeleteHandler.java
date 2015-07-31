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
package org.wcs.smart.report;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

/**
 * Handler for deleting reports.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportCaDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed before the conservation area is deleted
	 */
	public static final int EXECUTE_ORDER = 2;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.ca.ICaDeleteListener#beforeDelete(org.wcs.smart.ca.ConservationArea, org.hibernate.Session)
	 */
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor)
			throws Exception {
		monitor.subTask(Messages.ReportCaDeleteHandler_Porgress_DeletingReports);
		deleteReports(ca, session);
		monitor.subTask(Messages.ReportCaDeleteHandler_Progress_DeletingFolders);
		deleteReportFolders(ca, session);
		//items in filestore will be deleted when filestore removed		
	}

	private void deleteReports(ConservationArea ca, Session session) throws Exception{
		moveCrossCaUserReports(ca, session);
		
		Query q = session.createQuery("delete from Report where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

	private void deleteReportFolders(ConservationArea ca, Session session) throws Exception{
		moveCrossCaFolders(ca, session);
		
		//first update parent folders to null; otherwise derby throws and error
		Query q = session.createQuery("update ReportFolder set parentFolder = null WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from ReportFolder where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	
	/*
	 * Checks and moves or deletes query folders that are cross-ca queries associated 
	 * with a user in the current conservation 
	 */
	private void moveCrossCaFolders(ConservationArea ca, Session session) throws Exception{

		StringBuilder sql = new StringBuilder();
		sql.append("update ReportFolder qf set qf.employee = ("); //$NON-NLS-1$
		sql.append("select min(c.uuid) from ");//$NON-NLS-1$
		sql.append(" Employee b, Employee c" ); //$NON-NLS-1$
		sql.append(" WHERE b.smartUserId = c.smartUserId " ); //$NON-NLS-1$
		sql.append(" and b.conservationArea != c.conservationArea "); //$NON-NLS-1$
		sql.append(" and qf.employee.uuid = b.uuid "); //$NON-NLS-1$
		sql.append(" and qf.conservationArea.uuid = :ca1 "); //$NON-NLS-1$
		sql.append(" and b.conservationArea = :ca2)" ); //$NON-NLS-1$
		sql.append(	"WHERE qf.uuid in (select h.uuid from "); //$NON-NLS-1$
		sql.append(" ReportFolder h, Employee i "); //$NON-NLS-1$
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
		q = session.createQuery("update ReportFolder set parentFolder = null WHERE uuid in (select b.uuid from ReportFolder b where b.conservationArea.uuid = :ca1 and b.employee.conservationArea = :ca2)");  //$NON-NLS-1$
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA);  //$NON-NLS-1$
		q.setParameter("ca2", ca);  //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("delete from ReportFolder WHERE uuid in (select b.uuid from ReportFolder b WHERE b.conservationArea.uuid = :ca1 and b.employee.conservationArea = :ca2)");  //$NON-NLS-1$
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA);  //$NON-NLS-1$
		q.setParameter("ca2", ca);  //$NON-NLS-1$
		q.executeUpdate();
		
	}
	
	/*
	 * Checks and moves or deletes queries that are cross-ca queries associated 
	 * with a user in the current conservation 
	 */
	private void moveCrossCaUserReports(ConservationArea ca, Session session) throws Exception{

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
		wheresql.append(" Report h, Employee i  "); //$NON-NLS-1$
		wheresql.append(" where i.smartUserId = h.owner.smartUserId and "); //$NON-NLS-1$
		wheresql.append(" h.owner.conservationArea != i.conservationArea "); //$NON-NLS-1$
		wheresql.append(" and h.conservationArea = :ca1 "); //$NON-NLS-1$
		wheresql.append(" and h.owner.conservationArea = :ca2"); //$NON-NLS-1$
		
		StringBuilder sql = new StringBuilder();
		sql.append("update Report "); //$NON-NLS-1$
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
		sql.append("FROM Report a"); //$NON-NLS-1$
		sql.append( " WHERE a.uuid in ("); //$NON-NLS-1$
		sql.append(" select b.uuid from Report"); //$NON-NLS-1$
		sql.append( " b WHERE a.conservationArea.uuid = :ca1 and "); //$NON-NLS-1$
		sql.append(" a.owner.conservationArea = :ca2)"); //$NON-NLS-1$
		
		q = session.createQuery(sql.toString());
		q.setParameter("ca1", ConservationArea.MULTIPLE_CA); //$NON-NLS-1$
		q.setParameter("ca2", ca); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Report> toDelete = q.list();
		for (Report r : toDelete){
			session.delete(r);
			try{
				String filename =  SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE) + File.separator + UuidUtils.uuidToString(ConservationArea.MULTIPLE_CA) + File.separator + ReportPlugIn.REPORT_DIR + File.separator + r.getFilename(); 
				(new File(filename)).delete();
			}catch (Exception ex){
				ReportPlugIn.log("Error deleting report file", ex); //$NON-NLS-1$
			}
		}
		
		
	}
}

