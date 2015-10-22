
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

@WebServlet("/connect/alert")
public class AlertServlet extends HttpServlet{
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<SmartUser> users = null;
		List<ConservationAreaInfo> cas = null;
		List<AlertType> alertTypes = null;
		List<Alert> alerts = null;
		List<MapLayer> mapLayers = null;
		List<AlertFilterDefault> defaults = null;
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			users = HibernateManager.getUsers(session);
			cas = HibernateManager.getConservationAreaInfos(session);
			alertTypes = HibernateManager.getAlertTypes(session);
			mapLayers = HibernateManager.getMapLayers(session);
			defaults = HibernateManager.getAlertFilterDefaults(session);
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
