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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.hibernate.Session;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItemProxy;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.SecurityManager;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Data Processing Queue API functions
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + DataQueue.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class DataQueueEventService {

	private static final Logger logger = Logger.getLogger(DataQueueEventService.class.getName());
	
	//TODO: fix threading issues
	private Set<ServerDataQueueItem> queue = new HashSet<>();
	
	@Context private HttpServletRequest request;
	@Context private ServletContext context;

	public static void addUpdateToQueue(ServerDataQueueItem item) {
		try {
			for (DataQueueEventService s : services) {
				//TODO: why is remove required here
				s.queue.remove(item);
				s.queue.add(item);
			}
			for (DataQueueEventService s : services) {
				synchronized (s) {

					s.notify();	
				}
			}
		}catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
	}
	
	private static List<DataQueueEventService> services = new ArrayList<>();
	
	public DataQueueEventService() {
		services.add(this);
		
	}
	
	private OutboundSseEvent.Builder eventBuilder;
	
	@Context
	public void setSse(Sse sse) {
		this.eventBuilder = sse.newEventBuilder();
		
	}
	
	
	@GET
    @Path("/updates")
	@Produces({ MediaType.SERVER_SENT_EVENTS })
	public void getUpdates(@Context SseEventSink sseEventSink, @Context Sse sse){

		while (true) {
			synchronized (this) {
				try {
					wait();
				} catch (InterruptedException e) {
					services.remove(this);
					return;
				}
			}
			
			if (sseEventSink.isClosed()) {
				services.remove(this);
				return;
			}
			
			//sleep for 1 second so we don't send events more than one a second
			try{
				Thread.sleep(1000);
			}catch (Exception ex) {
				services.remove(this);
				return;
			}
			
			Set<ServerDataQueueItem> unique = new HashSet<>(queue);
			queue.clear();
			
			for (ServerDataQueueItem item : unique) {
				ConservationAreaInfo info = null;
				try(Session session = HibernateManager.getSession(context)){
					session.beginTransaction();
					try {
						if (!SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), 
								DataQueueAction.VIEW_KEY, item.getConservationArea())){
							continue;
						}
						
						info = session.get(ConservationAreaInfo.class, item.getConservationArea());
					}finally {
						session.getTransaction().rollback();
					}
				}
				
				OutboundSseEvent sseEvent = eventBuilder
						.name("dataqueue")
						.mediaType(MediaType.APPLICATION_JSON_TYPE)
						.data(ServerDataQueueItemProxy.class, new ServerDataQueueItemProxy(item, info))
						.build();
				sseEventSink.send(sseEvent);
			}

		}
		//sseEventSink.close();
	}
	
	
}
