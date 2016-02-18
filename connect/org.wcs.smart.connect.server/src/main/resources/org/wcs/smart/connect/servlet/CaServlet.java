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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.ConservationAreas;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.ConservationAreaProxy;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;

/**
 * Conservation area servlet.
 * 
 * @author Emily
 *
 */
@WebServlet(ConnectRESTApplication.SERVLET_PATH + "ca")
public class CaServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
     
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		ConservationAreas cas = new ConservationAreas();
		cas.configure(request.getServletContext(), null, response, request);
		
		List<ConservationAreaProxy> info = cas.getConservationAreas();
		request.setAttribute("cas", info); //$NON-NLS-1$
		
		boolean canAdd = false;
		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
			canAdd = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.ADDCA_KEY);
		}finally{
			s.getTransaction().rollback();
		}
		request.setAttribute("canadd", canAdd);
		
		
		request.getRequestDispatcher("/WEB-INF/ca.jsp").forward(request, response); //$NON-NLS-1$
		
	}


}