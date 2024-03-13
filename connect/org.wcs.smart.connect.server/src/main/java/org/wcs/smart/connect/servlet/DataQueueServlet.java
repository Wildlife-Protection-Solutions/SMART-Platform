/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.cybertracker.json.importer.SmartMobileJsonFileProcessor;
import org.wcs.smart.connect.cybertracker.json.importer.SmartMobileJsonProcessorManager;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.DataQueueManager;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "dataqueue")
public class DataQueueServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		List<ConservationAreaInfo> cas = null;
			
		boolean canUpload = false;
		boolean canRun = false;
		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), DataQueueAction.VIEW_KEY)){
				//do not allow
				response.sendError(Status.UNAUTHORIZED.getStatusCode());
				return;
			}
			canUpload = SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), DataQueueAction.ADD_KEY);
			
			if (SmartMobileJsonProcessorManager.INSTANCE.canProcessOnConnect(s)) {
				canRun = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY);	
			}
			
			cas = HibernateManager.getConservationAreaInfosWithoutCCAA(s, false);
			for (Iterator<ConservationAreaInfo> iterator = cas.iterator(); iterator.hasNext();) {
				ConservationAreaInfo conservationAreaInfo = (ConservationAreaInfo) iterator.next();
				if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), DataQueueAction.ADD_KEY, conservationAreaInfo.getUuid())){
					iterator.remove();
				}
				
			}
		}finally{
			s.getTransaction().rollback();
		}
		
		Locale l = SmartUtils.getRequestLocale(request);
		List<Object[]> uploadTypes = new ArrayList<Object[]>();
		for ( String type : DataQueueManager.INSTANCE.getDataTypes()){
			switch(type){
			case "INCIDENT_XML": //$NON-NLS-1$
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.IncidentXmlName", l), type}); //$NON-NLS-1$
				break;
			case "MISSION_XML": //$NON-NLS-1$
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.MissionXmlName", l), type}); //$NON-NLS-1$
				break;
			case "PATROL_XML": //$NON-NLS-1$
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.PatrolXmlName",l), type}); //$NON-NLS-1$
				break;
			case SmartMobileJsonFileProcessor.CT_TYPE: 
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.CtJsonName1",l), type}); //$NON-NLS-1$
				break;
			case SmartMobileJsonFileProcessor.CT_ZIP_TYPE: 
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.CtZLibJsonName1",l), type}); //$NON-NLS-1$
				break;
			case "I2_RECORD_XML": //$NON-NLS-1$
				uploadTypes.add(new Object[]{Messages.getString("DataQueueServlet.I2RecordXmlName", l), type}); //$NON-NLS-1$
				break;
			default:
				uploadTypes.add(new Object[]{type, type});
				break;
			}
		}
		
		List<Object[]> statusTypes = new ArrayList<Object[]>();
		for (ServerDataQueueItem.Status item : new ServerDataQueueItem.Status[]{
				ServerDataQueueItem.Status.QUEUED,
				ServerDataQueueItem.Status.COMPLETE,
				ServerDataQueueItem.Status.ERROR}){
			statusTypes.add(new String[]{item.getGuiName(l), item.name()});	
		}
		
		
		request.setAttribute("cas", cas); //$NON-NLS-1$		
		request.setAttribute("uploadtypes", uploadTypes); //$NON-NLS-1$
		request.setAttribute("statusTypes", statusTypes); //$NON-NLS-1$
		request.setAttribute("canupload", canUpload); //$NON-NLS-1$
		request.setAttribute("canrun", canRun); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/dataqueue.jsp").forward(request, response); //$NON-NLS-1$
		
	}

}