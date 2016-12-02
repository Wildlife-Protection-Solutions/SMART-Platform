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
package org.wcs.smart.connect.api;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.query.QueryManager;


/**
 * SMART Connect Query Type REST API
 * @author Emily, Jeff
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + QueryTypeApi.PATH)
public class QueryTypeApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	
	public static final String PATH = "querytype"; //$NON-NLS-1$

	@Context private HttpServletRequest request;
	
	/**
	 * returns all valid Query types 
	 * URL: ../server/api/querytype/
	 * Call Type: GET
	 * 
	 * @return A JSON list of query type keys that are valid.
	 */
	@GET
    @Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
    public String[] getAllQueryTypes(){

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			return QueryManager.INSTANCE.getQueryTypes();			
		} catch (Exception e) {
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, "Error getting query types."); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}

}
