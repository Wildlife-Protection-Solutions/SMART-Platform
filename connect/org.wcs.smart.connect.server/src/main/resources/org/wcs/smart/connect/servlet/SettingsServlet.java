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
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.model.StyleConfiguration;
@WebServlet(ConnectRESTApplication.SERVLET_PATH + "settings")
@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=1024*1024*5, maxRequestSize=1024*1024*5*5)
public class SettingsServlet extends HttpServlet{

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		List<StyleConfiguration> styles = null;
		List<MapLayer> layers = null;
		List<ConservationAreaInfo> cas = null;
		List<AlertType> alertTypes = null;
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			styles = HibernateManager.getStyleConfigurations(session);
			cas = HibernateManager.getConservationAreaInfos(session);
			alertTypes = HibernateManager.getAlertTypes(session);
		}finally{
			session.getTransaction().rollback();
		}
		
		List<String>status = new ArrayList<String>();
		for (Alert.AlertStatusEnum x : Alert.AlertStatusEnum.values()) {
			status.add(x.getValue());
		}
		request.setAttribute("status", status);
		
		request.setAttribute("styles", styles); //$NON-NLS-1$
		request.setAttribute("numstyles", styles.size()); //$NON-NLS-1$
		request.setAttribute("cas", cas); //$NON-NLS-1$
		request.setAttribute("alertTypes", alertTypes); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/settings.jsp").forward(request, response); //$NON-NLS-1$
		
	}
	
	 protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	     StyleConfiguration style = new StyleConfiguration();

	     style.setStyleId(request.getParameter("style_id"));
	     Part filePart = request.getPart("bg_image"); 
	     String fileName = filePart.getSubmittedFileName();
	     InputStream fileContent = filePart.getInputStream();
	     byte[] bg_image_bytes = IOUtils.toByteArray(fileContent);
	     style.setActive(true);
	     style.setServerName("Server #1");
	     style.setBackgroundImage(bg_image_bytes);
	     style.setHeaderImage(bg_image_bytes);
	     style.setLoginImage(bg_image_bytes);
	     style.setUsersImage(bg_image_bytes);
	     style.setFooterText("Footer Text");

	     
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
	 
	 private static String getFileName(Part part) {
		    for (String cd : part.getHeader("content-disposition").split(";")) {
		        if (cd.trim().startsWith("filename")) {
		            String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
		            return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
		        }
		    }
		    return null;
	}
}
