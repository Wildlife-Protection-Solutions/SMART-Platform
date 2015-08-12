package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.i18n.Messages;

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
		try {
			request.setAttribute("logintarget", new URI(request.getContextPath() + "/j_security_check").toASCIIString()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (URISyntaxException e) {
			logger.info("Error redirecting login."); //$NON-NLS-1$
			throw new ServletException("Could not redirect login.", e); //$NON-NLS-1$
		}
		request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response); //$NON-NLS-1$
		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);		
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
