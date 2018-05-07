package org.wcs.smart.connect.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.GlobalFireWatchApi;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.GlobalFireWatch;
import org.wcs.smart.util.UuidUtils;

@WebServlet(ConnectRESTApplication.NO_AUTH_PATH  + GlobalFireWatchApi.PATH + "/*")
public class GlobalFireWatchProcessorServlet extends HttpServlet {
	
	private final Logger logger = Logger.getLogger(GlobalFireWatchProcessorServlet.class.getName());

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
		
		String pathInfo = request.getPathInfo();
		if (pathInfo.startsWith("/")) {
			pathInfo = pathInfo.substring(1);
		}
		
		UUID gfwUuid = null;
		try {
			gfwUuid = UuidUtils.stringToUuid(pathInfo);
		}catch (Exception ex) {
			response.setStatus(Status.BAD_REQUEST.getStatusCode());
			return;
		}
		
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try {
			GlobalFireWatch gw = s.get(GlobalFireWatch.class, gfwUuid);
			if (gw == null) {
				response.setStatus(Status.NOT_FOUND.getStatusCode());
				return;
			}
			
			//log the request json so I can figure out how to parse it
			StringBuilder json = new StringBuilder();
			try(BufferedReader reader = request.getReader()){
				reader.lines().forEach(e->json.append(e));
			}
			logger.log(Level.SEVERE, json.toString());
			
		}finally {
			s.getTransaction().rollback();
		}
	}

}
