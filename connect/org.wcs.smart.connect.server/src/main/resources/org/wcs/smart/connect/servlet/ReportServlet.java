package org.wcs.smart.connect.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.report.ReportFormat;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "report")
public class ReportServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String[][] formats = new String[ReportFormat.values().length][2];
		int cnt = 0;
		for (ReportFormat rf : ReportFormat.values()){
			formats[cnt][0] = rf.getTypeKey();
			formats[cnt++][1] = rf.getGuiName(request.getLocale());
		}

		request.setAttribute("reportformats", formats); //$NON-NLS-1$
		request.getRequestDispatcher("/WEB-INF/report.jsp").forward(request, response); //$NON-NLS-1$
	}


}