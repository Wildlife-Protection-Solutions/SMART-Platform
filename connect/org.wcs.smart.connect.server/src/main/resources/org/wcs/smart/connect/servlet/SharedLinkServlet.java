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
import java.sql.Timestamp;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.query.QueryManager;

/**
 * Shared Link servlet. Used to allowed shared links access to Queries and Report, uses a sharedLink instead of Basic Auth
 * 
 * @author Jeff
 *
 */
@WebServlet(ConnectRESTApplication.NO_AUTH_PATH + "sharedlink/*")
public class SharedLinkServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		UUID uuid = UUID.fromString(request.getParameter("uuid"));
		String path = null;
		String username = "";
		
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try{
			SharedLink link = QueryManager.INSTANCE.findSharedLink(uuid, s);
			if (link == null){
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			//check if link is still valid
			java.util.Date today = new java.util.Date();
			Timestamp now = new Timestamp(today.getTime());
			if(link.getExpiresAt().before(now)){
				response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.LinkExpired", request.getLocale()));
				return;
			}
			path =  link.getUrl();
			username = QueryManager.INSTANCE.findUser(link.getOwnerUuid(), s).getUsername();
		}finally{
			s.getTransaction().rollback();
		}
		//Fill out the username to the link creator. This allows the Query Api to re-check if this user still has access to the query, or if it was revoked, which should revoke all links that user created. 
		request.setAttribute("j_username", username);
		
		if(path != null){
			RequestDispatcher rd = request.getRequestDispatcher(path);
			rd.forward(request, response);
		}else{
			response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.LinkNotFound", request.getLocale()));
		}
	}


}
