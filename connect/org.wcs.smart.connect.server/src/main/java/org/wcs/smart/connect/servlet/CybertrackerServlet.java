package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.security.CyberTrackerAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "cybertracker")
public class CybertrackerServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//get smart collect package api
		URL url = new URL(request.getRequestURL().toString());
		Path path = Paths.get(url.getPath());
		String sp = path.getParent().getParent().toString();
		sp = sp.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		URL t = new URL(url.getProtocol(), url.getHost(), url.getPort(), sp);
		String x = SmartCollectPackage.generateSmartMobileAppLink(t, null);
		
		request.setAttribute("smartmobilelink", x); //$NON-NLS-1$
		
		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY)){
				//do not allow
				response.sendError(Status.UNAUTHORIZED.getStatusCode());
				return;
			}
		}finally{
			s.getTransaction().rollback();
		}
		request.getRequestDispatcher("/WEB-INF/cybertracker.jsp").forward(request, response); //$NON-NLS-1$
	}
}
