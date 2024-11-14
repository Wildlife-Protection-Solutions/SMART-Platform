/* Copyright (C) 2022 Wildlife Conservation Society
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.CaPluginVersion;
import org.wcs.smart.connect.model.ConnectPluginVersion;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.IpAlias;
import org.wcs.smart.connect.model.WorkItemSummary;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.persistence.Tuple;

/**
 * API for listing, downloading and updating CyberTracker packages
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + SmartInfo.PATH)

@Produces({ MediaType.APPLICATION_JSON })
public class SmartInfo extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	
	public static final String PATH = "info"; //$NON-NLS-1$
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	@Context private HttpHeaders headers;
	
	
	/**
	 * Gets the server info
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
	@Operation(description="Lists version information for SMART Connect and associated Conservation Areas")
	public Response getInfo(){
		
		Session session = HibernateManager.getSession(context);
		session.beginTransaction();
		
		JSONObject info = new JSONObject();
		
		try{
			Tuple data = session.createNativeQuery("SELECT version, last_updated, filestore_version FROM connect.connect_version", Tuple.class).uniqueResult(); //$NON-NLS-1$
			info.put("db-version", data.get(0).toString()); //$NON-NLS-1$
			info.put("db-last-updated", data.get(1).toString()); //$NON-NLS-1$
			info.put("file-store-version", data.get(2).toString()); //$NON-NLS-1$
			
			List<ConservationAreaInfo> areas = QueryFactory.buildQuery(session, ConservationAreaInfo.class).list();
			JSONArray cainfo = new JSONArray();
			info.put("conservation-areas", cainfo); //$NON-NLS-1$
			for (ConservationAreaInfo caarea : areas ) {
				
				if (!SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY, caarea.getUuid())) continue;
				
				JSONObject ca = new JSONObject();
				ca.put("name", caarea.getLabel()); //$NON-NLS-1$
				ca.put("uuid", caarea.getUuid().toString()); //$NON-NLS-1$
				ca.put("version", caarea.getVersion().toString()); //$NON-NLS-1$
				ca.put("status", caarea.getStatus().name()); //$NON-NLS-1$
				ca.put("lock-key", caarea.getLockKey()); //$NON-NLS-1$
				cainfo.add(ca);
			}
			
			List<ConnectPluginVersion> plugins = QueryFactory.buildQuery(session, ConnectPluginVersion.class).list();
			JSONArray connectplugins = new JSONArray();
			info.put("connect-plugin-version", connectplugins); //$NON-NLS-1$
			for (ConnectPluginVersion item : plugins ) {
				JSONObject plugin = new JSONObject();
				plugin.put("plugin-id", item.getPluginId()); //$NON-NLS-1$
				plugin.put("version", item.getVersion()); //$NON-NLS-1$
				connectplugins.add(plugin);
			}
			
			List<CaPluginVersion> caversions = QueryFactory.buildQuery(session, CaPluginVersion.class).list();
			JSONArray capluginversions = new JSONArray();
			info.put("ca-plugin-version", capluginversions); //$NON-NLS-1$
			for (CaPluginVersion item : caversions ) {
				
				//only include if have access to view ca 
				if (!SecurityManager.INSTANCE.canAccess(session, request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY, item.getConservationAreaUuid())) continue;
				
				JSONObject plugin = new JSONObject();
				plugin.put("ca-uuid", item.getConservationAreaUuid().toString()); //$NON-NLS-1$
				plugin.put("plugin-id", item.getPluginId()); //$NON-NLS-1$
				plugin.put("version", item.getVersion()); //$NON-NLS-1$
				capluginversions.add(plugin);
			}			
			
		}finally{
			session.getTransaction().rollback();
		}
		
		//add build information
		String buildVersion = "unknown"; //$NON-NLS-1$
		String buildDate = "unknown"; //$NON-NLS-1$
		try(InputStream is = getClass().getResourceAsStream("/version.txt"); //$NON-NLS-1$
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
			String line = null;
			while((line = reader.readLine()) != null) {
				String[] bits = line.split("="); //$NON-NLS-1$
				if(bits[0].equalsIgnoreCase("version")) { //$NON-NLS-1$
					buildVersion = bits[1];
				}else if (bits[0].equalsIgnoreCase("build.date")) { //$NON-NLS-1$
					buildDate = bits[1];
				}
			}
		}catch (IOException ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error reading version file"); //$NON-NLS-1$
		}
		info.put("build-version", buildVersion); //$NON-NLS-1$
		info.put("build-date", buildDate); //$NON-NLS-1$
		
		return Response.ok(info.toJSONString()).build();	
	}

	
	@GET
    @Path("/sync")
	@Operation(description="Lists version user Conservation Area last sync dates")
	public List<WorkItemSummary> getSyncInfo(){

		//must be admin user
		Session session = HibernateManager.getSession(context);
		session.beginTransaction();
		try{
			List<WorkItemSummary> allitems = QueryFactory.buildQuery(session, WorkItemSummary.class).list();
			for (Iterator<WorkItemSummary> iterator = allitems.iterator(); iterator.hasNext();) {
				WorkItemSummary workItemSummary = (WorkItemSummary) iterator.next();
				if (!validateRead(workItemSummary.getConservationAreaInfo().getUuid(), session)) {
					iterator.remove();
				}	
			}
			
			for (WorkItemSummary i : allitems) {
				IpAlias a = session.get(IpAlias.class, i.getIp());
				if (a != null) i.setAlias(a.getAlias());
			}
			
			allitems.sort((a,b)->{
				if (a.getConservationAreaInfo().equals(b.getConservationAreaInfo())) {
					if (a.getUsername().equals(b.getUsername())) {
						return a.getIp().compareTo(b.getIp());
					}
					return a.getUsername().compareTo(b.getUsername());
				}
				return a.getConservationAreaInfo().getLabel().compareTo(b.getConservationAreaInfo().getLabel());
			});
			return allitems;
		}finally{
			session.getTransaction().rollback();
		}
	
	}
	
	@PUT
    @Path("/sync/{ip}")
	@Operation(description="Update alias associated with ip address")
	public Response updateAlias(@Parameter(description="the ip address to update")
		@PathParam("ip") String ip,
			@RequestBody(description ="New name") String newAlias){

		//TODO: do we want to do any validation on these?
		//ip address length; newalias length?
		
		
		//must be admin user
		Session session = HibernateManager.getSession(context);
		session.beginTransaction();
		try{
			IpAlias alias = session.get(IpAlias.class, ip);
			if (alias == null) {
				alias = new IpAlias();
				alias.setIp(ip);
				
				session.persist(alias);
			}
			alias.setAlias(newAlias);
			session.getTransaction().commit();
			
			return Response.ok().build();
			
		}catch (Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
	
	}
	
	
	/*
	 * Ensures the current user has read access.
	 */
	private boolean validateRead(UUID cauuid, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.VIEWCA_KEY,
				cauuid)){
			return false;			
		}
		return true;
	}
}
