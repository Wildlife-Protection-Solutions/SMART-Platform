package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.QueryProxy;
@WebServlet(ConnectRESTApplication.SERVLET_PATH + "query")
public class QueryServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Session session = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		session.beginTransaction();
		try{
			List<QueryProxy> allQueries = QueryManager.INSTANCE.getQueries(session, request.getLocale());
			request.setAttribute("allqueries", allQueries);
		}finally{
			session.getTransaction().commit();
		}
		request.getRequestDispatcher("/WEB-INF/query.jsp").forward(request, response); //$NON-NLS-1$
	}


}