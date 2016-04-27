/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.CaPluginVersion;
import org.wcs.smart.connect.model.ConnectPluginVersion;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;

/**
 * Connect VersionInfo servlet. Provide current version information to users.
 * 
 * @author Jeff
 *
 */
@WebServlet(ConnectRESTApplication.SERVLET_PATH + "info")
public class VersionInfo extends HttpServlet{

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<CaPluginVersion> versions = null;
		List<ConservationAreaInfo> areas = null;
		List<ConnectPluginVersion> plugins = null;
		String connectVersion = ""; //$NON-NLS-1$
		String connectUpdated = ""; //$NON-NLS-1$
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		
		try{
			if (!SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			versions = (List<CaPluginVersion>)session
					.createCriteria(CaPluginVersion.class)
					.list();
			areas = (List<ConservationAreaInfo>)session
					.createCriteria(ConservationAreaInfo.class)
					.list();
			plugins = (List<ConnectPluginVersion>)session
					.createCriteria(ConnectPluginVersion.class)
					.list();
			
			Object[] data = (Object[]) session.createSQLQuery("SELECT version, last_updated FROM connect.connect_version").uniqueResult(); //$NON-NLS-1$
			connectVersion = data[0].toString();
			connectUpdated = data[1].toString();
			 
			
		}finally{
			session.getTransaction().rollback();
		}
		
		try {
			request.setAttribute("vars", EnvironmentVariables.INSTANCE.getAllEnvironmentVariables() ); //$NON-NLS-1$
		} catch (NamingException e) {
			e.printStackTrace();
		}
		request.setAttribute("versions", versions); //$NON-NLS-1$
		request.setAttribute("connectVersion", connectVersion); //$NON-NLS-1$
		request.setAttribute("connectUpdated", connectUpdated); //$NON-NLS-1$
		request.setAttribute("areas", areas); //$NON-NLS-1$
		request.setAttribute("plugins", plugins); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/info.jsp").forward(request, response); //$NON-NLS-1$
		
	}
}
