package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.ConservationAreas;
import org.wcs.smart.connect.model.ConservationAreaInfo;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "ca")
public class CaServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		ConservationAreas cas = new ConservationAreas();
		cas.configure(request.getServletContext(), null, response, request);
		
		List<ConservationAreaInfo> info = cas.getConservationAreas();
		
		request.setAttribute("cas", info); //$NON-NLS-1$
		
		request.getRequestDispatcher("/WEB-INF/ca.jsp").forward(request, response); //$NON-NLS-1$
		
	}


}