package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.engine.CsvExporter;
import org.wcs.smart.connect.query.engine.ShpExporter;
import org.wcs.smart.connect.query.engine.TiffRasterExporter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "report")
public class ReportServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
//		List<String[]> dateFilters = new ArrayList<String[]>();
//		for (IDateFieldFilter dateFilter : QueryManager.dateFields){
//			dateFilters.add(new String[]{dateFilter.getKey(), dateFilter.getGuiName(request.getLocale())});
//		}
//		String[][] exporters = new String[][]{
//				{CsvExporter.FORMAT_KEY, CsvExporter.getName(request.getLocale())},
//				{ShpExporter.FORMAT_KEY, ShpExporter.getName(request.getLocale())},
//				{TiffRasterExporter.FORMAT_KEY, TiffRasterExporter.getName(request.getLocale())}};
//		
//		//date filters with name
//		request.setAttribute("datefilters", dateFilters); //$NON-NLS-1$
//		//date filters associated with query types
//		request.setAttribute("qdatefilters", QueryManager.DATE_FILTERS); //$NON-NLS-1$
//		request.setAttribute("exporters", exporters); //$NON-NLS-1$
//		request.setAttribute("search", request.getParameter("search")); //$NON-NLS-1$ //$NON-NLS-2$
		request.getRequestDispatcher("/WEB-INF/report.jsp").forward(request, response); //$NON-NLS-1$
	}


}