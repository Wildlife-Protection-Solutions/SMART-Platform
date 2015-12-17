/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


/**
 * JAX-RS REST client api for SMART Connect requests 
 * 
 * @author Emily
 *
 */
public interface ConnectClient {
	
	public static final String USER_PATH = "connectuser"; //$NON-NLS-1$
	public static final String CA_PATH = "conservationarea"; //$NON-NLS-1$
	public static final String UPLOAD_PATH = "uploader"; //$NON-NLS-1$
	
	public static final String DATA_PARAM_ALL = "all"; //$NON-NLS-1$
	public static final String DATA_PARAM_PACKAGE = "changelog"; //$NON-NLS-1$
	
	@GET
    @Path("query/{queryuuid}")
	public Response getQueryResults(@PathParam("queryuuid") String queryUuid, 
			@QueryParam("format") String format,
			@QueryParam("start_date") String start,
			@QueryParam("end_date") String end,
			@QueryParam("date_filter") String filter,
			@QueryParam("delimiter") String delimiter);
}
