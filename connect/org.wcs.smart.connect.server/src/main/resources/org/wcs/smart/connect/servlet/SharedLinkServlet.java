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
 * Shared Link servlet. Used to allowed shared links/token access to Queries and Report, uses a sharedLink instead of Basic Auth
 * 
 * @author Jeff
 *
 */
@WebServlet(ConnectRESTApplication.NO_AUTH_PATH + "sharedlink/*")
public class SharedLinkServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	/**
	 * 
	 * @param uuid - the uuid of the sharedlink / token 
	 * @param request  - optional. The relative url of the service request being made with your token eg &request="server/connect/query/api/559f91b4-acd7-4122-b374-33a776b3c42c?format=csv&date_filter=waypointdate" 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SharedLink link;
		UUID uuid; 
		try{
			uuid = UUID.fromString(request.getParameter("uuid")); //$NON-NLS-1$
		}catch(Exception e){
			response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.InvalidUuid", request.getLocale())); //$NON-NLS-1$
			return;
		}
		String path = null;
		String username = ""; //$NON-NLS-1$
		
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try{
			link = QueryManager.INSTANCE.findSharedLink(uuid, s);
			if (link == null){
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			//check if link is still valid
			java.util.Date today = new java.util.Date();
			Timestamp now = new Timestamp(today.getTime());
			if(link.getExpiresAt().before(now)){
				response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.LinkExpired", request.getLocale())); //$NON-NLS-1$
				return;
			}
			
			String userip = request.getRemoteAddr();
			if(link.getAllowedIp() == null || link.getAllowedIp().equals(userip)){
				//all good, IP is blank or matches
			}else{
				response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.InvalidIp", request.getLocale())+ " " + userip ); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			
			path =  link.getUrl();
			username = QueryManager.INSTANCE.findUser(link.getOwnerUuid(), s).getUsername();
		}finally{
			s.getTransaction().rollback();
		}
		//Fill out the username in a new wrapped request that will return this username just like the tomcat session-version would. 
		UserRoleRequestWrapper wrappedRequest = new UserRoleRequestWrapper(username, request);
		
		//Forward the request to the URL from the database.
		if(path != null){
			RequestDispatcher rd = request.getRequestDispatcher(path);
			rd.forward(wrappedRequest, response); //wrapped request actually works nicer here to, no code to look for j_username anymore.
		}else{
			response.sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.LinkNotFound", request.getLocale())); //$NON-NLS-1$
		}
	}

	/**
	 * People may need to use POST to access some type of requests. We treat them the same as Gets
	 * 
	 * @param uuid - the uuid of the sharedlink / token 
	 * @param request  - optional. The relative url of the service request being made with your token eg &request=%22%2Fapi%2Fconservationarea%2F%3FincludeSpatialBoundaries%3Dfalse%22 
	 */
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
