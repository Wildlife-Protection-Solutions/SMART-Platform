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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "settings")
@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=1024*1024*5, maxRequestSize=1024*1024*5*5)
public class SettingsServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<StyleConfiguration> styles = null;
		List<ConservationAreaInfo> cas = null;
		List<AlertType> alertTypes = null;
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			
			styles = HibernateManager.getStyleConfigurations(session);
			cas = HibernateManager.getConservationAreaInfos(session);
			alertTypes = HibernateManager.getAlertTypes(session);
		}finally{
			session.getTransaction().rollback();
		}
		
		List<String[]>status = new ArrayList<String[]>();
		for (Alert.AlertStatusEnum x : Alert.AlertStatusEnum.values()) {
			status.add(new String[]{x.name(), x.getGuiName(request.getLocale())});
		}
		request.setAttribute("status", status); //$NON-NLS-1$
		
		request.setAttribute("styles", styles); //$NON-NLS-1$
		request.setAttribute("numstyles", styles.size()); //$NON-NLS-1$
		request.setAttribute("cas", cas); //$NON-NLS-1$
		request.setAttribute("alertTypes", alertTypes); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/settings.jsp").forward(request, response); //$NON-NLS-1$
		
	}
	
	 protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	     StyleConfiguration style = new StyleConfiguration();

	     style.setStyleId(request.getParameter("style_id")); //$NON-NLS-1$
	     Part filePart = request.getPart("bg_image");  //$NON-NLS-1$
	     byte[] bg_image_bytes = null;
	     try(InputStream fileContent = filePart.getInputStream()){
	    	 bg_image_bytes = IOUtils.toByteArray(fileContent);
	     }
	     style.setActive(true);
	     style.setBackgroundImage(bg_image_bytes);
	     style.setHeaderImage(bg_image_bytes);
	     style.setLoginImage(bg_image_bytes);
	     style.setUsersImage(bg_image_bytes);

	     Session session = HibernateManager.getSession(request.getServletContext());
	     session.beginTransaction();
		 try{
			 session.save(style);
			 session.getTransaction().commit();			 
		 }finally{
			 session.close();
		 }
		 
		 request.getRequestDispatcher("/WEB-INF/settings.jsp").forward(request, response); //$NON-NLS-1$
	 }
	 
}
