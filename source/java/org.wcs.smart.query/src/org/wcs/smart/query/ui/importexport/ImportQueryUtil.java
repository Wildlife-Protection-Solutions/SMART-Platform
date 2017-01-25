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
package org.wcs.smart.query.ui.importexport;

import java.io.File;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.user.UserLevelManager;
import org.wcs.smart.util.SharedUtils;

/**
 * Utilities for importing queries.
 * @author Emily
 *
 */
public class ImportQueryUtil {

	/**
	 * Searches for an employee to associate with a query.  If the provided
	 * conservation area is the same as the current conservation area then the
	 * current user is returned.  Otherwise it finds an employee with the same
	 * user name as the current user.  If that's not found it will return
	 * the first active admin user for the given conservation area.
	 * 
	 * @param ca the conservation area to search
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Employee findEmployee(ConservationArea ca) {
		if (ca.equals(SmartDB.getCurrentConservationArea())){
			return SmartDB.getCurrentEmployee();
		}
		
		//otherwise look for an employee with the same username 
		Session s = HibernateManager.openSession();
		try{
			Employee e = (Employee)s.createCriteria(Employee.class)
					.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
					.add(Restrictions.eq("smartUserId", SmartDB.getCurrentEmployee().getSmartUserId())) //$NON-NLS-1$
					.uniqueResult();
			if (e != null){
				return e;
			}
			
			//look for any active admin users
			List<Employee> others = s.createCriteria(Employee.class)
					.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
					.add(Restrictions.isNull("endEmploymentDate")) //$NON-NLS-1$
					.add(Restrictions.isNotNull("smartUserLevelKeys")) //$NON-NLS-1$
					.list();
			for (Employee o : others){
				if (UserLevelManager.INSTANCE.supportsUser(o, UserLevelManager.ADMIN)) return o;
			}
			
			//this should never happen
			throw new IllegalStateException("Not admin employee found for imported conservation area."); //$NON-NLS-1$
		}finally{
			s.close();
		}
	}
	
	/**
	 * Imports the query represented by the file into the 
	 * provided ConservationArea and the associated query folder.
	 * 
	 */
	public static List<Query> importQuery(File file, QueryFolder qf, ConservationArea ca, Shell shell) throws Exception{
		QueryImportEngine importer = new QueryImportEngine();
		List<Query> queries = importer.importQuery(file, ca);
		
		List<String> warnings = importer.getWarnings();
		if (warnings.size() > 0){
			StringBuilder sb = new StringBuilder();
			for (String str: warnings){
				sb.append(str);
				sb.append(SharedUtils.LINE_SEPARATOR);
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
		
			ConfirmInputDialog dialog = new ConfirmInputDialog(
					shell,
					Messages.ImportQueryWizard_Confirm_DialogTitle,
					Messages.ImportQueryWizard_Confirm_DialogMessage,
					sb.toString(), null);
			if (dialog.open() != ConfirmInputDialog.OK){
				//skip this query
				return null;
			}	
		}
	
		for (Query query: queries){
			if (!qf.isRootFolder()){
				query.setFolder(qf);
				query.setIsShared(qf.getEmployee() == null);
			}else if (qf.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY)){
				query.setIsShared(true);
			}
		
			//set the owner
			if (query.getIsShared() && ca.getIsCcaa()){
				//shared queries in the cross-ca analysis do not have a user
				query.setOwner(SmartDB.getSharedEmployee());
			}
		}
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			for (Query query : queries){
				//generate id
				query.setId(QueryHibernateManager.getInstance().generateQueryId(session));
				session.save(query);
			}
			session.flush();
			importer.beforeCommit();
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			session.close();
		}				
		
		return queries;
	}
}
