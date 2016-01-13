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
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.ConnectUser;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.SmartUser;

/**
 * Servlet to support resetting of web application.  This
 * supports get request to display a web page where user
 * can enter new password and post request to 
 * get a password reset link and reset the password (depending
 * on parameter supplied).
 * 
 * @author Emily
 *
 */
@WebServlet(ConnectRESTApplication.PATH_SEPERATOR + ResetPasswordServlet.PATH 
		+ ConnectRESTApplication.PATH_SEPERATOR + "*")
public class ResetPasswordServlet extends HttpServlet{
	
	/**
	 * Servlet path
	 */
	public static final String PATH = "reset";
	
	private static final long serialVersionUID = 1L;
	/*
	 * reset link length
	 */
	private static final int RESET_LINK_SIZE = 64;
	/*
	 * length of time reset link is valid for
	 */
	private static final int RESET_VALID_MIN = 15;
	
	private final Logger logger = Logger.getLogger(ResetPasswordServlet.class.getName());
	
	/**
	 * Displays a web page where users can reset their password.  Get
	 * requests should be for the format /reset/<resetToken>.  If
	 * the resetToken is not supplied or invalid an empty page will
	 * be displayed.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		boolean found = false;
		//validate reset token
		String resetlink = request.getPathInfo();
		if (resetlink != null && resetlink.length() > 1){
			resetlink = resetlink.substring(1);
			//ensure reset link is 64 alpha numeric characters
			if (Pattern.matches("[0-9a-z]{" + RESET_LINK_SIZE + "}", resetlink)){
				
				Session s = HibernateManager.getSession(request.getServletContext());
				s.beginTransaction();
				try{
					SmartUser su = (SmartUser) s.createCriteria(SmartUser.class)
						.add(Restrictions.eq("resetId", resetlink))
						.uniqueResult();
					if (su != null){
						found = true;
					}
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}finally{
					s.getTransaction().rollback();
				}
			}
		}
		
		//set resttoken; a value of null means not found
		if (found){
			request.setAttribute("resettoken", resetlink); //$NON-NLS-1$
		}else{
			request.setAttribute("resettoken", null); //$NON-NLS-1$
		}
		//forward request to jsp page
		request.getRequestDispatcher("/WEB-INF/resetpassword.jsp").forward(request, response); //$NON-NLS-1$
	}
	
	/**
	 * Post request will either set the resettoken and send an email if username is
	 * supplied OR if resettoken and new are supplied it will reset the
	 * password for the user which matches the resset token.
	 * 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String userName = request.getParameter("username");
		String resetId = request.getParameter("resettoken");
		if (userName != null && resetId == null){
			//create token and send email
			try{
				setPasswordResetToken(userName, request);
				response.setStatus(Status.OK.getStatusCode());
			}catch(SmartConnectException ex){
				response.setStatus(ex.getResponseCode());
			}
		}else if (userName == null && resetId != null){
			//reset password
			try{
				String newPassword = request.getParameter("new");
				resetUserPasswordToken(resetId, newPassword, request);
				response.setStatus(Status.OK.getStatusCode()); 
			}catch(SmartConnectException ex){
				response.setStatus(ex.getResponseCode());
				if (((SmartConnectException)ex).getMessage() != null){
					response.getWriter().write("{\"error\": \"" + ex.getMessage() + "\"}");
					response.getWriter().flush();
				}	
			}
		}else{
			response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
		}	
	}
	
	/**
	 * Creates a reset token and sends email
	 * @param username
	 * @param request
	 * @throws SmartConnectException
	 */
	private void setPasswordResetToken(String username,
			HttpServletRequest request) throws SmartConnectException {
		SmartUser su = null;
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try {
			su = (SmartUser) s.createCriteria(SmartUser.class)
					.add(Restrictions.eq("username", username)).uniqueResult();
			if (su == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND,
						"Username not found.");
			}
			su.setResetDatetime(new Date());

			su.setResetId(SmartUtils.randomString(RESET_LINK_SIZE));
			s.getTransaction().commit();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}

		String resetLink = request.getRequestURL().append(ConnectRESTApplication.PATH_SEPERATOR)
				.append(su.getResetId()).toString();
		
		// send email
		try {
			javax.naming.Context initCtx = new InitialContext();
			javax.naming.Context envCtx = (javax.naming.Context) initCtx
					.lookup("java:comp/env");
			javax.mail.Session session = (javax.mail.Session) envCtx
					.lookup("mail/Session");

			Message message = new MimeMessage(session);
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(su.getEmail());			
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject("SMART Connect");
			message.setContent(
					MessageFormat.format("Use the link below to reset your password.  This link can only be used once and is only valid for {0} minutes.", RESET_VALID_MIN)
					+ "\n\n" + resetLink, "text/plain");
			Transport.send(message);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error sending forgot password email."
					+ ex.getMessage(), ex);
			throw new SmartConnectException(
					Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Resets password and disable reset token.
	 */
	private void resetUserPasswordToken(String resetToken, String newPassword,
			HttpServletRequest request) {
		if (resetToken == null || resetToken.trim().length() != RESET_LINK_SIZE) {
			throw new SmartConnectException(Status.BAD_REQUEST);
		}
		SmartUser su = null;
		Date linkDateTime = null;
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try {
			su = (SmartUser) s.createCriteria(SmartUser.class)
					.add(Restrictions.eq("resetId", resetToken)).uniqueResult();
			if (su == null) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST);
			}
			linkDateTime = su.getResetDatetime();

			su.setResetDatetime(null);
			su.setResetId(null);
			s.getTransaction().commit();
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Status.BAD_REQUEST);
		}
		// valid password
		String passValidation = ConnectUser.validatePassword(newPassword,
				request.getLocale());
		if (passValidation != null) {
			throw new SmartConnectException(Status.BAD_REQUEST,
					"Invalid password. " + passValidation);

		}
		// validate reset link time
		if ((new Date()).getTime() - linkDateTime.getTime() > RESET_VALID_MIN * 60 * 1000) {
			logger.log(Level.WARNING, "Reset password link expired.");
			throw new SmartConnectException(Status.BAD_REQUEST);
		}

		s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try {
			su = (SmartUser) s.get(SmartUser.class, su.getUuid());

			su.setPassword(BcryptCredentialHandler.hashPassword(newPassword));
			s.getTransaction().commit();
		} catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Status.BAD_REQUEST);
		}
	}
}
