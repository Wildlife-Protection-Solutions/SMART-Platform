package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.SmartUser;

@WebServlet("/connect/alert")
public class AlertServlet extends HttpServlet{
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
		request.setAttribute("firsttime", "true"); //$NON-NLS-1$
		
		String mobile = request.getParameter("mobile"); //$NON-NLS-1$
		String tab = request.getParameter("tab"); //$NON-NLS-1$
		if(tab == null){
			tab="1"; //$NON-NLS-1$
		}
		
		request.setAttribute("mobile", mobile); //$NON-NLS-1$
		request.setAttribute("tab", tab); //$NON-NLS-1$
		
		
		
		request.getRequestDispatcher("/WEB-INF/alert.jsp").forward(request, response); //$NON-NLS-1$

	}
}
