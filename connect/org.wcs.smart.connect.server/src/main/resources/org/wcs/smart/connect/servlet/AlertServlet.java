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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertFilterDefault;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;

/*
 * AlertServlet - Alerts/Map page in Connect provides information used to load the page: users, cas, maplayers etc.
 * Much more information is requested with ajax/resteasy API once the page is loaded. 
 */

@WebServlet("/connect/alert")
public class AlertServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<SmartUser> users = null;
		List<ConservationAreaInfo> cas = null;
		List<AlertType> alertTypes = null;
		List<MapLayer> mapLayers = null;
		List<AlertFilterDefault> defaults = null;
		boolean canUpdate = false;
		boolean canDelete = false;
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			users = HibernateManager.getUsers(session);
			cas = HibernateManager.getConservationAreaInfos(session);
			alertTypes = HibernateManager.getAlertTypes(session);
			mapLayers = HibernateManager.getMapLayers(session);
			defaults = HibernateManager.getAlertFilterDefaults(session);
			canUpdate = SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), AlertAction.UPDATE_ALL_KEY);
			canDelete = SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), AlertAction.DELETE_ALL_KEY);
		}finally{
			session.getTransaction().rollback();
		}
		
		request.setAttribute("users", users); //$NON-NLS-1$
		request.setAttribute("cas", cas); //$NON-NLS-1$
		request.setAttribute("alertTypes", alertTypes); //$NON-NLS-1$
		request.setAttribute("mapLayers", mapLayers); //$NON-NLS-1$
		request.setAttribute("startingZoom", defaults.get(0).getStartingZoomLevel()); //$NON-NLS-1$
		request.setAttribute("startingLong", defaults.get(0).getStartingLong()); //$NON-NLS-1$
		request.setAttribute("startingLat", defaults.get(0).getStartingLat()); //$NON-NLS-1$
		
		request.setAttribute("canupdate", canUpdate); //$NON-NLS-1$
		request.setAttribute("candelete", canDelete); //$NON-NLS-1$
		
		List<String>status = new ArrayList<String>();
		for (Alert.AlertStatusEnum x : Alert.AlertStatusEnum.values()) {
			status.add(x.getValue());
		}
		request.setAttribute("status", status);

		
		//allow using "...alert?tab=2#tab2" on the url to specify whether to start on a tab other than the default
		//allow using  "?mobile=true on the url to hide the menus
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
