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
package org.wcs.smart.connect.dataqueue.internal.server;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.wcs.smart.connect.dataqueue.model.DataQueueItem;

/**
 * JAX-RS REST client api for SMART Connect dataqueue requests 
 * 
 * @author Emily
 *
 */
public interface DataQueueApi {
	
	public static final String DATAQUEUE_PATH = "dataqueue"; //$NON-NLS-1$

	public enum ServerStatus{
		QUEUED,
		PROCESSING,
		COMPLETE,
		ERROR
	}
	
	@GET
    @Path("/" + DATAQUEUE_PATH + "/items")
	public List<DataQueueItem> getItems(@QueryParam("cafilter") String caFilter,
			@QueryParam("status") String status);

	@PUT
    @Path("/" + DATAQUEUE_PATH + "/items/{uuid}/{status}")
	public void updateStatus(@PathParam("uuid") String itemUuid, @PathParam("status") String newStatus);

}
