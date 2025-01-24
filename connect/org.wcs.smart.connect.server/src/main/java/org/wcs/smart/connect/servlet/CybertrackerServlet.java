package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.CyberTrackerAction;
import org.wcs.smart.connect.security.SecurityManager;

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
		
		List<ConservationAreaInfo> authorizedCas = new ArrayList<>();

		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY)){
				//do not allow
				response.sendError(Status.UNAUTHORIZED.getStatusCode());
				return;
			}
			
			List<ConservationAreaInfo> cas = HibernateManager.getConservationAreaInfosWithoutCCAA(s, false);
			for(ConservationAreaInfo c: cas){
				if(SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.UPDATECA_KEY, c.getUuid())){
					authorizedCas.add(c);
				}
			}
			
		}finally{
			s.getTransaction().rollback();
		}
		
		request.setAttribute("cas", authorizedCas); //$NON-NLS-1$

		request.getRequestDispatcher("/WEB-INF/cybertracker.jsp").forward(request, response); //$NON-NLS-1$
	}
}
