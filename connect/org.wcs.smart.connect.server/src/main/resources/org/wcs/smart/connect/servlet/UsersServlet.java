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
import org.wcs.smart.connect.model.SmartUser;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "users")
public class UsersServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<SmartUser> users = null;
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			users = HibernateManager.getUsers(session);
		}finally{
			session.getTransaction().rollback();
		}
		
		request.setAttribute("users", users); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/users.jsp").forward(request, response); //$NON-NLS-1$
		
	}
}
