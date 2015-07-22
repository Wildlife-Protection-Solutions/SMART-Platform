package org.wcs.smart.connect.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.SmartUser;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "myaccount")
public class AccountServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String username = request.getUserPrincipal().getName();
		
		SmartUser su = null;
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			su = HibernateManager.getUser(session, username);
		}finally{
			session.getTransaction().rollback();
		}
		if (su == null){
			//TODO: fail here; maybe redirect to login?
		}
		request.setAttribute("username", su.getUsername()); //$NON-NLS-1$
		request.setAttribute("email", su.getEmail()); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/myaccount.jsp").forward(request, response); //$NON-NLS-1$
	}
}
