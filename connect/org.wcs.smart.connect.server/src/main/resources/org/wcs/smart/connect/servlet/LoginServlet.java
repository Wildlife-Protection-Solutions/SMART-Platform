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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.api.ConnectUser;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.security.AdminAccountAction;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet(urlPatterns = {"/login", "/loginerror"})
public class LoginServlet extends HttpServlet {
	
	private final Logger logger = Logger.getLogger(LoginServlet.class.getName());
	
	private static final long serialVersionUID = 1L;
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getRequestURI().contains("loginerror")){ //$NON-NLS-1$
			request.setAttribute("loginerror",  //$NON-NLS-1$
					Messages.getString("LoginServlet.LoginFail", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$ 
		}
		
		Session s = HibernateManager.getSession(request.getServletContext());
		Long userCnt = 1l;
		try{
			s.beginTransaction();
			userCnt = (Long)s.createCriteria(SmartUser.class)
					.setProjection(Projections.rowCount())
					.uniqueResult();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}finally{
			s.getTransaction().rollback();
		}
		
		if (userCnt > 0){
			try {
				request.setAttribute("logintarget", new URI(request.getContextPath() + "/j_security_check").toASCIIString()); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (URISyntaxException e) {
				logger.info("Error redirecting login."); //$NON-NLS-1$
				throw new ServletException("Could not redirect login.", e); //$NON-NLS-1$
			}
			
			request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response); //$NON-NLS-1$
		}else{
			request.setAttribute("logintarget", request.getRequestURI()); //$NON-NLS-1$ //$NON-NLS-2$
			request.getRequestDispatcher("WEB-INF/setup.jsp").forward(request, response); //$NON-NLS-1$
		}
		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			String create = request.getParameter("createuser");
			if (create!= null && create.equals("firstuser")){
				String username = request.getParameter("username");
				String email = request.getParameter("email");
				String password1 = request.getParameter("password1");
				String password2 = request.getParameter("password2");
				
				if (!password1.equals(password2)){
					request.setAttribute("loginerror", "Passwords do not match." );
					return;
				}
				String validate = ConnectUser.validatePassword(password1, SmartUtils.getRequestLocale(request));
				if (validate != null){
					request.setAttribute("loginerror", validate);
					return;
				}
				validate = ConnectUser.validateUserName(username, SmartUtils.getRequestLocale(request));
				if (validate != null){
					request.setAttribute("loginerror", validate);
					return;
				}
				createInitialAdminUser(username, email, password1, request);
			}
		}finally{
			doGet(request, response);
		}
	}
	
	private synchronized void createInitialAdminUser(String username, String email, String password, HttpServletRequest request){
		Session s = HibernateManager.getSession(request.getServletContext());
		Long userCnt = 1l;
		try{
			s.beginTransaction();
			
			userCnt = (Long)s.createCriteria(SmartUser.class)
					.setProjection(Projections.rowCount())
					.uniqueResult();
			
			if (userCnt == 0){
				//create new user
				SmartUser su = new SmartUser();
				su.setEmail(email);
				su.setUsername(username);
				su.setPassword(BcryptCredentialHandler.hashPassword(password));
				s.save(su);
				
				SmartUserAction sa = new SmartUserAction();
				sa.setUsername(username);
				sa.setAction(AdminAccountAction.KEY);
				s.save(sa);
				
			}else{
				request.setAttribute("loginerror", "An administrator user already exists.  You cannot create another administrator user without logging in." );	
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			request.setAttribute("loginerror", "Could not create user. " + ex.getMessage());
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
	
	/**
	 * @see HttpServlet#doPut(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String user = request.getParameter("username"); //$NON-NLS-1$
		String pass = request.getParameter("password"); //$NON-NLS-1$
		
		try{
			//create a session
			request.getSession();
			request.login(user, pass);
			response.setStatus(HttpURLConnection.HTTP_OK);
		}catch(ServletException ex){
			response.setStatus(HttpURLConnection.HTTP_UNAUTHORIZED);
		}		
	}
}
