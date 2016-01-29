package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.DataQueue;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItemProxy;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.security.SecurityManager;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "dataqueue")
public class DataQueueServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
		
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), DataQueueAction.VIEW_KEY)){
				//do not allow
				response.setStatus(Status.UNAUTHORIZED.getStatusCode());
				return;
			}
		}finally{
			s.getTransaction().rollback();
		}
		
		DataQueue dqapi = new DataQueue();
		dqapi.configure(request.getServletContext(), null, response, request);
		
		List<DataQueueItem> info = dqapi.getItems(null, null);
		
		request.setAttribute("items", info); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/dataqueue.jsp").forward(request, response); //$NON-NLS-1$
		
	}


}